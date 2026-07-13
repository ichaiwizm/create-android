package com.wizycode.create.ui.gallery

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wizycode.create.core.util.Haptic
import com.wizycode.create.data.model.Generation
import com.wizycode.create.ui.theme.IrisBlue
import com.wizycode.create.ui.theme.LocalHaptics

/**
 * Écran Galerie (DESIGN §5.8, CONTRACTS §4.3/§4.4).
 *
 * Grille 2 colonnes de **toutes** les générations (tri `-created`), bandeau « N en cours » avec
 * `LinearWavyProgressIndicator` iris, pull-to-refresh teinté iris, poll foreground 4 s piloté par le
 * cycle de vie. Tap sur une carte `DONE` → lightbox (shared element).
 *
 * @param onOpen ouvre la lightbox pour la génération donnée (nav gérée par l'appelant).
 * @param sharedTransitionScope / @param animatedVisibilityScope fournis par le
 *   `SharedTransitionLayout` racine (CONTRACTS §4.2), transmis aux cartes pour l'animation partagée.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onOpen: (Generation) -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalHaptics.current

    // Poll foreground actif tant que l'écran est démarré (STARTED), coupé sinon (CONTRACTS §3.7).
    LifecycleStartEffect(viewModel) {
        viewModel.startPolling()
        onStopOrDispose { viewModel.stopPolling() }
    }

    // Un seul retour haptique par batch de transitions (SUCCESS/ERROR relayés par le repository).
    LaunchedEffect(viewModel) {
        viewModel.hapticEvents.collect { haptics.fire(it) }
    }

    val onCancel: (String) -> Unit = { id ->
        haptics.fire(Haptic.ERROR) // geste utilisateur (une seule haptique)
        viewModel.cancel(id)
    }

    val pullState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = viewModel::refresh,
        state = pullState,
        modifier = modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = state.refreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                color = IrisBlue,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        },
    ) {
        GalleryGrid(
            state = state,
            onOpen = onOpen,
            onCancel = onCancel,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun GalleryGrid(
    state: GalleryUiState,
    onOpen: (Generation) -> Unit,
    onCancel: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
) {
    val layoutDirection = LocalLayoutDirection.current
    val safe = WindowInsets.safeDrawing.asPaddingValues()
    val contentPadding = androidx.compose.foundation.layout.PaddingValues(
        start = 12.dp + safe.calculateStartPadding(layoutDirection),
        end = 12.dp + safe.calculateEndPadding(layoutDirection),
        top = 12.dp,
        bottom = 24.dp + safe.calculateBottomPadding(),
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Bandeau « N en cours » (pleine largeur), seulement s'il reste des PENDING.
        if (state.pendingCount > 0) {
            item(key = "pending-banner", span = { GridItemSpan(maxLineSpan) }) {
                PendingBanner(count = state.pendingCount)
            }
        }

        if (state.isEmpty) {
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                EmptyState()
            }
        }

        items(
            items = state.items,
            key = { it.id },
        ) { generation ->
            GalleryCard(
                generation = generation,
                onOpen = onOpen,
                onCancel = onCancel,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    }
}

@Composable
private fun PendingBanner(count: Int) {
    val label = if (count > 1) "$count générations en cours" else "1 génération en cours"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        LinearProgressIndicator(
            color = IrisBlue,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 120.dp, start = 24.dp, end = 24.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Rien à montrer… pour l'instant",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Vos images et vidéos générées apparaîtront ici.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
