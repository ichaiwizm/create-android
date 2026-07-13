package com.wizycode.create.ui.create

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.wizycode.create.core.util.Haptic
import com.wizycode.create.data.catalog.ModelCatalog
import com.wizycode.create.data.model.Generation
import com.wizycode.create.ui.common.TopBar
import com.wizycode.create.ui.common.UiEvent
import com.wizycode.create.ui.theme.LocalHaptics

/**
 * Écran cœur « Créer » (CONTRACTS §4.4 / DESIGN §5.3-5.7).
 *
 * Compose le feed inversé (ou l'état vide), le composer fixe bas et les deux bottom sheets
 * (Modèle / Réglages) pilotées par un état local. Gère le chargement initial, la boucle de polling
 * sous `repeatOnLifecycle(STARTED)` et l'affichage des événements one-shot ([UiEvent]).
 *
 * @param viewModel ViewModel de l'écran (câblé par le graphe d'activité).
 * @param onOpenLightbox ouverture de la lightbox pour la génération tapée (id).
 * @param onOpenSettings ouverture de l'écran Réglages (thème / compte).
 * @param thumbnailModifier hook shared-element par génération, fourni par la couche navigation.
 */
@Composable
fun CreateScreen(
    viewModel: CreateViewModel,
    onOpenLightbox: (String) -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    thumbnailModifier: (Generation) -> Modifier = { Modifier },
) {
    val composer by viewModel.composer.collectAsStateWithLifecycle()
    val feed by viewModel.feed.collectAsStateWithLifecycle()
    val recording by viewModel.recording.collectAsStateWithLifecycle()
    val haptics = LocalHaptics.current
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    var showModelSheet by rememberSaveable { mutableStateOf(false) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) { viewModel.reload() }

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.runPolling()
        }
    }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Snackbar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.Error -> {
                    haptics.fire(Haptic.ERROR)
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.Navigate -> onOpenLightbox(event.route)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                actions = {
                    IconButton(
                        onClick = {
                            haptics.fire(Haptic.TAP)
                            onOpenSettings()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Réglages",
                        )
                    }
                },
            )
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (feed.isEmpty()) {
                    EmptyState(
                        onSuggestion = viewModel::onPromptChange,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Feed(
                        items = feed,
                        onOpen = onOpenLightbox,
                        onReuse = viewModel::reuse,
                        onCancel = viewModel::cancel,
                        modifier = Modifier.fillMaxSize(),
                        thumbnailModifier = thumbnailModifier,
                    )
                }
            }
            Composer(
                state = composer,
                recording = recording,
                onPromptChange = viewModel::onPromptChange,
                onAddRef = viewModel::addRef,
                onRemoveRef = viewModel::removeRef,
                onSend = viewModel::send,
                onOpenModelSheet = { showModelSheet = true },
                onOpenSettingsSheet = { showSettingsSheet = true },
                onStartRecording = viewModel::startRecording,
                onStopRecording = viewModel::stopRecordingAndTranscribe,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 96.dp, start = 12.dp, end = 12.dp),
        )
    }

    if (showModelSheet) {
        ModelSheet(
            mode = composer.mode,
            activeFamilyKey = composer.familyKey,
            onSetMode = viewModel::setMode,
            onPickModel = viewModel::pickModel,
            onDismiss = { showModelSheet = false },
        )
    }

    if (showSettingsSheet) {
        val family = ModelCatalog.family(composer.familyKey)
        if (family != null) {
            val variant = family.variants?.firstOrNull { it.key == composer.variantKey }
            SettingsSheet(
                family = family,
                variant = variant,
                paramValues = composer.paramValues,
                editing = composer.editing,
                onPickVariant = viewModel::pickVariant,
                onSetParam = viewModel::setParam,
                onDismiss = { showSettingsSheet = false },
            )
        }
    }
}
