package com.wizycode.create.core.net

import com.squareup.moshi.Moshi
import retrofit2.HttpException
import java.io.IOException

/**
 * Erreurs réseau normalisées, exposées à la couche UI avec un message FR affichable.
 * CONTRACTS §3.4 — noms figés.
 */
sealed class ApiError : Exception() {

    /** 401 — token invalide / expiré (après échec du refresh). */
    data object Unauthorized : ApiError()

    /** 400 — requête refusée par le backend (message serveur récupéré si possible). */
    data class BadRequest(val serverMessage: String) : ApiError()

    /** 502 — upstream kie indisponible. */
    data class Upstream(val detail: String) : ApiError()

    /** Panne de transport (pas de réseau, timeout, DNS…). */
    data class Network(val detail: String) : ApiError()

    /** Réponse illisible / JSON inattendu. */
    data class Decoding(val detail: String) : ApiError()

    /** Message FR prêt à afficher (snackbar / état d'erreur). */
    val frenchMessage: String
        get() = when (this) {
            is Unauthorized -> "Session expirée. Reconnecte-toi."
            is BadRequest -> serverMessage.ifBlank { "Requête invalide." }
            is Upstream -> "Le service de génération est momentanément indisponible. Réessaie."
            is Network -> "Pas de connexion. Vérifie ton réseau."
            is Decoding -> "Réponse inattendue du serveur."
        }

    companion object {
        /**
         * Traduit n'importe quel [Throwable] issu d'un appel réseau en [ApiError].
         * À utiliser dans les repositories : `try { … } catch (t: Throwable) { throw ApiError.from(t) }`.
         */
        fun from(t: Throwable): ApiError = when (t) {
            is ApiError -> t
            is HttpException -> fromHttp(t.code(), t.response()?.errorBody()?.string())
            is com.squareup.moshi.JsonDataException -> Decoding(t.message ?: "JSON invalide")
            is IOException -> Network(t.message ?: "Erreur réseau")
            else -> Network(t.message ?: t.javaClass.simpleName)
        }

        /** Construit l'erreur à partir d'un code HTTP + corps d'erreur brut. */
        fun fromHttp(code: Int, rawBody: String?): ApiError {
            val serverMsg = parseServerMessage(rawBody)
            return when (code) {
                401, 403 -> Unauthorized
                400, 422 -> BadRequest(serverMsg ?: "Requête invalide.")
                502, 503, 504 -> Upstream(serverMsg ?: "Upstream indisponible (HTTP $code)")
                else -> Network(serverMsg ?: "HTTP $code")
            }
        }

        private val errorMoshi: Moshi by lazy { Moshi.Builder().build() }

        /** Extrait `message` d'un corps d'erreur PocketBase / Next.js (`{"message": "..."}`). */
        private fun parseServerMessage(rawBody: String?): String? {
            if (rawBody.isNullOrBlank()) return null
            return try {
                @Suppress("UNCHECKED_CAST")
                val map = errorMoshi.adapter(Map::class.java).fromJson(rawBody) as? Map<String, Any?>
                (map?.get("message") ?: map?.get("error"))?.toString()?.takeIf { it.isNotBlank() }
            } catch (_: Throwable) {
                null
            }
        }
    }
}
