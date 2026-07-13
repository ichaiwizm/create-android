package com.wizycode.create.core.net.dto

import com.squareup.moshi.JsonClass

/**
 * DTOs requête / réponse des routes `/api/…` et de l'auth PocketBase.
 * CONTRACTS §3.3 — noms & champs figés.
 */

// ---------------------------------------------------------------------------
// CreateApi (backend Next.js, base APP_BASE)
// ---------------------------------------------------------------------------

/** Corps de `POST /api/generate` (génération standard). `options` sérialisé tel quel. */
@JsonClass(generateAdapter = true)
data class GenerateRequest(
    val model: String,
    val prompt: String,
    val imageUrls: List<String>? = null,
    val options: Map<String, Any?>? = null,
)

/** Corps de `POST /api/generate` (outil 1-clic). `tool` = "upscale" | "removeBg". */
@JsonClass(generateAdapter = true)
data class ToolRequest(
    val tool: String,
    val toolImageUrl: String,
)

/** Réponse d'une génération lancée : identifiant local + tâche kie. */
@JsonClass(generateAdapter = true)
data class GenerateResponse(
    val id: String,
    val taskId: String,
)

/** Réponse d'`POST /api/upload` : URL temporaire kie (~3 jours). */
@JsonClass(generateAdapter = true)
data class UploadResponse(
    val url: String,
)

/** Réponse d'`GET /api/credits`. */
@JsonClass(generateAdapter = true)
data class CreditsResponse(
    val credits: Int,
)

/** Réponse d'`POST /api/transcribe` (dictée vocale). */
@JsonClass(generateAdapter = true)
data class TranscribeResponse(
    val transcript: String,
)

/** Corps d'`POST /api/push/register-fcm`. */
@JsonClass(generateAdapter = true)
data class FcmTokenRequest(
    val token: String,
    val platform: String = "android",
)

// ---------------------------------------------------------------------------
// PbAuthApi (PocketBase direct, base PB_BASE)
// ---------------------------------------------------------------------------

/** Corps de `auth-with-password`. `identity` = email ou username. */
@JsonClass(generateAdapter = true)
data class PbAuthRequest(
    val identity: String,
    val password: String,
)

/** Réponse d'auth PocketBase : token Bearer + enregistrement utilisateur. */
@JsonClass(generateAdapter = true)
data class PbAuthResponse(
    val token: String,
    val record: PbUserRecord,
)

/** Sous-objet `record` d'une réponse d'auth PocketBase. */
@JsonClass(generateAdapter = true)
data class PbUserRecord(
    val id: String,
    val email: String,
    val name: String? = null,
)
