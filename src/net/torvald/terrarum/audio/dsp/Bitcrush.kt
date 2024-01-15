package net.torvald.terrarum.audio.dsp

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.jme3.math.FastMath
import net.torvald.terrarum.App
import net.torvald.terrarum.audio.linToLogPerc
import net.torvald.terrarum.roundToFloat
import net.torvald.terrarum.ui.BasicDebugInfoWindow
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.toIntAndFrac
import net.torvald.terrarum.ui.Toolkit

class Bitcrush(var steps: Int, var inputGain: Float = 1f): TerrarumAudioFilter() {
    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        for (ch in outbuf.indices) {
            for (i in 0 until App.audioBufferSize) {
                val inn = ((inbuf[ch][i] * inputGain).coerceIn(-1f, 1f) + 1f) / 2f // 0f..1f
                val stepped = (inn * (steps - 1)).roundToFloat() / (steps - 1)
                val out = (stepped * 2f) - 1f // -1f..1f
                outbuf[ch][i] = out
            }
        }
    }

    override fun drawDebugView(batch: SpriteBatch, x: Int, y: Int) {
        val perc = linToLogPerc(steps.toDouble(), 2.0, 65536.0).toFloat()
        batch.color = BasicDebugInfoWindow.COL_METER_GRAD2
        Toolkit.fillArea(batch, x.toFloat(), y.toFloat(), BasicDebugInfoWindow.STRIP_W * perc, 14f)
        batch.color = BasicDebugInfoWindow.COL_METER_GRAD
        Toolkit.fillArea(batch, x.toFloat(), y+14f, BasicDebugInfoWindow.STRIP_W * perc, 2f)

        val bits = FastMath.log(steps.toFloat(), 2f).toIntAndFrac(1,2)

        batch.color = BasicDebugInfoWindow.FILTER_NAME_ACTIVE
        App.fontSmallNumbers.draw(batch, "B:$bits", x+3f, y+1f)
    }

    override val debugViewHeight = 16
}