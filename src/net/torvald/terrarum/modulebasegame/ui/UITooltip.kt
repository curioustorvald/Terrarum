package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.Second
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2017-11-25.
 */
class UITooltip : UICanvas() {

    override var openCloseTime: Second = 0f

    var message: String = ""
        set(value) {
            field = value
            msgWidth = font.getWidth(value)
        }

    private val textures = TextureRegionPack("assets/graphics/gui/tooltip_black.tga", 8, 36)

    private val font = Terrarum.fontGame
    private var msgWidth = 0

    val textMarginX = 4

    override var width: Int
        get() = msgWidth + (textMarginX + textures.tileW) * 2
        set(value) { throw Error("You are not supposed to set the width of the tooltip manually.") }
    override var height: Int
        get() = textures.tileH
        set(value) { throw Error("You are not supposed to set the height of the tooltip manually.") }


    init {
        textures.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        val mouseX = 4f
        val mouseY = 6f

        val tooltipY = mouseY - textures.tileH

        val txtW = msgWidth + 2f * textMarginX

        batch.color = Color.WHITE
        batch.draw(textures.get(0, 0), mouseX, tooltipY)
        batch.draw(textures.get(1, 0), mouseX + textures.tileW, tooltipY, txtW, height.toFloat())
        batch.draw(textures.get(2, 0), mouseX + textures.tileW + txtW, tooltipY)
        font.draw(batch, message, mouseX + textures.tileW + textMarginX, mouseY - textures.tileH + (textures.tileH - font.lineHeight) / 2)
    }

    override fun updateUI(delta: Float) {
        setPosition(Terrarum.mouseScreenX, Terrarum.mouseScreenY)
    }

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
    }

    override fun dispose() {
        textures.dispose()
    }

}