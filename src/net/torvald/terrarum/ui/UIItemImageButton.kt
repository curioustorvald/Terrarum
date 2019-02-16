package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.BlendMode
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.fillRect

/**
 * Created by minjaesong on 2017-07-16.
 */
open class UIItemImageButton(
        parent: UICanvas,
        val image: TextureRegion,

        /** Colour when mouse is over */
        val activeCol: Color = UIItemTextButton.defaultActiveCol,
        /** Colour when mouse is over */
        val activeBackCol: Color = UIItemTextButtonList.DEFAULT_BACKGROUND_ACTIVECOL,
        /** Colour when mouse is over */
        val activeBackBlendMode: String = BlendMode.NORMAL,
        /** Colour when clicked/selected */
        val highlightCol: Color = UIItemTextButton.defaultHighlightCol,
        /** Colour when clicked/selected */
        val highlightBackCol: Color = UIItemTextButtonList.DEFAULT_BACKGROUND_HIGHLIGHTCOL,
        /** Colour when clicked/selected */
        val highlightBackBlendMode: String = BlendMode.NORMAL,
        /** Colour on normal status */
        val inactiveCol: Color = UIItemTextButton.defaultInactiveCol,
        val backgroundCol: Color = UIItemTextButtonList.DEFAULT_BACKGROUNDCOL,
        val backgroundBlendMode: String = BlendMode.NORMAL,

        override var posX: Int,
        override var posY: Int,
        override val width: Int = image.regionWidth,
        override val height: Int = image.regionHeight,

        /** When clicked, toggle its "lit" status */
        var highlightable: Boolean
) : UIItem(parent) {

    // deal with the moving position
    override var oldPosX = posX
    override var oldPosY = posY

    var highlighted = false

    override fun render(batch: SpriteBatch, camera: Camera) {
        // draw background
        if (highlighted) {
            BlendMode.resolve(highlightBackBlendMode, batch)
            batch.color = highlightBackCol
            batch.fillRect(posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())
        }
        else if (mouseUp) {
            BlendMode.resolve(activeBackBlendMode, batch)
            batch.color = activeBackCol
            batch.fillRect(posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())
        }
        else {
            batch.color = backgroundCol
            BlendMode.resolve(backgroundBlendMode, batch)
            batch.fillRect(posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())
        }


        // draw image
        blendNormal(batch)

        batch.color = if (highlighted) highlightCol
        else if (mouseUp) activeCol
        else inactiveCol

        batch.draw(image, (posX + (width - image.regionWidth) / 2).toFloat(), (posY + (height - image.regionHeight) / 2).toFloat())
    }

    override fun dispose() {
        image.texture.dispose()
    }

    override fun keyDown(keycode: Int): Boolean {
        return super.keyDown(keycode)
    }

    override fun keyUp(keycode: Int): Boolean {
        return super.keyUp(keycode)
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return super.mouseMoved(screenX, screenY)
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return super.touchDragged(screenX, screenY, pointer)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (mouseUp && highlightable) {
            highlighted = !highlighted
        }

        return super.touchDown(screenX, screenY, pointer, button)
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return super.touchUp(screenX, screenY, pointer, button)
    }

    override fun scrolled(amount: Int): Boolean {
        return super.scrolled(amount)
    }
}