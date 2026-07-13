package com.wizycode.create.ui.common

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.wizycode.create.ui.theme.Accent
import com.wizycode.create.ui.theme.Motion
import com.wizycode.create.ui.theme.glassStrong

/**
 * Barre supérieure des deux surfaces principales (CONTRACTS §4.4, DESIGN §5.1).
 *
 * - Épinglée en haut sur une surface **`glassStrong`** translucide, sous l'inset de status bar.
 * - **Gauche** : wordmark « Create. » (`displaySmall` Instrument Serif, le point final en [Accent]).
 * - **Droite** : slot [actions] — typiquement [NotifBell] + [CreditsChip].
 * - Hauteur 56 dp + inset status bar. Une **ombre douce apparaît au scroll** ([elevated] = true),
 *   animée en fondu.
 *
 * Le slot [actions] découple la barre des ViewModels : l'écran y place les composants connectés
 * (crédits / notifications), gardant la barre elle-même réutilisable et sans dépendance data.
 *
 * @param elevated `true` quand le contenu est scrollé sous la barre → apparition de l'ombre.
 */
@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val shadowElevation by animateDpAsState(
        targetValue = if (elevated) 8.dp else 0.dp,
        animationSpec = tween(durationMillis = Motion.DUR_STD),
        label = "topBarShadow",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            // Ombre portée sous la barre (bord bas) — croît au scroll.
            .shadow(elevation = shadowElevation, shape = RectangleShape, clip = false)
            .glassStrong(RectangleShape)
            // La barre déborde derrière la status bar (verre continu) mais son contenu est inseté.
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Wordmark()
            Spacer(Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                content = actions,
            )
        }
    }
}

/**
 * Wordmark « Create. » : `displaySmall` Instrument Serif, mot en `onSurface` (encre) et point final
 * en [Accent] (bleu), reproduisant la ponctuation colorée du web (DESIGN §1). Annoncé comme titre.
 */
@Composable
private fun Wordmark(modifier: Modifier = Modifier) {
    val text = buildAnnotatedString {
        append("Create")
        withStyle(SpanStyle(color = Accent)) { append(".") }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.semantics { heading() },
    )
}
