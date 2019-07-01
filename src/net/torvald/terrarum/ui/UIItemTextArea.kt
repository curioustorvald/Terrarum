package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.floor
import net.torvald.terrarum.ui.UIItemTextButton.Companion.Alignment

class UIItemTextArea(
        parentUI: UICanvas,
        override var posX: Int,
        override var posY: Int,
        override val width: Int,
        override val height: Int,
        val lineGap: Int = 0,
        val lineCount: Int = ((height + lineGap) / AppLoader.fontGame.lineHeight).toInt(),
        val align: Alignment = Alignment.LEFT
) : UIItem(parentUI) {

    // deal with the moving position
    override var oldPosX = posX
    override var oldPosY = posY

    private var entireText: List<String> = listOf("") // placeholder

    var scrollPos = 0

    fun setWallOfText(listOfString: List<String>) {
        entireText = listOfString
    }

    override fun update(delta: Float) {
        super.update(delta)

        if (scrollPos > entireText.size - lineCount) scrollPos = entireText.size - lineCount
        if (scrollPos < 0) scrollPos = 0
    }

    override fun render(batch: SpriteBatch, camera: Camera) {
        for (i in scrollPos until minOf(lineCount + scrollPos, entireText.size)) {
            val yPtr = i - scrollPos

            val textWidth = AppLoader.fontGame.getWidth(entireText[i])

            when (align) {
                Alignment.LEFT -> AppLoader.fontGame.draw(batch, entireText[i], posX.toFloat(), posY + yPtr * (AppLoader.fontGame.lineHeight + lineGap))
                Alignment.CENTRE -> AppLoader.fontGame.draw(batch, entireText[i], posX + ((width - textWidth) / 2f).floor(), posY + yPtr * (AppLoader.fontGame.lineHeight + lineGap))
                Alignment.RIGHT -> AppLoader.fontGame.draw(batch, entireText[i], posX + width - textWidth.toFloat(), posY + yPtr * (AppLoader.fontGame.lineHeight + lineGap))
            }
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