package com.wizycode.create

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.wizycode.create.core.di.AppContainer
import com.wizycode.create.ui.create.CreateViewModel
import com.wizycode.create.ui.gallery.GalleryViewModel
import com.wizycode.create.ui.lightbox.LightboxViewModel
import com.wizycode.create.ui.login.LoginViewModel
import com.wizycode.create.ui.settings.SettingsViewModel

/**
 * Câblage `ViewModel` ↔ singletons du graphe (CONTRACTS §4.1 / §4.3).
 *
 * `ViewModelProvider.Factory` lisant l'[AppContainer] pour instancier chaque `ViewModel` avec ses
 * dépendances exactes (signatures figées CONTRACTS §4.3). L'implémentation à base de
 * [CreationExtras] fournit un [androidx.lifecycle.SavedStateHandle] (`extras.createSavedStateHandle()`)
 * survivant à la **mort de process** :
 * `CreateViewModel` reçoit en plus l'`AudioRecorder` partagé du graphe (dictée du prompt) ;
 * `LightboxViewModel` lit dans le handle l'argument de navigation `generationId` (route
 * `lightbox/{generationId}`).
 *
 * S'utilise via [rememberAppViewModelFactory] : chaque écran appelle
 * `viewModel(factory = rememberAppViewModelFactory())`, le `ViewModelStoreOwner` étant le
 * `NavBackStackEntry` (portée par destination) — ce qui alimente aussi le `SavedStateHandle`.
 */
class CreateActivityGraph(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val handle = extras.createSavedStateHandle()
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) ->
                LoginViewModel(container.session)

            modelClass.isAssignableFrom(CreateViewModel::class.java) ->
                CreateViewModel(
                    container.generations,
                    container.credits,
                    container.uploads,
                    container.settings,
                    container.api,
                    container.recorder,
                )

            modelClass.isAssignableFrom(GalleryViewModel::class.java) ->
                GalleryViewModel(container.generations)

            modelClass.isAssignableFrom(LightboxViewModel::class.java) ->
                LightboxViewModel(
                    handle.get<String>(ARG_GENERATION_ID).orEmpty(),
                    container.generations,
                    container.api,
                )

            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(container.settings, container.session)

            else -> error("CreateActivityGraph: ViewModel non câblé — ${modelClass.name}")
        } as T
    }

    private companion object {
        /** Clé de l'argument de nav `lightbox/{generationId}` (voir `ui.nav.Routes.LIGHTBOX`). */
        const val ARG_GENERATION_ID = "generationId"
    }
}

/**
 * Fabrique un [CreateActivityGraph] mémoïsé à partir de l'[AppContainer] du process. À passer à
 * `viewModel(factory = …)` depuis un `NavBackStackEntry` pour bénéficier du `SavedStateHandle`.
 */
@Composable
fun rememberAppViewModelFactory(): ViewModelProvider.Factory {
    val container = (LocalContext.current.applicationContext as CreateApp).container
    return remember(container) { CreateActivityGraph(container) }
}
