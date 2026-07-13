package com.wizycode.create.core.net

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wizycode.create.BuildConfig
import com.wizycode.create.core.auth.SessionManager
import com.wizycode.create.core.auth.TokenStore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Fabrique et détient les clients réseau partagés de l'app.
 *
 * Fournit :
 * - [api] : Retrofit [CreateApi] (base [ApiConfig.APP_BASE]) — client authentifié
 *   (`AuthInterceptor` + `TokenAuthenticator`).
 * - [pbAuthApi] : Retrofit [PbAuthApi] (base [ApiConfig.PB_BASE]) — **sans** auth interceptor.
 * - [imageLoader] : Coil [ImageLoader] partagé, **sans** en-tête Bearer (les fichiers PB sont
 *   servis via leur token de fichier public).
 *
 * OkHttp : timeouts connect 15 s / read 60 s, `HttpLoggingInterceptor` en debug uniquement.
 * Moshi : [KotlinJsonAdapterFactory] (parse tolérant) ; `options` sérialisé tel quel.
 *
 * CONTRACTS §3.4. Le [Context] applicatif est requis pour construire le Coil [ImageLoader]
 * (l'`AppContainer` le fournit) — le reste des dépendances suit la signature `(session, tokenStore)`.
 */
class ApiClient(
    private val session: SessionManager,
    private val tokenStore: TokenStore,
    context: Context,
) {
    private val appContext: Context = context.applicationContext

    /** Moshi partagé — Kotlin reflect en dernier pour rester le fallback. */
    val moshi: Moshi = defaultMoshi()

    private val loggingInterceptor: HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    /** Client "nu" (sans Bearer) : auth PocketBase + chargement d'images Coil. */
    private val plainClient: OkHttpClient = baseClientBuilder()
        .addInterceptor(loggingInterceptor)
        .build()

    /** PbAuthApi sur le client nu — utilisé aussi par le [TokenAuthenticator]. */
    val pbAuthApi: PbAuthApi = retrofit(ApiConfig.PB_BASE, plainClient)
        .create(PbAuthApi::class.java)

    /** Client authentifié : ajoute le Bearer et gère le refresh transparent sur 401. */
    private val authedClient: OkHttpClient = baseClientBuilder()
        .addInterceptor(AuthInterceptor(tokenStore))
        .authenticator(TokenAuthenticator(pbAuthApi, tokenStore, session))
        .addInterceptor(loggingInterceptor)
        .build()

    /** CreateApi sur le backend Next.js réutilisé. */
    val api: CreateApi = retrofit(ApiConfig.APP_BASE, authedClient)
        .create(CreateApi::class.java)

    /** ImageLoader Coil partagé — réseau via le client nu (aucun Bearer). */
    val imageLoader: ImageLoader = ImageLoader.Builder(appContext)
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = { plainClient }))
        }
        .build()

    private fun baseClientBuilder(): OkHttpClient.Builder =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)

    private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()

    companion object {
        /** Moshi standard de l'app (réutilisé par l'`AppContainer` si besoin). */
        fun defaultMoshi(): Moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        /**
         * Construit un [PbAuthApi] autonome (client nu, sans Bearer).
         * Permet à l'`AppContainer` d'instancier le [SessionManager] **avant** l'[ApiClient],
         * puisque `SessionManager(pbAuthApi, tokenStore)` en dépend.
         */
        fun createPbAuthApi(moshi: Moshi = defaultMoshi()): PbAuthApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .apply {
                    if (BuildConfig.DEBUG) {
                        addInterceptor(
                            HttpLoggingInterceptor().apply {
                                level = HttpLoggingInterceptor.Level.BODY
                            },
                        )
                    }
                }
                .build()
            return Retrofit.Builder()
                .baseUrl(ApiConfig.PB_BASE)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
                .build()
                .create(PbAuthApi::class.java)
        }
    }
}
