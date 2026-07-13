package com.wizycode.create.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Mini-icône de **format** (ratio d'aspect) pour les chips de la sheet Réglages (CONTRACTS §4.4,
 * DESIGN §5.6).
 *
 * Dessine sur un [Canvas] un rectangle au trait dont les proportions reflètent [ratio] (`"16:9"`,
 * `"9:16"`, `"1:1"`…), centré et mis à l'échelle pour tenir dans [size]. Les valeurs non numériques
 * (`"auto"`, `"adaptive"`) retombent sur un carré. Symétrique → **non miroité** en RTL.
 *
 * Décoratif : le label du chip (« 16:9 ») porte déjà l'information ; l'icône est masquée de l'a11y.
 */
@Composable
fun RatioIcon(
    ratio: String,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val (rw, rh) = remember(ratio) { parseRatio(ratio) }

    Canvas(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics {},
    ) {
        val strokePx = 1.5.dp.toPx()
        val availW = this.size.width - strokePx
        val availH = this.size.height - strokePx
        val scale = min(availW / rw, availH / rh)
        val rectW = rw * scale
        val rectH = rh * scale
        val left = (this.size.width - rectW) / 2f
        val top = (this.size.height - rectH) / 2f

        drawRoundRect(
            color = tint,
            topLeft = Offset(left, top),
            size = Size(rectW, rectH),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(width = strokePx),
        )
    }
}

/** Parse `"w:h"` en couple de floats positifs ; retombe sur `1:1` si non numérique/invalide. */
private fun parseRatio(ratio: String): Pair<Float, Float> {
    val parts = ratio.split(":")
    if (parts.size == 2) {
        val w = parts[0].trim().toFloatOrNull()
        val h = parts[1].trim().toFloatOrNull()
        if (w != null && h != null && w > 0f && h > 0f) return w to h
    }
    return 1f to 1f
}
