package com.wizycode.create.ui.nav

/**
 * Table de routage de la navigation Compose racine (CONTRACTS §4.2).
 *
 * Les libellés de routes sont **figés** par le contrat et partagés avec les deep-links FCM
 * (`create://gallery`, `create://lightbox/{generationId}`). Aucun autre module ne doit inventer
 * de chaîne de route : tout passe par cet objet.
 *
 * - [LOGIN] / [CREATE] / [GALLERY] / [SETTINGS] : destinations sans argument.
 * - [LIGHTBOX] : destination overlay plein écran paramétrée par l'id de génération. Construire
 *   l'instance concrète via [lightbox] (jamais de concaténation à la main ailleurs).
 */
object Routes {
    const val LOGIN = "login"
    const val CREATE = "create"
    const val GALLERY = "gallery"
    const val SETTINGS = "settings"

    /** Nom de l'argument de chemin de la lightbox. */
    const val LIGHTBOX_ARG = "generationId"

    /** Motif de route de la lightbox (avec placeholder d'argument). */
    const val LIGHTBOX = "lightbox/{$LIGHTBOX_ARG}"

    /** Construit la route concrète d'ouverture de la lightbox pour [generationId]. */
    fun lightbox(generationId: String): String = "lightbox/$generationId"
}

/**
 * Onglets de la barre de navigation basse (les deux surfaces principales du produit, NATIVE_SPEC §1).
 *
 * Chaque onglet est adossé à sa route [Routes] afin que la sélection courante puisse être dérivée
 * de la back-stack sans table de correspondance dupliquée.
 */
enum class TopTab(val route: String) {
    /** Écran d'accueil : composer + feed inversé. */
    CREATE(Routes.CREATE),

    /** Grille 2 colonnes de toutes les créations. */
    GALLERY(Routes.GALLERY);

    companion object {
        /** Onglet correspondant à [route], ou `null` si la route n'est pas un onglet (login/lightbox/settings). */
        fun fromRoute(route: String?): TopTab? = entries.firstOrNull { it.route == route }
    }
}
