package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App

/**
 * Created by minjaesong on 2021-10-25.
 */
class UIItemInlineRadioButtons(
        parentUI: UICanvas,
        initialX: Int,
        initialY: Int,
        val cellWidth: Int,
        val labelfuns: List<() -> String>,
        initialSelection: Int = 0
) : UIItem(parentUI, initialX, initialY) {

    companion object {
        const val HEIGHT = 24
    }

    override val width = cellWidth * labelfuns.size + 3 * (labelfuns.size - 1)
    override val height = HEIGHT

    var selectionChangeListener: (Int) -> Unit = {}
    var selection = initialSelection
    var mouseOnSelection = -1

    private fun getCellX(i: Int) = posX - 3 + cellWidth * i + 3 * (i + 1)

    override fun update(delta: Float) {
        val mx = itemRelativeMouseX
        val my = itemRelativeMouseY
        mouseOnSelection = -1
        val oldSelection = selection
        if (my in 0 until height) {
            for (i in labelfuns.indices) {
                val cx = getCellX(i) - posX
                if (mx in cx until cx + cellWidth) {
                    mouseOnSelection = i
                    if (mousePushed) {
                        selection = i
                        if (i != oldSelection) selectionChangeListener(i)
                    }
                    break
                }
            }
        }

        super.update(delta)
    }

    override fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        // backgrounds
        batch.color = UIItemTextLineInput.TEXTINPUT_COL_BACKGROUND
        for (i in labelfuns.indices) {
            Toolkit.fillArea(batch, getCellX(i), posY, cellWidth, height)
        }

        // back border
        batch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, width + 2, height + 2)
        for (i in labelfuns.indices) {
            val xpos = getCellX(i)
            Toolkit.drawBoxBorder(batch, xpos - 1, posY - 1, cellWidth + 2, height + 2)
        }

        // text
        for (i in labelfuns.indices) {
            if (i != mouseOnSelection && i != selection) {
                val xpos = getCellX(i)
                val text = labelfuns[i]()
                val tw = App.fontGame.getWidth(text)
                App.fontGame.draw(batch, text, xpos + (cellWidth - tw) / 2, posY)
            }
        }

        // mouseover borders and text
        if (mouseOnSelection > -1 && mouseOnSelection != selection) {
            val xpos = getCellX(mouseOnSelection)
            batch.color = Toolkit.Theme.COL_MOUSE_UP
            Toolkit.drawBoxBorder(batch, xpos - 1, posY - 1, cellWidth + 2, height + 2)
            val text = labelfuns[mouseOnSelection]()
            val tw = App.fontGame.getWidth(text)
            App.fontGame.draw(batch, text, xpos + (cellWidth - tw) / 2, posY)
        }

        // selection borders and text
        val xpos = getCellX(selection)
        batch.color = Toolkit.Theme.COL_SELECTED
        Toolkit.drawBoxBorder(batch, xpos - 1, posY - 1, cellWidth + 2, height + 2)
        val text = labelfuns[selection]()
        val tw = App.fontGame.getWidth(text)
        App.fontGame.draw(batch, text, xpos + (cellWidth - tw) / 2, posY)


        super.render(frameDelta, batch, camera)
    }

    override fun dispose() {
    }
}