package com.wizycode.create.core.util

import java.time.Instant

/**
 * Formatage temporel relatif en français (« il y a … »), utilisé par les cartes de feed/galerie
 * et la lightbox (DESIGN §5.3/§5.8/§5.10). Fondé sur `java.time.Instant` (dates ISO-8601 décodées,
 * CONTRACTS §5.8) — disponible dès minSdk 26.
 *
 * Paliers :
 * - < 1 min          → « à l'instant »
 * - < 1 h            → « il y a N min »
 * - < 24 h           → « il y a N h »
 * - < 7 j            → « il y a N j »
 * - < 4 sem (~30 j)  → « il y a N sem »
 * - < 12 mois (~365 j) → « il y a N mois »
 * - au-delà          → « il y a N an » / « il y a N ans »
 *
 * Les instants futurs (horloge désynchronisée) retombent sur « à l'instant ».
 */
fun timeAgo(instant: Instant, now: Instant = Instant.now()): String {
    val seconds = now.epochSecond - instant.epochSecond
    if (seconds < MINUTE) return "à l'instant"

    val minutes = seconds / MINUTE
    if (minutes < 60) return "il y a $minutes min"

    val hours = seconds / HOUR
    if (hours < 24) return "il y a $hours h"

    val days = seconds / DAY
    if (days < 7) return "il y a $days j"

    val weeks = days / 7
    if (weeks < 4) return "il y a $weeks sem"

    val months = days / 30
    if (months < 12) return "il y a $months mois"

    val years = days / 365
    val safeYears = if (years < 1) 1 else years
    return "il y a $safeYears ${if (safeYears > 1) "ans" else "an"}"
}

/** Variante défensive tolérant un [instant] nul (média sans date) → chaîne vide. */
fun timeAgoOrEmpty(instant: Instant?, now: Instant = Instant.now()): String =
    if (instant == null) "" else timeAgo(instant, now)

private const val MINUTE = 60L
private const val HOUR = 60L * 60L
private const val DAY = 24L * 60L * 60L
