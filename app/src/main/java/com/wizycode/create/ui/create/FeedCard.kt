package com.wizycode.create.ui.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.wizycode.create.core.util.Haptic
import com.wizycode.create.core.util.timeAgo
import com.wizycode.create.data.model.Generation
import com.wizycode.create.data.model.GenStatus
import com.wizycode.create.data.model.isVideo
import com.wizycode.create.data.model.thumbFeedUrl
import com.wizycode.create.ui.theme.IrisBlue
import com.wizycode.create.ui.theme.LocalHaptics
import com.wizycode.create.ui.theme.cardSolid
import com.wizycode.create.ui.theme.feedBubbleShape
import com.wizycode.create.ui.theme.pressScale
import com.wizycode.create.ui.theme.shimmer

/**
 * Carte de feed (bulle sortante, CONTRACTS §4.4 / DESIGN §5.3). Aiguille sur la variante d'état ;
 * la largeur (72 %) et l'alignement à droite sont posés par [Feed]. Coin-queue bas-droit via
 * [feedBubbleShape] (miroité en RTL).
 *
 * @param generation génération à peindre.
 * @param onOpen ouverture de la lightbox (id) sur une génération `DONE`.
 * @param onReuse réinjection du prompt (`FAILED` / `CANCELLED`).
 * @param onCancel annulation d'une génération `PENDING`.
 * @param thumbnailModifier hook shared-element fourni par la couche navigation (transition vers la lightbox).
 */
@Composable
fun FeedCard(
    generation: Generation,
    onOpen: (String) -> Unit,
    onReuse: (String) -> Unit,
    onCancel: (String) -> Unit,
    modifier: Modifier = Modifier,
    thumbnailModifier: Modifier = Modifier,
) {
    val shape = feedBubbleShape(LocalLayoutDirection.current)
    when (generation.status) {
        GenStatus.PENDING -> PendingFeedCard(generation, shape, onCancel, modifier)
        GenStatus.DONE -> DoneFeedCard(generation, shape, onOpen, modifier, thumbnailModifier)
        GenStatus.FAILED -> FailedFeedCard(generation, shape, onReuse, modifier)
        GenStatus.CANCELLED -> CancelledFeedCard(generation, shape, onReuse, modifier)
    }
}

/** En cours : shimmer + progression circulaire + prompt + progression linéaire iris + annuler. */
@Composable
private fun PendingFeedCard(
    generation: Generation,
    shape: androidx.compose.ui.graphics.Shape,
    onCancel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHaptics.current
    Column(
        modifier = modifier
            .cardSolid(shape)
            .shimmer()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = IrisBlue,
            )
            Text(
                text = generation.prompt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            FilledIconButton(
                onClick = {
                    haptics.fire(Haptic.ERROR)
                    onCancel(generation.id)
                },
                modifier = Modifier
                    .size(32.dp)
                    .semantics { contentDescription = "Annuler la génération" },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = IrisBlue,
        )
    }
}

/** Terminé : miniature (image `?thumb=600x0` ou vidéo muette) + prompt · timeAgo · crédits. */
@Composable
private fun DoneFeedCard(
    generation: Generation,
    shape: androidx.compose.ui.graphics.Shape,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    thumbnailModifier: Modifier = Modifier,
) {
    val haptics = LocalHaptics.current
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .cardSolid(shape)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null) {
                haptics.fire(Haptic.TAP)
                onOpen(generation.id)
            }
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val thumbShape = RoundedCornerShape(20.dp)
        Box(
            modifier = thumbnailModifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
                .clip(thumbShape),
        ) {
            if (generation.isVideo) {
                VideoThumbnail(
                    generation = generation,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                AsyncImage(
                    model = generation.thumbFeedUrl(),
                    contentDescription = generation.prompt,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Text(
            text = generation.prompt,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Text(
            text = metaLine(generation),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
}

/** Échec : carte `errorContainer`, tap → réinjecte le prompt. */
@Composable
private fun FailedFeedCard(
    generation: Generation,
    shape: androidx.compose.ui.graphics.Shape,
    onReuse: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHaptics.current
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.errorContainer)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null) {
                haptics.fire(Haptic.SELECT)
                onReuse(generation.prompt)
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Échec — ${generation.error ?: "erreur inconnue"}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = generation.prompt,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Touchez pour réutiliser le prompt",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
        )
    }
}

/** Annulée : carte grisée, tap → réinjecte le prompt. */
@Composable
private fun CancelledFeedCard(
    generation: Generation,
    shape: androidx.compose.ui.graphics.Shape,
    onReuse: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val faded = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null) {
                haptics.fire(Haptic.SELECT)
                onReuse(generation.prompt)
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Annulée",
            style = MaterialTheme.typography.labelLarge,
            color = faded,
        )
        Text(
            text = generation.prompt,
            style = MaterialTheme.typography.bodySmall,
            color = faded,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Vignette vidéo muette : première frame via Coil + pastille lecture. Aucun lecteur ni audio dans le
 * feed (la lecture se fait dans la lightbox).
 */
@Composable
fun VideoThumbnail(
    generation: Generation,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = generation.thumbFeedUrl(),
            contentDescription = generation.prompt,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = "Vidéo",
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

/** « il y a … · N cr » (crédits omis si inconnus). */
private fun metaLine(generation: Generation): String {
    val time = timeAgo(generation.created)
    val credits = generation.creditsConsumed
    return if (credits != null) "$time · $credits cr" else time
}
