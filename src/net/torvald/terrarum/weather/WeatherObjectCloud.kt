package net.torvald.terrarum.weather

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import net.torvald.terrarum.App
import kotlin.math.pow
import kotlin.math.sign

/**
 * Created by minjaesong on 2023-08-21.
 */
class WeatherObjectCloud(private val texture: TextureRegion, private val flipW: Boolean) : WeatherObject(), Comparable<WeatherObjectCloud> {

    /**
     * To actually utilise this value, your render code must begin the spritebatch per-object, like so:
     * ```
     * batch.shader = cloudShader
     * for (it in clouds) {
     *     batch.begin()
     *     batch.shader.setUniformf("gamma", it.darkness)
     *     batch.draw(it, ...)
     *     batch.end()
     * }
     */
    var darkness: Vector2 = Vector2(0.5f, 2.0f) // the "gamma" value fed into the clouds shader

    override fun update() {
        throw UnsupportedOperationException()
    }

    /**
     * FlowVector: In which direction the cloud flows. Vec3(dX, dY, dScale)
     * Resulting vector: (x + dX, y + dY, scale * dScale)
     */
    fun update(flowVector: Vector3, gait: Float) {
        pos.add(flowVector.cpy().scl(vecMult).scl(gait))

        alpha = -(posZ / ALPHA_ROLLOFF_Z).pow(1.703f) + 1f

        val lrCoord = screenCoordBottomLR
        if (lrCoord.x > WeatherMixer.oobMarginR || lrCoord.z < WeatherMixer.oobMarginL || posZ !in 0.05f..ALPHA_ROLLOFF_Z || alpha < 1f / 255f) {
            flagToDespawn = true
        }
    }

    private val w = App.scr.halfwf
    private val h = App.scr.hf * 0.5f
    private val vecMult = Vector3(1f, 1f, 1f / (2f * h))

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
            val z = posZ // must be at least 1.0

            val drawXL = (xL + w * (z-1)) / z
            val drawXR = (xR + w * (z-1)) / z
            val drawY = (y + h * (z-1)) / z

            return Vector3(drawXL, drawY, drawXR)
        }

    override fun dispose() { /* cloud texture will be disposed of by the WeatherMixer */ }
    override fun compareTo(other: WeatherObjectCloud): Int = (other.posZ - this.posZ).sign.toInt()

    companion object {
        fun screenXtoWorldX(screenX: Float, z: Float) = screenX * z - App.scr.halfwf * (z - 1f)
        const val ALPHA_ROLLOFF_Z = 16f
    }
}