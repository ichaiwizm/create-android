package com.wizycode.create.ui.common

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.firebase.messaging.FirebaseMessaging
import com.wizycode.create.core.net.CreateApi
import com.wizycode.create.core.net.dto.FcmTokenRequest
import com.wizycode.create.core.util.Haptic
import com.wizycode.create.ui.theme.LocalHaptics
import com.wizycode.create.ui.theme.LocalIrisBrushes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * États possibles de la cloche de notifications (DESIGN §5.12).
 *
 * - [UNSUPPORTED] : notifications indisponibles (bouton désactivé).
 * - [DENIED] : refusées / coupées au niveau système (tap → réglages de l'app).
 * - [AVAILABLE] : disponibles mais non abonné (tap → demande la permission puis s'abonne).
 * - [SUBSCRIBED] : abonné et permission accordée (glyphe + **tint iris**).
 */
enum class NotifBellState { UNSUPPORTED, DENIED, AVAILABLE, SUBSCRIBED }

/**
 * Cloche de notifications **connectée** (CONTRACTS §4.4, DESIGN §5.12).
 *
 * Gère la permission runtime `POST_NOTIFICATIONS` (API 33+) et l'inscription **FCM** :
 * - Tap en [NotifBellState.AVAILABLE] → demande la permission (33+) ; si accordée (ou < 33) récupère
 *   le token FCM et l'enregistre via `CreateApi.registerFcm` (route `POST /api/push/register-fcm`).
 * - Tap en [NotifBellState.SUBSCRIBED] → se désabonne localement.
 * - Tap en [NotifBellState.DENIED] → ouvre les réglages de notifications de l'app.
 *
 * L'état d'abonnement est persisté en `SharedPreferences` ; l'état système est réévalué à chaque
 * `ON_RESUME` (l'utilisateur peut couper les notifications depuis les réglages).
 */
@Composable
fun NotifBell(
    api: CreateApi,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalHaptics.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val prefs = remember(context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    var subscribedPref by remember { mutableStateOf(prefs.getBoolean(KEY_SUBSCRIBED, false)) }
    var hasPermission by remember { mutableStateOf(hasNotifPermission(context)) }
    var systemEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }

    // Réévalue l'état système au retour au premier plan.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = hasNotifPermission(context)
                systemEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        systemEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (granted) {
            subscribedPref = true
            prefs.edit().putBoolean(KEY_SUBSCRIBED, true).apply()
            registerFcmToken(api, scope)
        }
    }

    val enabled = hasPermission && systemEnabled
    val state = when {
        enabled && subscribedPref -> NotifBellState.SUBSCRIBED
        enabled -> NotifBellState.AVAILABLE
        // Permission accordée mais notifications coupées côté système → refus explicite.
        hasPermission && !systemEnabled -> NotifBellState.DENIED
        // < 33 : pas de permission runtime ; > = 33 non accordée → on peut encore demander.
        else -> NotifBellState.AVAILABLE
    }

    NotifBell(
        state = state,
        onClick = {
            haptics.fire(Haptic.SELECT)
            when (state) {
                NotifBellState.SUBSCRIBED -> {
                    subscribedPref = false
                    prefs.edit().putBoolean(KEY_SUBSCRIBED, false).apply()
                }

                NotifBellState.AVAILABLE -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        subscribedPref = true
                        prefs.edit().putBoolean(KEY_SUBSCRIBED, true).apply()
                        registerFcmToken(api, scope)
                    }
                }

                NotifBellState.DENIED -> openAppNotificationSettings(context)
                NotifBellState.UNSUPPORTED -> Unit
            }
        },
        modifier = modifier,
    )
}

/**
 * Cloche **présentationnelle** : `IconButton` dont le glyphe et la teinte reflètent [state].
 * L'état [NotifBellState.SUBSCRIBED] est peint en **iris** ; les autres en `onSurfaceVariant`.
 * Chaque état porte une `contentDescription` distincte (a11y — info jamais par la seule couleur).
 */
@Composable
fun NotifBell(
    state: NotifBellState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brushes = LocalIrisBrushes.current
    val icon = when (state) {
        NotifBellState.SUBSCRIBED -> Icons.Rounded.NotificationsActive
        NotifBellState.DENIED, NotifBellState.UNSUPPORTED -> Icons.Rounded.NotificationsOff
        NotifBellState.AVAILABLE -> Icons.Rounded.Notifications
    }
    val description = when (state) {
        NotifBellState.SUBSCRIBED -> "Notifications activées"
        NotifBellState.AVAILABLE -> "Activer les notifications"
        NotifBellState.DENIED -> "Notifications refusées, ouvrir les réglages"
        NotifBellState.UNSUPPORTED -> "Notifications indisponibles"
    }

    IconButton(
        onClick = onClick,
        enabled = state != NotifBellState.UNSUPPORTED,
        modifier = modifier,
    ) {
        if (state == NotifBellState.SUBSCRIBED) {
            IrisIcon(imageVector = icon, contentDescription = description, brush = brushes.fill)
        } else {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val PREFS_NAME = "create_notifications"
private const val KEY_SUBSCRIBED = "subscribed"

/** `true` si l'app peut poster des notifications (permission runtime accordée, ou < API 33). */
private fun hasNotifPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

/**
 * Récupère le token FCM courant et l'enregistre auprès du backend. Non bloquant : toute erreur
 * (utilisateur non connecté, réseau, Play services absents) est ignorée silencieusement.
 */
private fun registerFcmToken(api: CreateApi, scope: CoroutineScope) {
    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
        scope.launch {
            try {
                api.registerFcm(FcmTokenRequest(token))
            } catch (_: Throwable) {
                // Enregistrement best-effort.
            }
        }
    }
}

/** Ouvre l'écran système des notifications de l'app (fallback quand elles sont coupées). */
private fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: Throwable) {
        // Certains OEM peuvent ne pas résoudre l'intent : on n'échoue pas.
    }
}
