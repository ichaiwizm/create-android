package com.wizycode.create.data.catalog

import com.wizycode.create.data.model.ModelFamily
import com.wizycode.create.data.model.ModelKind
import com.wizycode.create.data.model.ModelVariant
import com.wizycode.create.data.model.ParamSpec
import com.wizycode.create.data.model.ToolSpec

/**
 * Catalogue kie statique — miroir **verbatim** de `models.ts` (NATIVE_SPEC §5).
 *
 * Data-driven : chaque [ModelFamily] déclare ses vrais champs API (specs OpenAPI kie) et le serveur
 * construit l'`input` sans mapping en dur. Les sheets de réglages se génèrent dynamiquement depuis
 * [ParamSpec] (aucun réglage codé en dur par famille) — voir [CatalogLogic].
 *
 * Rien ne s'invente ici : noms, slugs, enums et valeurs par défaut sont recopiés à la lettre depuis
 * `/root/apps/create/src/lib/kie/models.ts`.
 */
object ModelCatalog {

    /** IMAGE — 3 familles : nano-banana-pro, gpt-image-2, seedream-5-pro. */
    val image: List<ModelFamily> = listOf(
        ModelFamily(
            key = "nano-banana-pro",
            kind = ModelKind.IMAGE,
            name = "Nano Banana Pro",
            tagline = "Détails fins · texte net",
            credits = "~18-24",
            textId = "nano-banana-pro",
            editId = "nano-banana-pro",
            imageField = "image_input",
            imageIsList = true,
            maxImages = 8,
            params = listOf(
                ParamSpec(
                    field = "aspect_ratio",
                    label = "Format",
                    values = listOf("1:1", "2:3", "3:2", "3:4", "4:3", "4:5", "5:4", "9:16", "16:9", "21:9", "auto"),
                    def = "1:1",
                ),
                ParamSpec(
                    field = "resolution",
                    label = "Résolution",
                    values = listOf("1K", "2K", "4K"),
                    def = "1K",
                ),
            ),
            extraInput = mapOf("output_format" to "png"),
        ),
        ModelFamily(
            key = "gpt-image-2",
            kind = ModelKind.IMAGE,
            name = "GPT Image 2",
            tagline = "Dernier OpenAI · très créatif",
            credits = "~15-40",
            textId = "gpt-image-2-text-to-image",
            editId = "gpt-image-2-image-to-image",
            imageField = "input_urls",
            imageIsList = true,
            maxImages = 16,
            params = listOf(
                ParamSpec(
                    field = "aspect_ratio",
                    label = "Format",
                    values = listOf("auto", "1:1", "3:2", "2:3", "4:3", "3:4", "5:4", "4:5", "16:9", "9:16", "21:9"),
                    def = "auto",
                ),
                ParamSpec(
                    field = "resolution",
                    label = "Résolution",
                    values = listOf("1K", "2K", "4K"),
                    def = "1K",
                ),
            ),
        ),
        ModelFamily(
            key = "seedream-5-pro",
            kind = ModelKind.IMAGE,
            name = "Seedream 5 Pro",
            tagline = "Le meilleur de ByteDance",
            credits = "~15-25",
            textId = "seedream/5-pro-text-to-image",
            editId = "seedream/5-pro-image-to-image",
            imageField = "image_urls",
            imageIsList = true,
            maxImages = 10,
            params = listOf(
                ParamSpec(
                    field = "aspect_ratio",
                    label = "Format",
                    values = listOf("1:1", "4:3", "3:4", "16:9", "9:16", "2:3", "3:2"),
                    def = "1:1",
                ),
                ParamSpec(
                    field = "quality",
                    label = "Qualité",
                    values = listOf("basic", "high"),
                    def = "basic",
                ),
            ),
        ),
    )

    /** VIDÉO — 3 familles : veo3.1, kling-3.0, seedance-2. */
    val video: List<ModelFamily> = listOf(
        ModelFamily(
            key = "veo3.1",
            kind = ModelKind.VIDEO,
            name = "Veo 3.1",
            tagline = "Google · le meilleur · audio natif",
            credits = "~80-300",
            textId = "veo3_fast",
            editId = "veo3_fast",
            imageField = "imageUrls",
            imageIsList = true,
            maxImages = 3,
            params = listOf(
                ParamSpec(
                    field = "aspect_ratio",
                    label = "Format",
                    values = listOf("16:9", "9:16"),
                    def = "16:9",
                ),
                ParamSpec(
                    field = "resolution",
                    label = "Résolution",
                    values = listOf("720p", "1080p"),
                    def = "720p",
                ),
                ParamSpec(
                    field = "duration",
                    label = "Durée",
                    values = listOf("4", "6", "8"),
                    def = "8",
                    numeric = true,
                ),
            ),
            variants = listOf(
                ModelVariant(key = "fast", label = "Rapide", id = "veo3_fast", credits = 80),
                ModelVariant(key = "quality", label = "Qualité", id = "veo3", credits = 300),
            ),
        ),
        ModelFamily(
            key = "kling-3.0",
            kind = ModelKind.VIDEO,
            name = "Kling 3.0",
            tagline = "Dernier Kling · 3-15 s · son",
            credits = "~150-400",
            textId = "kling-3.0/video",
            editId = "kling-3.0/video",
            imageField = "image_urls",
            imageIsList = true,
            maxImages = 2,
            params = listOf(
                ParamSpec(
                    field = "aspect_ratio",
                    label = "Format",
                    values = listOf("16:9", "9:16", "1:1"),
                    def = "16:9",
                ),
                ParamSpec(
                    field = "mode",
                    label = "Mode",
                    values = listOf("std", "pro", "4K"),
                    def = "pro",
                ),
                ParamSpec(
                    field = "duration",
                    label = "Durée",
                    values = listOf("3", "5", "8", "10", "15"),
                    def = "5",
                ),
                ParamSpec(
                    field = "sound",
                    label = "Son",
                    values = listOf("true", "false"),
                    def = "false",
                    boolean = true,
                    boolLabels = "Avec son" to "Sans son",
                ),
            ),
            extraInput = mapOf(
                "multi_shots" to false,
                "multi_prompt" to emptyList<String>(),
            ),
        ),
        ModelFamily(
            key = "seedance-2",
            kind = ModelKind.VIDEO,
            name = "Seedance 2.0",
            tagline = "ByteDance · très bon rapport qualité/prix",
            credits = "~100-300",
            textId = "bytedance/seedance-2",
            editId = "bytedance/seedance-2",
            imageField = "first_frame_url",
            imageIsList = false,
            maxImages = 1,
            params = listOf(
                ParamSpec(
                    field = "aspect_ratio",
                    label = "Format",
                    values = listOf("16:9", "9:16", "1:1", "4:3", "3:4", "21:9", "adaptive"),
                    def = "16:9",
                ),
                ParamSpec(
                    field = "resolution",
                    label = "Résolution",
                    values = listOf("480p", "720p", "1080p"),
                    def = "720p",
                ),
                ParamSpec(
                    field = "duration",
                    label = "Durée",
                    values = listOf("4", "5", "8", "10", "12"),
                    def = "5",
                    numeric = true,
                ),
                ParamSpec(
                    field = "generate_audio",
                    label = "Son",
                    values = listOf("true", "false"),
                    def = "true",
                    boolean = true,
                    boolLabels = "Avec son" to "Sans son",
                ),
            ),
        ),
    )

    /** Outils 1-clic sur une image existante (NATIVE_SPEC §5 · TOOLS). */
    val tools: List<ToolSpec> = listOf(
        ToolSpec(key = "upscale", label = "Upscale", id = "topaz/image-upscale", credits = 4),
        ToolSpec(key = "removeBg", label = "Détourer", id = "recraft/remove-background", credits = 4),
    )

    /** Familles d'un [ModelKind] donné (image ou vidéo). */
    fun families(kind: ModelKind): List<ModelFamily> = when (kind) {
        ModelKind.IMAGE -> image
        ModelKind.VIDEO -> video
    }

    /** Famille par clé (`nano-banana-pro`, `veo3.1`…), ou `null` si inconnue. */
    fun family(key: String): ModelFamily? =
        image.firstOrNull { it.key == key } ?: video.firstOrNull { it.key == key }

    const val DEFAULT_IMAGE_FAMILY_KEY = "nano-banana-pro"
    const val DEFAULT_VIDEO_FAMILY_KEY = "veo3.1"
}
