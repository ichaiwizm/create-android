package com.wizycode.create

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.wizycode.create.core.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.wizycode.create.core.push.NotificationHelper

/**
 * Point d'entrée de l'application (CONTRACTS §4.1).
 *
 * Responsabilités, toutes exécutées une seule fois au démarrage du process :
 * 1. **DI manuel** — construit l'[AppContainer] (singletons partagés) exposé via [container].
 * 2. **Coil** — s'installe comme `SingletonImageLoader.Factory` : tous les `AsyncImage` du projet
 *    réutilisent l'`ImageLoader` partagé issu du client OkHttp de l'app.
 * 3. **Canaux de notification** — crée le canal `generations` (importance HIGH) via
 *    [NotificationHelper.ensureChannel] pour les push de complétion (jalon M7).
 * 4. **WorkManager** — fournit sa configuration à la demande (`Configuration.Provider`) : évite
 *    l'initialiseur par défaut et aligne le niveau de log sur le type de build.
 * 5. **Session** — amorce l'auth (`bootstrap`) pour résoudre `AuthState.Unknown` → `LoggedIn/LoggedOut`
 *    dès le lancement (refresh proactif du token PocketBase).
 *
 * NB : `applicationId = "com.wizycode.create"` == `namespace` (source de `BuildConfig`/`R`).
 * Play / CI `PACKAGE_NAME` utilisent ce même identifiant.
 */
class CreateApp : Application(), SingletonImageLoader.Factory, Configuration.Provider {

    /** Graphe de dépendances du process. Valide dès la fin de [onCreate]. */
    lateinit var container: AppContainer
        private set

    /** Portée liée au cycle de vie du process pour les amorçages non liés à un écran. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()

        // 1. DI manuel — singletons partagés.
        container = AppContainer(this)

        // 3. Canal de notification "generations" (idempotent).
        NotificationHelper.ensureChannel(this)

        // 5. Amorçage de session : lecture du TokenStore + refresh proactif du token.
        applicationScope.launch {
            runCatching { container.session.bootstrap() }
        }
    }

    // 2. Coil — ImageLoader singleton partagé (client OkHttp de l'app, sans en-tête Bearer).
    override fun newImageLoader(context: PlatformContext): ImageLoader = container.imageLoader

    // 4. WorkManager — configuration à la demande.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
            .build()
}
