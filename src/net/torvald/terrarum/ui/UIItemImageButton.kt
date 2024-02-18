package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.BlendMode
import net.torvald.terrarum.abs
import net.torvald.terrarum.blendNormalStraightAlpha

/**
 * Created by minjaesong on 2017-07-16.
 */
open class UIItemImageButton(
        parent: UICanvas,
        var image: TextureRegion,

        /** Colour when mouse is over */
        val activeCol: Color = Toolkit.Theme.COL_MOUSE_UP,
        /** Colour when mouse is over */
        val activeBackCol: Color = Color(0),//UIItemTextButtonList.DEFAULT_BACKGROUND_ACTIVECOL,
        /** Colour when mouse is over */
        val activeBackBlendMode: String = BlendMode.NORMAL,
        /** Colour when clicked/selected */
        val highlightCol: Color = Toolkit.Theme.COL_SELECTED,
        /** Colour when clicked/selected */
        val highlightBackCol: Color = Color(0),//UIItemTextButtonList.DEFAULT_BACKGROUND_HIGHLIGHTCOL,
        /** Colour when clicked/selected */
        val highlightBackBlendMode: String = BlendMode.NORMAL,
        /** Colour on normal status */
        val inactiveCol: Color = Toolkit.Theme.COL_LIST_DEFAULT,
        val backgroundCol: Color = Color(0),//UIItemTextButtonList.DEFAULT_BACKGROUNDCOL,
        val backgroundBlendMode: String = BlendMode.NORMAL,

        initialX: Int,
        initialY: Int,
        /** this does NOT resize the image; use imageDrawWidth to actually resize the image */
        override val width: Int = image.regionWidth,
        /** this does NOT resize the image; use imageDrawHeight to actually resize the image */
        override val height: Int = image.regionHeight,

        /** When clicked, toggle its "lit" status */
        var highlightable: Boolean,
        /** Changes the appearance to use a border instead of colour-changing image for highlighter */
        val useBorder: Boolean = false,

        /** Image won't be place at right position if `image.regionWidth != imageDrawWidth`; define the `width` argument to avoid the issue */
        val imageDrawWidth: Int = image.regionWidth,
        /** Image won't be place at right position if `image.regionHeight != imageDrawHeight`; define the `height` argument to avoid the issue */
        val imageDrawHeight: Int = image.regionHeight,
) : UIItem(parent, initialX, initialY) {

    var highlighted = false
    var extraDrawOp: (UIItem, SpriteBatch) -> Unit = { _,_ -> }

    fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera, offX: Int, offY: Int) {
        val posX = this.posX + offX
        val posY = this.posY + offY

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

        blendNormalStraightAlpha(batch)

        // draw image
        batch.color = if (highlighted) highlightCol
        else if (mouseUp) activeCol
        else if (useBorder) Toolkit.Theme.COL_INACTIVE else inactiveCol
        if (useBorder) {
            Toolkit.drawBoxBorder(batch, posX - 1f, posY - 1f, width + 2f, height + 2f)
            batch.color = Color.WHITE
        }
        batch.draw(image, (posX + (width - imageDrawWidth) / 2).toFloat(), (posY + (height - imageDrawHeight) / 2).toFloat(), imageDrawWidth.toFloat(), imageDrawHeight.toFloat())

        batch.color = if (highlighted) highlightCol
        else if (mouseUp) activeCol
        else inactiveCol
        extraDrawOp(this, batch)
    }

    override fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        render(frameDelta, batch, camera, 0, 0)
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