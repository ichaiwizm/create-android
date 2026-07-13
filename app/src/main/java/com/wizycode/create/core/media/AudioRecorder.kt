package com.wizycode.create.core.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.io.File

/**
 * Enregistreur audio natif pour la dictée vocale (CONTRACTS §3.8, NATIVE_SPEC §2.5).
 *
 * MediaRecorder AAC dans un conteneur MPEG-4 (`.m4a`). Tap start / tap stop, auto-stop à 60 s via
 * [MediaRecorder.setMaxDuration]. Le résultat expose le [File], les octets bruts et le [mimeType]
 * (`audio/mp4`) à POSTer sur `/api/transcribe` (Content-Type = mime). Un enregistrement trop court
 * (< 1.2 ko, tap accidentel) est ignoré et renvoyé comme [RecordResult.TooShort].
 *
 * Nécessite la permission runtime [Manifest.permission.RECORD_AUDIO] : [start] lève
 * [SecurityException] si elle n'est pas accordée. Instance non thread-safe : à piloter depuis le
 * thread principal (le ViewModel), la finalisation restant rapide.
 */
class AudioRecorder(private val context: Context) {

    /** Résultat d'un enregistrement finalisé. */
    sealed interface RecordResult {
        /** Audio valide capturé. [mimeType] = `audio/mp4` pour `/api/transcribe`. */
        data class Success(
            val file: File,
            val bytes: ByteArray,
            val mimeType: String,
        ) : RecordResult {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Success) return false
                return file == other.file && mimeType == other.mimeType && bytes.contentEquals(other.bytes)
            }

            override fun hashCode(): Int {
                var result = file.hashCode()
                result = 31 * result + bytes.contentHashCode()
                result = 31 * result + mimeType.hashCode()
                return result
            }
        }

        /** Enregistrement trop court (< [MIN_SIZE_BYTES]) : tap accidentel, à ignorer. */
        data object TooShort : RecordResult
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    @Volatile
    private var recording = false

    /** `true` entre [start] et [stop]/[cancel]. */
    val isRecording: Boolean get() = recording

    /** Mime des fichiers produits, à passer tel quel en `Content-Type` de `/api/transcribe`. */
    val mimeType: String get() = MIME_TYPE

    /**
     * Callback (thread principal) déclenché quand l'auto-stop de 60 s est atteint. Le consommateur
     * (ViewModel) doit alors appeler [stop] pour finaliser et transcrire. L'enregistrement audio
     * s'est déjà arrêté côté MediaRecorder ; seul le fichier reste à clôturer.
     */
    var onMaxDurationReached: (() -> Unit)? = null

    /** `true` si la permission micro est accordée (à vérifier avant [start]). */
    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /**
     * Démarre l'enregistrement vers un fichier temporaire du cache.
     * @throws SecurityException si [Manifest.permission.RECORD_AUDIO] n'est pas accordée.
     * @throws IllegalStateException si un enregistrement est déjà en cours.
     */
    fun start() {
        check(!recording) { "AudioRecorder: enregistrement déjà en cours" }
        if (!hasPermission()) throw SecurityException("Permission RECORD_AUDIO non accordée")

        val file = File.createTempFile("create_rec_", ".m4a", context.cacheDir)
        val rec = newRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(AAC_BITRATE)
            setAudioSamplingRate(SAMPLE_RATE)
            setMaxDuration(MAX_DURATION_MS)
            setOutputFile(file.absolutePath)
            setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    mainHandler.post { onMaxDurationReached?.invoke() }
                }
            }
        }

        try {
            rec.prepare()
            rec.start()
        } catch (t: Throwable) {
            runCatching { rec.reset() }
            runCatching { rec.release() }
            file.delete()
            throw t
        }

        recorder = rec
        outputFile = file
        recording = true
    }

    /**
     * Arrête et finalise l'enregistrement.
     * @return [RecordResult.Success] si l'audio est exploitable, [RecordResult.TooShort] si le
     *   fichier fait moins de [MIN_SIZE_BYTES] (ou si MediaRecorder n'a rien capturé).
     * @throws IllegalStateException si aucun enregistrement n'est en cours.
     */
    fun stop(): RecordResult {
        val rec = recorder ?: error("AudioRecorder: aucun enregistrement en cours")
        val file = outputFile ?: error("AudioRecorder: aucun enregistrement en cours")

        recording = false
        recorder = null
        outputFile = null

        val captured = try {
            rec.stop()
            true
        } catch (_: RuntimeException) {
            // MediaRecorder.stop() lève si stop() suit start() trop vite (aucune donnée valide).
            false
        } finally {
            runCatching { rec.reset() }
            runCatching { rec.release() }
        }

        if (!captured || file.length() < MIN_SIZE_BYTES) {
            file.delete()
            return RecordResult.TooShort
        }

        val bytes = file.readBytes()
        return RecordResult.Success(file, bytes, MIME_TYPE)
    }

    /** Interrompt et supprime l'enregistrement en cours sans produire de résultat. No-op si inactif. */
    fun cancel() {
        val rec = recorder ?: return
        val file = outputFile

        recording = false
        recorder = null
        outputFile = null

        runCatching { rec.stop() }
        runCatching { rec.reset() }
        runCatching { rec.release() }
        file?.delete()
    }

    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

    private companion object {
        const val MIME_TYPE = "audio/mp4"
        const val MAX_DURATION_MS = 60_000
        const val MIN_SIZE_BYTES = 1_200L
        const val AAC_BITRATE = 128_000
        const val SAMPLE_RATE = 44_100
    }
}
