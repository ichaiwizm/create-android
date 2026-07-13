package com.wizycode.create.ui.common

/**
 * Événements one-shot émis par les `ViewModel` vers leur écran (CONTRACTS §4.3).
 *
 * Contrairement au `StateFlow<UiState>` (état durable, ré-émis à la re-souscription), un [UiEvent]
 * se consomme **une seule fois** : il transite par un `SharedFlow` / `Channel` collecté sous
 * `repeatOnLifecycle(STARTED)`. Sert aux snackbars, messages d'erreur et navigations impératives
 * (ouverture de la lightbox après une génération, par exemple).
 *
 * Noms figés (CONTRACTS §4.3) :
 * ```
 * sealed interface UiEvent {
 *     data class Snackbar(val message: String) : UiEvent
 *     data class Error(val message: String) : UiEvent
 *     data class Navigate(val route: String) : UiEvent
 * }
 * ```
 */
sealed interface UiEvent {
    /** Message informatif transitoire (ex. « Transcription ajoutée »). */
    data class Snackbar(val message: String) : UiEvent

    /** Message d'erreur affichable (déjà traduit en français par le `ViewModel`). */
    data class Error(val message: String) : UiEvent

    /** Demande de navigation impérative vers [route] (routes de `ui.nav.Routes`). */
    data class Navigate(val route: String) : UiEvent
}
