package net.torvald.terrarum.weather

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import kotlin.math.sign

/**
 * Created by minjaesong on 2023-08-21.
 */
class WeatherObjectCloud(private val texture: TextureRegion) : WeatherObject() {

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
        posX += flowVector.x * gait * scale * scale
        posY += flowVector.y * gait * scale * scale
        scale *= flowVector.z
    }

    /**
     * X/Y position is a bottom-centre point of the image
     * Shader must be prepared prior to the render() call
     */
    override fun render(batch: SpriteBatch, offsetX: Float, offsetY: Float) {
        val x = posX + offsetX - texture.regionWidth * scale * 0.5f
        val y = posY + offsetY - texture.regionHeight * scale

        batch.draw(texture, x, y, texture.regionWidth * scale, texture.regionHeight * scale)
    }

    override fun dispose() { /* cloud texture will be disposed of by the WeatherMixer */ }
}