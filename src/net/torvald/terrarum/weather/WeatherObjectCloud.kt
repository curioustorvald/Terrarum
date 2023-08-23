package net.torvald.terrarum.weather

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.jme3.math.FastMath
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import kotlin.math.pow
import kotlin.math.sign

/**
 * Created by minjaesong on 2023-08-21.
 */
class WeatherObjectCloud(private val texture: TextureRegion, private val flipW: Boolean) : WeatherObject(), Comparable<WeatherObjectCloud> {

    override fun update() {
        throw UnsupportedOperationException()
    }

    var life = 0; private set

    private fun getZflowMult(z: Float) = z / ((z / 4f).pow(1.5f))

    /**
     * FlowVector: In which direction the cloud flows. Vec3(dX, dY, dScale)
     * Resulting vector: (x + dX, y + dY, scale * dScale)
     */
    fun update(flowVector: Vector3) {
        pos.add(
            flowVector.cpy().
            scl(1f, 1f, getZflowMult(posZ)). // this will break the perspective if flowVector.z.abs() is close to 1, but it has to be here to "keep the distance"
            scl(vecMult)
        )

        alpha = if (posZ < 1f) posZ.pow(0.5f) else -((posZ - 1f) / ALPHA_ROLLOFF_Z) + 1f

        val lrCoord = screenCoordBottomLRforDespawnCalculation
        if (lrCoord.x > WeatherMixer.oobMarginR || lrCoord.z < WeatherMixer.oobMarginL || posZ !in 0.0001f..ALPHA_ROLLOFF_Z + 1f || alpha < 0f) {
            flagToDespawn = true
        }
        else {
            life += 1
        }
    }

    private val w = App.scr.halfwf
    private val h = App.scr.hf * 0.5f
    private val vecMult = Vector3(1f, 1f, 1f / (4f * h))

    /**
     * X/Y position is a bottom-centre point of the image
     * Shader must be prepared prior to the render() call
     */
    override fun render(batch: SpriteBatch, offsetX: Float, offsetY: Float) {
        val sc = screenCoord

        if (flipW)
            batch.draw(texture, sc.x + texture.regionWidth / posZ, sc.y, -texture.regionWidth * sc.z, texture.regionHeight * sc.z)
        else
            batch.draw(texture, sc.x, sc.y, texture.regionWidth * sc.z, texture.regionHeight * sc.z)
    }

    /**
     * vec3(screen X, screenY, draw scale)
     */
    val screenCoord: Vector3
        get() {
            val x = posX - texture.regionWidth * scale * 0.5f
            val y = posY - texture.regionHeight * scale
            val z = posZ // must be at least 1.0

            val drawX = (x + w * (z-1)) / z
            val drawY = (y + h * (z-1)) / z
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

            val drawXL = (xL + w * (z-1)) / z
            val drawXR = (xR + w * (z-1)) / z
            val drawY = (y + h * (z-1)) / z

            return Vector3(drawXL, drawY, drawXR)
        }

    private val screenCoordBottomLRforDespawnCalculation: Vector3
        get() {
            val xL = posX - texture.regionWidth * scale * 0.5f
            val xR = posX + texture.regionWidth * scale * 0.5f
            val y = posY - texture.regionHeight * scale
            val z = FastMath.interpolateLinear(posZ / ALPHA_ROLLOFF_Z, ALPHA_ROLLOFF_Z / 4f, ALPHA_ROLLOFF_Z)

            val drawXL = (xL + w * (z-1)) / z
            val drawXR = (xR + w * (z-1)) / z
            val drawY = (y + h * (z-1)) / z

            return Vector3(drawXL, drawY, drawXR)
        }

    override fun dispose() { /* cloud texture will be disposed of by the WeatherMixer */ }
    override fun compareTo(other: WeatherObjectCloud): Int = (other.posZ - this.posZ).sign.toInt()

    companion object {
        fun screenXtoWorldX(screenX: Float, z: Float) = screenX * z - App.scr.halfwf * (z - 1f)
        const val ALPHA_ROLLOFF_Z = 64f
    }
}