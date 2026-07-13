package com.wizycode.create.data

import com.wizycode.create.core.net.CreateApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Source de vérité du solde de crédits kie (CONTRACTS §3.6).
 *
 * Le compteur est rafraîchi automatiquement toutes les ~45 s et au retour au premier plan
 * (`Lifecycle.Event.ON_RESUME`) — le pilotage temporel/lifecycle est assuré par la couche UI
 * (ViewModel racine) qui appelle [refresh] ; ce repository ne fait que l'appel réseau et publie
 * la valeur. `null` = solde encore inconnu (avant le premier fetch).
 */
class CreditsRepository(private val api: CreateApi) {

    private val _credits = MutableStateFlow<Int?>(null)
    val credits: StateFlow<Int?> = _credits.asStateFlow()

    /** GET /api/credits → publie le solde. Propage l'exception (mappée en [ApiError] par OkHttp). */
    suspend fun refresh() {
        _credits.value = api.credits().credits
    }
}
