package com.wizycode.create.core.util

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Vocabulaire haptique de Create (CONTRACTS §2.8, DESIGN §6.3).
 *
 * | Token   | Déclencheur                       | Effet Android                          |
 * |---------|-----------------------------------|----------------------------------------|
 * | TAP     | tous boutons, copier prompt       | CONTEXT_CLICK (léger)                  |
 * | SELECT  | changement modèle, toggle, onglet | SEGMENT_TICK / CLOCK_TICK              |
 * | LAUNCH  | appui Générer                     | CONFIRM (ou one-shot 15 ms)            |
 * | SUCCESS | génération prête, transcript reçu | VibrationEffect.EFFECT_HEAVY_CLICK     |
 * | ERROR   | échec, annulation, delete         | waveform [0, 20, 60, 20]               |
 */
enum class Haptic { TAP, SELECT, LAUNCH, SUCCESS, ERROR }

/**
 * Abstraction haptique injectée dans le graphe (AppContainer) et exposée à Compose via
 * `LocalHaptics` (fourni par le design system — voir CONTRACTS §2.9). Les modules appellent
 * uniquement `fire(_)` / `prepare(_)` ; **au plus une haptique par action utilisateur**.
 */
interface Haptics {
    /** Joue immédiatement le retour haptique associé à [h] (respecte le réglage système). */
    fun fire(h: Haptic)

    /**
     * Prépare / réchauffe le canal haptique de [h] pour réduire la latence du prochain [fire]
     * (résolution paresseuse du vibrateur). Sans effet audible.
     */
    fun prepare(h: Haptic)
}

/**
 * Implémentation figée (CONTRACTS §2.8) :
 * - TAP / SELECT / LAUNCH → passent par `LocalHapticFeedback` (Compose) **quand disponible**,
 *   sinon repli vibrateur.
 * - SUCCESS / ERROR → toujours `VibratorManager` (API 31+) / `Vibrator` + `VibrationEffect`
 *   (API 26) car aucun `HapticFeedbackType` ne les représente fidèlement.
 *
 * Le pont Compose est fourni par `CreateTheme` qui appelle [bindComposeHaptic] avec
 * `LocalHapticFeedback.current`. Tant qu'il n'est pas lié, tout passe par le vibrateur : l'app
 * reste fonctionnelle avant/hors composition.
 *
 * Ne dépend d'aucun module `ui` (seulement de la lib `androidx.compose.ui.hapticfeedback`).
 */
class HapticsImpl(context: Context) : Haptics {

    private val appContext: Context = context.applicationContext

    @Volatile
    private var composeHaptic: HapticFeedback? = null

    private val vibrator: Vibrator? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { resolveVibrator() }

    /**
     * Lie (ou délie avec `null`) le retour haptique Compose courant. Appelé par `CreateTheme`
     * dans un `SideEffect` : `haptics.bindComposeHaptic(LocalHapticFeedback.current)`.
     */
    fun bindComposeHaptic(feedback: HapticFeedback?) {
        composeHaptic = feedback
    }

    override fun prepare(h: Haptic) {
        // Résout paresseusement le vibrateur (réchauffe le service) ; aucune vibration émise.
        if (h == Haptic.SUCCESS || h == Haptic.ERROR || composeHaptic == null) {
            vibrator?.hasVibrator()
        }
    }

    override fun fire(h: Haptic) {
        when (h) {
            Haptic.TAP -> firePreferCompose(HapticFeedbackType.ContextClick) {
                vibrateOneShot(durationMs = 8, amplitude = LIGHT_AMPLITUDE)
            }
            Haptic.SELECT -> firePreferCompose(HapticFeedbackType.SegmentTick) {
                vibratePredefinedOrOneShot(VibrationEffect.EFFECT_TICK, fallbackMs = 10)
            }
            Haptic.LAUNCH -> firePreferCompose(HapticFeedbackType.Confirm) {
                vibrateOneShot(durationMs = 15, amplitude = VibrationEffect.DEFAULT_AMPLITUDE)
            }
            // SUCCESS / ERROR : toujours par le vibrateur (contrat).
            Haptic.SUCCESS -> vibratePredefinedOrOneShot(VibrationEffect.EFFECT_HEAVY_CLICK, fallbackMs = 40)
            Haptic.ERROR -> vibrateWaveform(ERROR_PATTERN)
        }
    }

    /** Joue via Compose si lié, sinon exécute le [fallback] vibrateur. */
    private inline fun firePreferCompose(type: HapticFeedbackType, fallback: () -> Unit) {
        val feedback = composeHaptic
        if (feedback != null) {
            feedback.performHapticFeedback(type)
        } else {
            fallback()
        }
    }

    // --- Repli vibrateur (respecte le réglage système « retour haptique ») ------------------

    private fun vibratePredefinedOrOneShot(predefined: Int, fallbackMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrate(VibrationEffect.createPredefined(predefined))
        } else {
            vibrateOneShot(durationMs = fallbackMs, amplitude = VibrationEffect.DEFAULT_AMPLITUDE)
        }
    }

    private fun vibrateOneShot(durationMs: Long, amplitude: Int) {
        if (durationMs <= 0L) return
        vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
    }

    private fun vibrateWaveform(pattern: LongArray) {
        vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    private fun vibrate(effect: VibrationEffect) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        if (!systemHapticsEnabled()) return
        val touchAttrs = TOUCH_VIBRATION_ATTRIBUTES
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && touchAttrs != null) {
            vib.vibrate(effect, touchAttrs)
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(effect, TOUCH_AUDIO_ATTRIBUTES)
        }
    }

    private fun resolveVibrator(): Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (_: Exception) {
        null
    }

    /** Respecte le toggle « Retour haptique » des réglages système (best-effort). */
    private fun systemHapticsEnabled(): Boolean = try {
        Settings.System.getInt(
            appContext.contentResolver,
            Settings.System.HAPTIC_FEEDBACK_ENABLED,
            1,
        ) != 0
    } catch (_: Exception) {
        true
    }

    private companion object {
        /** ERROR = double-tick (DESIGN §6.3). */
        private val ERROR_PATTERN = longArrayOf(0, 20, 60, 20)

        private const val LIGHT_AMPLITUDE = 60

        private val TOUCH_VIBRATION_ATTRIBUTES: VibrationAttributes? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_TOUCH)
                    .build()
            } else {
                null
            }

        private val TOUCH_AUDIO_ATTRIBUTES: AudioAttributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
    }
}
