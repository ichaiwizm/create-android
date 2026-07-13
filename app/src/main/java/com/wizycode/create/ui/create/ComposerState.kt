package com.wizycode.create.ui.create

import android.net.Uri
import com.wizycode.create.data.catalog.ModelCatalog
import com.wizycode.create.data.model.ModelKind

/**
 * Une image de référence attachée au composer (CONTRACTS §1.6).
 *
 * Elle vit d'abord en local (URI du picker) le temps de l'upload vers `/api/upload`, puis reçoit son
 * [uploadedUrl] kie (temporaire ~3 j) une fois l'upload terminé.
 *
 * @param localUri contenu local (`content://`) issu du PhotoPicker — clé d'identité de la vignette.
 * @param uploadedUrl URL kie renvoyée par le serveur, ou `null` tant que l'upload n'est pas fini.
 * @param uploading `true` pendant l'upload (vignette en cours de chargement).
 */
data class RefImage(
    val localUri: Uri,
    val uploadedUrl: String? = null,
    val uploading: Boolean = false,
)

/**
 * État immuable du composer « Créer » (CONTRACTS §1.6).
 *
 * Source de vérité de l'écran cœur : prompt, images de référence, mode (image/vidéo), famille de
 * modèle + variante éventuelle, réglages choisis (`paramValues` = les `selections` de
 * [com.wizycode.create.data.catalog.CatalogLogic]) et drapeaux d'occupation.
 *
 * @param prompt texte saisi / dicté.
 * @param refs images de référence (0..`family.maxImages`).
 * @param mode nature courante (image ou vidéo) — pilote la liste de familles et le placeholder.
 * @param familyKey clé de la famille sélectionnée (défaut : famille image par défaut du catalogue).
 * @param variantKey clé de variante (Veo Fast/Quality), `null` si la famille n'a pas de variante.
 * @param paramValues réglages retenus (`field` → valeur brute String), avant cast numeric/boolean.
 * @param uploading `true` si au moins une référence est encore en cours d'upload.
 * @param submitting `true` pendant l'envoi d'une génération (`send()`).
 */
data class ComposerState(
    val prompt: String = "",
    val refs: List<RefImage> = emptyList(),
    val mode: ModelKind = ModelKind.IMAGE,
    val familyKey: String = ModelCatalog.DEFAULT_IMAGE_FAMILY_KEY,
    val variantKey: String? = null,
    val paramValues: Map<String, String> = emptyMap(),
    val uploading: Boolean = false,
    val submitting: Boolean = false,
) {
    /**
     * Mode édition (dérivé) : des références sont jointes **et** la famille courante sait éditer
     * (`editId` non vide). En édition, la sheet Réglages masque les [com.wizycode.create.data.model.ParamSpec]
     * `textOnly` et `send()` résout le slug d'édition (i2i / i2v).
     */
    val editing: Boolean
        get() {
            if (refs.isEmpty()) return false
            val family = ModelCatalog.family(familyKey) ?: return false
            return family.editId.isNotBlank()
        }

    /** `true` si l'envoi est possible : prompt non vide et rien en cours. */
    val canSend: Boolean
        get() = prompt.isNotBlank() && !submitting && !uploading && refs.none { it.uploading }
}
