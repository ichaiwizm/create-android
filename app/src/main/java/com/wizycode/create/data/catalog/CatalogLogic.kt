package com.wizycode.create.data.catalog

import com.wizycode.create.data.model.ModelFamily
import com.wizycode.create.data.model.ModelVariant
import com.wizycode.create.data.model.ParamSpec

/**
 * Logique de résolution du catalogue kie (CONTRACTS §1.4, PLAN §3.4).
 *
 * Miroir des helpers `models.ts` (`paramsFor`, `buildInput`, sélection de slug) et de l'affichage
 * `Studio.tsx` (label de bouton modèle, résumé des réglages).
 *
 * `selections` est un `Map<field, valeurChoisie>` de chaînes brutes (avant cast numeric/boolean) :
 * exactement l'état `ComposerState.paramValues`. Le **serveur** applique `extraInput` + `imageField` ;
 * [buildOptions] ne produit que les options des [ParamSpec] visibles.
 */
object CatalogLogic {

    /**
     * Params visibles pour une famille : en mode édition on masque les [ParamSpec] `textOnly`.
     *
     * `params.filter { !editing || !it.textOnly }`
     */
    fun paramsFor(family: ModelFamily, editing: Boolean): List<ParamSpec> =
        family.params.filter { !editing || !it.textOnly }

    /**
     * Slug kie à envoyer :
     * 1. variante (Veo Fast/Quality) si présente ▸ `variant.id` ;
     * 2. sinon `editId` si des références sont jointes **et** la famille est éditable ;
     * 3. sinon `textId`.
     */
    fun resolveModelId(family: ModelFamily, variant: ModelVariant?, hasRefs: Boolean): String {
        variant?.let { return it.id }
        return if (hasRefs && family.editId.isNotBlank()) family.editId else family.textId
    }

    /**
     * Construit le bloc `options` envoyé à `/api/generate`.
     *
     * Pour chaque param visible : valeur choisie si elle appartient à `values`, sinon `def` ; puis
     * cast en `Int` si [ParamSpec.numeric], en `Boolean` si [ParamSpec.boolean], sinon `String` brut.
     */
    fun buildOptions(
        family: ModelFamily,
        selections: Map<String, String>,
        editing: Boolean,
    ): Map<String, Any?> =
        paramsFor(family, editing).associate { p ->
            val raw = optValue(p, selections)
            val value: Any? = when {
                p.numeric -> raw.toIntOrNull() ?: p.def.toIntOrNull() ?: raw
                p.boolean -> raw.toBooleanStrictOrNull() ?: (raw == "true")
                else -> raw
            }
            p.field to value
        }

    /**
     * Résumé compact des réglages pour le bouton dédié — Format puis Durée uniquement (ex : `16:9 · 8s`).
     * Renvoie `Réglages` si la famille n'expose ni Format ni Durée.
     */
    fun settingsSummary(
        family: ModelFamily,
        variant: ModelVariant?,
        selections: Map<String, String>,
    ): String {
        val ratio = family.params.firstOrNull { it.label == "Format" }
        val duration = family.params.firstOrNull { it.label == "Durée" }
        val parts = buildList {
            ratio?.let { add(optValue(it, selections)) }
            duration?.let { add(optValue(it, selections) + "s") }
        }
        return if (parts.isEmpty()) "Réglages" else parts.joinToString(" · ")
    }

    /**
     * Libellé du bouton de sélection de modèle (ex : `Veo 3.1 Rapide · ~80cr`).
     *
     * Si la famille a des variantes, on retient celle passée sinon la première ; sinon on affiche le
     * libellé de crédits de la famille (`family.credits`, déjà préfixé `~`).
     */
    fun modelButtonLabel(family: ModelFamily, variant: ModelVariant?): String {
        val v = variant ?: family.variants?.firstOrNull()
        return if (v != null) {
            "${family.name} ${v.label} · ~${v.credits}cr"
        } else {
            "${family.name} · ${family.credits}cr"
        }
    }

    /** Valeur retenue pour un param : sélection si valide (∈ `values`), sinon la valeur par défaut. */
    private fun optValue(p: ParamSpec, selections: Map<String, String>): String {
        val chosen = selections[p.field]
        return if (chosen != null && chosen in p.values) chosen else p.def
    }
}
