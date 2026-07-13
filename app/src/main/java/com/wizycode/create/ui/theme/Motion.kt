package com.wizycode.create.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

/**
 * Motion de Create — springs Expressive & modifiers (CONTRACTS §2.7 / DESIGN §6).
 *
 * Ressorts nommés figés ; l'aurora reste statique de toute façon. Les animations décoratives
 * (press, shimmer) sont coupées quand [rememberReduceMotion] est vrai.
 */
object Motion {
    /** Position / taille (Expressive). */
    val spatialSpring: SpringSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 380f)

    /** Couleur / alpha (Expressive). */
    val effectsSpring: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 700f)

    const val DUR_FAST: Int = 120
    const val DUR_STD: Int = 240
    const val DUR_EMPHATIC: Int = 400
}

/**
 * Surcharge applicative de la réduction de mouvement (préférence a11y). Si non-`null`, elle prime
 * sur la détection système dans [rememberReduceMotion].
 */
var reduceMotionOverride: Boolean? = null

/**
 * `.press` — scale 0.94 sur appui, via l'[MutableInteractionSource] (`effectsSpring`). Désactivé
 * si réduction de mouvement.
 */
fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val reduce = rememberReduceMotion()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduce) 0.94f else 1f,
        animationSpec = Motion.effectsSpring,
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * `.shimmer` — skeleton : bande diagonale translatée en boucle (1200 ms linéaire) teintée en
 * `surfaceVariant` → `surface`. `SrcAtop` pour ne teinter que le placeholder existant. Désactivé
 * (statique) si réduction de mouvement ou `active == false`.
 */
fun Modifier.shimmer(active: Boolean = true): Modifier = composed {
    val reduce = rememberReduceMotion()
    if (!active || reduce) return@composed this

    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )

    drawWithCache {
        val width = size.width
        val travel = width * 2f
        val startX = -width + progress * travel
        val brush = Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(startX, 0f),
            end = Offset(startX + width, size.height),
        )
        onDrawWithContent {
            drawContent()
            drawRect(brush = brush, blendMode = BlendMode.SrcAtop)
        }
    }
}

/**
 * Réduction de mouvement (CONTRACTS §2.7) : `ANIMATOR_DURATION_SCALE == 0` (réglage développeur /
 * accessibilité) ou surcharge applicative [reduceMotionOverride].
 */
@Composable
fun rememberReduceMotion(): Boolean {
    reduceMotionOverride?.let { return it }
    val context = LocalContext.current
    return remember {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        scale == 0f
    }
}
