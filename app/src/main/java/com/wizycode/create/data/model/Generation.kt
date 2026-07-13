package com.wizycode.create.data.model

import com.wizycode.create.core.util.PbFiles
import java.time.Instant

/**
 * Nature du média produit par une génération.
 * Sérialisé côté réseau en `"image"` / `"video"` (mapping fait par `GenerationMapper`).
 */
enum class MediaKind { IMAGE, VIDEO }

/** Cycle de vie d'une génération asynchrone kie. */
enum class GenStatus { PENDING, DONE, FAILED, CANCELLED }

/**
 * Modèle domaine d'une génération (pur data, aucune logique).
 *
 * Les URLs de [mediaUrls] pointent vers des fichiers déjà rapatriés dans PocketBase.
 * @param created décodé depuis un ISO-8601 vers [java.time.Instant] par le mapper.
 */
data class Generation(
    val id: String,
    val kind: MediaKind,
    val model: String,
    val prompt: String,
    val status: GenStatus,
    val mediaUrls: List<String>,
    val error: String? = null,
    val creditsConsumed: Int? = null,
    val created: Instant,
)

/** `true` si la génération produit une vidéo. */
val Generation.isVideo: Boolean
    get() = kind == MediaKind.VIDEO

/** Première URL média disponible, ou `null` si aucune. */
val Generation.firstMediaUrl: String?
    get() = mediaUrls.firstOrNull()

/** Miniature calibrée pour le feed (largeur 600, hauteur auto) : `?thumb=600x0`. */
fun Generation.thumbFeedUrl(): String? =
    firstMediaUrl?.let { PbFiles.thumb(it, "600x0") }

/** Miniature carrée pour la grille galerie : `?thumb=600x600`. */
fun Generation.thumbGridUrl(): String? =
    firstMediaUrl?.let { PbFiles.thumb(it, "600x600") }

/** URL de téléchargement forcé : `?download=1`. */
fun Generation.downloadUrl(): String? =
    firstMediaUrl?.let { PbFiles.download(it) }
