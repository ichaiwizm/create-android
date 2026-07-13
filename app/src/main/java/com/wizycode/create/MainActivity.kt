package com.wizycode.create

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wizycode.create.data.prefs.ThemeMode
import com.wizycode.create.ui.nav.CreateRoot
import com.wizycode.create.ui.theme.CreateTheme

/**
 * Intent courant de l'Activity (initial + `onNewIntent`), exposé à l'arbre Compose pour le routage
 * des deep-links `create://gallery`. `CreateRoot` le consomme pour naviguer vers la galerie / une
 * génération (`generationId` en extra) quand l'app est déjà lancée (`launchMode="singleTop"`), le
 * `NavHost` consommant l'intent initial de lui-même à la première composition.
 */
val LocalDeepLinkIntent = staticCompositionLocalOf<Intent?> { null }

/**
 * Unique Activity de l'app (CONTRACTS §4.1).
 *
 * - `enableEdgeToEdge()` avec barres **transparentes** (status + navigation) : l'aurora peint jusque
 *   sous les barres système.
 * - Le contraste des icônes système suit le thème **résolu** (indépendant du système quand
 *   l'utilisateur force clair/sombre) : icônes sombres en thème clair, claires en thème sombre.
 * - `setContent { CreateTheme(darkTheme, dynamicColor) { CreateRoot() } }`, `darkTheme` et
 *   `dynamicColor` provenant du [com.wizycode.create.data.prefs.SettingsStore].
 * - Deep-links `create://gallery` fournis à l'arbre via [LocalDeepLinkIntent].
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Barres transparentes ; scrim automatique seulement < API 29 (nav bar 3 boutons).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        val container = (application as CreateApp).container

        setContent {
            // Intent courant : mis à jour à chaque nouveau deep-link reçu (singleTop).
            var deepLinkIntent by remember { mutableStateOf(intent) }
            DisposableEffect(Unit) {
                val listener = Consumer<Intent> { newIntent -> deepLinkIntent = newIntent }
                addOnNewIntentListener(listener)
                onDispose { removeOnNewIntentListener(listener) }
            }

            val themeMode by container.settings.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val dynamicColor by container.settings.dynamicColor
                .collectAsStateWithLifecycle(initialValue = false)

            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Contraste des icônes de barres système aligné sur le thème résolu.
            LaunchedEffect(darkTheme) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }

            CompositionLocalProvider(LocalDeepLinkIntent provides deepLinkIntent) {
                CreateTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
                    CreateRoot()
                }
            }
        }
    }
}
