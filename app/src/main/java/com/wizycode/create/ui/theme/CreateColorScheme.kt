package com.wizycode.create.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * ColorSchemes Material 3 de Create — valeurs hex verbatim de DESIGN §2.2 (light) / §2.3 (dark).
 *
 * L'iris reste la signature : `primary` = IrisBlue en light, relevé en luminosité en dark. Ces
 * schemes sont FIGÉS (CONTRACTS §2.2). Les surfaces translucides « verre » ne sont PAS des tokens
 * M3 : voir [GlassModifiers].
 */

// ---------------------------------------------------------------------------------------------
// LIGHT (défaut) — palette tonale iris (seed #3B82F6)
// ---------------------------------------------------------------------------------------------
val CreateLightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF0B2A6B),
    secondary = Color(0xFF8B5CF6),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEBE2FF),
    onSecondaryContainer = Color(0xFF2C1466),
    tertiary = Color(0xFF06B6D4),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC9F3FB),
    onTertiaryContainer = Color(0xFF043F49),
    background = Color(0xFFEEF0FD),
    onBackground = Color(0xFF2A3142),
    surface = Color(0xFFF4F5FE),
    onSurface = Color(0xFF2A3142),
    surfaceVariant = Color(0xFFE2E5F3),
    onSurfaceVariant = Color(0xFF5D6478),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F8FF),
    surfaceContainer = Color(0xFFF0F2FD),
    surfaceContainerHigh = Color(0xFFE9ECFB),
    surfaceContainerHighest = Color(0xFFE2E6F8),
    surfaceTint = Color(0xFF3B82F6),
    outline = Color(0xFFAEB4CB),
    outlineVariant = Color(0xFFD3D8EC),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    scrim = Color(0xFF1A1F2E),
)

// ---------------------------------------------------------------------------------------------
// DARK (fourni, non-défaut sauf réglage système) — aurora sombre, iris relevé
// ---------------------------------------------------------------------------------------------
val CreateDarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF9EC0FF),
    onPrimary = Color(0xFF0A2A5C),
    primaryContainer = Color(0xFF274A86),
    onPrimaryContainer = Color(0xFFD8E6FF),
    secondary = Color(0xFFC4B0FF),
    onSecondary = Color(0xFF241356),
    secondaryContainer = Color(0xFF3E2C79),
    onSecondaryContainer = Color(0xFFE8DEFF),
    tertiary = Color(0xFF6FE0F2),
    onTertiary = Color(0xFF00363F),
    tertiaryContainer = Color(0xFF0C4E5A),
    onTertiaryContainer = Color(0xFFB6ECF6),
    background = Color(0xFF0E1220),
    onBackground = Color(0xFFE3E6F4),
    surface = Color(0xFF121728),
    onSurface = Color(0xFFE3E6F4),
    surfaceVariant = Color(0xFF3A3F52),
    onSurfaceVariant = Color(0xFFC3C7DA),
    surfaceContainerLowest = Color(0xFF0B0F1B),
    surfaceContainerLow = Color(0xFF141A2B),
    surfaceContainer = Color(0xFF181D30),
    surfaceContainerHigh = Color(0xFF222840),
    surfaceContainerHighest = Color(0xFF2C3350),
    surfaceTint = Color(0xFF9EC0FF),
    outline = Color(0xFF8B90A6),
    outlineVariant = Color(0xFF3A3F52),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF3A0A0A),
    errorContainer = Color(0xFF5C1A1A),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color(0xFF000000),
)

/**
 * Résout le [ColorScheme] à appliquer (CONTRACTS §2.2 / DESIGN §2.4).
 *
 * `useDynamic = dynamic && SDK_INT >= 31`. Le dynamic color (Material You) pilote les neutres et
 * les surfaces, mais **l'accent produit reste toujours IrisBlue** et le **brush iris n'est JAMAIS
 * remplacé** (le brush est indépendant du scheme — voir [IrisBrush] / [LocalIrisBrushes]). En
 * l'absence de dynamic color, on retourne les schemes de marque figés.
 */
@Composable
fun createColorScheme(dark: Boolean, dynamic: Boolean): ColorScheme {
    val context = LocalContext.current
    val useDynamic = dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val base = when {
        useDynamic && dark -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        dark -> CreateDarkScheme
        else -> CreateLightScheme
    }
    // L'iris survit toujours : on ré-imprime la signature d'accent même sous dynamic color.
    return if (useDynamic) {
        base.copy(primary = IrisBlue, onPrimary = Color.White, surfaceTint = IrisBlue)
    } else {
        base
    }
}
