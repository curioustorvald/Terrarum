package net.torvald.terrarum

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion

/**
 * Don't flip the assets! Flip the draw command instead!
 *
 * Created by minjaesong on 2021-12-13.
 */
class FlippingSpriteBatch(size: Int = 1000) : SpriteBatch(size, if (App.isAppleM) MacosGL32Shaders.createSpriteBatchShader() else null) {

    /**
     * This function draws the flipped version of the image by giving flipped uv-coord to the SpriteBatch
     */
    override fun draw(texture: Texture, x: Float, y: Float, width: Float, height: Float) =
            draw(texture, x, y, width, height, 0f, 0f, 1f, 1f)

    override fun draw(texture: Texture, x: Float, y: Float) =
            draw(texture, x, y, texture.width.toFloat(), texture.height.toFloat(), 0f, 0f, 1f, 1f)

    fun drawFlipped(texture: Texture, x: Float, y: Float, width: Float, height: Float) =
            draw(texture, x, y, width, height, 0f, 1f, 1f, 0f)
    fun drawFlipped(texture: Texture, x: Float, y: Float) =
            draw(texture, x, y, texture.width.toFloat(), texture.height.toFloat(), 0f, 1f, 1f, 0f)


    /**
     * This function does obey the flipping set to the TextureRegion and try to draw flipped version of it,
     * without touching the flipping setting of the given region.
     */
    override fun draw(region: TextureRegion, x: Float, y: Float, width: Float, height: Float) =
            draw(region.texture, x, y, width, height, region.u, region.v, region.u2, region.v2)

    override fun draw(region: TextureRegion, x: Float, y: Float) =
            draw(region.texture, x, y, region.regionWidth.toFloat(), region.regionHeight.toFloat(), region.u, region.v, region.u2, region.v2)

    fun drawFlipped(region: TextureRegion, x: Float, y: Float, width: Float, height: Float) =
            draw(region.texture, x, y, width, height, region.u, region.v2, region.u2, region.v)
    fun drawFlipped(region: TextureRegion, x: Float, y: Float) =
            draw(region.texture, x, y, region.regionWidth.toFloat(), region.regionHeight.toFloat(), region.u, region.v2, region.u2, region.v)


    /**
     * NOTE TO SELF:
     *
     * It seems that original SpriteBatch Y-flips when it's drawing a texture, but NOT when it's drawing a textureregion
     *
     * (textureregion's default uv-coord is (0,0,1,1)
     */
}