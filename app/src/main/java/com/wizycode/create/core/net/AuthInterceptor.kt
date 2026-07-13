package com.wizycode.create.core.net

import com.wizycode.create.core.auth.TokenStore
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Ajoute `Authorization: Bearer <token>` à chaque requête vers [CreateApi] quand un
 * token est présent dans le [TokenStore]. Ne touche pas les requêtes qui portent déjà
 * l'en-tête (ex. refresh explicite). CONTRACTS §3.4.
 */
class AuthInterceptor(
    private val tokenStore: TokenStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.header("Authorization") != null) {
            return chain.proceed(original)
        }
        val token = tokenStore.loadToken()
        if (token.isNullOrBlank()) {
            return chain.proceed(original)
        }
        val authed = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(authed)
    }
}
