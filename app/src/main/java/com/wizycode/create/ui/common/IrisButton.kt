package com.wizycode.create.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wizycode.create.core.util.Haptic
import com.wizycode.create.ui.theme.LocalHaptics
import com.wizycode.create.ui.theme.LocalIrisBrushes
import com.wizycode.create.ui.theme.pressScale

/** Opacité appliquée à un bouton iris désactivé (DESIGN §5.4/§5.11). */
private const val DISABLED_ALPHA = 0.4f

/**
 * Bouton peint avec le **brush iris** (CONTRACTS §4.4/§5 règle 2, DESIGN §5.11).
 *
 * Variante « texte / large » : fond iris, contenu en `onPrimary`, `labelLarge`. Utilisé pour les
 * actions primaires (« Se connecter »…). Applique [pressScale] à l'appui et **grise à 40 %** quand
 * [enabled] est `false`. L'haptique [haptic] est jouée **une fois** au clic (par défaut [Haptic.TAP] ;
 * passer `null` pour la gérer soi-même, ou [Haptic.LAUNCH] pour un lancement de génération).
 */
@Composable
fun IrisButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.large,
    haptic: Haptic? = Haptic.TAP,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val brushes = LocalIrisBrushes.current
    val haptics = LocalHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val disabledFill = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    Row(
        modifier = modifier
            .pressScale(interaction)
            .graphicsLayer { alpha = if (enabled) 1f else DISABLED_ALPHA }
            .clip(shape)
            .then(if (enabled) Modifier.background(brushes.fill) else Modifier.background(disabledFill))
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptic?.let { haptics.fire(it) }
                    onClick()
                },
            )
            .minimumInteractiveComponentSize()
            .padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimary) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) { content() }
        }
    }
}

/**
 * Bouton **rond** peint iris (CONTRACTS §5.4, DESIGN §5.4) : le bouton « Générer » (flèche ↑) et les
 * autres actions circulaires primaires. Diamètre [size] (48 dp par défaut → cible tactile conforme),
 * `CircleShape`, icône en `onPrimary`, [pressScale] à l'appui, grisé à 40 % si désactivé.
 *
 * @param contentDescription requis (a11y — le bouton n'a pas de label texte).
 */
@Composable
fun IrisIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    haptic: Haptic? = Haptic.TAP,
) {
    val brushes = LocalIrisBrushes.current
    val haptics = LocalHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val disabledFill = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    Box(
        modifier = modifier
            .pressScale(interaction)
            .graphicsLayer { alpha = if (enabled) 1f else DISABLED_ALPHA }
            .size(size)
            .clip(CircleShape)
            .then(if (enabled) Modifier.background(brushes.fill) else Modifier.background(disabledFill))
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptic?.let { haptics.fire(it) }
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}
