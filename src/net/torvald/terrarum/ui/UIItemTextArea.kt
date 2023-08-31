package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.roundToFloat
import net.torvald.terrarum.ui.UIItemTextButton.Companion.Alignment
import kotlin.math.min

class UIItemTextArea(
        parentUI: UICanvas,
        initialX: Int,
        initialY: Int,
        override val width: Int,
        override val height: Int,
        val lineGap: Int = 0,
        val lineCount: Int = ((height + lineGap) / App.fontGame.lineHeight).toInt(),
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

    override fun render(batch: SpriteBatch, camera: OrthographicCamera) {
        for (i in scrollPos until min(lineCount + scrollPos, entireText.size)) {
            val yPtr = i - scrollPos

            val textWidth = App.fontGame.getWidth(entireText[i])

            when (align) {
                Alignment.LEFT -> App.fontGame.draw(batch, entireText[i], posX.toFloat(), posY + yPtr * (App.fontGame.lineHeight + lineGap))
                Alignment.CENTRE -> App.fontGame.draw(batch, entireText[i], posX + ((width - textWidth) / 2f).roundToFloat(), posY + yPtr * (App.fontGame.lineHeight + lineGap))
                Alignment.RIGHT -> App.fontGame.draw(batch, entireText[i], posX + width - textWidth.toFloat(), posY + yPtr * (App.fontGame.lineHeight + lineGap))
            }
        }
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        scrollPos += Math.round(amountY * 5)
        if (scrollPos > entireText.size - lineCount) scrollPos = entireText.size - lineCount
        if (scrollPos < 0) scrollPos = 0

        return super.scrolled(amountX, amountY)
    }

    override fun dispose() {
    }
}