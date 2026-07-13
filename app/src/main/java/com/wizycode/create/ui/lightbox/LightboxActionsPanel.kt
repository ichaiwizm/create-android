package com.wizycode.create.ui.lightbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wizycode.create.data.catalog.ModelCatalog
import com.wizycode.create.ui.theme.glassSurface

/**
 * Panneau d'actions bas de la visionneuse (DESIGN §5.10) — verre `glassStrong`, coins `extraLarge`.
 *
 * Contenu :
 * 1. le **prompt** (tap = copier, feedback haptique + Snackbar « Copié ») ;
 * 2. une grille d'actions `FilledTonalButton` : **Partager** (toujours), **Sauver** (média prêt),
 *    **Upscale** & **Détourer** (images prêtes) ;
 * 3. un `TextButton` **Supprimer** en couleur `error`, à **double-tap** de confirmation.
 *
 * Le panneau lui-même est rendu (verre + forme) par l'appelant [LightboxScreen] ; ici on ne gère que
 * la mise en page interne, ce qui garde le composant réutilisable et le flou centralisé.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LightboxActionsPanel(
    state: LightboxUiState,
    onCopyPrompt: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onUpscale: () -> Unit,
    onRemoveBg: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val prompt = state.generation?.prompt.orEmpty()
    val upscaleCredits = ModelCatalog.tools.firstOrNull { it.key == "upscale" }?.credits
    val removeBgCredits = ModelCatalog.tools.firstOrNull { it.key == "removeBg" }?.credits

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (prompt.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .glassSurface(RoundedCornerShape(18.dp))
                    .clickable(role = Role.Button, onClick = onCopyPrompt)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ActionButton(
                label = "Partager",
                enabled = state.hasMedia && !state.working,
                onClick = onShare,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                },
            )

            ActionButton(
                label = "Sauver",
                enabled = state.hasMedia && !state.working,
                onClick = onSave,
            )

            if (state.canRunImageTools) {
                ActionButton(
                    label = upscaleCredits?.let { "Upscale · ${it}cr" } ?: "Upscale",
                    enabled = !state.working,
                    onClick = onUpscale,
                )
                ActionButton(
                    label = removeBgCredits?.let { "Détourer · ${it}cr" } ?: "Détourer",
                    enabled = !state.working,
                    onClick = onRemoveBg,
                )
            }
        }

        DeleteButton(
            confirming = state.confirmingDelete,
            enabled = !state.working,
            onClick = onDelete,
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        leadingIcon?.invoke()
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun DeleteButton(
    confirming: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Delete,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = if (confirming) "Confirmer la suppression ?" else "Supprimer",
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
