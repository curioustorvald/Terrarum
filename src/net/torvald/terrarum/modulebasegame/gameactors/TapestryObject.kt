package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.ceilInt
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2017-01-07.
 */
class TapestryObject(pixmap: Pixmap, val artName: String, val artAuthor: String) :
        FixtureBase(BlockBox(BlockBox.NO_COLLISION, 1, 1)) // placeholder blockbox
{

    // physics = false only speeds up for ~2 frames with 50 tapestries

    init {
        val texture = Texture(pixmap)
        pixmap.dispose()
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        val texturePack = TextureRegionPack(texture, texture.width, texture.height)

        makeNewSprite(texturePack)
        setHitboxDimension(texture.width, texture.height, 0, 0)
        setPosition(Terrarum.mouseX, Terrarum.mouseY)
        // you CAN'T destroy the image

        // redefine blockbox
        blockBox.redefine(texture.width.div(TILE_SIZEF).ceilInt(), texture.height.div(TILE_SIZEF).ceilInt())
    }

    override fun update(delta: Float) {
        super.update(delta)
    }

    override fun drawBody(batch: SpriteBatch) {
        super.drawBody(batch)
    }

    override var tooltipText: String? = "$artName\n$artAuthor"
}
