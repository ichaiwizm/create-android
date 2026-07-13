package com.wizycode.create.core.net.dto

import com.squareup.moshi.JsonClass

/**
 * DTO réseau d'une génération — parse **tolérant** (Moshi, champs manquants OK).
 * Mappé vers le modèle domaine `Generation` par `GenerationMapper`.
 *
 * CONTRACTS §1.2 — noms & champs figés.
 */
@JsonClass(generateAdapter = true)
data class GenerationDto(
    val id: String,
    val kind: String,
    val model: String,
    val prompt: String,
    val status: String,
    val mediaUrls: List<String> = emptyList(),
    val error: String? = null,
    val creditsConsumed: Int? = null,
    val created: String,
)

/** Réponse de `GET /api/generations` — liste tolérante (vide par défaut). */
@JsonClass(generateAdapter = true)
data class GenerationsResponse(
    val items: List<GenerationDto> = emptyList(),
)
