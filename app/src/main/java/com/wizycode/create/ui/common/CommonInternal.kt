package com.wizycode.create.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Helpers internes partagés par les composables communs (`ui/common`). Rien de public : ces
 * fonctions ne font pas partie du contrat (CONTRACTS §4.4) et ne servent qu'à factoriser du rendu.
 */

/**
 * Peint une icône vectorielle avec le **brush iris** de remplissage (violet → bleu → cyan).
 *
 * Compose ne sait pas teinter une icône avec un [Brush] via le paramètre `tint` (qui n'accepte
 * qu'une [Color]). On force donc une couche de composition isolée (`graphicsLayer { alpha = .99f }`)
 * puis on repeint le glyphe avec le brush en [BlendMode.SrcAtop] : le dégradé n'apparaît que là où
 * le glyphe est opaque. La teinte de base est noire pour garantir une couverture alpha pleine.
 *
 * Conforme à l'identité (CONTRACTS §2.1, DESIGN §2.1) : le brush iris **n'est jamais miroité** en
 * RTL — son orientation 135° fait partie de la marque.
 *
 * @param brush généralement `LocalIrisBrushes.current.fill`.
 */
@Composable
internal fun IrisIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    brush: Brush,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = Color.Black,
        modifier = modifier
            .size(size)
            .graphicsLayer { alpha = 0.99f }
            .drawWithContent {
                drawContent()
                drawRect(brush = brush, blendMode = BlendMode.SrcAtop)
            },
    )
}
