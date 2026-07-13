package com.wizycode.create.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wizycode.create.core.util.Haptic
import com.wizycode.create.rememberAppViewModelFactory
import com.wizycode.create.ui.common.IrisButton
import com.wizycode.create.ui.theme.Accent
import com.wizycode.create.ui.theme.IrisTextBrush
import com.wizycode.create.ui.theme.LocalHaptics
import com.wizycode.create.ui.theme.glassStrong

/**
 * Écran de connexion (CONTRACTS §4.2 / §4.4, DESIGN §5.11).
 *
 * Affiché par l'auth-gate racine (`CreateRoot`) tant que `AuthState == LoggedOut` — il vit **au-dessus**
 * de l'`AuroraBackground` déjà posée par la racine, donc n'a pas à peindre son propre fond. Le succès
 * n'entraîne aucune navigation impérative : `SessionManager.authState` bascule sur `LoggedIn` et la
 * racine remplace seule ce Login par les onglets.
 *
 * Rendu (DESIGN §5.11) : carte `glassStrong` `extraLarge` (34 dp) centrée sur l'aurora, wordmark
 * « Create. » (`displaySmall` Instrument Serif, mot en dégradé iris + point d'accent bleu), champs
 * identifiant / mot de passe (bord actif iris via `colorScheme.primary`), bouton plein-largeur peint
 * au **brush iris** ([IrisButton]) et erreur inline.
 *
 * Le `LoginViewModel` est câblé par le graphe d'activité ([rememberAppViewModelFactory]).
 */
@Composable
fun LoginScreen(modifier: Modifier = Modifier) {
    val viewModel: LoginViewModel = viewModel(factory = rememberAppViewModelFactory())
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalHaptics.current
    var passwordVisible by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary,
    )

    fun submit() {
        haptics.fire(Haptic.LAUNCH)
        viewModel.login()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .glassStrong(MaterialTheme.shapes.extraLarge)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Wordmark hero : « Create » peint en dégradé iris, point d'accent bleu (DESIGN §1.2).
            Text(
                text = buildAnnotatedString {
                    withStyle(MaterialTheme.typography.displaySmall.toSpanStyle().copy(brush = IrisTextBrush)) {
                        append("Create")
                    }
                    withStyle(MaterialTheme.typography.displaySmall.toSpanStyle().copy(color = Accent)) {
                        append(".")
                    }
                },
                style = MaterialTheme.typography.displaySmall,
            )

            Text(
                text = "Connecte-toi pour créer.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = state.identity,
                onValueChange = viewModel::onIdentityChange,
                label = { Text("Identifiant ou email") },
                singleLine = true,
                enabled = !state.submitting,
                isError = state.error != null,
                colors = fieldColors,
                shape = MaterialTheme.shapes.small,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Mot de passe") },
                singleLine = true,
                enabled = !state.submitting,
                isError = state.error != null,
                colors = fieldColors,
                shape = MaterialTheme.shapes.small,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Rounded.VisibilityOff
                            } else {
                                Icons.Rounded.Visibility
                            },
                            contentDescription = if (passwordVisible) {
                                "Masquer le mot de passe"
                            } else {
                                "Afficher le mot de passe"
                            },
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth(),
            )

            // Erreur inline : icône + texte (jamais une info par la seule couleur — CONTRACTS §5.10),
            // annoncée par TalkBack via `liveRegion`.
            if (state.error != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.height(18.dp),
                    )
                    Text(
                        text = state.error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Bouton primaire peint au brush iris (CONTRACTS §5 règle 2, DESIGN §5.11).
            // haptic = null : l'haptique LAUNCH est jouée par submit() une seule fois.
            IrisButton(
                onClick = ::submit,
                enabled = state.canSubmit,
                haptic = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (state.submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(text = "Se connecter", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
