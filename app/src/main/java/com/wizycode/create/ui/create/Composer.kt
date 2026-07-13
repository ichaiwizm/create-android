package com.wizycode.create.ui.create

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.wizycode.create.core.util.Haptic
import com.wizycode.create.data.catalog.CatalogLogic
import com.wizycode.create.data.catalog.ModelCatalog
import com.wizycode.create.data.model.ModelKind
import com.wizycode.create.ui.theme.LocalHaptics
import com.wizycode.create.ui.theme.glassStrong

/**
 * Composer fixe bas de l'écran Créer (CONTRACTS §4.4 / DESIGN §5.4).
 *
 * Panneau `glassStrong` 28 dp, marge 12 dp, soulevé au-dessus du clavier (`imePadding`) et de la barre
 * de navigation. Empile : rangée de références (si présentes), champ prompt auto-grow, rangée d'actions.
 */
@Composable
fun Composer(
    state: ComposerState,
    recording: Boolean,
    onPromptChange: (String) -> Unit,
    onAddRef: (Uri) -> Unit,
    onRemoveRef: (Uri) -> Unit,
    onSend: () -> Unit,
    onOpenModelSheet: () -> Unit,
    onOpenSettingsSheet: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val family = ModelCatalog.family(state.familyKey)
    val variant = family?.variants?.firstOrNull { it.key == state.variantKey }
    val modelLabel = if (family != null) CatalogLogic.modelButtonLabel(family, variant) else "Modèle"
    val settingsSummary = if (family != null) {
        CatalogLogic.settingsSummary(family, variant, state.paramValues)
    } else {
        "Réglages"
    }
    val maxImages = family?.maxImages ?: 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .glassStrong(RoundedCornerShape(28.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (state.refs.isNotEmpty()) {
            RefsRow(
                refs = state.refs,
                mode = state.mode,
                onRemoveRef = onRemoveRef,
            )
        }
        PromptField(
            value = state.prompt,
            editing = state.editing,
            mode = state.mode,
            onValueChange = onPromptChange,
            onSend = onSend,
        )
        ActionRow(
            canAddPhoto = state.refs.size < maxImages,
            canSend = state.canSend,
            recording = recording,
            modelLabel = modelLabel,
            settingsSummary = settingsSummary,
            onAddRef = onAddRef,
            onSend = onSend,
            onOpenModelSheet = onOpenModelSheet,
            onOpenSettingsSheet = onOpenSettingsSheet,
            onStartRecording = onStartRecording,
            onStopRecording = onStopRecording,
        )
    }
}

/**
 * Rangée des images de référence (DESIGN §5.4.1) : badge de mode + `LazyRow` de vignettes 56 dp avec
 * bouton ✕. Badge « ÉDITION » (image) ou « IMAGE → VIDÉO » (vidéo), fond `tertiaryContainer`.
 */
@Composable
fun RefsRow(
    refs: List<RefImage>,
    mode: ModelKind,
    onRemoveRef: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val badge = if (mode == ModelKind.VIDEO) "IMAGE → VIDÉO" else "ÉDITION"
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
        ) {
            items(items = refs, key = { it.localUri.toString() }) { ref ->
                RefThumb(ref = ref, onRemove = { onRemoveRef(ref.localUri) })
            }
        }
    }
}

/** Vignette 56 dp d'une référence : image + spinner d'upload + bouton ✕ superposé. */
@Composable
private fun RefThumb(
    ref: RefImage,
    onRemove: () -> Unit,
) {
    val haptics = LocalHaptics.current
    Box(modifier = Modifier.size(56.dp)) {
        AsyncImage(
            model = ref.localUri,
            contentDescription = "Image de référence",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        if (ref.uploading) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable {
                    haptics.fire(Haptic.TAP)
                    onRemove()
                }
                .semantics { contentDescription = "Retirer l'image" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/**
 * Champ de saisie du prompt (DESIGN §5.4.2) : `BasicTextField` sans bordure, auto-grow jusqu'à ~150 dp
 * puis scroll interne, placeholder contextuel, action clavier « envoyer ».
 */
@Composable
fun PromptField(
    value: String,
    editing: Boolean,
    mode: ModelKind,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val placeholder = when {
        editing -> "Décris la retouche à faire…"
        mode == ModelKind.VIDEO -> "Décris ta vidéo…"
        else -> "Décris ton image…"
    }
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 24.dp, max = 150.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                inner()
            },
        )
    }
}
