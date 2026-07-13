package com.wizycode.create.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.wizycode.create.ui.theme.shimmer

/**
 * Bloc squelette (skeleton) de chargement (CONTRACTS §4.4, DESIGN §6.2).
 *
 * Surface `surfaceVariant` découpée à [shape] et balayée par le dégradé diagonal `Modifier.shimmer`
 * du design system (1200 ms, désactivé automatiquement en réduction de mouvement). La taille est
 * fournie par l'appelant via [modifier]. Purement décoratif : masqué de l'arbre d'accessibilité.
 *
 * @param active `false` fige le squelette (couche translucide sans animation).
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    active: Boolean = true,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .shimmer(active)
            .clearAndSetSemantics {},
    )
}
