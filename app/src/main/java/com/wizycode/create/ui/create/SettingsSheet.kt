package com.wizycode.create.ui.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wizycode.create.core.util.Haptic
import com.wizycode.create.data.catalog.CatalogLogic
import com.wizycode.create.data.model.ModelFamily
import com.wizycode.create.data.model.ModelVariant
import com.wizycode.create.data.model.ParamSpec
import com.wizycode.create.ui.common.RatioIcon
import com.wizycode.create.ui.theme.LocalHaptics
import com.wizycode.create.ui.theme.glassStrong

/**
 * Bottom sheet des réglages, **générée dynamiquement** depuis le catalogue (CONTRACTS §4.4 / DESIGN
 * §5.6). Aucun réglage codé en dur : la sheet se construit à partir de [CatalogLogic.paramsFor].
 *
 * - Si la famille a des `variants` (Veo) : rangée « Qualité » de `FilterChip`.
 * - Une rangée par param visible : titre + `FlowRow` de `FilterChip`.
 *   - booléens → deux chips via `boolLabels` ;
 *   - « Durée » → suffixe `s` ;
 *   - « Format » (`aspect_ratio`) → mini-icône ratio dessinée.
 * - Aucun param → « Ce modèle n'a pas de réglages. »
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    family: ModelFamily,
    variant: ModelVariant?,
    paramValues: Map<String, String>,
    editing: Boolean,
    onPickVariant: (ModelVariant?) -> Unit,
    onSetParam: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = LocalHaptics.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val params = CatalogLogic.paramsFor(family, editing)
    val variants = family.variants

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassStrong(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = "Réglages",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (!variants.isNullOrEmpty()) {
                ParamSection(title = "Qualité") {
                    variants.forEach { v ->
                        val selected = (variant?.key ?: variants.firstOrNull()?.key) == v.key
                        SettingChip(
                            selected = selected,
                            label = v.label,
                            onClick = {
                                haptics.fire(Haptic.SELECT)
                                onPickVariant(v)
                            },
                        )
                    }
                }
            }

            if (params.isEmpty() && variants.isNullOrEmpty()) {
                Text(
                    text = "Ce modèle n'a pas de réglages.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                )
            } else {
                params.forEach { param ->
                    val current = paramValues[param.field]?.takeIf { it in param.values } ?: param.def
                    ParamSection(title = param.label) {
                        param.values.forEach { value ->
                            SettingChip(
                                selected = value == current,
                                label = chipLabel(param, value),
                                leading = if (isRatio(param)) {
                                    { RatioIcon(ratio = value, modifier = Modifier.size(16.dp)) }
                                } else {
                                    null
                                },
                                onClick = {
                                    haptics.fire(Haptic.SELECT)
                                    onSetParam(param.field, value)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParamSection(
    title: String,
    content: @Composable FlowRowScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        leadingIcon = leading,
        border = if (selected) {
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = true,
                selectedBorderColor = MaterialTheme.colorScheme.primary,
                selectedBorderWidth = 1.5.dp,
            )
        } else {
            FilterChipDefaults.filterChipBorder(enabled = true, selected = false)
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

/** Libellé d'un chip : booléens → `boolLabels`, « Durée » → suffixe `s`, sinon la valeur brute. */
private fun chipLabel(param: ParamSpec, value: String): String {
    param.boolLabels?.let { (onLabel, offLabel) ->
        return if (value == "true") onLabel else offLabel
    }
    return if (param.label == "Durée") "${value}s" else value
}

private fun isRatio(param: ParamSpec): Boolean =
    param.field == "aspect_ratio" || param.label == "Format"
