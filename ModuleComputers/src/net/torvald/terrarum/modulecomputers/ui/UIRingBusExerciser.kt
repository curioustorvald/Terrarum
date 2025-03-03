package net.torvald.terrarum.modulecomputers.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.ifNaN
import net.torvald.terrarum.modulecomputers.gameactors.FixtureRingBusCore
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemVertSlider
import net.torvald.terrarum.ui.Toolkit
import kotlin.math.roundToInt

class UIRingBusExerciser(val host: FixtureRingBusCore) : UICanvas() {

    override var width = Toolkit.drawWidth
    override var height = App.scr.height

    private val analyserPosX = 10
    private val analyserPosY = 10
    private val analyserWidth = width - 20
    private val analyserHeight = height - 20

    private val TEXT_LINE_HEIGHT = 24
    private var analysisTextBuffer = ArrayList<String>()

    private val analyserScroll = UIItemVertSlider(this,
        analyserPosX - 18,
        analyserPosY + 1,
        0.0, 0.0, 1.0, analyserHeight - 2, analyserHeight - 2
    )

    init {
        addUIitem(analyserScroll)
        refreshAnalysis()
    }

    private fun refreshAnalysis() {
        analysisTextBuffer.clear()

        host.msgLog.forEach { frame ->
            analysisTextBuffer.add(frame.toString())
        }

        // update scrollbar
        analyserScroll.handleHeight = if (analysisTextBuffer.isEmpty())
            analyserHeight
        else
            (analyserHeight.toFloat() / analysisTextBuffer.size.times(TEXT_LINE_HEIGHT))
                .times(analyserHeight)
                .roundToInt()
                .coerceIn(12, analyserHeight)
    }

    private fun drawAnalysis(batch: SpriteBatch) {
        val scroll = (analyserScroll.value * analysisTextBuffer.size.times(TEXT_LINE_HEIGHT)
            .minus(analyserHeight - 3))
            .ifNaN(0.0)
            .roundToInt()
            .coerceAtLeast(0)

        analysisTextBuffer.forEachIndexed { index, s ->
            App.fontGame.draw(batch, s,
                analyserPosX + 6f,
                analyserPosY + 3f + index * TEXT_LINE_HEIGHT - scroll
            )
        }
    }

    override fun updateImpl(delta: Float) {
        refreshAnalysis()
        uiItems.forEach { it.update(delta) }
    }

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        // Draw background box
        batch.color = Color(0x7F)
        Toolkit.fillArea(batch, analyserPosX, analyserPosY, analyserWidth, analyserHeight)
        batch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, analyserPosX, analyserPosY, analyserWidth, analyserHeight)

        // Draw text content
        batch.color = Color.WHITE
        drawAnalysis(batch)

        // Draw UI elements
        uiItems.forEach { it.render(frameDelta, batch, camera) }
    }

    override fun doOpening(delta: Float) {
        refreshAnalysis()
    }

    override fun doClosing(delta: Float) {
        // nothing needed
    }

    override fun dispose() {

    }
}