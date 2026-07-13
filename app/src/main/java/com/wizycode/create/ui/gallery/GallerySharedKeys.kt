package com.wizycode.create.ui.gallery

/**
 * Clés de transition partagée (shared element) galerie → lightbox (DESIGN §5.8 / §6, CONTRACTS §4.2).
 *
 * La même clé est employée par la miniature de la [GalleryCard] et par le média plein écran de la
 * lightbox, ce qui permet au `SharedTransitionLayout` racine d'animer l'un vers l'autre. La clé est
 * dérivée de l'`id` de génération (stable et unique).
 */
object GallerySharedKeys {
    /** Clé du média (miniature ↔ plein écran) pour la génération [id]. */
    fun media(id: String): String = "gen-media-$id"
}
