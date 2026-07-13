package com.wizycode.create.ui.create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.wizycode.create.core.util.Haptic
import com.wizycode.create.data.model.Generation
import com.wizycode.create.ui.theme.IrisTextBrush
import com.wizycode.create.ui.theme.LocalHaptics

/**
 * Feed inversé de l'écran Créer (CONTRACTS §4.4 / DESIGN §5.3).
 *
 * `LazyColumn(reverseLayout = true)` : l'élément d'indice 0 (le plus récent, le repo trie `-created`)
 * est rendu **en bas**, collé au composer ; on scrolle vers le haut pour remonter le temps. Charge les
 * 12 dernières générations. Chaque carte fait 72 % de large et s'aligne à droite (bulle sortante, RTL
 * miroité via [Alignment.TopEnd], layout-direction-aware).
 */
@Composable
fun Feed(
    items: List<Generation>,
    onOpen: (String) -> Unit,
    onReuse: (String) -> Unit,
    onCancel: (String) -> Unit,
    modifier: Modifier = Modifier,
    thumbnailModifier: (Generation) -> Modifier = { Modifier },
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    ) {
        items(items = items, key = { it.id }) { generation ->
            Box(
                modifier = Modifier.fillMaxWidth().animateItem(),
                contentAlignment = Alignment.TopEnd,
            ) {
                FeedCard(
                    generation = generation,
                    onOpen = onOpen,
                    onReuse = onReuse,
                    onCancel = onCancel,
                    modifier = Modifier.fillMaxWidth(0.72f),
                    thumbnailModifier = thumbnailModifier(generation),
                )
            }
        }
    }
}

/**
 * État vide (DESIGN §5.3) : hero serif « Qu'est-ce qu'on *crée* aujourd'hui ? » (*crée* en italique
 * peint au brush iris) + 3 suggestions cliquables (NATIVE_SPEC §2.2) qui pré-remplissent le prompt.
 */
@Composable
fun EmptyState(
    onSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHaptics.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val hero = buildAnnotatedString {
            append("Qu'est-ce qu'on ")
            withStyle(SpanStyle(brush = IrisTextBrush, fontStyle = FontStyle.Italic)) {
                append("crée")
            }
            append(" aujourd'hui ?")
        }
        Text(
            text = hero,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Column(
            modifier = Modifier.padding(top = 28.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SUGGESTIONS.forEach { suggestion ->
                val interaction = remember { MutableInteractionSource() }
                SuggestionChip(
                    onClick = {
                        haptics.fire(Haptic.SELECT)
                        onSuggestion(suggestion)
                    },
                    label = {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    interactionSource = interaction,
                    shape = SuggestionChipDefaults.shape,
                )
            }
        }
    }
}

private val SUGGESTIONS = listOf(
    "Un logo minimaliste pour un café de quartier",
    "Portrait studio d'un golden retriever, fond pastel",
    "Une cuisine scandinave baignée de lumière du matin",
)
