package com.wizycode.create.ui.theme

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.random.Random

/**
 * Fond « Aurora » de Create (CONTRACTS §2.5 / DESIGN §2.5).
 *
 * **Statique, zéro animation** (perf + batterie). Reproduit le multi-radial du web : base linéaire
 * + halos pastel violet/bleu/cyan/bleu-clair (light), halos iris sombres (dark). Posé à la racine
 * du Scaffold, derrière tout le contenu.
 */

/** Base linéaire 165° + 4 radiaux pastel (light). Aucune animation. */
fun DrawScope.auroraLight() {
    val w = size.width
    val h = size.height
    val maxDim = max(w, h)

    drawRect(
        Brush.linearGradient(
            0.0f to Color(0xFFEEF0FD),
            0.45f to Color(0xFFEAF2FC),
            1.0f to Color(0xFFEAFAF9),
            start = Offset(0f, 0f),
            end = Offset(w * 0.26f, h),
        ),
    )
    // Violet — haut-gauche
    drawRect(
        Brush.radialGradient(
            listOf(Color(0x66A78BFA), Color.Transparent),
            center = Offset(0.12f * w, 0.08f * h),
            radius = 0.60f * maxDim,
        ),
    )
    // Bleu — bas-droite
    drawRect(
        Brush.radialGradient(
            listOf(Color(0x5960A5FA), Color.Transparent),
            center = Offset(0.88f * w, 0.92f * h),
            radius = 0.55f * maxDim,
        ),
    )
    // Cyan — centre
    drawRect(
        Brush.radialGradient(
            listOf(Color(0x4767E8F9), Color.Transparent),
            center = Offset(0.55f * w, 0.40f * h),
            radius = 0.45f * maxDim,
        ),
    )
    // Bleu clair — haut-droite
    drawRect(
        Brush.radialGradient(
            listOf(Color(0x4D93C5FD), Color.Transparent),
            center = Offset(0.85f * w, 0.10f * h),
            radius = 0.40f * maxDim,
        ),
    )
}

/** Base #0E1220 + halos iris à ~20 % (dark). Aucune animation. */
fun DrawScope.auroraDark() {
    val w = size.width
    val h = size.height
    val maxDim = max(w, h)

    drawRect(Color(0xFF0E1220))
    // Violet
    drawRect(
        Brush.radialGradient(
            listOf(Color(0x338B5CF6), Color.Transparent),
            center = Offset(0.12f * w, 0.08f * h),
            radius = 0.60f * maxDim,
        ),
    )
    // Bleu
    drawRect(
        Brush.radialGradient(
            listOf(Color(0x333B82F6), Color.Transparent),
            center = Offset(0.88f * w, 0.92f * h),
            radius = 0.55f * maxDim,
        ),
    )
    // Cyan
    drawRect(
        Brush.radialGradient(
            listOf(Color(0x2622D3EE), Color.Transparent),
            center = Offset(0.55f * w, 0.40f * h),
            radius = 0.45f * maxDim,
        ),
    )
}

/**
 * Fond Aurora complet : dessine [auroraLight] / [auroraDark] (choisi d'après la luminance du
 * `background` du thème courant, indépendamment du réglage système) + une couche de **grain 128 px**
 * (bruit tuilé, `alpha = 0.04`, `BlendMode.Overlay`). Ne s'anime jamais.
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val darkBackground = colorScheme.background.luminance() < 0.5f
    val grain = remember { generateNoiseBitmap(128) }
    val grainBrush = remember(grain) {
        ShaderBrush(ImageShader(grain, TileMode.Repeated, TileMode.Repeated))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                if (darkBackground) auroraDark() else auroraLight()
                drawRect(brush = grainBrush, alpha = 0.04f, blendMode = BlendMode.Overlay)
            },
    )
}

/** Génère une texture de bruit monochrome 128×128 déterministe pour la couche de grain. */
private fun generateNoiseBitmap(dimension: Int): ImageBitmap {
    val random = Random(42)
    val pixels = IntArray(dimension * dimension)
    for (i in pixels.indices) {
        val v = random.nextInt(256)
        pixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
    }
    val bitmap = Bitmap.createBitmap(dimension, dimension, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, dimension, 0, 0, dimension, dimension)
    return bitmap.asImageBitmap()
}
