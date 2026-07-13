package com.wizycode.create.core.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.wizycode.create.R

/**
 * Construit et affiche les notifications de complétion de génération (jalon M7,
 * CONTRACTS §3.8 + DESIGN §5.12).
 *
 * Canal unique `generations` (importance HIGH), petite icône monochrome
 * `ic_stat_notif`, couleur d'accent iris blue, deep-link `create://gallery`,
 * `tag = generationId` (ré-émettre pour le même id remplace la notif existante).
 *
 * Titres par défaut : « ✨ Ta création est prête » / « ❌ Génération échouée ».
 * Corps = prompt tronqué à 90 caractères.
 */
object NotificationHelper {

    /** Identifiant du canal de notification (DESIGN §5.12). */
    const val CHANNEL_ID = "generations"

    /** Deep-link ouvert au tap (CONTRACTS §3.8). */
    const val DEEP_LINK_GALLERY = "create://gallery"

    /** Titre affiché quand une génération réussit. */
    const val TITLE_SUCCESS = "✨ Ta création est prête"

    /** Titre affiché quand une génération échoue. */
    const val TITLE_FAILURE = "❌ Génération échouée"

    /** Couleur d'accent de la notif = iris blue (#3B82F6, DESIGN §2.1). */
    private const val ACCENT_IRIS_BLUE = 0xFF3B82F6.toInt()

    /** Longueur maximale du corps (prompt tronqué, NATIVE_SPEC §2.6). */
    private const val PROMPT_MAX_CHARS = 90

    private const val CHANNEL_NAME = "Générations"
    private const val CHANNEL_DESCRIPTION =
        "Notifie quand une création image ou vidéo est prête ou a échoué."

    /**
     * Crée (idempotent) le canal de notification `generations` en importance HIGH.
     * À appeler au démarrage (`CreateApp`) et de façon défensive avant chaque notif.
     */
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableLights(true)
            lightColor = ACCENT_IRIS_BLUE
            enableVibration(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Affiche une notification de complétion de génération.
     *
     * @param title   titre déjà formaté par le serveur, ou `null` → dérivé de [success].
     * @param body    corps (prompt), tronqué à 90 caractères ici de façon défensive.
     * @param tag     `generationId` : sert de tag de notif (dédoublonnage) et de cible deep-link.
     * @param success `true` → titre succès, `false` → titre échec (utilisé si [title] est nul).
     */
    fun notifyGeneration(
        context: Context,
        title: String?,
        body: String?,
        tag: String?,
        success: Boolean,
    ) {
        ensureChannel(context)

        // Permission runtime requise à partir d'API 33 (implicite en dessous).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val resolvedTitle = title?.takeIf { it.isNotBlank() }
            ?: if (success) TITLE_SUCCESS else TITLE_FAILURE
        val resolvedBody = truncatePrompt(body)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notif)
            .setColor(ACCENT_IRIS_BLUE)
            .setContentTitle(resolvedTitle)
            .setContentText(resolvedBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(resolvedBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(buildDeepLinkIntent(context, tag))
            .build()

        val notifId = (tag ?: DEEP_LINK_GALLERY).hashCode()
        NotificationManagerCompat.from(context).notify(tag, notifId, notification)
    }

    /** Construit le `PendingIntent` deep-link vers la galerie (create://gallery). */
    private fun buildDeepLinkIntent(context: Context, generationId: String?): PendingIntent {
        val uri = Uri.parse(DEEP_LINK_GALLERY).buildUpon().apply {
            if (!generationId.isNullOrBlank()) appendQueryParameter("generationId", generationId)
        }.build()

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (!generationId.isNullOrBlank()) putExtra("generationId", generationId)
        }

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        val requestCode = (generationId ?: DEEP_LINK_GALLERY).hashCode()
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }

    /** Tronque le prompt à 90 caractères (ellipse si dépassement). */
    private fun truncatePrompt(text: String?): String {
        val clean = text?.trim().orEmpty()
        if (clean.length <= PROMPT_MAX_CHARS) return clean
        return clean.take(PROMPT_MAX_CHARS - 1).trimEnd() + "…"
    }
}
