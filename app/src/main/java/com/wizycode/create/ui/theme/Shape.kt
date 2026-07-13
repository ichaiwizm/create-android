package com.wizycode.create.ui.theme

import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.pill
import androidx.graphics.shapes.star

/**
 * Formes Material 3 Expressive de Create (CONTRACTS §2.4 / DESIGN §4).
 *
 * Échelle gonflée : les coins sont volontairement très arrondis pour porter l'esthétique web.
 */
val CreateShapes: Shapes = Shapes(
    extraSmall = AbsoluteRoundedCornerShape(8.dp),
    small = AbsoluteRoundedCornerShape(14.dp),
    medium = AbsoluteRoundedCornerShape(22.dp),
    large = AbsoluteRoundedCornerShape(28.dp),
    extraLarge = AbsoluteRoundedCornerShape(34.dp),
)

/**
 * Bulle de feed (DESIGN §4) : coins arrondis à 28 dp avec le **coin bas-droit resserré à 10 dp**
 * (queue de bulle « sortante », alignée à droite). En **RTL**, l'ensemble est miroité : c'est le
 * **coin bas-gauche** qui est resserré.
 *
 * On utilise [AbsoluteRoundedCornerShape] (coins en coordonnées absolues gauche/droite) et on
 * choisit explicitement le coin resserré selon [layoutDirection] pour éviter tout double miroir.
 */
fun feedBubbleShape(layoutDirection: LayoutDirection): Shape {
    val big = 28.dp
    val tight = 10.dp
    return if (layoutDirection == LayoutDirection.Ltr) {
        AbsoluteRoundedCornerShape(topLeft = big, topRight = big, bottomRight = tight, bottomLeft = big)
    } else {
        AbsoluteRoundedCornerShape(topLeft = big, topRight = big, bottomRight = big, bottomLeft = tight)
    }
}

/**
 * Accents Expressive ponctuels (DESIGN §4). Construits directement à partir des fabriques
 * `androidx.graphics.shapes` (les mêmes primitives que celles des formes Material Expressive), puis
 * enrobés dans une [Shape] Compose via [RoundedPolygonShape] :
 *  - [Cookie12SidedShape] : badge de crédits animé — étoile douce à 12 pointes (« cookie ») ;
 *  - [PillShape] : avatar d'état vide / accent pilule.
 *
 * Réservés aux accents — pas partout.
 */
val Cookie12SidedShape: Shape = RoundedPolygonShape(
    RoundedPolygon.star(
        numVerticesPerRadius = 12,
        radius = 1f,
        innerRadius = 0.928f,
        rounding = CornerRounding(radius = 0.1f),
    ),
)

val PillShape: Shape = RoundedPolygonShape(
    RoundedPolygon.pill(width = 1.5f, height = 1f),
)

/**
 * Enrobe un [RoundedPolygon] (androidx.graphics.shapes) en [Shape] Compose : les cubiques du
 * polygone sont normalisées sur leur bounding box puis mises à l'échelle de la [Size] du composant.
 */
private class RoundedPolygonShape(private val polygon: RoundedPolygon) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val bounds = polygon.calculateBounds(FloatArray(4))
        val left = bounds[0]
        val top = bounds[1]
        val srcWidth = (bounds[2] - left).takeIf { it != 0f } ?: 1f
        val srcHeight = (bounds[3] - top).takeIf { it != 0f } ?: 1f
        val sx = size.width / srcWidth
        val sy = size.height / srcHeight

        fun mapX(x: Float) = (x - left) * sx
        fun mapY(y: Float) = (y - top) * sy

        val path = Path()
        val cubics = polygon.cubics
        cubics.forEachIndexed { index, cubic ->
            if (index == 0) {
                path.moveTo(mapX(cubic.anchor0X), mapY(cubic.anchor0Y))
            }
            path.cubicTo(
                mapX(cubic.control0X), mapY(cubic.control0Y),
                mapX(cubic.control1X), mapY(cubic.control1Y),
                mapX(cubic.anchor1X), mapY(cubic.anchor1Y),
            )
        }
        path.close()
        return Outline.Generic(path)
    }
}
