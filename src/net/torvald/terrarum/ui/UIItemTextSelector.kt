package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import net.torvald.terrarum.*
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

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
        intialSelection: Int,
        override val width: Int,
        private val drawBorder: Boolean = true
) : UIItem(parentUI, initialX, initialY) {

    init {
        CommonResourcePool.addToLoadingList("inventory_category") {
            TextureRegionPack("assets/graphics/gui/inventory/category.tga", 20, 20)
        }
        CommonResourcePool.loadAll()
    }

    private val labels = CommonResourcePool.getAsTextureRegionPack("inventory_category")

    override val height = 24
    private val buttonW = 30

    private val fbo = FrameBuffer(Pixmap.Format.RGBA8888, width - 2*buttonW - 6, height - 4, true)

    var selection = intialSelection
    private var fboUpdateLatch = true

    private var mouseOnButton = 0 // 0: nothing, 1: left, 2: right

    var selectionChangeListener: (Int) -> Unit = {}

    override fun update(delta: Float) {
        super.update(delta)

        mouseOnButton =
                if (relativeMouseX in 0..buttonW && relativeMouseY in 0..height)
                    1
                else if (relativeMouseX in width - buttonW..width && relativeMouseY in 0..height)
                    2
                else if (relativeMouseX in buttonW + 3..width - buttonW - 3 && relativeMouseY in 0..height)
                    3
                else
                    0

        if (!mouseLatched && Terrarum.mouseDown && mouseOnButton in 1..2) {
            mouseLatched = true
            selection = (selection + (mouseOnButton * 2) - 3) fmod labelfuns.size
            fboUpdateLatch = true
            selectionChangeListener(selection)
        }
        else if (!Terrarum.mouseDown) mouseLatched = false
    }

    override fun render(batch: SpriteBatch, camera: Camera) {

        batch.end()

        if (fboUpdateLatch) {
            fboUpdateLatch = false
            fbo.inAction(camera as OrthographicCamera, batch) { batch.inUse {
                gdxClearAndSetBlend(0f, 0f, 0f, 0f)

                it.color = Color.WHITE
                val t = labelfuns[selection]()
                val tw = App.fontGame.getWidth(t)
                App.fontGameFBO.draw(it, t, (fbo.width - tw) / 2, 0)
            } }
        }

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
            if (mouseOnButton != 3) {
                batch.color = Toolkit.Theme.COL_INACTIVE
                Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, width + 2, height + 2)
            }

            // left button border
            batch.color = if (mouseOnButton == 1 && mousePushed) Toolkit.Theme.COL_HIGHLIGHT
            else if (mouseOnButton == 1) Toolkit.Theme.COL_ACTIVE else Toolkit.Theme.COL_INACTIVE
            Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, buttonW + 2, height + 2)

            // right button border
            batch.color = if (mouseOnButton == 2 && mousePushed) Toolkit.Theme.COL_HIGHLIGHT
            else if (mouseOnButton == 2) Toolkit.Theme.COL_ACTIVE else Toolkit.Theme.COL_INACTIVE
            Toolkit.drawBoxBorder(batch, posX + width - buttonW - 1, posY - 1, buttonW + 2, height + 2)

            // text area border (again)
            if (mouseOnButton == 3) {
                batch.color = Toolkit.Theme.COL_ACTIVE
                Toolkit.drawBoxBorder(batch, posX + buttonW + 2, posY - 1, width - 2*buttonW - 4, height + 2)
            }
        }

        // left button icon
        batch.color = if (mouseOnButton == 1 && mousePushed) Toolkit.Theme.COL_HIGHLIGHT
        else if (mouseOnButton == 1) Toolkit.Theme.COL_ACTIVE else UIItemTextLineInput.TEXTINPUT_COL_TEXT
        batch.draw(labels.get(16,0), posX + (buttonW - labels.tileW) / 2f, posY + (height - labels.tileH) / 2f)

        // right button icon
        batch.color = if (mouseOnButton == 2 && mousePushed) Toolkit.Theme.COL_HIGHLIGHT
        else if (mouseOnButton == 2) Toolkit.Theme.COL_ACTIVE else UIItemTextLineInput.TEXTINPUT_COL_TEXT
        batch.draw(labels.get(17,0), posX + width - buttonW + (buttonW - labels.tileW) / 2f, posY + (height - labels.tileH) / 2f)

        // draw text
        batch.color = UIItemTextLineInput.TEXTINPUT_COL_TEXT
        batch.draw(fbo.colorBufferTexture, posX + buttonW + 3f, posY + 2f, fbo.width.toFloat(), fbo.height.toFloat())


        super.render(batch, camera)
    }

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
        fbo.dispose()
    }
}
