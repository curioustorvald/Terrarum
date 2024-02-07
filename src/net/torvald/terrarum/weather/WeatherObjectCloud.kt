package net.torvald.terrarum.weather

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.UnpackedColourSpriteBatch
import com.badlogic.gdx.math.Vector3
import com.jme3.math.FastMath
import com.jme3.math.FastMath.PI
import com.jme3.math.FastMath.sin
import net.torvald.terrarum.App
import net.torvald.terrarum.gameworld.GameWorld
import kotlin.math.*

/**
 * Created by minjaesong on 2023-08-21.
 */
class WeatherObjectCloud(
    private val texture: TextureRegion,
    private val flipW: Boolean,
    private val rgbGamma: Float,
    private val aGamma: Float
) : WeatherObject(), Comparable<WeatherObjectCloud> {

    override fun update() {
        throw UnsupportedOperationException()
    }

    var life = 0
    var despawnCode = ""; private set

    private val lifespan = 40000 + ((Math.random() + Math.random()) * 20000).roundToInt() // triangular distibution of 40000..80000

    private fun getZflowMult(z: Float) = z / ((z / 4f).pow(1.5f))

    private var eigenAlpha = 0f

    /**
     * FlowVector: In which direction the cloud flows. Vec3(dX, dY, dScale)
     * Resulting vector: (x + dX, y + dY, scale * dScale)
     */
    fun update(world: GameWorld, flowVector: Vector3) {
        pos.add(
            flowVector.cpy().
            scl(1f, 1f, getZflowMult(posZ)). // this will break the perspective if flowVector.z.abs() is close to 1, but it has to be here to "keep the distance"
            scl(vecMult).
            scl(world.worldTime.timeDelta.toFloat())
        )

//        eigenAlpha = if (posZ >= CLOUD_STAGE_DEPTH) 0f else if (posZ < 1f) posZ.pow(0.5f) else cosh((posZ - CLOUD_STAGE_DEPTH) / (0.75636f * CLOUD_STAGE_DEPTH)) - 1f //-((posZ - 1f) / CLOUD_STAGE_DEPTH) + 1f
        eigenAlpha = if (posZ >= CLOUD_STAGE_DEPTH) 0f else if (posZ < 1f) posZ.pow(0.5f) else ((posZ - CLOUD_STAGE_DEPTH) / (CLOUD_STAGE_DEPTH - 1)).pow(4f)

        val alphaMult = if (life < NEWBORN_GROWTH_TIME)
            life / NEWBORN_GROWTH_TIME
        else if (life < lifespan)
            1f
        else
            1f - (life - lifespan) / OLD_AGE_DECAY

        alpha = eigenAlpha * alphaMult


        val lrCoord = screenCoordBottomLRforDespawnCalculation
        if (lrCoord.x > WeatherMixer.oobMarginR || lrCoord.z < WeatherMixer.oobMarginL || posZ !in 0.0001f..CLOUD_STAGE_DEPTH + 1f || alpha < 0f) {
            flagToDespawn = true

            despawnCode = if (lrCoord.x > WeatherMixer.oobMarginR) "OUT_OF_SCREEN_RIGHT"
            else if (lrCoord.z < WeatherMixer.oobMarginL) "OUT_OF_SCREEN_LEFT"
            else if (posZ < 0.0001f) "OUT_OF_SCREEN_TOO_CLOSE"
            else if (posZ > CLOUD_STAGE_DEPTH + 1f) "OUT_OF_SCREEN_TOO_FAR"
            else if (life >= lifespan + OLD_AGE_DECAY) "OLD_AGE"
            else if (alpha < 0f) "ALPHA_BELOW_ZERO"
            else "UNKNOWN"
        }
        else {
            life += world.worldTime.timeDelta
        }
    }

    private val vecMult = Vector3(1f, 1f, 1f / (4f * H))

    private fun packFloat(f: Float, g: Float): Float {
        val fi = java.lang.Float.floatToRawIntBits(f)
        val gi = java.lang.Float.floatToRawIntBits(g)
        val hi = (fi and 0xffff0000.toInt()) or ((gi and 0xffff0000.toInt()) ushr 16)
        return java.lang.Float.intBitsToFloat(hi)
    }

    fun render(frameDelta: Float, batch: UnpackedColourSpriteBatch, cloudDrawColour0: Color, shadiness: Float) {
        val sc = screenCoord

//        printdbg(this, "gamma: (${rgbGamma}, ${aGamma}) index: ($rgbGammaIndex, $aGammaIndex)")

        val cloudCol = cloudDrawColour0.cpy().also {
            it.a = alpha
        }
        batch.color = cloudCol
        batch.generic.set(packFloat(rgbGamma, aGamma), shadiness, 0f, 0f)

        if (flipW)
            batch.draw(texture, sc.x + texture.regionWidth / posZ, sc.y, -texture.regionWidth * sc.z, texture.regionHeight * sc.z)
        else
            batch.draw(texture, sc.x, sc.y, texture.regionWidth * sc.z, texture.regionHeight * sc.z)
    }

    /**
     * X/Y position is a bottom-centre point of the image
     * Shader must be prepared prior to the render() call
     */
    override fun render(frameDelta: Float, batch: SpriteBatch, offsetX: Float, offsetY: Float) {
        throw UnsupportedOperationException()
    }

    /**
     * vec3(screen X, screenY, draw scale)
     */
    val screenCoord: Vector3
        get() {
            val x = posX - texture.regionWidth * scale * 0.5f
            val y = posY - texture.regionHeight * scale
            val z = posZ // must be larger than 0

            val drawX = (x + W * (z-1)) / z
            val drawY = (y + H * (z-1)) / z
            val drawScale = scale / z

            return Vector3(drawX, drawY, drawScale)
        }

    /**
     * vec3(screen-X of bottom-left point, screen-Y, screen-X of bottom-right point)
     */
    val screenCoordBottomLR: Vector3
        get() {
            val xL = posX - texture.regionWidth * scale * 0.5f
            val xR = posX + texture.regionWidth * scale * 0.5f
            val y = posY - texture.regionHeight * scale
            val z = posZ // must be larger than 0

            val drawXL = (xL + W * (z-1)) / z
            val drawXR = (xR + W * (z-1)) / z
            val drawY = (y + H * (z-1)) / z

            return Vector3(drawXL, drawY, drawXR)
        }

    private val screenCoordBottomLRforDespawnCalculation: Vector3
        get() {
            val xL = posX - texture.regionWidth * scale * 0.5f
            val xR = posX + texture.regionWidth * scale * 0.5f
            val y = posY - texture.regionHeight * scale
            val z = FastMath.interpolateLinear(posZ / ALPHA_ROLLOFF_Z, ALPHA_ROLLOFF_Z / 4f, ALPHA_ROLLOFF_Z)

            val drawXL = (xL + W * (z-1)) / z
            val drawXR = (xR + W * (z-1)) / z
            val drawY = (y + H * (z-1)) / z

            return Vector3(drawXL, drawY, drawXR)
        }

    override fun dispose() { /* cloud texture will be disposed of by the WeatherMixer */ }
    override fun compareTo(other: WeatherObjectCloud): Int = (other.posZ - this.posZ).sign.toInt()

    companion object {
        private val W = App.scr.halfwf
        private val H = App.scr.hf * 0.5f

        /**
         * Given screen-x and world-z position, calculates a world-x position that would make the cloud appear at the given screen-x position
         */
        fun screenXtoWorldX(screenX: Float, z: Float) = screenX * z - W * (z - 1f) // rearrange screenCoord equations to derive this eq :p

        /**
         * Given a world-y position, calculates a world-z position that would put the cloud to screen-y of zero
         */
        fun worldYtoWorldZforScreenYof0(y: Float) = 1f - (y / H) // rearrange screenCoord equations to derive this eq :p

        const val ALPHA_ROLLOFF_Z = 64f
        const val CLOUD_STAGE_DEPTH = 256f
        const val OLD_AGE_DECAY = 5000f
        const val NEWBORN_GROWTH_TIME = 1000f
    }
}