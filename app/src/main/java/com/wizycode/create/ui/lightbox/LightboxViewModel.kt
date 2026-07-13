package com.wizycode.create.ui.lightbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.wizycode.create.core.net.ApiError
import com.wizycode.create.core.net.CreateApi
import com.wizycode.create.core.net.dto.ToolRequest
import com.wizycode.create.data.GenerationRepository
import com.wizycode.create.data.catalog.ModelCatalog
import com.wizycode.create.data.model.Generation
import com.wizycode.create.data.model.ToolSpec
import com.wizycode.create.data.model.firstMediaUrl
import com.wizycode.create.data.model.isVideo
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de la visionneuse plein écran (CONTRACTS §4.3 — signature figée).
 *
 * Ne détient aucune dépendance `Context` : il observe la génération dans le cache partagé du
 * [GenerationRepository] (même source que feed / galerie) et émet des [LightboxEffect] one-shot que
 * [LightboxScreen] exécute (presse-papier, MediaStore, partage, snackbar, fermeture).
 *
 * Actions figées : [copyPrompt] / [share] / [save] / [upscale] / [removeBg] / [delete].
 * Les outils 1-clic passent par `POST /api/generate {tool, toolImageUrl}` via
 * [GenerationRepository.runTool] ; la suppression par `DELETE /api/generations/{id}` avec confirmation
 * en **double-tap** (1er tap → « Confirmer ? » 2 s ; 2e tap → suppression).
 */
class LightboxViewModel(
    private val generationId: String,
    private val generations: GenerationRepository,
    private val api: CreateApi,
) : ViewModel() {

    /** Flags transitoires non dérivables du repository (actions locales à l'écran). */
    private data class Transient(
        val working: Boolean = false,
        val confirmingDelete: Boolean = false,
        val everLoaded: Boolean = false,
    )

    private val transient = MutableStateFlow(Transient())

    private val _effects = Channel<LightboxEffect>(Channel.BUFFERED)
    /** Effets one-shot collectés par l'écran. */
    val effects: Flow<LightboxEffect> = _effects.receiveAsFlow()

    private val upscaleSpec: ToolSpec = ModelCatalog.tools.first { it.key == "upscale" }
    private val removeBgSpec: ToolSpec = ModelCatalog.tools.first { it.key == "removeBg" }

    /** Job d'expiration de la confirmation de suppression (2 s). */
    private var confirmJob: Job? = null

    /**
     * État exposé : combine la génération observée dans le cache partagé avec les flags transitoires.
     * `notFound` bascule quand une génération auparavant présente disparaît (suppression concurrente).
     */
    val state: StateFlow<LightboxUiState> =
        combine(generations.generations, transient) { list, t ->
            val gen = list.firstOrNull { it.id == generationId }
            LightboxUiState(
                generation = gen,
                modelLabel = gen?.let { labelFor(it.model) } ?: "",
                working = t.working,
                confirmingDelete = t.confirmingDelete,
                loading = !t.everLoaded && gen == null,
                notFound = t.everLoaded && gen == null,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = LightboxUiState(),
        )

    init {
        // Marque `everLoaded` dès que la génération apparaît dans le cache (pour distinguer
        // « pas encore chargé » de « supprimée »).
        viewModelScope.launch {
            generations.generations.collect { list ->
                if (!transient.value.everLoaded && list.any { it.id == generationId }) {
                    transient.update { it.copy(everLoaded = true) }
                }
            }
        }
    }

    // --- Actions figées -------------------------------------------------------------------------

    /** Copie le prompt de la génération dans le presse-papier (feedback « Copié »). */
    fun copyPrompt() {
        val prompt = current()?.prompt?.takeIf { it.isNotBlank() } ?: return
        emit(LightboxEffect.CopyPrompt(prompt))
        emit(LightboxEffect.Snackbar("Copié"))
    }

    /** Partage le média via le sélecteur système. */
    fun share() {
        val gen = current() ?: return
        val url = gen.firstMediaUrl ?: return
        emit(LightboxEffect.Share(url, gen.isVideo))
    }

    /** Enregistre le média dans la photothèque (téléchargement forcé `?download=1`). */
    fun save() {
        val gen = current() ?: return
        val url = gen.firstMediaUrl ?: return
        emit(LightboxEffect.Save(url, gen.isVideo))
    }

    /** Lance l'outil Upscale (`topaz/image-upscale`) sur l'image courante. */
    fun upscale() = runTool(upscaleSpec)

    /** Lance l'outil Détourer (`recraft/remove-background`) sur l'image courante. */
    fun removeBg() = runTool(removeBgSpec)

    /**
     * Suppression en double-tap : le premier appel arme la confirmation (2 s), le second déclenche
     * réellement `DELETE /api/generations/{id}` puis referme l'écran.
     */
    fun delete() {
        val gen = current() ?: return
        if (!transient.value.confirmingDelete) {
            transient.update { it.copy(confirmingDelete = true) }
            confirmJob?.cancel()
            confirmJob = viewModelScope.launch {
                kotlinx.coroutines.delay(CONFIRM_WINDOW_MS)
                transient.update { it.copy(confirmingDelete = false) }
            }
            return
        }
        confirmJob?.cancel()
        transient.update { it.copy(confirmingDelete = false, working = true) }
        viewModelScope.launch {
            try {
                generations.delete(gen.id)
                emit(LightboxEffect.Dismiss)
            } catch (t: Throwable) {
                emit(LightboxEffect.Snackbar(ApiError.from(t).frenchMessage))
            } finally {
                transient.update { it.copy(working = false) }
            }
        }
    }

    // --- Interne --------------------------------------------------------------------------------

    private fun runTool(spec: ToolSpec) {
        val gen = current() ?: return
        val url = gen.firstMediaUrl ?: return
        if (transient.value.working) return
        transient.update { it.copy(working = true) }
        viewModelScope.launch {
            try {
                generations.runTool(ToolRequest(tool = spec.key, toolImageUrl = url))
                emit(LightboxEffect.Snackbar("${spec.label} lancé"))
                emit(LightboxEffect.Dismiss)
            } catch (t: Throwable) {
                emit(LightboxEffect.Snackbar(ApiError.from(t).frenchMessage))
            } finally {
                transient.update { it.copy(working = false) }
            }
        }
    }

    private fun current(): Generation? = state.value.generation

    private fun emit(effect: LightboxEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    /** Résout un slug de modèle (`veo3_fast`, `nano-banana-pro`, `topaz/image-upscale`…) en libellé. */
    private fun labelFor(model: String): String {
        (ModelCatalog.image + ModelCatalog.video).forEach { family ->
            family.variants?.firstOrNull { it.id == model }?.let { return "${family.name} ${it.label}" }
            if (model == family.key || model == family.textId || model == family.editId) return family.name
        }
        ModelCatalog.tools.firstOrNull { it.id == model || it.key == model }?.let { return it.label }
        return model
    }

    companion object {
        private const val CONFIRM_WINDOW_MS = 2_000L
        private const val STOP_TIMEOUT_MS = 5_000L

        /**
         * Fabrique la [ViewModelProvider.Factory] pour un [generationId] donné (le ViewModel dépend
         * d'un argument runtime absent de l'`AppContainer`). Câblée par la navigation :
         * `viewModel(factory = LightboxViewModel.provideFactory(id, container.generations, container.api))`.
         */
        fun provideFactory(
            generationId: String,
            generations: GenerationRepository,
            api: CreateApi,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                LightboxViewModel(generationId, generations, api) as T
        }
    }
}
