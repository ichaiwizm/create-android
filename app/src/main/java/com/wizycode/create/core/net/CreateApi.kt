package com.wizycode.create.core.net

import com.wizycode.create.core.net.dto.CreditsResponse
import com.wizycode.create.core.net.dto.FcmTokenRequest
import com.wizycode.create.core.net.dto.GenerateRequest
import com.wizycode.create.core.net.dto.GenerateResponse
import com.wizycode.create.core.net.dto.GenerationDto
import com.wizycode.create.core.net.dto.GenerationsResponse
import com.wizycode.create.core.net.dto.ToolRequest
import com.wizycode.create.core.net.dto.TranscribeResponse
import com.wizycode.create.core.net.dto.UploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Endpoints du backend Next.js réutilisé (base [ApiConfig.APP_BASE]).
 * Toutes les requêtes portent l'en-tête `Authorization: Bearer <token>`
 * (ajouté par [AuthInterceptor]). CONTRACTS §3.2 — signatures figées.
 */
interface CreateApi {

    @POST("generate")
    suspend fun generate(@Body body: GenerateRequest): GenerateResponse

    @POST("generate")
    suspend fun runTool(@Body body: ToolRequest): GenerateResponse

    @GET("generations")
    suspend fun list(): GenerationsResponse

    @GET("generations/{id}")
    suspend fun poll(@Path("id") id: String): GenerationDto

    @DELETE("generations/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>

    @POST("generations/{id}/cancel")
    suspend fun cancel(@Path("id") id: String): Response<Unit>

    @Multipart
    @POST("upload")
    suspend fun upload(@Part file: MultipartBody.Part): UploadResponse

    @GET("credits")
    suspend fun credits(): CreditsResponse

    /** Audio brut : le `Content-Type` porté par le [RequestBody] définit le mime. */
    @POST("transcribe")
    suspend fun transcribe(@Body audio: RequestBody): TranscribeResponse

    @POST("push/register-fcm")
    suspend fun registerFcm(@Body body: FcmTokenRequest): Response<Unit>
}
