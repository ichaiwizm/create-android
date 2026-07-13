package com.wizycode.create.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.wizycode.create.core.net.CreateApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Upload d'une image de référence (CONTRACTS §3.6, NATIVE_SPEC §2.2 / §3).
 *
 * Lit le contenu du [Uri] (picker), le pousse en `multipart/form-data` (champ `file`) vers
 * `POST /api/upload` (max 10 Mo) et renvoie l'URL kie temporaire (~3 jours).
 */
class UploadRepository(
    private val api: CreateApi,
    context: Context,
) {

    private val appContext = context.applicationContext

    /**
     * @param uri contenu local (content:// / file://) issu du picker.
     * @return URL temporaire kie renvoyée par le serveur.
     * @throws IOException si le contenu est illisible ou dépasse 10 Mo.
     */
    suspend fun upload(uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = appContext.contentResolver

        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Impossible de lire le fichier sélectionné.")

        if (bytes.size > MAX_UPLOAD_BYTES) {
            throw IOException("Fichier trop volumineux (max 10 Mo).")
        }

        val mime = resolver.getType(uri) ?: DEFAULT_MIME
        val fileName = resolveFileName(resolver, uri, mime)

        val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", fileName, body)

        api.upload(part).url
    }

    private fun resolveFileName(
        resolver: android.content.ContentResolver,
        uri: Uri,
        mime: String,
    ): String {
        val queried = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
                }
        }.getOrNull()

        if (!queried.isNullOrBlank()) return queried

        val extension = when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            mime.contains("gif") -> "gif"
            mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
            else -> "bin"
        }
        return "ref-${System.currentTimeMillis()}.$extension"
    }

    private companion object {
        const val MAX_UPLOAD_BYTES = 10 * 1024 * 1024 // 10 Mo
        const val DEFAULT_MIME = "application/octet-stream"
    }
}
