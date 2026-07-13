package com.wizycode.create.core.net

import com.wizycode.create.core.auth.SessionManager
import com.wizycode.create.core.auth.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Rejoue **une seule fois** une requête après un 401 en rafraîchissant le token PocketBase.
 *
 * - Sur 401 : appelle `PbAuthApi.refresh("Bearer <token courant>")`, persiste le nouveau
 *   token/record, puis rejoue la requête avec le nouvel en-tête.
 * - Si le refresh échoue (ou déjà retenté / aucun token) : purge la session
 *   ([SessionManager.logout] → état `LoggedOut`) et renonce (`null`).
 *
 * CONTRACTS §3.4.
 */
class TokenAuthenticator(
    private val pbAuthApi: PbAuthApi,
    private val tokenStore: TokenStore,
    private val session: SessionManager,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Une seule tentative de refresh par requête d'origine.
        if (responseCount(response) >= 2) {
            session.logout()
            return null
        }

        val currentToken = tokenStore.loadToken()
        if (currentToken.isNullOrBlank()) {
            session.logout()
            return null
        }

        val newToken: String? = synchronized(this) {
            // Un autre thread a peut-être déjà rafraîchi pendant qu'on attendait le verrou.
            val latest = tokenStore.loadToken()
            if (!latest.isNullOrBlank() && latest != currentToken) {
                latest
            } else {
                refreshToken(currentToken)
            }
        }

        if (newToken.isNullOrBlank()) {
            session.logout()
            return null
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }

    private fun refreshToken(currentToken: String): String? = try {
        val fresh = runBlocking { pbAuthApi.refresh("Bearer $currentToken") }
        tokenStore.saveToken(fresh.token)
        tokenStore.saveRecord(fresh.record)
        fresh.token
    } catch (_: Throwable) {
        null
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
