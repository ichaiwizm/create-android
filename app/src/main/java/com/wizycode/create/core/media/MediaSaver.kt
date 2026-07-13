package com.wizycode.create.core.media

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Enregistrement d'un média (image / vidéo) dans la photothèque via MediaStore (CONTRACTS §3.8,
 * NATIVE_SPEC §2.4). Télécharge l'URL PB en forçant `?download=1`, puis écrit le fichier :
 * - **API 29+** : scoped storage (RELATIVE_PATH `Pictures/Create` ou `Movies/Create`, IS_PENDING).
 * - **API < 29** : fallback disque public + insertion MediaStore (nécessite `WRITE_EXTERNAL_STORAGE`).
 *
 * Les fonctions suspendent et s'exécutent sur [Dispatchers.IO]. Elles renvoient l'[Uri] MediaStore
 * du média enregistré, ou lèvent en cas d'échec réseau / d'écriture.
 */
object MediaSaver {

    private const val ALBUM = "Create"

    /** Télécharge et enregistre une image dans `Pictures/Create`. Renvoie l'Uri MediaStore. */
    suspend fun saveImage(context: Context, url: String): Uri =
        save(context, url, isVideo = false)

    /** Télécharge et enregistre une vidéo dans `Movies/Create`. Renvoie l'Uri MediaStore. */
    suspend fun saveVideo(context: Context, url: String): Uri =
        save(context, url, isVideo = true)

    private suspend fun save(context: Context, url: String, isVideo: Boolean): Uri =
        withContext(Dispatchers.IO) {
            val conn = openConnection(url)
            try {
                val mime = resolveMime(conn.contentType, url, isVideo)
                val name = fileName(mime, isVideo)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveScoped(context, conn, name, mime, isVideo)
                } else {
                    saveLegacy(context, conn, name, mime, isVideo)
                }
            } finally {
                conn.disconnect()
            }
        }

    /** Scoped storage (API 29+) : insertion MediaStore avec RELATIVE_PATH + IS_PENDING. */
    private fun saveScoped(
        context: Context,
        conn: HttpURLConnection,
        name: String,
        mime: String,
        isVideo: Boolean,
    ): Uri {
        val resolver = context.contentResolver
        val collection = if (isVideo) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val relativePath = buildString {
            append(if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES)
            append(File.separator)
            append(ALBUM)
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val item = resolver.insert(collection, values)
            ?: error("MediaSaver: insertion MediaStore échouée")

        try {
            resolver.openOutputStream(item)?.use { out ->
                conn.inputStream.use { it.copyTo(out) }
            } ?: error("MediaSaver: flux de sortie indisponible")

            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(item, done, null, null)
            return item
        } catch (t: Throwable) {
            runCatching { resolver.delete(item, null, null) }
            throw t
        }
    }

    /** Fallback API < 29 : écriture dans le dossier public puis enregistrement MediaStore. */
    private fun saveLegacy(
        context: Context,
        conn: HttpURLConnection,
        name: String,
        mime: String,
        isVideo: Boolean,
    ): Uri {
        @Suppress("DEPRECATION")
        val baseDir = Environment.getExternalStoragePublicDirectory(
            if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES,
        )
        val dir = File(baseDir, ALBUM).apply { mkdirs() }
        val file = File(dir, name)

        try {
            file.outputStream().use { out ->
                conn.inputStream.use { it.copyTo(out) }
            }
        } catch (t: Throwable) {
            file.delete()
            throw t
        }

        val resolver = context.contentResolver
        val collection = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            @Suppress("DEPRECATION")
            put(MediaStore.MediaColumns.DATA, file.absolutePath)
        }
        val uri = resolver.insert(collection, values) ?: Uri.fromFile(file)

        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mime), null)
        return uri
    }

    private fun openConnection(url: String): HttpURLConnection {
        val conn = (URL(withDownloadFlag(url)).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = true
        }
        conn.connect()
        if (conn.responseCode !in 200..299) {
            val code = conn.responseCode
            conn.disconnect()
            error("MediaSaver: téléchargement échoué (HTTP $code)")
        }
        return conn
    }

    /** Ajoute `?download=1` (ou `&download=1`) aux URLs fichiers PocketBase. */
    private fun withDownloadFlag(url: String): String = when {
        url.contains("download=1") -> url
        url.contains("?") -> "$url&download=1"
        else -> "$url?download=1"
    }

    private fun resolveMime(contentType: String?, url: String, isVideo: Boolean): String {
        val fromHeader = contentType?.substringBefore(';')?.trim()?.lowercase(Locale.US)
        if (!fromHeader.isNullOrEmpty() && fromHeader.contains('/') && fromHeader != "application/octet-stream") {
            return fromHeader
        }
        val ext = url.substringBefore('?').substringAfterLast('.', "").lowercase(Locale.US)
        return mimeForExt(ext, isVideo)
    }

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

    private fun fileName(mime: String, isVideo: Boolean): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        return "Create_$stamp.${extForMime(mime, isVideo)}"
    }
}
