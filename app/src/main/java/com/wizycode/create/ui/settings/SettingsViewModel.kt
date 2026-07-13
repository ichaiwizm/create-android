package com.wizycode.create.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wizycode.create.core.auth.SessionManager
import com.wizycode.create.data.prefs.SettingsStore
import com.wizycode.create.data.prefs.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * État de l'écran Réglages (CONTRACTS §4.3).
 *
 * Contrat figé : `{themeMode, dynamicColor}`. Reflète en continu les valeurs persistées par
 * [SettingsStore] (source de vérité) ; l'UI est un pur reflet de ce state.
 *
 * @param themeMode préférence de thème (Système / Clair / Sombre).
 * @param dynamicColor couleurs dynamiques Material You activées (n'a d'effet visible qu'en API 31+ ;
 *   l'accent iris reste la signature immuable, quel que soit ce réglage — DESIGN §2.4).
 */
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
)

/**
 * ViewModel de l'écran Réglages (CONTRACTS §4.3).
 *
 * ```
 * class SettingsViewModel(settings: SettingsStore, session: SessionManager) : ViewModel {
 *     val state: StateFlow<SettingsUiState>   // {themeMode, dynamicColor}
 *     fun setThemeMode(mode: ThemeMode)
 *     fun setDynamicColor(enabled: Boolean)
 *     fun logout()
 * }
 * ```
 *
 * MVVM unidirectionnel : [state] agrège les flux [SettingsStore.themeMode] /
 * [SettingsStore.dynamicColor] ; les mutations sont écrites de façon atomique dans le DataStore et
 * re-remontent naturellement par le flux (pas d'état local dupliqué). La déconnexion délègue à
 * [SessionManager.logout] : l'auth-gate racine (`CreateRoot`) observe `authState` et bascule seul
 * vers l'écran Login — aucune navigation impérative n'est requise ici.
 */
class SettingsViewModel(
    private val settings: SettingsStore,
    private val session: SessionManager,
) : ViewModel() {

    /** Reflet réactif des préférences persistées. Démarré paresseusement, conservé 5 s hors écran. */
    val state: StateFlow<SettingsUiState> =
        combine(settings.themeMode, settings.dynamicColor) { mode, dynamic ->
            SettingsUiState(themeMode = mode, dynamicColor = dynamic)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    /** Persiste le mode de thème choisi (Système / Clair / Sombre). */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    /** Active / désactive les couleurs dynamiques Material You (persisté ; effet réel API 31+). */
    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settings.setDynamicColor(enabled) }
    }

    /**
     * Déconnecte l'utilisateur : efface le token ([SessionManager.logout]) et bascule l'état global
     * sur `LoggedOut`, ce que l'auth-gate racine traduit par un retour à l'écran Login.
     */
    fun logout() {
        session.logout()
    }

    companion object {
        /**
         * Fabrique le [SettingsViewModel] à partir des singletons résolus depuis l'`AppContainer`
         * (DI manuel, CONTRACTS §4.1). Utilisée par `viewModel(factory = …)` à la destination Réglages.
         */
        fun factory(
            settings: SettingsStore,
            session: SessionManager,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(settings = settings, session = session) }
        }
    }
}
