package net.torvald.terrarum.weather

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.JsonValue
import net.torvald.terrarum.GdxColorMap
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Note: Colour maps are likely to have sparse data points
 * (i.e., they have 2 475 px in width and you'll need 79 200 data points for a day)
 * so between two must be interpolated.
 *
 * Created by minjaesong on 2016-07-11.
 */
data class BaseModularWeather(
        val json: JsonValue,
        var skyboxGradColourMap: GdxColorMap, // row 0: skybox grad top, row 1: skybox grad bottom, row 2: sunlight (RGBA)
        val daylightClut: GdxColorMap,
        val classification: String,
        val cloudChance: Float,
        val windSpeed: Float,
        val cloudGamma: Vector2,
        val cloudGammaVariance: Vector2,
        var clouds: List<CloudProps>, // sorted by CloudProps.probability

        val mixFrom: String? = null,
        val mixPercentage: Double? = null
)

data class CloudProps(
        val category: String,
        val spriteSheet: TextureRegionPack,
        val probability: Float,
        val baseScale: Float,
        val scaleVariance: Float,
        val altLow: Float,
        val altHigh: Float,
) {
}