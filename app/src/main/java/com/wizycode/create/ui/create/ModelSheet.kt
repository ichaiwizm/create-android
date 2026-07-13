package com.wizycode.create.ui.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.wizycode.create.core.util.Haptic
import com.wizycode.create.data.catalog.ModelCatalog
import com.wizycode.create.data.model.ModelFamily
import com.wizycode.create.data.model.ModelKind
import com.wizycode.create.ui.theme.IrisBrush
import com.wizycode.create.ui.theme.LocalHaptics
import com.wizycode.create.ui.theme.glassStrong

/**
 * Bottom sheet de sélection du modèle (CONTRACTS §4.4 / DESIGN §5.5).
 *
 * `ModalBottomSheet` `glassStrong` : segment Image/Vidéo en haut (re-clampe les refs au `maxImages`),
 * puis liste des familles du mode. La famille active porte un **contour iris 2 dp** + une coche. Tap
 * famille → sélectionne, ferme la sheet, haptique `select`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSheet(
    mode: ModelKind,
    activeFamilyKey: String,
    onSetMode: (ModelKind) -> Unit,
    onPickModel: (ModelFamily) -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = LocalHaptics.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassStrong(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Modèle",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = mode == ModelKind.IMAGE,
                    onClick = {
                        if (mode != ModelKind.IMAGE) {
                            haptics.fire(Haptic.SELECT)
                            onSetMode(ModelKind.IMAGE)
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text("Image") }
                SegmentedButton(
                    selected = mode == ModelKind.VIDEO,
                    onClick = {
                        if (mode != ModelKind.VIDEO) {
                            haptics.fire(Haptic.SELECT)
                            onSetMode(ModelKind.VIDEO)
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text("Vidéo") }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items = ModelCatalog.families(mode), key = { it.key }) { family ->
                    FamilyRow(
                        family = family,
                        active = family.key == activeFamilyKey,
                        onClick = {
                            haptics.fire(Haptic.SELECT)
                            onPickModel(family)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

/** Ligne de famille : nom + tagline + pill crédits ; contour iris + coche si active. */
@Composable
private fun FamilyRow(
    family: ModelFamily,
    active: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val base = Modifier
        .fillMaxWidth()
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        .clickable(onClick = onClick)
        .padding(16.dp)
    val decorated = if (active) base.then(Modifier.border(BorderStroke(2.dp, IrisBrush), shape)) else base

    Row(
        modifier = decorated,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = family.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = family.tagline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = "${family.credits} cr",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
        if (active) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = "Sélectionné",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
