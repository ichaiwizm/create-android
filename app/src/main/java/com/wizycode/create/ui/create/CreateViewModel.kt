package com.wizycode.create.ui.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wizycode.create.core.media.AudioRecorder
import com.wizycode.create.core.net.ApiError
import com.wizycode.create.core.net.CreateApi
import com.wizycode.create.core.net.dto.GenerateRequest
import com.wizycode.create.data.CreditsRepository
import com.wizycode.create.data.GenerationRepository
import com.wizycode.create.data.UploadRepository
import com.wizycode.create.data.catalog.CatalogLogic
import com.wizycode.create.data.catalog.ModelCatalog
import com.wizycode.create.data.model.Generation
import com.wizycode.create.data.model.ModelFamily
import com.wizycode.create.data.model.ModelKind
import com.wizycode.create.data.model.ModelVariant
import com.wizycode.create.data.prefs.SettingsStore
import com.wizycode.create.ui.common.UiEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.coroutineContext

/**
 * ViewModel de l'écran cœur « Créer » (CONTRACTS §4.3).
 *
 * MVVM unidirectionnel : expose l'état du composer ([composer]), le feed inversé dérivé du
 * [GenerationRepository] ([feed]), l'état d'enregistrement micro ([recording]) et un flux
 * d'événements one-shot ([events]). Toute mutation passe par les fonctions publiques figées.
 *
 * `send()` = [CatalogLogic.resolveModelId] + [CatalogLogic.buildOptions] → insertion optimiste via le
 * repo puis relance du polling ; le dernier modèle est persisté dans [SettingsStore].
 *
 * Le [recorder] (service média `core/media`) est câblé par le graphe d'activité ; les cinq dépendances
 * de données (generations, credits, uploads, settings, api) sont les singletons partagés de
 * l'`AppContainer`.
 */
class CreateViewModel(
    private val generations: GenerationRepository,
    private val credits: CreditsRepository,
    private val uploads: UploadRepository,
    private val settings: SettingsStore,
    private val api: CreateApi,
    private val recorder: AudioRecorder,
) : ViewModel() {

    private val _composer = MutableStateFlow(ComposerState())
    val composer: StateFlow<ComposerState> = _composer.asStateFlow()

    /** Feed inversé : 12 dernières générations (le repo trie déjà par `-created`). */
    val feed: StateFlow<List<Generation>> = generations.generations
        .map { it.take(FEED_LIMIT) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val _recording = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    init {
        // Restaure le dernier modèle / réglages persistés (NATIVE_SPEC §1).
        viewModelScope.launch {
            val familyKey = settings.familyKey.first()
            val variantKey = settings.variantKey.first()
            val params = settings.paramValues.first()
            val family = ModelCatalog.family(familyKey)
            _composer.update {
                it.copy(
                    familyKey = familyKey,
                    variantKey = variantKey,
                    paramValues = params,
                    mode = family?.kind ?: ModelKind.IMAGE,
                )
            }
        }
    }

    // --- Chargement & polling (pilotés par le lifecycle de l'écran) --------------------------

    /** Rafraîchit feed + solde crédits (appelé au démarrage de l'écran et au retour au premier plan). */
    fun reload() {
        viewModelScope.launch { runCatching { generations.refresh() } }
        viewModelScope.launch { runCatching { credits.refresh() } }
    }

    /**
     * Boucle de polling à exécuter sous `repeatOnLifecycle(STARTED)` (CONTRACTS §3.7). Interroge les
     * générations `PENDING` toutes les 4 s tant qu'il en reste ; les transitions déclenchent 1 haptique
     * (gérée dans le repository). Suspend jusqu'à annulation par le lifecycle.
     */
    suspend fun runPolling() {
        while (coroutineContext.isActive) {
            if (generations.pendingCount.value > 0) {
                runCatching { generations.pollPending() }
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    // --- Prompt & références -----------------------------------------------------------------

    fun onPromptChange(text: String) {
        _composer.update { it.copy(prompt = text) }
    }

    /** Ajoute une référence (picker) et lance son upload. Ignore si la limite du modèle est atteinte. */
    fun addRef(uri: Uri) {
        val current = _composer.value
        val family = ModelCatalog.family(current.familyKey)
        val max = family?.maxImages ?: 0
        if (current.refs.size >= max) return
        if (current.refs.any { it.localUri == uri }) return

        _composer.update {
            it.copy(refs = it.refs + RefImage(localUri = uri, uploading = true), uploading = true)
        }
        viewModelScope.launch {
            try {
                val url = uploads.upload(uri)
                _composer.update { st ->
                    val refs = st.refs.map { r ->
                        if (r.localUri == uri) r.copy(uploadedUrl = url, uploading = false) else r
                    }
                    st.copy(refs = refs, uploading = refs.any { it.uploading })
                }
            } catch (t: Throwable) {
                _composer.update { st ->
                    val refs = st.refs.filterNot { it.localUri == uri }
                    st.copy(refs = refs, uploading = refs.any { it.uploading })
                }
                emit(UiEvent.Error(errorMessage(t, "Échec de l'upload de l'image.")))
            }
        }
    }

    fun removeRef(uri: Uri) {
        _composer.update { st ->
            val refs = st.refs.filterNot { it.localUri == uri }
            st.copy(refs = refs, uploading = refs.any { it.uploading })
        }
    }

    // --- Sélection modèle / mode / réglages --------------------------------------------------

    fun pickModel(family: ModelFamily) = applyFamily(family.key, family.kind)

    fun pickVariant(variant: ModelVariant?) {
        _composer.update { it.copy(variantKey = variant?.key) }
    }

    fun setParam(field: String, value: String) {
        _composer.update { it.copy(paramValues = it.paramValues + (field to value)) }
    }

    /** Bascule Image/Vidéo : sélectionne la famille par défaut du mode et re-clampe les références. */
    fun setMode(mode: ModelKind) {
        if (_composer.value.mode == mode) return
        val key = when (mode) {
            ModelKind.IMAGE -> ModelCatalog.DEFAULT_IMAGE_FAMILY_KEY
            ModelKind.VIDEO -> ModelCatalog.DEFAULT_VIDEO_FAMILY_KEY
        }
        applyFamily(key, mode)
    }

    private fun applyFamily(key: String, mode: ModelKind) {
        val family = ModelCatalog.family(key)
        _composer.update { st ->
            val clamped = if (family != null) st.refs.take(family.maxImages) else st.refs
            st.copy(
                mode = mode,
                familyKey = key,
                variantKey = family?.variants?.firstOrNull()?.key,
                paramValues = emptyMap(),
                refs = clamped,
                uploading = clamped.any { it.uploading },
            )
        }
    }

    // --- Envoi -------------------------------------------------------------------------------

    /**
     * Envoie la génération : résout le slug modèle, construit les options, insère une carte optimiste
     * `PENDING` (repo) puis vide le composer. Persiste le dernier modèle. Le polling reprend
     * automatiquement dès qu'une génération `PENDING` existe.
     */
    fun send() {
        val state = _composer.value
        if (!state.canSend) return
        val family = ModelCatalog.family(state.familyKey) ?: return
        val variant = family.variants?.firstOrNull { it.key == state.variantKey }
        val hasRefs = state.refs.isNotEmpty()
        val model = CatalogLogic.resolveModelId(family, variant, hasRefs)
        val options = CatalogLogic.buildOptions(family, state.paramValues, state.editing)
        val imageUrls = state.refs.mapNotNull { it.uploadedUrl }.ifEmpty { null }

        _composer.update { it.copy(submitting = true) }
        viewModelScope.launch {
            try {
                generations.generate(
                    GenerateRequest(
                        model = model,
                        prompt = state.prompt.trim(),
                        imageUrls = imageUrls,
                        options = options,
                    ),
                )
                settings.setLastModel(state.familyKey, state.variantKey, state.paramValues)
                _composer.update { it.copy(prompt = "", refs = emptyList(), uploading = false, submitting = false) }
                runCatching { credits.refresh() }
            } catch (t: Throwable) {
                _composer.update { it.copy(submitting = false) }
                emit(UiEvent.Error(errorMessage(t, "Impossible de lancer la génération.")))
            }
        }
    }

    fun cancel(id: String) {
        viewModelScope.launch {
            try {
                generations.cancel(id)
            } catch (t: Throwable) {
                emit(UiEvent.Error(errorMessage(t, "Annulation impossible.")))
            }
        }
    }

    /** Réinjecte un prompt (carte échouée / annulée) dans le champ. */
    fun reuse(prompt: String) {
        _composer.update { it.copy(prompt = prompt) }
    }

    // --- Dictée vocale -----------------------------------------------------------------------

    /** Démarre l'enregistrement micro (la permission runtime est demandée côté UI). */
    fun startRecording() {
        if (_recording.value) return
        if (!recorder.hasPermission()) {
            emit(UiEvent.Error("Micro non autorisé."))
            return
        }
        recorder.onMaxDurationReached = { stopRecordingAndTranscribe() }
        try {
            recorder.start()
            _recording.value = true
        } catch (t: Throwable) {
            _recording.value = false
            recorder.onMaxDurationReached = null
            emit(UiEvent.Error("Impossible de démarrer l'enregistrement."))
        }
    }

    /** Arrête l'enregistrement, transcrit via `/api/transcribe` et ajoute le texte au prompt. */
    fun stopRecordingAndTranscribe() {
        if (!_recording.value) return
        _recording.value = false
        recorder.onMaxDurationReached = null

        val result = try {
            recorder.stop()
        } catch (t: Throwable) {
            emit(UiEvent.Error("Enregistrement interrompu."))
            return
        }

        when (result) {
            is AudioRecorder.RecordResult.TooShort -> Unit
            is AudioRecorder.RecordResult.Success -> viewModelScope.launch {
                try {
                    val body = result.bytes.toRequestBody(result.mimeType.toMediaTypeOrNull())
                    val transcript = api.transcribe(body).transcript.trim()
                    if (transcript.isNotEmpty()) {
                        _composer.update { st ->
                            val prefix = if (st.prompt.isBlank()) "" else st.prompt.trimEnd() + " "
                            st.copy(prompt = prefix + transcript)
                        }
                        emit(UiEvent.Snackbar("Transcription ajoutée"))
                    }
                } catch (t: Throwable) {
                    emit(UiEvent.Error(errorMessage(t, "Transcription impossible.")))
                } finally {
                    runCatching { result.file.delete() }
                }
            }
        }
    }

    // --- Helpers -----------------------------------------------------------------------------

    private fun emit(event: UiEvent) {
        _events.tryEmit(event)
    }

    private fun errorMessage(t: Throwable, fallback: String): String =
        (t as? ApiError)?.frenchMessage ?: t.message ?: fallback

    private companion object {
        const val FEED_LIMIT = 12
        const val POLL_INTERVAL_MS = 4_000L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
