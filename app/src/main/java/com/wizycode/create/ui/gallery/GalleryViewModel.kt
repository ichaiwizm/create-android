package com.wizycode.create.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wizycode.create.core.util.Haptic
import com.wizycode.create.data.GenerationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel de l'écran Galerie (CONTRACTS §4.3).
 *
 * Ne détient aucun cache propre : il **projette** le [GenerationRepository] partagé (le même
 * qu'utilise l'écran Créer) dans un [GalleryUiState]. Le rafraîchissement manuel et le poll
 * foreground 4 s sont pilotés ici (CONTRACTS §3.7 : le repository n'a pas de boucle interne).
 *
 * Les transitions asynchrones `PENDING→DONE/FAILED` émettent au plus **un** [Haptic] par batch via
 * [GenerationRepository.hapticEvents] ; l'écran collecte [hapticEvents] et déclenche le retour.
 */
class GalleryViewModel(
    private val generations: GenerationRepository,
) : ViewModel() {

    private val _refreshing = MutableStateFlow(false)

    /** État observable de l'écran (items triés `-created`, nombre en cours, rafraîchissement). */
    val state: StateFlow<GalleryUiState> = combine(
        generations.generations,
        generations.pendingCount,
        _refreshing,
    ) { items, pending, refreshing ->
        GalleryUiState(items = items, pendingCount = pending, refreshing = refreshing)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GalleryUiState(
            items = generations.generations.value,
            pendingCount = generations.pendingCount.value,
            refreshing = false,
        ),
    )

    /** Haptiques issues des transitions découvertes par le poll (relayées depuis le repository). */
    val hapticEvents: SharedFlow<Haptic> = generations.hapticEvents

    private var pollJob: Job? = null

    init {
        // Premier chargement si le cache partagé est encore vide.
        if (generations.generations.value.isEmpty()) refresh()
    }

    /** Rafraîchit la liste complète (GET /api/generations). Silencieux en cas d'échec réseau. */
    fun refresh() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            try {
                generations.refresh()
            } catch (_: Throwable) {
                // L'erreur ne bloque pas l'écran : le cache existant reste affiché.
            } finally {
                _refreshing.value = false
            }
        }
    }

    /**
     * Démarre la boucle de poll foreground : tant qu'il reste des `PENDING`, appelle
     * [GenerationRepository.pollPending] toutes les 4 s (CONTRACTS §3.7). Idempotent.
     */
    fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                if (generations.pendingCount.value > 0) {
                    runCatching { generations.pollPending() }
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    /** Arrête la boucle de poll (appelé quand l'écran passe en arrière-plan). */
    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    /**
     * Annule une génération en cours (bouton ✕ des cartes `PENDING`, DESIGN §5.8).
     * L'haptique `ERROR` de ce geste est déclenchée par l'UI pour éviter un double retour.
     */
    fun cancel(id: String) {
        viewModelScope.launch { runCatching { generations.cancel(id) } }
    }

    override fun onCleared() {
        stopPolling()
        super.onCleared()
    }

    private companion object {
        const val POLL_INTERVAL_MS = 4_000L
    }
}
