package com.wizycode.create.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wizycode.create.core.auth.SessionManager
import com.wizycode.create.core.net.ApiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * État de l'écran de connexion (CONTRACTS §4.3).
 *
 * Contrat figé : `{identity, password, submitting, error?}`. L'UI est un pur reflet de ce state ;
 * les mutations passent par [LoginViewModel].
 */
data class LoginUiState(
    val identity: String = "",
    val password: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
) {
    /** Bouton actif seulement si les deux champs sont renseignés et aucune requête en cours. */
    val canSubmit: Boolean
        get() = identity.isNotBlank() && password.isNotBlank() && !submitting
}

/**
 * ViewModel de l'écran Login (CONTRACTS §4.3).
 *
 * ```
 * class LoginViewModel(session: SessionManager) : ViewModel {
 *     val state: StateFlow<LoginUiState>   // {identity, password, submitting, error?}
 *     fun login()
 * }
 * ```
 *
 * Délègue l'authentification à [SessionManager.login] (PocketBase `users/auth-with-password`). En cas
 * de succès, `SessionManager.authState` bascule sur `LoggedIn` et l'auth-gate racine (`CreateRoot`)
 * remplace seul le Login par les onglets — aucune navigation impérative ici. En cas d'échec, le
 * message est traduit en français ([ApiError.frenchMessage], ou message dédié pour un couple
 * identifiant / mot de passe invalide).
 */
class LoginViewModel(
    private val session: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    /** Met à jour l'identifiant saisi (efface l'erreur affichée). */
    fun onIdentityChange(value: String) {
        _state.update { it.copy(identity = value, error = null) }
    }

    /** Met à jour le mot de passe saisi (efface l'erreur affichée). */
    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, error = null) }
    }

    /** Lance l'authentification (idempotent tant qu'une requête est déjà en cours). */
    fun login() {
        val current = _state.value
        if (!current.canSubmit) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            try {
                session.login(current.identity.trim(), current.password)
                // Succès : l'auth-gate racine observe `authState` et quitte le Login.
                _state.update { it.copy(submitting = false) }
            } catch (t: Throwable) {
                _state.update { it.copy(submitting = false, error = messageFor(t)) }
            }
        }
    }

    /** Traduit l'échec réseau en message affichable (cas creds invalides isolé). */
    private fun messageFor(t: Throwable): String {
        if (t is HttpException && t.code() in 400..403) {
            return "Identifiant ou mot de passe incorrect."
        }
        return ApiError.from(t).frenchMessage
    }
}
