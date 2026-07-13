package com.wizycode.create.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wizycode.create.R
import com.wizycode.create.data.prefs.ThemeMode
import com.wizycode.create.core.util.Haptic
import com.wizycode.create.ui.theme.LocalHaptics
import com.wizycode.create.ui.theme.glassStrong

/**
 * Écran Réglages natif (CONTRACTS §4.3, DESIGN §5 / §7). Accessible via l'overflow de la TopBar.
 *
 * Sur fond aurora (fourni par `CreateRoot`), présente deux blocs de verre ([Modifier.glassStrong]) :
 * - **Apparence** : sélecteur de thème Système / Clair / Sombre + interrupteur Couleurs dynamiques
 *   (visible uniquement en API 31+ ; l'iris survit toujours, DESIGN §2.4).
 * - **Compte** : bouton Déconnexion → [SettingsViewModel.logout] → retour Login via l'auth-gate.
 *
 * Écran purement piloté par l'état : chaque interaction délègue au [SettingsViewModel] qui persiste
 * via `SettingsStore` (DataStore). Une seule haptique par action utilisateur (CONTRACTS §5).
 *
 * @param viewModel ViewModel de l'écran (construit via [SettingsViewModel.factory]).
 * @param onBack remontée de navigation (flèche retour de la TopBar).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalHaptics.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        // Transparent : laisse transparaître l'aurora posée à la racine (CONTRACTS §4.2).
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            haptics.fire(Haptic.TAP)
                            onBack()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AppearanceSection(
                themeMode = state.themeMode,
                dynamicColor = state.dynamicColor,
                onThemeModeChange = { mode ->
                    haptics.fire(Haptic.SELECT)
                    viewModel.setThemeMode(mode)
                },
                onDynamicColorChange = { enabled ->
                    haptics.fire(Haptic.SELECT)
                    viewModel.setDynamicColor(enabled)
                },
            )

            AccountSection(
                onLogout = {
                    haptics.fire(Haptic.TAP)
                    viewModel.logout()
                },
            )

            Spacer(Modifier.size(8.dp))
        }
    }
}

/* ------------------------------------------------------------------------------------------------
 * Sections
 * ---------------------------------------------------------------------------------------------- */

/** Bloc « Apparence » : thème + couleurs dynamiques (verre `glassStrong`, DESIGN §3 / §5). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSection(
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassStrong(shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_appearance),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // --- Thème : Système / Clair / Sombre --------------------------------------------------
        Text(
            text = stringResource(R.string.settings_theme),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val modes = ThemeMode.entries
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = mode == themeMode,
                    onClick = { onThemeModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                ) {
                    Text(text = stringResource(mode.labelRes()))
                }
            }
        }

        // --- Couleurs dynamiques : API 31+ uniquement (l'iris survit toujours) -----------------
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow(
                title = stringResource(R.string.settings_dynamic_color),
                subtitle = stringResource(R.string.settings_dynamic_color_desc),
                checked = dynamicColor,
                onCheckedChange = onDynamicColorChange,
            )
        }
    }
}

/** Bloc « Compte » : action de déconnexion (verre `glassStrong`). */
@Composable
private fun AccountSection(
    onLogout: () -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassStrong(shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_account),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        FilledTonalButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(text = stringResource(R.string.settings_logout))
        }
    }
}

/* ------------------------------------------------------------------------------------------------
 * Composables communs
 * ---------------------------------------------------------------------------------------------- */

/**
 * Rangée titre + sous-titre + [Switch]. Toute la rangée est basculable (cible ≥ 48 dp, a11y §8) :
 * le `Switch` n'a pas de handler propre — c'est la rangée qui porte la sémantique `Role.Switch`.
 */
@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // La rangée porte déjà l'état bascule : on neutralise la sémantique du Switch pour éviter
        // une double annonce TalkBack.
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

/** Libellé localisé du mode de thème (Système / Clair / Sombre). */
private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}
