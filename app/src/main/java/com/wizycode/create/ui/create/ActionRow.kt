package com.wizycode.create.ui.create

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.wizycode.create.core.util.Haptic
import com.wizycode.create.ui.theme.IrisBrush
import com.wizycode.create.ui.theme.LocalHaptics
import com.wizycode.create.ui.theme.pressScale

/**
 * Rangée d'actions du composer (DESIGN §5.4.3) : Photo · Micro · Modèle · Réglages · (spacer) · Générer.
 */
@Composable
fun ActionRow(
    canAddPhoto: Boolean,
    canSend: Boolean,
    recording: Boolean,
    modelLabel: String,
    settingsSummary: String,
    onAddRef: (Uri) -> Unit,
    onSend: () -> Unit,
    onOpenModelSheet: () -> Unit,
    onOpenSettingsSheet: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhotoButton(enabled = canAddPhoto, onAddRef = onAddRef)
        MicButton(
            recording = recording,
            onStartRecording = onStartRecording,
            onStopRecording = onStopRecording,
        )
        ModelChip(label = modelLabel, onClick = onOpenModelSheet)
        SettingsChip(summary = settingsSummary, onClick = onOpenSettingsSheet)
        Spacer(Modifier.weight(1f))
        GenerateButton(enabled = canSend, onSend = onSend)
    }
}

/** Bouton photo → PhotoPicker (images), désactivé si la limite de références est atteinte. */
@Composable
private fun PhotoButton(
    enabled: Boolean,
    onAddRef: (Uri) -> Unit,
) {
    val haptics = LocalHaptics.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) onAddRef(uri) }

    IconButton(
        onClick = {
            haptics.fire(Haptic.TAP)
            launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        enabled = enabled,
        modifier = Modifier.semantics { contentDescription = "Ajouter une image de référence" },
    ) {
        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
    }
}

/**
 * Bouton micro → dictée (DESIGN §5.7). Halo rouge pulsant pendant l'enregistrement via
 * `rememberInfiniteTransition` (900 ms) — la **seule** boucle infinie tolérée de l'app.
 * Gère la permission runtime `RECORD_AUDIO`.
 */
@Composable
private fun MicButton(
    recording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
) {
    val haptics = LocalHaptics.current
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) onStartRecording() }

    Box(contentAlignment = Alignment.Center) {
        if (recording) {
            val transition = rememberInfiniteTransition(label = "mic-halo")
            val pulse by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 900),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "mic-halo-pulse",
            )
            val haloColor = MaterialTheme.colorScheme.error
            Canvas(modifier = Modifier.size(48.dp)) {
                val maxRadius = size.minDimension / 2f
                val radius = maxRadius * (0.6f + 0.4f * pulse)
                drawCircle(
                    color = haloColor.copy(alpha = 0.35f * (1f - pulse)),
                    radius = radius,
                    center = center,
                )
            }
        }
        IconButton(
            onClick = {
                if (recording) {
                    haptics.fire(Haptic.TAP)
                    onStopRecording()
                } else {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                    haptics.fire(Haptic.TAP)
                    if (granted) {
                        onStartRecording()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
            modifier = Modifier.semantics {
                contentDescription = if (recording) "Arrêter la dictée" else "Dicter le prompt"
            },
        ) {
            Icon(
                imageVector = if (recording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                contentDescription = null,
                tint = if (recording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Chip modèle : « {nom} {variante?} · {credits}cr ▾ » → ouvre la sheet Modèle. */
@Composable
private fun ModelChip(label: String, onClick: () -> Unit) {
    val haptics = LocalHaptics.current
    AssistChip(
        onClick = {
            haptics.fire(Haptic.TAP)
            onClick()
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingIcon = {
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize),
            )
        },
    )
}

/** Chip réglages : résumé (« 16:9 · 8s ▾ ») → ouvre la sheet Réglages. */
@Composable
private fun SettingsChip(summary: String, onClick: () -> Unit) {
    val haptics = LocalHaptics.current
    AssistChip(
        onClick = {
            haptics.fire(Haptic.TAP)
            onClick()
        },
        label = {
            Text(
                text = summary,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingIcon = {
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize),
            )
        },
    )
}

/**
 * Bouton Générer (DESIGN §5.4.3) : bouton rond 48 dp peint au brush iris, icône ↑ blanche. Micro-morph
 * (press-scale) au toucher, haptique `launch`. Désactivé (opacité réduite, brush grisé) si l'envoi est
 * impossible.
 */
@Composable
fun GenerateButton(
    enabled: Boolean,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val disabledBrush = SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (enabled) IrisBrush else disabledBrush)
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
            ) {
                haptics.fire(Haptic.LAUNCH)
                onSend()
            }
            .semantics { contentDescription = "Générer" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.ArrowUpward,
            contentDescription = null,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
        )
    }
}
