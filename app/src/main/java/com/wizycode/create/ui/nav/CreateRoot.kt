package com.wizycode.create.ui.nav

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wizycode.create.CreateApp
import com.wizycode.create.rememberAppViewModelFactory
import com.wizycode.create.data.model.AuthState
import com.wizycode.create.ui.common.CreateBottomBar
import com.wizycode.create.ui.create.CreateScreen
import com.wizycode.create.ui.create.CreateViewModel
import com.wizycode.create.ui.gallery.GalleryScreen
import com.wizycode.create.ui.gallery.GalleryViewModel
import com.wizycode.create.ui.lightbox.LightboxScreen
import com.wizycode.create.ui.login.LoginScreen
import com.wizycode.create.ui.settings.SettingsScreen
import com.wizycode.create.ui.settings.SettingsViewModel
import com.wizycode.create.ui.theme.AuroraBackground
import com.wizycode.create.ui.theme.Motion

/**
 * Racine de l'application (CONTRACTS §4.2). Structure :
 *
 * ```
 * Box
 *  ├─ AuroraBackground            // fond statique, derrière tout (DESIGN §2.5)
 *  └─ auth-gate sur AuthState :
 *       • Unknown   → splash discret (ni login ni onglets)
 *       • LoggedOut → LoginScreen
 *       • LoggedIn  → Scaffold + CreateBottomBar (2 onglets flottants) + NavHost
 * ```
 *
 * La transition partagée carte → lightbox est portée par un [SharedTransitionLayout] qui englobe
 * le `NavHost` : la lightbox est une **destination overlay plein écran** (et non un `Dialog`), ce
 * qui est la condition pour partager un élément entre la miniature du feed/galerie et le média
 * plein écran (CONTRACTS §4.2 prime sur la mention `Dialog` de DESIGN §5.10).
 *
 * Les bottom sheets `ModelSheet` / `SettingsSheet` ne sont **pas** des destinations : elles sont
 * pilotées par l'état local du `CreateViewModel` (CONTRACTS §4.2). Seuls le login, les deux onglets,
 * l'écran Réglages et la lightbox transitent par ce routeur.
 *
 * `MaterialExpressiveTheme`, l'edge-to-edge et le dynamic color sont appliqués en amont par
 * `CreateTheme` / `MainActivity` (CONTRACTS §2.9 / §4.1).
 */
@Composable
fun CreateRoot() {
    val container = (LocalContext.current.applicationContext as CreateApp).container
    val rootViewModel: RootViewModel = viewModel(factory = RootViewModel.factory(container.session))
    val authState by rootViewModel.authState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        // Fond aurora unique, posé derrière tout le contenu. Ne s'anime jamais.
        AuroraBackground(modifier = Modifier.fillMaxSize())

        when (authState) {
            is AuthState.Unknown -> BootSplash(modifier = Modifier.fillMaxSize())
            is AuthState.LoggedOut -> LoginScreen(modifier = Modifier.fillMaxSize())
            is AuthState.LoggedIn -> MainScaffold(modifier = Modifier.fillMaxSize())
        }
    }
}

/**
 * Écran d'attente pendant la résolution de la session (`AuthState.Unknown`). Volontairement sobre :
 * uniquement un indicateur centré sur l'aurora, conformément à « on n'affiche ni login ni onglets ».
 */
@Composable
private fun BootSplash(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

/**
 * Coquille des sessions authentifiées : barre de navigation basse flottante + `NavHost` des deux
 * onglets, de l'écran Réglages et de la lightbox overlay, le tout enveloppé dans le
 * [SharedTransitionLayout] pour la transition partagée carte → lightbox.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MainScaffold(modifier: Modifier = Modifier) {
    val container = (LocalContext.current.applicationContext as CreateApp).container
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTab = TopTab.fromRoute(currentRoute)

    Scaffold(
        modifier = modifier,
        // L'aurora vit derrière le Scaffold : conteneur transparent.
        containerColor = Color.Transparent,
        // Edge-to-edge : chaque écran gère ses propres insets (status bar, ime, navigation bar).
        // Ainsi la lightbox occupe réellement tout l'écran quand la barre basse disparaît.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // La barre n'apparaît que sur les destinations onglets ; masquée sur la lightbox
            // (overlay plein écran), l'écran Réglages et le splash.
            if (currentTab != null) {
                CreateBottomBar(
                    selected = currentTab,
                    onSelect = { tab ->
                        if (tab.route != currentRoute) {
                            navController.navigate(tab.route) {
                                // Bascule d'onglet idempotente avec préservation d'état (back-stack unique).
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Routes.CREATE,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(route = Routes.CREATE) {
                    val createViewModel: CreateViewModel =
                        viewModel(factory = rememberAppViewModelFactory())
                    CreateScreen(
                        viewModel = createViewModel,
                        onOpenLightbox = { id -> navController.navigate(Routes.lightbox(id)) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    )
                }

                composable(route = Routes.GALLERY) {
                    val galleryViewModel: GalleryViewModel =
                        viewModel(factory = rememberAppViewModelFactory())
                    GalleryScreen(
                        viewModel = galleryViewModel,
                        onOpen = { generation -> navController.navigate(Routes.lightbox(generation.id)) },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this,
                    )
                }

                composable(
                    route = Routes.LIGHTBOX,
                    arguments = listOf(
                        navArgument(Routes.LIGHTBOX_ARG) { type = NavType.StringType },
                    ),
                    // La continuité visuelle est portée par le shared element ; le reste fond en douceur.
                    enterTransition = { fadeIn(tween(Motion.DUR_STD)) },
                    exitTransition = { fadeOut(tween(Motion.DUR_FAST)) },
                ) { entry ->
                    val generationId = entry.arguments?.getString(Routes.LIGHTBOX_ARG).orEmpty()
                    LightboxScreen(
                        generationId = generationId,
                        generations = container.generations,
                        api = container.api,
                        onDismiss = { navController.popBackStack() },
                    )
                }

                composable(route = Routes.SETTINGS) {
                    val settingsViewModel: SettingsViewModel =
                        viewModel(factory = rememberAppViewModelFactory())
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
