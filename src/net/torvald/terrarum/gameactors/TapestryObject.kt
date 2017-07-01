package net.torvald.terrarum.gameactors

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.TerrarumGDX
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2017-01-07.
 */
class TapestryObject(pixmap: Pixmap, val artName: String, val artAuthor: String) : FixtureBase(physics = false) {

    // physics = false only speeds up for ~2 frames with 50 tapestries

    init {
        val texture = Texture(pixmap)
        pixmap.dispose()
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        val texturePack = TextureRegionPack(texture, texture.width, texture.height)

        makeNewSprite(texturePack)
        setHitboxDimension(texture.width, texture.height, 0, 0)
        setPosition(TerrarumGDX.mouseX, TerrarumGDX.mouseY)
        // you CAN'T destroy the image
    }

    override fun update(delta: Float) {
        super.update(delta)
    }

    override fun drawBody(batch: SpriteBatch) {
        super.drawBody(batch)
    }

    override var tooltipText: String? = "$artName\n$artAuthor"
}
