package com.wizycode.create.ui.gallery

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.wizycode.create.data.catalog.ModelCatalog
import com.wizycode.create.data.model.Generation

/**
 * Rend une carte cliquable avec une `interactionSource` partagée (pour composer avec `pressScale`),
 * l'indication par défaut du thème, un rôle bouton et un libellé d'accessibilité (DESIGN §8).
 */
@Composable
fun Modifier.clickableCard(
    interaction: MutableInteractionSource,
    label: String,
    onClick: () -> Unit,
): Modifier = this.clickable(
    interactionSource = interaction,
    indication = LocalIndication.current,
    onClickLabel = label,
    role = Role.Button,
    onClick = onClick,
)

/**
 * Nom lisible d'un modèle à partir de son slug kie (`veo3_fast`, `nano-banana-pro`…), résolu depuis
 * [ModelCatalog]. Retombe sur le slug brut si inconnu (tolérant aux futurs modèles serveur).
 */
fun modelDisplayName(slug: String): String {
    val families = ModelCatalog.image + ModelCatalog.video
    for (family in families) {
        family.variants?.forEach { variant ->
            if (variant.id == slug) return "${family.name} ${variant.label}"
        }
        if (family.textId == slug || family.editId == slug || family.key == slug) return family.name
    }
    ModelCatalog.tools.forEach { tool ->
        if (tool.id == slug || tool.key == slug) return tool.label
    }
    return slug
}

/** Message d'échec affiché sur une carte `FAILED` — inclut le détail serveur s'il existe. */
fun failureMessage(generation: Generation): String {
    val detail = generation.error?.trim().orEmpty()
    return if (detail.isEmpty()) "Échec de la génération" else "Échec — $detail"
}
