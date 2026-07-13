package com.wizycode.create.core.auth

import com.wizycode.create.core.net.PbAuthApi
import com.wizycode.create.core.net.dto.PbAuthRequest
import com.wizycode.create.data.model.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException
import java.io.IOException

/**
 * Gestion de la session d'authentification (CONTRACTS §3.5).
 *
 * ```
 * class SessionManager(pbAuthApi: PbAuthApi, tokenStore: TokenStore) {
 *     val authState: StateFlow<AuthState>          // Unknown → LoggedIn/LoggedOut
 *     suspend fun bootstrap()
 *     suspend fun login(identity: String, password: String)
 *     suspend fun refresh(): Boolean
 *     fun logout()
 *     fun currentToken(): String?
 * }
 * ```
 *
 * [authState] pilote l'auth-gate racine (`CreateRoot`) : tant qu'il vaut
 * [AuthState.Unknown], on n'affiche ni login ni onglets (lecture du TokenStore
 * en cours). [bootstrap] est appelé au démarrage foreground et effectue un
 * refresh proactif du token. [currentToken] est lu par `AuthInterceptor` pour
 * injecter l'en-tête `Authorization: Bearer <token>`.
 */
class SessionManager(
    private val pbAuthApi: PbAuthApi,
    private val tokenStore: TokenStore,
) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /**
     * Lit le TokenStore au démarrage puis rafraîchit le token de façon proactive.
     *
     * - Pas de token → [AuthState.LoggedOut].
     * - Token + record présents → session restaurée immédiatement (affichage
     *   optimiste), puis [refresh] valide/renouvelle le token en tâche de fond
     *   de cet appel suspendu.
     * - Un échec réseau conserve la session locale ; un échec d'auth (401/403)
     *   déconnecte via [logout].
     */
    suspend fun bootstrap() {
        val token = tokenStore.loadToken()
        if (token == null) {
            _authState.value = AuthState.LoggedOut
            return
        }
        // Restauration optimiste depuis le record persisté (évite un flash de login).
        val record = tokenStore.loadRecord()
        if (record != null) {
            _authState.value = AuthState.LoggedIn(userId = record.id, email = record.email)
        }
        // Refresh proactif : renouvelle le token PB (JWT ~14 j) au lancement.
        val refreshed = refresh()
        if (!refreshed && record == null && tokenStore.loadToken() == null) {
            // Aucune session restaurable et le token a été invalidé.
            _authState.value = AuthState.LoggedOut
        }
    }

    /**
     * Authentifie contre PocketBase (`users/auth-with-password`) et persiste le
     * token + record. Lève l'exception réseau/HTTP en cas d'échec (gérée par le
     * `LoginViewModel` pour afficher « Identifiant ou mot de passe incorrect »).
     */
    suspend fun login(identity: String, password: String) {
        val res = pbAuthApi.login(PbAuthRequest(identity = identity, password = password))
        tokenStore.saveToken(res.token)
        tokenStore.saveRecord(res.record)
        _authState.value = AuthState.LoggedIn(userId = res.record.id, email = res.record.email)
    }

    /**
     * Rafraîchit le token via `users/auth-refresh`.
     *
     * @return `true` si le token a été renouvelé, `false` sinon.
     *
     * Un échec d'authentification (401/403) déclenche [logout]. Un échec réseau
     * (IOException) renvoie `false` **sans** détruire la session locale — le
     * token existant reste utilisable hors-ligne / lors d'une coupure passagère.
     */
    suspend fun refresh(): Boolean {
        val token = tokenStore.loadToken() ?: return false
        return try {
            val res = pbAuthApi.refresh("Bearer $token")
            tokenStore.saveToken(res.token)
            tokenStore.saveRecord(res.record)
            _authState.value = AuthState.LoggedIn(userId = res.record.id, email = res.record.email)
            true
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                logout()
            }
            false
        } catch (e: IOException) {
            // Réseau indisponible : on garde la session courante.
            false
        }
    }

    /** Efface le TokenStore et bascule l'état sur [AuthState.LoggedOut]. */
    fun logout() {
        tokenStore.clear()
        _authState.value = AuthState.LoggedOut
    }

    /** Token Bearer courant (lu par `AuthInterceptor`), ou `null` si déconnecté. */
    fun currentToken(): String? = tokenStore.loadToken()
}
