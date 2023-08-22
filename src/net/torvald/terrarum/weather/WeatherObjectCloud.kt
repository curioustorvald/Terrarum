package net.torvald.terrarum.weather

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import net.torvald.terrarum.App
import kotlin.math.sign

/**
 * Created by minjaesong on 2023-08-21.
 */
class WeatherObjectCloud(private val texture: TextureRegion, private val flipW: Boolean) : WeatherObject() {

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
    }

    /**
     * FlowVector: In which direction the cloud flows. Vec3(dX, dY, dScale)
     * Resulting vector: (x + dX, y + dY, scale * dScale)
     */
    fun update(flowVector: Vector3, gait: Float) {
        val vecMult = Vector3(1f, 1f, 1f / (2f * App.scr.hf * 0.35f))
        pos.add(flowVector.cpy().scl(vecMult).scl(gait))
    }

    /**
     * X/Y position is a bottom-centre point of the image
     * Shader must be prepared prior to the render() call
     */
    override fun render(batch: SpriteBatch, offsetX: Float, offsetY: Float) {
        val x = posX + offsetX - texture.regionWidth * scale * 0.5f
        val y = posY + offsetY - texture.regionHeight * scale
        val z = posZ // must be at least 1.0
        val w = App.scr.halfwf
        val h = App.scr.hf * 0.35f

        val drawX = (x + w * (z-1)) / z
        val drawY = (y + h * (z-1)) / z
        val drawScale = scale / z

        if (flipW)
            batch.draw(texture, drawX + texture.regionWidth / z, drawY, -texture.regionWidth * drawScale, texture.regionHeight * drawScale)
        else
            batch.draw(texture, drawX, drawY, texture.regionWidth * drawScale, texture.regionHeight * drawScale)
    }

    override fun dispose() { /* cloud texture will be disposed of by the WeatherMixer */ }
}