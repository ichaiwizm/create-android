package com.wizycode.create.ui.lightbox

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wizycode.create.core.media.FileShare
import com.wizycode.create.core.media.MediaSaver
import com.wizycode.create.core.net.CreateApi
import com.wizycode.create.core.util.Haptic
import com.wizycode.create.core.util.timeAgo
import com.wizycode.create.data.GenerationRepository
import com.wizycode.create.ui.theme.LocalHaptics
import com.wizycode.create.ui.theme.Motion
import com.wizycode.create.ui.theme.glassStrong
import com.wizycode.create.ui.theme.rememberReduceMotion
import kotlinx.coroutines.launch

/**
 * Visionneuse plein écran (CONTRACTS §4.3/§4.4, DESIGN §5.10, NATIVE_SPEC §2.4).
 *
 * `Dialog` plein écran (`usePlatformDefaultWidth = false`) posé au-dessus des onglets, avec :
 * - scrim assombrissant modulé par le geste ;
 * - média centré : [ZoomableImage] (`ContentScale.Fit` + zoom/pan) ou [VideoPlayer] (ExoPlayer,
 *   contrôles + autoplay) ;
 * - **swipe-down** (`Modifier.draggable`, seuil 110 dp) → le média suit le doigt (translation +
 *   réduction d'échelle + fondu du scrim) puis se referme (retour visuel type shared-element) ;
 * - barre haute : bouton fermer + « {modèle} · {timeAgo} · {crédits}cr » ;
 * - panneau bas `glassStrong` `extraLarge` : prompt copiable + grille d'actions.
 *
 * Le ViewModel n'a pas de `Context` : il émet des [LightboxEffect] que cet écran exécute
 * (presse-papier, MediaStore, partage, snackbar, fermeture).
 *
 * @param generations cache partagé (fourni par la navigation depuis l'`AppContainer`).
 * @param api client réseau (idem) — passé au ViewModel pour les outils 1-clic.
 * @param onDismiss remonte à la navigation pour retirer la destination overlay.
 */
@Composable
fun LightboxScreen(
    generationId: String,
    generations: GenerationRepository,
    api: CreateApi,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LightboxViewModel = viewModel(
        key = "lightbox-$generationId",
        factory = LightboxViewModel.provideFactory(generationId, generations, api),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var dismissed by remember { mutableStateOf(false) }
    val requestDismiss: () -> Unit = {
        if (!dismissed) {
            dismissed = true
            onDismiss()
        }
    }

    // Génération disparue (supprimée ailleurs) → on referme proprement.
    LaunchedEffect(state.notFound) {
        if (state.notFound) requestDismiss()
    }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val ioScope = rememberCoroutineScope()

    // Exécution des effets one-shot (APIs liées au Context).
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LightboxEffect.CopyPrompt ->
                    clipboard.setText(AnnotatedString(effect.text))

                is LightboxEffect.Snackbar -> ioScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(effect.message)
                }

                is LightboxEffect.Share -> ioScope.launch {
                    val ok = runCatching { FileShare.share(context, effect.url, effect.isVideo) }.isSuccess
                    if (!ok) snackbarHostState.showSnackbar("Partage impossible")
                }

                is LightboxEffect.Save -> ioScope.launch {
                    val ok = runCatching {
                        if (effect.isVideo) MediaSaver.saveVideo(context, effect.url)
                        else MediaSaver.saveImage(context, effect.url)
                    }.isSuccess
                    snackbarHostState.showSnackbar(
                        if (ok) "Enregistré dans la galerie" else "Enregistrement impossible",
                    )
                }

                LightboxEffect.Dismiss -> requestDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = requestDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        LightboxContent(
            state = state,
            snackbarHostState = snackbarHostState,
            onClose = requestDismiss,
            onDismissByGesture = requestDismiss,
            onCopyPrompt = viewModel::copyPrompt,
            onShare = viewModel::share,
            onSave = viewModel::save,
            onUpscale = viewModel::upscale,
            onRemoveBg = viewModel::removeBg,
            onDelete = viewModel::delete,
            modifier = modifier,
        )
    }
}

@Composable
private fun LightboxContent(
    state: LightboxUiState,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
    onDismissByGesture: () -> Unit,
    onCopyPrompt: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onUpscale: () -> Unit,
    onRemoveBg: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHaptics.current
    val reduceMotion = rememberReduceMotion()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val thresholdPx = with(density) { DISMISS_THRESHOLD.toPx() }
    val dismissRefPx = thresholdPx * 3f
    val exitTargetPx = with(density) { 900.dp.toPx() }

    val offsetY = remember { Animatable(0f) }
    // Échelle de zoom courante de l'image (1f = pas de zoom) : gèle le swipe-de-fermeture si > 1.
    var mediaScale by remember { mutableFloatStateOf(1f) }
    val canDrag = state.generation != null && mediaScale <= 1.02f

    val dragState = rememberDraggableState { delta ->
        scope.launch { offsetY.snapTo((offsetY.value + delta).coerceAtLeast(0f)) }
    }

    Box(modifier = modifier.fillMaxSize()) {

        // Scrim assombrissant, fondu avec le geste.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = (offsetY.value / dismissRefPx).coerceIn(0f, 1f)
                    alpha = SCRIM_ALPHA * (1f - 0.6f * p)
                }
                .background(MaterialTheme.colorScheme.scrim),
        )

        // Média centré, suivant le doigt (translation + réduction d'échelle).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = (offsetY.value / dismissRefPx).coerceIn(0f, 1f)
                    translationY = offsetY.value
                    val s = if (reduceMotion) 1f else 1f - 0.14f * p
                    scaleX = s
                    scaleY = s
                }
                .then(
                    if (canDrag) {
                        Modifier.draggable(
                            state = dragState,
                            orientation = Orientation.Vertical,
                            onDragStopped = { velocity ->
                                if (offsetY.value > thresholdPx || velocity > FLING_VELOCITY) {
                                    offsetY.animateTo(exitTargetPx, Motion.spatialSpring)
                                    onDismissByGesture()
                                } else {
                                    offsetY.animateTo(0f, Motion.spatialSpring)
                                }
                            },
                        )
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            val gen = state.generation
            val mediaUrl = gen?.mediaUrls?.firstOrNull()
            when {
                gen != null && state.isVideo && mediaUrl != null ->
                    VideoPlayer(url = mediaUrl)

                gen != null && mediaUrl != null ->
                    ZoomableImage(
                        url = mediaUrl,
                        contentDescription = gen.prompt.ifBlank { "Média généré" },
                        onScaleChange = { mediaScale = it },
                    )

                state.loading ->
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        // Barre haute : fermer + métadonnées.
        TopBar(
            state = state,
            onClose = {
                haptics.fire(Haptic.TAP)
                onClose()
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    alpha = 1f - (offsetY.value / dismissRefPx).coerceIn(0f, 1f)
                },
        )

        // Panneau d'actions bas (verre fort, coins extraLarge).
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = 1f - (offsetY.value / dismissRefPx).coerceIn(0f, 1f)
                }
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .glassStrong(MaterialTheme.shapes.extraLarge)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            LightboxActionsPanel(
                state = state,
                onCopyPrompt = {
                    haptics.fire(Haptic.TAP)
                    onCopyPrompt()
                },
                onShare = {
                    haptics.fire(Haptic.TAP)
                    onShare()
                },
                onSave = {
                    haptics.fire(Haptic.TAP)
                    onSave()
                },
                onUpscale = {
                    haptics.fire(Haptic.LAUNCH)
                    onUpscale()
                },
                onRemoveBg = {
                    haptics.fire(Haptic.LAUNCH)
                    onRemoveBg()
                },
                onDelete = {
                    // 1er tap = TAP (arme la confirmation) ; 2e tap = ERROR (suppression).
                    haptics.fire(if (state.confirmingDelete) Haptic.ERROR else Haptic.TAP)
                    onDelete()
                },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 8.dp),
        ) { data ->
            Snackbar(snackbarData = data)
        }
    }
}

@Composable
private fun TopBar(
    state: LightboxUiState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gen = state.generation
    val label = remember(state.modelLabel, gen?.created, state.creditsConsumed) {
        buildList {
            if (state.modelLabel.isNotBlank()) add(state.modelLabel)
            gen?.created?.let { add(timeAgo(it)) }
            state.creditsConsumed?.let { add("${it}cr") }
        }.joinToString(" · ")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier.glassStrong(CircleShape),
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Fermer",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(Modifier.width(12.dp))

        if (label.isNotBlank()) {
            Box(
                modifier = Modifier
                    .glassStrong(CircleShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .semantics { contentDescription = label },
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private val DISMISS_THRESHOLD = 110.dp
private const val SCRIM_ALPHA = 0.62f
private const val FLING_VELOCITY = 2_400f
