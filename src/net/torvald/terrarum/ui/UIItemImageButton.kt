package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.BlendMode
import net.torvald.terrarum.blendNormal

/**
 * Created by minjaesong on 2017-07-16.
 */
open class UIItemImageButton(
        parent: UICanvas,
        var image: TextureRegion,

        /** Colour when mouse is over */
        val activeCol: Color = Toolkit.Theme.COL_ACTIVE,
        /** Colour when mouse is over */
        val activeBackCol: Color = UIItemTextButtonList.DEFAULT_BACKGROUND_ACTIVECOL,
        /** Colour when mouse is over */
        val activeBackBlendMode: String = BlendMode.NORMAL,
        /** Colour when clicked/selected */
        val highlightCol: Color = Toolkit.Theme.COL_HIGHLIGHT,
        /** Colour when clicked/selected */
        val highlightBackCol: Color = UIItemTextButtonList.DEFAULT_BACKGROUND_HIGHLIGHTCOL,
        /** Colour when clicked/selected */
        val highlightBackBlendMode: String = BlendMode.NORMAL,
        /** Colour on normal status */
        val inactiveCol: Color = Toolkit.Theme.COL_LIST_DEFAULT,
        val backgroundCol: Color = UIItemTextButtonList.DEFAULT_BACKGROUNDCOL,
        val backgroundBlendMode: String = BlendMode.NORMAL,

        initialX: Int,
        initialY: Int,
        override val width: Int = image.regionWidth,
        override val height: Int = image.regionHeight,

        /** When clicked, toggle its "lit" status */
        var highlightable: Boolean
) : UIItem(parent, initialX, initialY) {

    var highlighted = false

    override fun render(batch: SpriteBatch, camera: Camera) {
        // draw background
        if (highlighted) {
            BlendMode.resolve(highlightBackBlendMode, batch)
            batch.color = highlightBackCol
            Toolkit.fillArea(batch, posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())
        }
        else if (mouseUp) {
            BlendMode.resolve(activeBackBlendMode, batch)
            batch.color = activeBackCol
            Toolkit.fillArea(batch, posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())
        }
        else {
            batch.color = backgroundCol
            BlendMode.resolve(backgroundBlendMode, batch)
            Toolkit.fillArea(batch, posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())
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

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (mouseUp && highlightable) {
            highlighted = !highlighted
        }

        return super.touchDown(screenX, screenY, pointer, button)
    }
}