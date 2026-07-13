package com.wizycode.create.ui.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wizycode.create.core.auth.SessionManager
import com.wizycode.create.data.model.AuthState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel racine porté par [CreateRoot]. Sa seule responsabilité est de piloter l'auth-gate :
 * il expose l'[AuthState] observé par le `Scaffold` racine et déclenche le bootstrap de session
 * (lecture du token persistant + refresh proactif) une fois, au premier attachement.
 *
 * Il ne détient aucun état d'écran : les `ViewModel` par écran sont créés au niveau de leurs
 * destinations respectives. On garde ce VM volontairement minimal (CONTRACTS §4.2 / §4.3).
 */
class RootViewModel(
    private val session: SessionManager,
) : ViewModel() {

    /** État d'authentification global : `Unknown` → `LoggedIn` / `LoggedOut`. */
    val authState: StateFlow<AuthState> = session.authState

    init {
        // Bootstrap unique : lit le TokenStore puis tente un refresh proactif. Tant qu'il n'a pas
        // abouti, [authState] reste à `Unknown` et l'UI n'affiche ni login ni onglets.
        viewModelScope.launch { session.bootstrap() }
    }

    companion object {
        /**
         * Fabrique le [RootViewModel] à partir du [session] singleton résolu depuis l'`AppContainer`
         * (DI manuel, CONTRACTS §4.1). Utilisée par `viewModel(factory = …)` dans [CreateRoot].
         */
        fun factory(session: SessionManager): ViewModelProvider.Factory = viewModelFactory {
            initializer { RootViewModel(session) }
        }
    }
}
