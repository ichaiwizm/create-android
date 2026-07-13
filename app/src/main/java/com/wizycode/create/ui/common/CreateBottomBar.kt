package com.wizycode.create.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.wizycode.create.core.util.Haptic
import com.wizycode.create.ui.nav.TopTab
import com.wizycode.create.ui.theme.LocalHaptics
import com.wizycode.create.ui.theme.LocalIrisBrushes
import com.wizycode.create.ui.theme.Motion
import com.wizycode.create.ui.theme.glassStrong
import com.wizycode.create.ui.theme.rememberReduceMotion

/**
 * Barre de navigation basse flottante (CONTRACTS §4.4, DESIGN §5.2).
 *
 * - Conteneur **`glassStrong`**, `RoundedCornerShape(26.dp)`, **détaché des bords** (marge 12 dp) et
 *   posé au-dessus de l'inset `navigationBars`.
 * - 2 destinations : **Créer** (`AutoAwesome`) · **Galerie** (`Image`).
 * - **Indicateur pill** `primaryContainer` glissant derrière l'onglet actif, animé en **spring**
 *   Expressive ([Motion.spatialSpring] — snap si réduction de mouvement). Icône + label de l'onglet
 *   actif teintés **iris**.
 * - Haptique **SELECT** au changement d'onglet (une seule par action).
 *
 * A11y : chaque item est `selectable` avec [Role.Tab], cible ≥ 48 dp, label lu par TalkBack.
 * RTL : l'ordre visuel des onglets et l'indicateur sont miroités via [LayoutDirection].
 */
@Composable
fun CreateBottomBar(
    selected: TopTab,
    onSelect: (TopTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHaptics.current
    val reduceMotion = rememberReduceMotion()
    val layoutDirection = LocalLayoutDirection.current
    val shape = RoundedCornerShape(26.dp)

    val items = TopTab.entries
    val selectedIndex = items.indexOf(selected).toFloat()
    val animIndex by animateFloatAsState(
        targetValue = selectedIndex,
        animationSpec = if (reduceMotion) snap() else Motion.spatialSpring,
        label = "navIndicator",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .glassStrong(shape)
                .padding(6.dp)
                .selectableGroup(),
        ) {
            val slotWidth = maxWidth / items.size
            // Position visuelle de l'indicateur, miroitée en RTL.
            val visualIndex = if (layoutDirection == LayoutDirection.Rtl) {
                (items.size - 1) - animIndex
            } else {
                animIndex
            }

            // Pill d'indicateur (derrière les items).
            Box(
                modifier = Modifier
                    .offset(x = slotWidth * visualIndex)
                    .width(slotWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            )

            Row(modifier = Modifier.fillMaxSize()) {
                items.forEach { tab ->
                    NavItem(
                        tab = tab,
                        active = tab == selected,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = {
                            if (tab != selected) {
                                haptics.fire(Haptic.SELECT)
                                onSelect(tab)
                            }
                        },
                    )
                }
            }
        }
    }
}

/** Un onglet : icône + label empilés, teintés iris quand actif. */
@Composable
private fun NavItem(
    tab: TopTab,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val brushes = LocalIrisBrushes.current
    val icon: ImageVector = when (tab) {
        TopTab.CREATE -> Icons.Rounded.AutoAwesome
        TopTab.GALLERY -> Icons.Rounded.Image
    }
    val label = when (tab) {
        TopTab.CREATE -> "Créer"
        TopTab.GALLERY -> "Galerie"
    }

    Column(
        modifier = modifier
            .clip(CircleShape)
            .selectable(
                selected = active,
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (active) {
            IrisIcon(imageVector = icon, contentDescription = null, brush = brushes.fill, size = 24.dp)
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(3.dp))
        if (active) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(brush = brushes.text),
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
