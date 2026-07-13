package com.wizycode.create.core.net

import com.wizycode.create.core.net.dto.PbAuthRequest
import com.wizycode.create.core.net.dto.PbAuthResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Auth PocketBase directe (base [ApiConfig.PB_BASE]) — **sans** [AuthInterceptor].
 * Seules routes autorisées à parler à PocketBase (login / refresh de token).
 * CONTRACTS §3.2 — signatures figées.
 */
interface PbAuthApi {

    @POST("api/collections/users/auth-with-password")
    suspend fun login(@Body body: PbAuthRequest): PbAuthResponse

    /** `bearer` = "Bearer <token>" du token courant à rafraîchir. */
    @POST("api/collections/users/auth-refresh")
    suspend fun refresh(@Header("Authorization") bearer: String): PbAuthResponse
}
