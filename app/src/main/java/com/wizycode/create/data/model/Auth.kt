package com.wizycode.create.data.model

/**
 * Enregistrement d'authentification renvoyé par PocketBase (app mono-utilisateur).
 *
 * @param id id du record `users`.
 * @param email email de connexion.
 * @param name nom affiché, optionnel.
 */
data class AuthRecord(
    val id: String,
    val email: String,
    val name: String? = null,
)

/**
 * État de session observé par l'UI.
 *
 * - [Unknown] : avant lecture du `TokenStore` (état de démarrage).
 * - [LoggedOut] : aucune session valide.
 * - [LoggedIn] : session active.
 */
sealed interface AuthState {
    /** Avant toute lecture du token persistant. */
    data object Unknown : AuthState

    /** Aucune session valide. */
    data object LoggedOut : AuthState

    /** Session active. */
    data class LoggedIn(
        val userId: String,
        val email: String,
    ) : AuthState
}
