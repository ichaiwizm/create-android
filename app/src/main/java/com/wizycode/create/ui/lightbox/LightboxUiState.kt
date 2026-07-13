package com.wizycode.create.ui.lightbox

import com.wizycode.create.data.model.GenStatus
import com.wizycode.create.data.model.Generation
import com.wizycode.create.data.model.firstMediaUrl
import com.wizycode.create.data.model.isVideo

/**
 * État immuable de la visionneuse plein écran (CONTRACTS §4.3, DESIGN §5.10).
 *
 * Source unique lue par [LightboxScreen] via `LightboxViewModel.state`. La génération vient du cache
 * partagé du [com.wizycode.create.data.GenerationRepository] (même source que feed / galerie) : la
 * lightbox ne fait donc aucun appel de lecture, elle observe et agit.
 *
 * @param generation génération affichée, ou `null` tant qu'elle n'est pas résolue / après suppression.
 * @param modelLabel libellé lisible du modèle (nom de famille + variante), résolu depuis le catalogue.
 * @param working action longue en cours (outil upscale/détourage ou suppression) — verrouille la barre.
 * @param confirmingDelete premier tap « Supprimer » reçu : le bouton demande confirmation (2 s).
 * @param loading cache pas encore peuplé et génération introuvable → skeleton.
 * @param notFound génération connue puis disparue (supprimée) → l'écran se referme.
 */
data class LightboxUiState(
    val generation: Generation? = null,
    val modelLabel: String = "",
    val working: Boolean = false,
    val confirmingDelete: Boolean = false,
    val loading: Boolean = true,
    val notFound: Boolean = false,
) {
    /** Un média exploitable est disponible (statut terminé + au moins une URL). */
    val hasMedia: Boolean
        get() = generation != null &&
            generation.status == GenStatus.DONE &&
            generation.firstMediaUrl != null

    /** L'image peut recevoir les outils 1-clic (Upscale / Détourer) : média prêt & non vidéo. */
    val canRunImageTools: Boolean
        get() = hasMedia && generation != null && !generation.isVideo

    /** Le média est une vidéo (rendu ExoPlayer plutôt qu'AsyncImage). */
    val isVideo: Boolean
        get() = generation?.isVideo == true

    /** Crédits consommés à afficher dans la barre haute, ou `null` si inconnus. */
    val creditsConsumed: Int?
        get() = generation?.creditsConsumed
}

/**
 * Effets one-shot émis par le ViewModel et exécutés par [LightboxScreen] (les APIs liées au `Context` —
 * presse-papier, MediaStore, partage de fichier, snackbar, fermeture — vivent côté composable, le
 * ViewModel restant sans dépendance Android hors modèle). CONTRACTS §4.3 (pattern `UiEvent`).
 */
sealed interface LightboxEffect {
    /** Copier [text] dans le presse-papier (déclenché par un tap sur le prompt). */
    data class CopyPrompt(val text: String) : LightboxEffect

    /** Partager le média [url] via le sélecteur système ([com.wizycode.create.core.media.FileShare]). */
    data class Share(val url: String, val isVideo: Boolean) : LightboxEffect

    /** Enregistrer le média [url] dans la photothèque ([com.wizycode.create.core.media.MediaSaver]). */
    data class Save(val url: String, val isVideo: Boolean) : LightboxEffect

    /** Afficher un message transitoire (Snackbar). */
    data class Snackbar(val message: String) : LightboxEffect

    /** Refermer la visionneuse (après suppression, lancement d'outil, ou génération disparue). */
    data object Dismiss : LightboxEffect
}
