package com.wizycode.create.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import com.wizycode.create.core.util.Haptic
import com.wizycode.create.core.util.Haptics

/**
 * Thème racine de Create + CompositionLocals (CONTRACTS §2.9 / DESIGN §4).
 *
 * Assemble [MaterialTheme] (scheme dynamic-aware, typographie serif/sans, formes Expressive
 * gonflées via [CreateShapes]) et fournit les brushes iris + l'implémentation haptique via
 * CompositionLocals.
 */

/** Paire de brushes iris exposée à toute l'app : `fill` (remplissage) et `text` (texte). */
data class IrisBrushes(val fill: Brush, val text: Brush)

/** Brushes iris. Défaut = les brushes de marque figés ([IrisBrush] / [IrisTextBrush]). */
val LocalIrisBrushes: ProvidableCompositionLocal<IrisBrushes> = staticCompositionLocalOf {
    IrisBrushes(fill = IrisBrush, text = IrisTextBrush)
}

/**
 * Implémentation haptique (interface figée dans `core/util`). Défaut = no-op ; l'app fournit
 * l'implémentation réelle (AppContainer) plus haut dans l'arbre ou via [CreateTheme].
 */
val LocalHaptics: ProvidableCompositionLocal<Haptics> = staticCompositionLocalOf { NoOpHaptics }

private object NoOpHaptics : Haptics {
    override fun fire(h: Haptic) {}
    override fun prepare(h: Haptic) {}
}

@Composable
fun CreateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = createColorScheme(dark = darkTheme, dynamic = dynamicColor)
    val irisBrushes = remember { IrisBrushes(fill = IrisBrush, text = IrisTextBrush) }

    CompositionLocalProvider(
        LocalIrisBrushes provides irisBrushes,
        // On propage la Haptics déjà fournie par l'app (AppContainer) le cas échéant, sinon no-op.
        LocalHaptics provides LocalHaptics.current,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CreateTypography,
            shapes = CreateShapes,
            content = content,
        )
    }
}
