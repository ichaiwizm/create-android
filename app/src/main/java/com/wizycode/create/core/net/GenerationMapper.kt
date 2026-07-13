package com.wizycode.create.core.net

import com.wizycode.create.core.net.dto.GenerationDto
import com.wizycode.create.data.model.GenStatus
import com.wizycode.create.data.model.Generation
import com.wizycode.create.data.model.MediaKind
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Conversion DTO réseau → modèle domaine. Tolérant : les valeurs `kind` / `status`
 * inconnues retombent sur des défauts sûrs, et les dates PocketBase
 * (`"yyyy-MM-dd HH:mm:ss.SSSZ"`) comme ISO-8601 sont décodées en [Instant].
 *
 * CONTRACTS §1.2 — `GenerationDto` → `Generation`.
 */
object GenerationMapper {

    fun toDomain(dto: GenerationDto): Generation = Generation(
        id = dto.id,
        kind = parseKind(dto.kind),
        model = dto.model,
        prompt = dto.prompt,
        status = parseStatus(dto.status),
        mediaUrls = dto.mediaUrls,
        error = dto.error?.takeIf { it.isNotBlank() },
        creditsConsumed = dto.creditsConsumed,
        created = parseInstant(dto.created),
    )

    fun toDomain(dtos: List<GenerationDto>): List<Generation> = dtos.map(::toDomain)

    private fun parseKind(raw: String): MediaKind =
        if (raw.trim().equals("video", ignoreCase = true)) MediaKind.VIDEO else MediaKind.IMAGE

    private fun parseStatus(raw: String): GenStatus =
        when (raw.trim().lowercase()) {
            "done", "completed", "complete", "success", "succeeded" -> GenStatus.DONE
            "failed", "error", "errored" -> GenStatus.FAILED
            "cancelled", "canceled" -> GenStatus.CANCELLED
            else -> GenStatus.PENDING // pending, processing, queued, running, generating…
        }

    private val pbFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]")

    /** Décode une date ISO-8601 (`…T…Z`) ou le format PocketBase (`… …Z`). */
    private fun parseInstant(raw: String): Instant {
        val value = raw.trim()
        if (value.isEmpty()) return Instant.EPOCH
        return try {
            Instant.parse(value)
        } catch (_: Throwable) {
            try {
                // Format PocketBase : espace séparateur, suffixe Z, millis optionnels.
                LocalDateTime.parse(value.removeSuffix("Z").trim(), pbFormatter)
                    .toInstant(ZoneOffset.UTC)
            } catch (_: Throwable) {
                Instant.EPOCH
            }
        }
    }
}
