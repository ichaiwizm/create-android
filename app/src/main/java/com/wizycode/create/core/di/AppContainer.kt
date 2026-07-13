package com.wizycode.create.core.di

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wizycode.create.core.auth.SessionManager
import com.wizycode.create.core.auth.TokenStore
import com.wizycode.create.core.media.AudioRecorder
import com.wizycode.create.core.net.ApiClient
import com.wizycode.create.core.net.ApiConfig
import com.wizycode.create.core.net.CreateApi
import com.wizycode.create.core.net.PbAuthApi
import com.wizycode.create.core.util.Haptics
import com.wizycode.create.core.util.HapticsImpl
import com.wizycode.create.data.CreditsRepository
import com.wizycode.create.data.GenerationRepository
import com.wizycode.create.data.UploadRepository
import com.wizycode.create.data.prefs.SettingsStore
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Conteneur d'injection de dépendances **manuel** (CONTRACTS §4.1 / conventions "DI manuel, pas de
 * Hilt"). Construit une seule fois dans [com.wizycode.create.CreateApp] et exposé via sa propriété
 * `container`. Détient les **singletons partagés** du process — repositories à cache mémoire, session,
 * client réseau — que la [com.wizycode.create.CreateActivityGraph] passe explicitement à chaque
 * `ViewModel`.
 *
 * Singletons exposés (figés par CONTRACTS §4.1) :
 * ```
 * val session: SessionManager ; val generations: GenerationRepository
 * val credits: CreditsRepository ; val uploads: UploadRepository
 * val settings: SettingsStore ; val api: CreateApi ; val haptics: Haptics
 * ```
 * [imageLoader] est exposé en plus (infrastructure) : `CreateApp` l'installe comme `ImageLoader`
 * singleton Coil (`SingletonImageLoader.Factory`). Il provient de [ApiClient] (client OkHttp partagé,
 * **sans** en-tête Bearer — les fichiers PocketBase sont publics par token de fichier).
 *
 * ### Ordre de construction & rupture du cycle DI
 * [ApiClient] a besoin de la [SessionManager] (pour son `TokenAuthenticator` qui rejoue les 401), et
 * la [SessionManager] a besoin d'une [PbAuthApi] (login / refresh). Comme [ApiClient] fabrique lui
 * aussi une [PbAuthApi], on obtiendrait un cycle `ApiClient → SessionManager → PbAuthApi(ApiClient)`.
 * On le brise en amorçant une [PbAuthApi] **autonome** ([bootstrapPbAuthApi]) — un petit Retrofit sur
 * [ApiConfig.PB_BASE] sans interceptor d'auth (login / refresh portent le Bearer explicitement) — puis
 * en construisant, dans l'ordre : `session` → `apiClient` → `api`/`imageLoader` → repositories.
 */
class AppContainer(app: Application) {

    private val appContext: Context = app.applicationContext

    init {
        // Coffre chiffré (token PB) prêt avant toute lecture par SessionManager. Idempotent.
        TokenStore.init(appContext)
    }

    /**
     * Client d'auth PocketBase autonome servant uniquement à amorcer la [SessionManager] et à briser
     * le cycle DI avec [ApiClient] (voir kdoc de classe). Aucun interceptor : `auth-with-password`
     * n'exige pas de Bearer, `auth-refresh` le porte via `@Header`.
     */
    private val bootstrapPbAuthApi: PbAuthApi = run {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(ApiConfig.PB_BASE)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PbAuthApi::class.java)
    }

    /** Session d'authentification partagée (StateFlow<AuthState> piloté par l'auth-gate racine). */
    val session: SessionManager = SessionManager(bootstrapPbAuthApi, TokenStore)

    /**
     * Client HTTP applicatif : Retrofit `CreateApi` (Bearer) + OkHttp + `ImageLoader` Coil.
     * Construit après [session] (dont dépend son `TokenAuthenticator`).
     */
    private val apiClient: ApiClient = ApiClient(session, TokenStore, appContext)

    /** API des routes utilisateur `/api/…` (auth Bearer PocketBase). */
    val api: CreateApi = apiClient.api

    /** `ImageLoader` Coil partagé (installé en singleton par `CreateApp`). */
    val imageLoader: ImageLoader = apiClient.imageLoader

    /** Source de vérité unique du feed & de la galerie (cache mémoire, tri -created). */
    val generations: GenerationRepository = GenerationRepository(api)

    /** Solde de crédits (refresh périodique + ON_RESUME). */
    val credits: CreditsRepository = CreditsRepository(api)

    /** Upload d'images de référence (`/api/upload`). */
    val uploads: UploadRepository = UploadRepository(api, appContext)

    /** Préférences persistées (DataStore) : dernier modèle/réglages, thème, dynamic color. */
    val settings: SettingsStore = SettingsStore(appContext)

    /** Retour haptique (résolu hors composition ; pontée à `LocalHapticFeedback` par `CreateTheme`). */
    val haptics: Haptics = HapticsImpl(appContext)

    /**
     * Enregistreur audio (dictée du prompt → `/api/transcribe`). Service `core/media` partagé, câblé au
     * `CreateViewModel` par la [com.wizycode.create.CreateActivityGraph] (`startRecording` /
     * `stopRecordingAndTranscribe`). Un seul enregistrement à la fois : un singleton de process suffit.
     */
    val recorder: AudioRecorder = AudioRecorder(appContext)
}
