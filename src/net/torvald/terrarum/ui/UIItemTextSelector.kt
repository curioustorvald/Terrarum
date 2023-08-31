package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.gameworld.fmod

/**
 * @param width width of the text input where the text gets drawn, not the entire item
 * @param height height of the text input where the text gets drawn, not the entire item
 *
 * Created by minjaesong on 2021-10-21.
 */
class UIItemTextSelector(
    parentUI: UICanvas,
    initialX: Int, initialY: Int,
    val labelfuns: List<() -> String>,
    initialSelection: Int,
    override val width: Int,
    private val drawBorder: Boolean = true,
    private val clickToShowPalette: Boolean = true,
    private val useSpinnerButtons: Boolean = false,
) : UIItem(parentUI, initialX, initialY) {

    init {
    }

    override val mouseUp: Boolean
        get() = (itemRelativeMouseX in 0 until width && itemRelativeMouseY in 0 until height) or (if (paletteShowing)
            (itemRelativeMouseX in buttonW + 3 until buttonW + 3 + palW &&
             itemRelativeMouseY in palY - posY until palY + palH - posY)
        else false)

    private val labels = CommonResourcePool.getAsTextureRegionPack("inventory_category")

    override val height = 24
    private val buttonW = 30

    private val fboWidth = width - 2*buttonW - 6
//    private val fbo = FrameBuffer(Pixmap.Format.RGBA8888, fboWidth, height - 4, false)

    var selection = initialSelection
    private var fboUpdateLatch = true

    private var mouseOnButton = 0 // 0: nothing, 1: left, 2: right, 3: middle

    var selectionChangeListener: (Int) -> Unit = {}

    private var mouseOnPaletteItem: Int? = null

    var paletteShowing = false; private set
    private val palCursorCol = Toolkit.Theme.COL_INACTIVE.cpy().mul(1f,1f,1f,0.5f)
    private val palCellHeight = height // must be same as the text area height
    private val palCursorGap = 6 // preferably multiple of 3
    private var palX = posX + buttonW + 3
    private var palY = posY - palCellHeight * selection - palCursorGap
    private var palW = width - 2*buttonW - 6
    private var palH = palCellHeight * labelfuns.size + 2*palCursorGap

    private var labelCache: List<String> = listOf()

    override fun show() {
        fboUpdateLatch = true
    }

    override fun update(delta: Float) {
        super.update(delta)

        mouseOnButton =
                if (itemRelativeMouseX in 0 until buttonW && itemRelativeMouseY in 0 until height)
                    1
                else if (itemRelativeMouseX in width - buttonW until width && itemRelativeMouseY in 0 until height)
                    2
                else if (itemRelativeMouseX in buttonW + 3 until width - buttonW - 3 && itemRelativeMouseY in 0 until height)
                    3
                else
                    0

        mouseOnPaletteItem = null // serves as the fallback value
        if (paletteShowing) {
            for (i in 0 until labelfuns.size) {
                val ys = getPalItemPosY(i) - 2 // text cell height is hardcoded as 24; text height is 20
                val ye = ys + palCellHeight
                // cannot use mouseOnButton == 3 as it also takes Y-position into account
                if (parentUI.relativeMouseY in ys until ye && itemRelativeMouseX in buttonW + 3 until width - buttonW - 3) {
                    mouseOnPaletteItem = i
                    break
                }
            }
        }


        if (!mouseLatched && Terrarum.mouseDown) {
            if (paletteShowing && mouseOnPaletteItem != null ) {
                selection = mouseOnPaletteItem!!
                fboUpdateLatch = true
                selectionChangeListener(selection)

                paletteShowing = false
            }
            else if (mouseOnButton in 1..2) {
                selection = (selection + (mouseOnButton * 2) - 3) fmod labelfuns.size
                fboUpdateLatch = true
                selectionChangeListener(selection)

                paletteShowing = false
            }
            else if (mouseOnButton == 3 && clickToShowPalette) {
                if (!paletteShowing) {
                    paletteShowing = true
                }
                else {
                    paletteShowing = false
                    selectionChangeListener(selection)
                }
            }
            else {
                paletteShowing = false
            }

            mouseLatched = true
        }
        else if (!Terrarum.mouseDown) mouseLatched = false
    }

    private val leftIcon = if (useSpinnerButtons) labels.get(9,2) else labels.get(16,0)
    private val rightIcon = if (useSpinnerButtons) labels.get(10,2) else labels.get(17,0)

    override fun render(batch: SpriteBatch, camera: OrthographicCamera) {
        labelCache = labelfuns.map { it() }

        batch.end()

        /*if (fboUpdateLatch) {
            fboUpdateLatch = false
            fbo.inAction(camera as OrthographicCamera, batch) { batch.inUse {
                gdxClearAndEnableBlend(0f, 0f, 0f, 0f)

                it.color = Color.WHITE
                val t = labelCache[selection]
                val tw = App.fontGame.getWidth(t)

                printdbg(this, "Drawing text: $t")

                App.fontGameFBO.draw(it, t, (fbo.width - tw) / 2, 0)
            } }
        }*/

        batch.begin()

        if (drawBorder) {
            batch.color = UIItemTextLineInput.TEXTINPUT_COL_BACKGROUND
            // left button cell back
            Toolkit.fillArea(batch, posX, posY, buttonW, height)
            // text area cell back
            Toolkit.fillArea(batch, posX + buttonW + 3, posY, width - 2*buttonW - 6, height)
            // right button cell back
            Toolkit.fillArea(batch, posX + width - buttonW, posY, buttonW, height)

            // text area border
            if (!paletteShowing && mouseOnButton != 3) {
                batch.color = Toolkit.Theme.COL_INACTIVE
                Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, width + 2, height + 2)
            }

            // left button border
            batch.color = if (mouseOnButton == 1 && mousePushed) Toolkit.Theme.COL_SELECTED
            else if (mouseOnButton == 1) Toolkit.Theme.COL_MOUSE_UP else Toolkit.Theme.COL_INACTIVE
            Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, buttonW + 2, height + 2)

            // right button border
            batch.color = if (mouseOnButton == 2 && mousePushed) Toolkit.Theme.COL_SELECTED
            else if (mouseOnButton == 2) Toolkit.Theme.COL_MOUSE_UP else Toolkit.Theme.COL_INACTIVE
            Toolkit.drawBoxBorder(batch, posX + width - buttonW - 1, posY - 1, buttonW + 2, height + 2)

            // text area border (again)
            if (!paletteShowing && mouseOnButton == 3) {
                batch.color = Toolkit.Theme.COL_MOUSE_UP
                Toolkit.drawBoxBorder(batch, posX + buttonW + 2, posY - 1, width - 2*buttonW - 4, height + 2)
            }
        }

        // left button icon
        batch.color = if (mouseOnButton == 1 && mousePushed) Toolkit.Theme.COL_SELECTED
        else if (mouseOnButton == 1) Toolkit.Theme.COL_MOUSE_UP else UIItemTextLineInput.TEXTINPUT_COL_TEXT
        batch.draw(leftIcon, posX + (buttonW - labels.tileW) / 2f, posY + (height - labels.tileH) / 2f)

        // right button icon
        batch.color = if (mouseOnButton == 2 && mousePushed) Toolkit.Theme.COL_SELECTED
        else if (mouseOnButton == 2) Toolkit.Theme.COL_MOUSE_UP else UIItemTextLineInput.TEXTINPUT_COL_TEXT
        batch.draw(rightIcon, posX + width - buttonW + (buttonW - labels.tileW) / 2f, posY + (height - labels.tileH) / 2f)

        // draw text
        if (!paletteShowing) {
            batch.color = UIItemTextLineInput.TEXTINPUT_COL_TEXT
            val t = labelCache[selection]
            val tw = App.fontGame.getWidth(t)
//            batch.draw(fbo.colorBufferTexture, posX + buttonW + 3f, posY + 2f, fbo.width.toFloat(), fbo.height.toFloat())
            App.fontGame.draw(batch, t, posX + buttonW + 3 + (fboWidth - tw) / 2, posY)
        }
        // palette
        else {
            palX = posX + buttonW + 3
            palY = posY - palCellHeight * selection - palCursorGap
            palH = palCellHeight * labelCache.size + 2*palCursorGap

            // palette background
            batch.color = UIItemTextLineInput.TEXTINPUT_COL_BACKGROUND
            Toolkit.fillArea(batch, palX-1, palY-1, palW+2, palH+2)
            Toolkit.fillArea(batch, palX-1, palY-1, palW+2, palH+2)

            // cursor
            batch.color = palCursorCol
            Toolkit.drawBoxBorder(batch, posX + buttonW + 2, posY - 1, width - 2*buttonW - 4, height + 2)
            // palette border
            batch.color = Toolkit.Theme.COL_MOUSE_UP
            Toolkit.drawBoxBorder(batch, palX-1, palY-1, palW+2, palH+2)

            // palette items
            labelCache.forEachIndexed { index, s ->
                batch.color = if (index == selection) Toolkit.Theme.COL_SELECTED
                    else if (index == mouseOnPaletteItem) Toolkit.Theme.COL_MOUSE_UP
                    else UIItemTextLineInput.TEXTINPUT_COL_TEXT
                val t = labelCache[index]
                val tw = App.fontGame.getWidth(t)
                App.fontGame.draw(batch, t,
                        palX + (palW - tw) / 2,
                        getPalItemPosY(index) - 2
                )
            }
        }

        super.render(batch, camera)

    }

    /**
     * posY, palY and selection must be updated beforehand!
     */
    private fun getPalItemPosY(index: Int) = palY + 2 + palCellHeight * index +
            (if (index > selection) 1 else if (index == selection) 0 else -1) * palCursorGap + palCursorGap


    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        if (mouseUp) {
            if (amountX <= -1 || amountY <= -1)
                selection = (selection - 1) fmod labelfuns.size
            else if (amountX >= 1 || amountY >= 1)
                selection = (selection + 1) fmod labelfuns.size

            selectionChangeListener(selection)
            fboUpdateLatch = true
            return true
        }
        else {
            return false
        }
    }

    override fun dispose() {
//        fbo.dispose()
    }
}
