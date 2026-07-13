package com.wizycode.create.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Constantes de marque « iris » et brushes signature.
 *
 * Ces valeurs sont FIGÉES (CONTRACTS §2.1 / DESIGN §2.1). Elles ne sont jamais dérivées du dynamic
 * color : l'iris (violet → bleu → cyan) est l'identité immuable de Create, y compris quand
 * l'utilisateur active « couleurs du fond d'écran ». Le brush iris n'est JAMAIS remplacé et n'est
 * JAMAIS miroité en RTL (l'orientation 135° du dégradé fait partie de la marque).
 */

// ---------------------------------------------------------------------------------------------
// Iris — dégradé signature (violet → bleu → cyan)
// ---------------------------------------------------------------------------------------------
val IrisViolet = Color(0xFF8B5CF6)
val IrisBlue = Color(0xFF3B82F6)
val IrisCyan = Color(0xFF22D3EE)

// Variante « texte » (équivalent du background-clip web `.text-iris`)
val IrisTextViolet = Color(0xFF7C3AED)
val IrisTextBlue = Color(0xFF2563EB)
val IrisTextCyan = Color(0xFF06B6D4)

// ---------------------------------------------------------------------------------------------
// Encre & accent ponctuel (light)
// ---------------------------------------------------------------------------------------------
/** Texte principal (light). */
val Ink = Color(0xFF2A3142)

/** Texte secondaire (light). */
val InkSoft = Color(0xFF5D6478)

/** Bleu accent — point final du wordmark « Create. ». */
val Accent = Color(0xFF2F6DF6)

// ---------------------------------------------------------------------------------------------
// Brushes réutilisables — exposés à l'app via LocalIrisBrushes (voir CreateTheme.kt)
// ---------------------------------------------------------------------------------------------
/**
 * Dégradé de remplissage 135° (haut-gauche → bas-droite) : bouton Générer, halo micro, wavy
 * progress, indicateurs actifs. Défini avec des color-stops explicites conformes à DESIGN §2.1.
 *
 * Les points de départ/arrivée par défaut (`Offset.Zero` → `Offset.Infinite`) produisent le
 * dégradé diagonal ; pour un angle 135° exact sur une bounding box précise, recalculer les
 * offsets via `drawWithCache` / `onGloballyPositioned` au point d'usage.
 */
val IrisBrush = Brush.linearGradient(0.0f to IrisViolet, 0.55f to IrisBlue, 1.0f to IrisCyan)

/** Variante « texte » du dégradé iris (mot hero *crée*, wordmark). */
val IrisTextBrush = Brush.linearGradient(0.0f to IrisTextViolet, 0.55f to IrisTextBlue, 1.0f to IrisTextCyan)
