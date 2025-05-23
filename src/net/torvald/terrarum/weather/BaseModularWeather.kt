package net.torvald.terrarum.weather

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.JsonValue
import com.jme3.math.FastMath
import net.torvald.terrarum.GdxColorMap
import net.torvald.terrarum.TaggedProp
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import kotlin.math.absoluteValue

/**
 * Note: Colour maps are likely to have sparse data points
 * (i.e., they have 2 475 px in width and you'll need 79 200 data points for a day)
 * so between two must be interpolated.
 *
 * Created by minjaesong on 2016-07-11.
 */
data class BaseModularWeather(
    val identifier: String,
    val json: JsonValue,
    var skyboxGradColourMap: SkyboxModel, // row 0: skybox grad top, row 1: skybox grad bottom, row 2: sunlight (RGBA)
    val daylightClut: GdxColorMap,
    val tags: List<String>,
    val cloudChance: Float,
    val windSpeed: Float,
    val windSpeedVariance: Float,
    val windSpeedDamping: Float,
    val cloudGamma: Vector2,
    val cloudGammaVariance: Vector2,
    var clouds: List<CloudProps>, // sorted by CloudProps.probability
    val shaderVibrancy: FloatArray,

    val mixFrom: String? = null,
    val mixPercentage: Double? = null,
) : TaggedProp {


    /**
     * @param rnd random number between -1 and +1
     */
    fun getRandomWindSpeed(old: Float, rnd: Float): Float {
        val v = 1f + rnd.absoluteValue * windSpeedVariance
        val r = if (rnd < 0) windSpeed / v else windSpeed * v
        return FastMath.interpolateLinear(1f - windSpeedDamping, old, r)
    }

    fun getRandomWindSpeed(rnd: Float): Float {
        val v = 1f + rnd.absoluteValue * windSpeedVariance
        val r = if (rnd < 0) windSpeed / v else windSpeed * v
        return r
    }

    fun getRandomCloudGamma(rnd1: Float, rnd2: Float): Vector2 {
        val v = 1f + rnd1.absoluteValue * cloudGammaVariance.x
        val gx = if (rnd1 < 0) cloudGamma.x / v else cloudGamma.x * v

        val u = 1f + rnd2.absoluteValue * cloudGammaVariance.y
        val gy = if (rnd2 < 0) cloudGamma.y / u else cloudGamma.y * u

        return Vector2(gx, gy)
    }


    override fun hasTag(s: String) = tags.contains(s)

}

data class CloudProps(
    val category: String,
    val spriteSheet: TextureRegionPack,
    val probability: Float,
    val baseScale: Float,
    val scaleVariance: Float,
    val altLow: Float,
    val altHigh: Float,
) {
    init {
        spriteSheet.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
    }

    /**
     * @param rnd random number between -1 and +1
     */
    fun getCloudScaleVariance(rnd: Float): Float {
        val v = 1f + rnd.absoluteValue * scaleVariance
        return if (rnd < 0) baseScale / v else baseScale * v
    }
}