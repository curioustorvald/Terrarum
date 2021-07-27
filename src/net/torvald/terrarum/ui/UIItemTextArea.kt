package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.floor
import net.torvald.terrarum.ui.UIItemTextButton.Companion.Alignment

class UIItemTextArea(
        parentUI: UICanvas,
        initialX: Int,
        initialY: Int,
        override val width: Int,
        override val height: Int,
        val lineGap: Int = 0,
        val lineCount: Int = ((height + lineGap) / AppLoader.fontGame.lineHeight).toInt(),
        val align: Alignment = Alignment.LEFT
) : UIItem(parentUI, initialX, initialY) {

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

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        scrollPos += Math.round(amountX * 3)
        if (scrollPos > entireText.size - lineCount) scrollPos = entireText.size - lineCount
        if (scrollPos < 0) scrollPos = 0

        return super.scrolled(amountX, amountY)
    }

    override fun dispose() {
    }
}