package com.wizycode.create.ui.common

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.wizycode.create.data.CreditsRepository
import com.wizycode.create.ui.theme.LocalIrisBrushes
import com.wizycode.create.ui.theme.glassSurface
import com.wizycode.create.ui.theme.rememberReduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.NumberFormat
import java.util.Locale

/**
 * Intervalle de rafraîchissement automatique du solde de crédits (DESIGN §5.9, NATIVE_SPEC §9).
 */
private const val CREDITS_REFRESH_MS = 45_000L

/**
 * Chip crédits **connecté** (CONTRACTS §4.4, DESIGN §5.9).
 *
 * Collecte le solde depuis [CreditsRepository] et pilote son rafraîchissement : à chaque passage au
 * premier plan (`ON_RESUME` → `repeatOnLifecycle(RESUMED)`) puis toutes les ~45 s tant que l'écran
 * est visible. Les erreurs réseau sont avalées (le solde précédent reste affiché).
 */
@Composable
fun CreditsChip(
    repository: CreditsRepository,
    modifier: Modifier = Modifier,
) {
    val credits by repository.credits.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(repository, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                try {
                    repository.refresh()
                } catch (_: Throwable) {
                    // Solde inchangé : on retentera au prochain cycle.
                }
                delay(CREDITS_REFRESH_MS)
            }
        }
    }

    CreditsChip(credits = credits, modifier = modifier)
}

/**
 * Chip crédits **présentationnel** : pill `glassSurface` avec l'éclair iris et le solde.
 *
 * Le nombre roule (roll-up) en **spring Expressive** via [animateIntAsState] à chaque changement,
 * et s'affiche en **chiffres tabulaires** (`tnum`) pour un défilement stable (CONTRACTS §5 règle 8).
 * `credits == null` (solde encore inconnu) affiche un tiret cadratin.
 */
@Composable
fun CreditsChip(
    credits: Int?,
    modifier: Modifier = Modifier,
) {
    val brushes = LocalIrisBrushes.current
    val reduceMotion = rememberReduceMotion()

    val animated by animateIntAsState(
        targetValue = credits ?: 0,
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.8f, stiffness = 380f),
        label = "creditsRollUp",
    )
    val display = if (credits == null) "—" else NumberFormat.getInstance(Locale.FRANCE).format(animated)
    val description = if (credits == null) "Crédits en cours de chargement" else "Crédits : ${credits} restants"

    Row(
        modifier = modifier
            .heightIn(min = 34.dp)
            .glassSurface(CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IrisIcon(
            imageVector = Icons.Rounded.Bolt,
            contentDescription = null,
            brush = brushes.fill,
            size = 16.dp,
        )
        Text(
            text = display,
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}
