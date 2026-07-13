package com.wizycode.create.core.media

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Partage natif d'un média via [FileProvider] + [Intent.ACTION_SEND] (CONTRACTS §3.8,
 * NATIVE_SPEC §2.4). Le média distant (URL fichier PocketBase) est d'abord rapatrié dans le cache
 * (`cacheDir/shared/`), exposé via FileProvider, puis proposé au sélecteur de partage système.
 *
 * L'autorité FileProvider est dérivée du package applicatif : `"${packageName}.fileprovider"`.
 * Le manifest doit déclarer ce provider avec un `file_paths.xml` couvrant `<cache-path>` (chemin
 * `shared/`). Aucune permission runtime n'est requise (URI content + grant temporaire de lecture).
 */
object FileShare {

    private const val SHARE_DIR = "shared"
    private const val CHOOSER_TITLE = "Partager"

    /**
     * Télécharge le média puis ouvre le sélecteur de partage. Le téléchargement s'effectue sur
     * [Dispatchers.IO] ; le lancement de l'intent revient sur le thread principal.
     *
     * @param url URL du média (fichier PocketBase).
     * @param isVideo hint pour le type MIME / l'extension quand l'URL n'est pas explicite.
     */
    suspend fun share(context: Context, url: String, isVideo: Boolean = false) {
        val file = withContext(Dispatchers.IO) { downloadToCache(context, url, isVideo) }
        val mime = mimeForFile(file, isVideo)
        withContext(Dispatchers.Main) { shareFile(context, file, mime) }
    }

    /** Partage un fichier déjà présent en local via le sélecteur système. */
    fun shareFile(context: Context, file: File, mimeType: String) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newRawUri(null, uri)
        }

        val chooser = Intent.createChooser(send, CHOOSER_TITLE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // Nécessaire si le contexte n'est pas une Activity (ex. appel depuis un service/callback).
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun downloadToCache(context: Context, url: String, isVideo: Boolean): File {
        val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
        val conn = openConnection(url)
        try {
            val ext = extForMime(mimeForContentType(conn.contentType, url, isVideo), isVideo)
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
            val file = File(dir, "Create_$stamp.$ext")
            try {
                file.outputStream().use { out ->
                    conn.inputStream.use { it.copyTo(out) }
                }
            } catch (t: Throwable) {
                file.delete()
                throw t
            }
            return file
        } finally {
            conn.disconnect()
        }
    }

    private fun openConnection(url: String): HttpURLConnection {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = true
        }
        conn.connect()
        if (conn.responseCode !in 200..299) {
            val code = conn.responseCode
            conn.disconnect()
            error("FileShare: téléchargement échoué (HTTP $code)")
        }
        return conn
    }

    private fun mimeForContentType(contentType: String?, url: String, isVideo: Boolean): String {
        val fromHeader = contentType?.substringBefore(';')?.trim()?.lowercase(Locale.US)
        if (!fromHeader.isNullOrEmpty() && fromHeader.contains('/') && fromHeader != "application/octet-stream") {
            return fromHeader
        }
        val ext = url.substringBefore('?').substringAfterLast('.', "").lowercase(Locale.US)
        return mimeForExt(ext, isVideo)
    }

    private fun mimeForFile(file: File, isVideo: Boolean): String =
        mimeForExt(file.extension.lowercase(Locale.US), isVideo)

    private fun mimeForExt(ext: String, isVideo: Boolean): String = when (ext) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "jpg", "jpeg" -> "image/jpeg"
        "mp4", "m4v" -> "video/mp4"
        "webm" -> "video/webm"
        "mov" -> "video/quicktime"
        else -> if (isVideo) "video/mp4" else "image/jpeg"
    }

    private fun extForMime(mime: String, isVideo: Boolean): String = when (mime) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        "image/jpeg" -> "jpg"
        "video/mp4" -> "mp4"
        "video/webm" -> "webm"
        "video/quicktime" -> "mov"
        else -> if (isVideo) "mp4" else "jpg"
    }
}
