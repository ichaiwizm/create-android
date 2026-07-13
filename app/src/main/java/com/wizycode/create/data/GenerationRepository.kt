package com.wizycode.create.data

import com.wizycode.create.core.net.CreateApi
import com.wizycode.create.core.net.GenerationMapper
import com.wizycode.create.core.net.dto.GenerateRequest
import com.wizycode.create.core.net.dto.ToolRequest
import com.wizycode.create.core.util.Haptic
import com.wizycode.create.data.model.GenStatus
import com.wizycode.create.data.model.Generation
import com.wizycode.create.data.model.MediaKind
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.UUID

/**
 * Source de vérité **unique et partagée** des générations (CONTRACTS §3.6 / §3.7).
 *
 * Le feed de l'écran Créer et la grille Galerie lisent le **même** cache mémoire
 * ([generations], tri `-created`). Pas de Room : `MutableStateFlow` in-memory.
 *
 * Retours haptiques des transitions asynchrones : le repository ne détient pas de [Haptics]
 * (constructeur figé `(api)`), il **émet** un [Haptic] sur [hapticEvents] ; le ViewModel/écran
 * actif collecte ce flux et déclenche `LocalHaptics.fire(...)`. Cela garantit la règle
 * « au plus 1 haptique par batch » de façon centralisée (CONTRACTS §2.8 / §3.7).
 */
class GenerationRepository(private val api: CreateApi) {

    private val mutex = Mutex()

    private val _generations = MutableStateFlow<List<Generation>>(emptyList())
    /** Toutes les générations connues, triées `-created` (source unique feed ↔ galerie). */
    val generations: StateFlow<List<Generation>> = _generations.asStateFlow()

    private val _pendingCount = MutableStateFlow(0)
    /** Nombre de générations `PENDING` (bandeau galerie, contrôle du poll). */
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val _hapticEvents = MutableSharedFlow<Haptic>(extraBufferCapacity = 8)
    /** Haptiques issues des transitions découvertes par [pollPending] (SUCCESS/ERROR). */
    val hapticEvents: SharedFlow<Haptic> = _hapticEvents.asSharedFlow()

    // --- Lecture ------------------------------------------------------------------------------

    /**
     * Les [limit] créations les plus récentes, **inversées** pour le feed collé au composer
     * (plus ancien en haut, plus récent en bas). Cf. NATIVE_SPEC §2.2 `.slice(0,12).reverse()`.
     */
    fun feed(limit: Int = 12): List<Generation> =
        _generations.value.take(limit).reversed()

    // --- Rafraîchissement ---------------------------------------------------------------------

    /** GET /api/generations → map DTO → domaine → remplace le cache. */
    suspend fun refresh() {
        val items = api.list().items.map { GenerationMapper.toDomain(it) }
        setCache { items }
    }

    // --- Écriture (optimistic) ----------------------------------------------------------------

    /**
     * Lance une génération : insère immédiatement une carte `PENDING` optimiste, puis réconcilie
     * par l'`id` renvoyé par le serveur. En cas d'échec réseau, la carte optimiste est retirée.
     */
    suspend fun generate(req: GenerateRequest): Generation {
        val optimistic = optimisticPending(model = req.model, prompt = req.prompt)
        setCache { it + optimistic }
        return reconcile(optimistic) { api.generate(req).id }
    }

    /**
     * Lance un outil 1-clic (upscale / removeBg) : même pattern optimiste que [generate].
     * Le prompt est vide (outil sans prompt) et le modèle porte la clé d'outil.
     */
    suspend fun runTool(req: ToolRequest): Generation {
        val optimistic = optimisticPending(model = req.tool, prompt = "")
        setCache { it + optimistic }
        return reconcile(optimistic) { api.runTool(req).id }
    }

    /**
     * Poll de chaque génération `PENDING` (foreground uniquement — cf. §3.7). Un **seul** haptique
     * par batch : SUCCESS si au moins une transition `PENDING→DONE`, sinon ERROR si au moins une
     * transition `PENDING→FAILED/CANCELLED`.
     */
    suspend fun pollPending() {
        val pendingIds = _generations.value
            .filter { it.status == GenStatus.PENDING && !it.id.startsWith(LOCAL_ID_PREFIX) }
            .map { it.id }
        if (pendingIds.isEmpty()) return

        var anyDone = false
        var anyFailed = false

        for (id in pendingIds) {
            val updated = runCatching { GenerationMapper.toDomain(api.poll(id)) }.getOrNull() ?: continue
            val previous = _generations.value.firstOrNull { it.id == id }
            if (previous?.status == GenStatus.PENDING && updated.status != GenStatus.PENDING) {
                when (updated.status) {
                    GenStatus.DONE -> anyDone = true
                    GenStatus.FAILED, GenStatus.CANCELLED -> anyFailed = true
                    GenStatus.PENDING -> Unit
                }
            }
            setCache { list -> list.map { if (it.id == id) updated else it } }
        }

        when {
            anyDone -> _hapticEvents.tryEmit(Haptic.SUCCESS)
            anyFailed -> _hapticEvents.tryEmit(Haptic.ERROR)
        }
    }

    /**
     * Annule une génération (best-effort côté kie) puis marque localement `CANCELLED`.
     * L'haptique `ERROR` de ce geste utilisateur est déclenchée par l'UI (pas ici, pour éviter
     * un double retour).
     */
    suspend fun cancel(id: String) {
        api.cancel(id)
        setCache { list ->
            list.map {
                if (it.id == id && it.status == GenStatus.PENDING) it.copy(status = GenStatus.CANCELLED) else it
            }
        }
    }

    /** Supprime une génération côté serveur puis la retire du cache. */
    suspend fun delete(id: String) {
        api.delete(id)
        setCache { list -> list.filterNot { it.id == id } }
    }

    // --- Interne ------------------------------------------------------------------------------

    private fun optimisticPending(model: String, prompt: String): Generation = Generation(
        id = LOCAL_ID_PREFIX + UUID.randomUUID(),
        kind = MediaKind.IMAGE, // provisoire : corrigé au 1er poll/refresh (le vrai kind vient du DTO)
        model = model,
        prompt = prompt,
        status = GenStatus.PENDING,
        mediaUrls = emptyList(),
        error = null,
        creditsConsumed = null,
        created = Instant.now(),
    )

    /** Remplace l'`id` local par celui du serveur ; retire la carte optimiste si l'appel échoue. */
    private suspend fun reconcile(optimistic: Generation, submit: suspend () -> String): Generation {
        return try {
            val serverId = submit()
            val reconciled = optimistic.copy(id = serverId)
            setCache { list -> list.map { if (it.id == optimistic.id) reconciled else it } }
            reconciled
        } catch (t: Throwable) {
            setCache { list -> list.filterNot { it.id == optimistic.id } }
            throw t
        }
    }

    /** Mutation atomique du cache : ré-applique le tri `-created` et recalcule [pendingCount]. */
    private suspend fun setCache(transform: (List<Generation>) -> List<Generation>) {
        mutex.withLock {
            val next = transform(_generations.value).sortedByDescending { it.created }
            _generations.value = next
            _pendingCount.value = next.count { it.status == GenStatus.PENDING }
        }
    }

    private companion object {
        const val LOCAL_ID_PREFIX = "local-"
    }
}
