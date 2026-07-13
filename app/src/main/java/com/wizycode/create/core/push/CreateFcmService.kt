package com.wizycode.create.core.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import com.wizycode.create.CreateApp
import com.wizycode.create.core.net.dto.FcmTokenRequest

/**
 * Service Firebase Cloud Messaging de l'app Create (jalon M7, CONTRACTS §3.7/§3.8 + §4.6).
 *
 * - `onNewToken` : enregistre le token auprès du backend via `CreateApi.registerFcm`
 *   (route `POST /api/push/register-fcm`, ajout serveur §4.6). Non bloquant : ignoré
 *   silencieusement si l'utilisateur n'est pas connecté ou en cas d'erreur réseau.
 * - `onMessageReceived` : construit la notification via [NotificationHelper] (canal
 *   `generations`, deep-link `create://gallery`, tag = `generationId`).
 *
 * Le push FCM est le mécanisme de réveil **background** (le poll 4 s n'est utilisé qu'au
 * premier plan, CONTRACTS §3.7).
 */
class CreateFcmService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val container = (application as? CreateApp)?.container ?: return
        // On n'enregistre le token que si une session existe (route auth user).
        if (container.session.currentToken() == null) return
        scope.launch {
            try {
                container.api.registerFcm(FcmTokenRequest(token = token))
            } catch (_: Exception) {
                // Non bloquant : le token sera re-tenté au prochain onNewToken / bootstrap.
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val notification = message.notification

        // Le serveur envoie un data-message (title/body/tag/url) ; on tolère aussi un
        // notification-message classique (champ `notification`).
        val title = notification?.title ?: data["title"]
        val body = notification?.body ?: data["body"] ?: data["prompt"]
        val tag = data["tag"] ?: data["generationId"]
        val success = resolveSuccess(data)

        NotificationHelper.notifyGeneration(
            context = this,
            title = title,
            body = body,
            tag = tag,
            success = success,
        )
    }

    /**
     * Déduit si la génération a réussi. Priorité au champ explicite `status`
     * (`done`/`failed`/`cancelled`), sinon au titre (préfixe ❌), défaut succès.
     */
    private fun resolveSuccess(data: Map<String, String>): Boolean {
        data["status"]?.lowercase()?.let { status ->
            return status == "done" || status == "success"
        }
        val title = data["title"].orEmpty()
        return !title.contains("❌") && !title.contains("échou", ignoreCase = true)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
