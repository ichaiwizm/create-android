package com.wizycode.create.ui.gallery

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.wizycode.create.core.util.timeAgo
import com.wizycode.create.data.model.GenStatus
import com.wizycode.create.data.model.Generation
import com.wizycode.create.data.model.isVideo
import com.wizycode.create.data.model.thumbGridUrl
import com.wizycode.create.ui.theme.IrisBlue
import com.wizycode.create.ui.theme.cardSolid
import com.wizycode.create.ui.theme.pressScale
import com.wizycode.create.ui.theme.shimmer

/**
 * Carte carrée de la grille galerie (DESIGN §5.8).
 *
 * `cardSolid` (JAMAIS de blur — cartes répétées en scroll), forme `medium` 22 dp. Le rendu dépend
 * du [Generation.status] :
 * - **DONE** : miniature `?thumb=600x600` (ou vidéo avec badge lecture) + prompt 1 ligne · modèle ·
 *   timeAgo en surimpression ; tap → lightbox (shared element).
 * - **PENDING** : shimmer + progress wavy iris + bouton ✕ (annuler).
 * - **FAILED** : fond `errorContainer`, message rouge (icône + texte — jamais la couleur seule).
 * - **CANCELLED** : carte grisée `surfaceVariant`, libellé « Annulée ».
 *
 * @param onOpen invoqué au tap d'une carte `DONE` (ouverture lightbox).
 * @param onCancel invoqué au tap du ✕ d'une carte `PENDING`.
 * @param sharedTransitionScope / @param animatedVisibilityScope fournis par le
 *   `SharedTransitionLayout` racine (CONTRACTS §4.2) ; si `null`, la carte s'affiche sans transition
 *   partagée (aucune régression).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryCard(
    generation: Generation,
    onOpen: (Generation) -> Unit,
    onCancel: (String) -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val shape = MaterialTheme.shapes.medium
    val base = modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .cardSolid(shape)
        .clip(shape)

    when (generation.status) {
        GenStatus.DONE -> DoneCard(
            generation = generation,
            shape = shape,
            modifier = base,
            onOpen = onOpen,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
        )

        GenStatus.PENDING -> PendingCard(
            generation = generation,
            modifier = base,
            onCancel = onCancel,
        )

        GenStatus.FAILED -> FailedCard(generation = generation, modifier = base)

        GenStatus.CANCELLED -> CancelledCard(generation = generation, modifier = base)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun DoneCard(
    generation: Generation,
    shape: Shape,
    modifier: Modifier,
    onOpen: (Generation) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) {
    val interaction = remember { MutableInteractionSource() }

    // Modifier de transition partagée appliqué à la miniature (galerie ↔ lightbox).
    val sharedModifier: Modifier =
        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            with(sharedTransitionScope) {
                Modifier.sharedElement(
                    rememberSharedContentState(key = GallerySharedKeys.media(generation.id)),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        } else {
            Modifier
        }

    Box(
        modifier = modifier
            .pressScale(interaction)
            .clickableCard(interaction, label = "Ouvrir la génération") { onOpen(generation) },
    ) {
        AsyncImage(
            model = generation.thumbGridUrl(),
            contentDescription = generation.prompt.ifBlank { "Média généré" },
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .then(sharedModifier),
        )

        // Badge lecture pour les vidéos.
        if (generation.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(46.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.Black.copy(alpha = 0.42f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Vidéo",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        // Voile bas + métadonnées lisibles par-dessus le média.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.62f),
                    ),
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Column {
                Text(
                    text = generation.prompt.ifBlank { modelDisplayName(generation.model) },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${modelDisplayName(generation.model)} · ${timeAgo(generation.created)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun PendingCard(
    generation: Generation,
    modifier: Modifier,
    onCancel: (String) -> Unit,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .shimmer(active = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                color = IrisBlue,
                modifier = Modifier.size(34.dp),
            )
            Text(
                text = generation.prompt.ifBlank { modelDisplayName(generation.model) },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        // Annulation (haptique ERROR déclenchée par l'appelant).
        FilledTonalIconButton(
            onClick = { onCancel(generation.id) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(36.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            ),
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Annuler la génération",
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun FailedCard(generation: Generation, modifier: Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = failureMessage(generation),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun CancelledCard(generation: Generation, modifier: Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .graphicsLayer { alpha = 0.7f },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Annulée",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = generation.prompt.ifBlank { modelDisplayName(generation.model) },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
