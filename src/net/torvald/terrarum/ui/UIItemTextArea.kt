package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.Terrarum

class UIItemTextArea(
        parentUI: UICanvas,
        override var posX: Int,
        override var posY: Int,
        override val width: Int,
        override val height: Int,
        val lineGap: Int = 0,
        val lineCount: Int = ((height + lineGap) / Terrarum.fontGame.lineHeight).toInt()
) : UIItem(parentUI) {





    var entireText: List<String> = listOf("") // placeholder

    var scrollPos = 0

    fun setWallOfText(listOfString: List<String>) {
        entireText = listOfString
    }

    override fun update(delta: Float) {
        super.update(delta)

        if (scrollPos > entireText.size - lineCount) scrollPos = entireText.size - lineCount
        if (scrollPos < 0) scrollPos = 0
    }

    override fun render(batch: SpriteBatch) {
        batch.color = Color.WHITE
        for (i in scrollPos until minOf(lineCount + scrollPos, entireText.size)) {
            val yPtr = i - scrollPos

            Terrarum.fontGame.draw(batch, entireText[i], posX.toFloat(), posY + yPtr * (Terrarum.fontGame.lineHeight + lineGap))
        }
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
        return super.touchDown(screenX, screenY, pointer, button)
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return super.touchUp(screenX, screenY, pointer, button)
    }

    override fun scrolled(amount: Int): Boolean {
        scrollPos += amount * 3
        if (scrollPos > entireText.size - lineCount) scrollPos = entireText.size - lineCount
        if (scrollPos < 0) scrollPos = 0

        return super.scrolled(amount)
    }

    override fun dispose() {
    }
}