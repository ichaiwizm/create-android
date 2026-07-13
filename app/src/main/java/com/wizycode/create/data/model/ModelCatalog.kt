package com.wizycode.create.data.model

/**
 * Types du catalogue kie — miroir verbatim de `models.ts` (NATIVE_SPEC §5).
 *
 * Pur data : ce fichier ne contient que les structures. Le catalogue statique
 * (`object ModelCatalog`) et la logique (`CatalogLogic`) vivent dans `data/catalog/`.
 */

/** Nature d'une famille de modèles. */
enum class ModelKind { IMAGE, VIDEO }

/**
 * Spécification d'un paramètre réglable d'un modèle (une ligne de la sheet de réglages).
 *
 * @param field clé envoyée dans `options` à `/api/generate` (ex. `"aspect_ratio"`).
 * @param label libellé affiché (ex. `"Format"`, `"Résolution"`, `"Durée"`, `"Son"`).
 * @param values valeurs proposées (String brut).
 * @param def valeur par défaut (doit appartenir à [values]).
 * @param numeric si `true`, la valeur choisie est castée en `Int` dans `options`.
 * @param boolean si `true`, la valeur choisie est castée en `Boolean` dans `options`.
 * @param boolLabels libellés du toggle booléen (ex. `("Avec son", "Sans son")`).
 * @param textOnly si `true`, masqué en mode édition.
 */
data class ParamSpec(
    val field: String,
    val label: String,
    val values: List<String>,
    val def: String,
    val numeric: Boolean = false,
    val boolean: Boolean = false,
    val boolLabels: Pair<String, String>? = null,
    val textOnly: Boolean = false,
)

/**
 * Variante d'un modèle (ex. Veo 3.1 Rapide / Qualité).
 *
 * @param key `"fast"` | `"quality"`.
 * @param label libellé affiché (`"Rapide"` | `"Qualité"`).
 * @param id slug kie (`"veo3_fast"` | `"veo3"`).
 * @param credits coût en crédits de la variante.
 */
data class ModelVariant(
    val key: String,
    val label: String,
    val id: String,
    val credits: Int,
)

/**
 * Famille de modèles (unité de choix dans le composer).
 *
 * @param key clé stable (ex. `"nano-banana-pro"`).
 * @param kind nature (image / vidéo).
 * @param name nom affiché.
 * @param tagline accroche courte.
 * @param credits libellé de coût (ex. `"~18-24"`).
 * @param textId slug kie texte→média.
 * @param editId slug kie édition (i2i / i2v).
 * @param imageField clé du champ images (`"image_input"`, `"input_urls"`, `"first_frame_url"`…).
 * @param imageIsList `true` si le champ images attend une liste.
 * @param maxImages nombre maximal d'images de référence.
 * @param params paramètres réglables.
 * @param extraInput entrées additionnelles injectées côté serveur (output_format / multi_shots…).
 * @param variants variantes optionnelles (ex. Veo Fast/Quality), `null` si aucune.
 */
data class ModelFamily(
    val key: String,
    val kind: ModelKind,
    val name: String,
    val tagline: String,
    val credits: String,
    val textId: String,
    val editId: String,
    val imageField: String,
    val imageIsList: Boolean,
    val maxImages: Int,
    val params: List<ParamSpec>,
    val extraInput: Map<String, Any?> = emptyMap(),
    val variants: List<ModelVariant>? = null,
)

/**
 * Outil 1-clic appliqué à un média existant (upscale / détourage).
 *
 * @param key `"upscale"` | `"removeBg"`.
 * @param label libellé affiché (`"Upscale"` | `"Détourer"`).
 * @param id slug kie (`"topaz/image-upscale"`, `"recraft/remove-background"`).
 * @param credits coût en crédits.
 */
data class ToolSpec(
    val key: String,
    val label: String,
    val id: String,
    val credits: Int,
)
