package com.wizycode.create.ui.gallery

import com.wizycode.create.data.model.Generation

/**
 * État immuable de l'écran Galerie (CONTRACTS §4.3, DESIGN §5.8).
 *
 * @param items toutes les générations connues, triées `-created` (source unique partagée avec le
 *   feed via [com.wizycode.create.data.GenerationRepository]).
 * @param pendingCount nombre de générations `PENDING` — pilote le bandeau « N en cours » et le poll.
 * @param refreshing `true` pendant un rafraîchissement manuel (pull-to-refresh).
 */
data class GalleryUiState(
    val items: List<Generation> = emptyList(),
    val pendingCount: Int = 0,
    val refreshing: Boolean = false,
) {
    /** `true` quand aucune génération n'existe et qu'aucun rafraîchissement n'est en cours. */
    val isEmpty: Boolean get() = items.isEmpty() && !refreshing
}
