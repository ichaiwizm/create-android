package com.wizycode.create.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * « Verre » Material — 3 niveaux de surface translucide tonale (CONTRACTS §2.6 / DESIGN §3).
 *
 * **Tous les modules passent par ces modifiers**, jamais un `background` translucide ad hoc.
 *
 * Note sur le blur : le vrai flou d'arrière-plan (backdrop) est une préoccupation de niveau
 * fenêtre (RenderNode Android 12+) et n'est pas appliqué au contenu de ces surfaces — flouter le
 * contenu nuirait à la lisibilité. Ici, [blurEnabled] pilote la **translucidité** : verre plus
 * transparent quand le blur/effets sont disponibles, opacité relevée à ≥ 92 % sinon (DESIGN §8).
 * **La lisibilité ne dépend jamais du blur.**
 */

/**
 * Drapeau « transparence réduite » (accessibilité). L'app le renseigne depuis ses préférences /
 * réglages système d'accessibilité. Quand `true`, le verre devient quasi-opaque et [blurEnabled]
 * repasse à `false`.
 */
var reduceTransparency: Boolean = false

/** Blur/effets disponibles : API 31+ et transparence non réduite (CONTRACTS §2.6). */
val blurEnabled: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !reduceTransparency

/** `.glass` — surface @ 60 % (92 % si effets coupés) : chips, petits panneaux flottants. */
fun Modifier.glassSurface(shape: Shape): Modifier = composed {
    val scheme = MaterialTheme.colorScheme
    val alpha = if (blurEnabled) 0.60f else 0.92f
    this
        .shadow(elevation = 8.dp, shape = shape, clip = false)
        .clip(shape)
        .background(scheme.surface.copy(alpha = alpha), shape)
        .border(1.dp, scheme.outlineVariant.copy(alpha = 0.5f), shape)
        .insetHighlight()
}

/** `.glass-strong` — surfaceContainer @ 80 % (92 % si effets coupés) : nav, top bar, sheets, composer, lightbox, toasts. */
fun Modifier.glassStrong(shape: Shape): Modifier = composed {
    val scheme = MaterialTheme.colorScheme
    val alpha = if (blurEnabled) 0.80f else 0.94f
    this
        .shadow(elevation = 12.dp, shape = shape, clip = false)
        .clip(shape)
        .background(scheme.surfaceContainer.copy(alpha = alpha), shape)
        .border(1.dp, scheme.outlineVariant, shape)
        .insetHighlight()
}

/**
 * `.card-solid` — surfaceContainerLowest **OPAQUE, JAMAIS de blur** : cartes répétées en scroll
 * (feed, galerie). Pas d'inset highlight (réservé aux surfaces `glass*`).
 */
fun Modifier.cardSolid(shape: Shape): Modifier = composed {
    val scheme = MaterialTheme.colorScheme
    this
        .shadow(elevation = 3.dp, shape = shape, clip = false)
        .clip(shape)
        .background(scheme.surfaceContainerLowest, shape)
        .border(1.dp, scheme.outlineVariant.copy(alpha = 0.6f), shape)
}

/**
 * Inset highlight blanc en haut des surfaces `glass*` : liseré d'~1 dp `Color.White @ 0.5f` qui
 * s'estompe verticalement. Dessiné au-dessus du fond mais sous le contenu ; le `clip(shape)`
 * appliqué en amont l'arrondit aux coins.
 */
private fun Modifier.insetHighlight(): Modifier = drawWithContent {
    val bandHeight = 3.dp.toPx()
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent),
            startY = 0f,
            endY = bandHeight,
        ),
        size = Size(size.width, bandHeight),
    )
    drawContent()
}
