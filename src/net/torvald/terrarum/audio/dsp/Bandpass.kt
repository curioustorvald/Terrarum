package net.torvald.terrarum.audio.dsp

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.FILTER_NAME_ACTIVE

/**
 * Created by minjaesong on 2024-12-22.
 */
class Bandpass(cutoffLo0: Float, cutoffHi0: Float): TerrarumAudioFilter() {

    private val LP = Lowpass(cutoffHi0)
    private val HP = Highpass(cutoffLo0)

    private var midbufLen = App.audioBufferSize
    private var midbuf: List<FloatArray> = listOf(FloatArray(midbufLen), FloatArray(midbufLen))

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        if (inbuf[0].size != midbufLen) {
            midbufLen = inbuf[0].size
            midbuf = listOf(FloatArray(midbufLen), FloatArray(midbufLen))
        }

        HP.thru(inbuf, midbuf)
        LP.thru(midbuf, outbuf)
    }

    override fun drawDebugView(batch: SpriteBatch, x: Int, y: Int) {
//        val perc = linToLogPerc(cutoff, 24.0, 24000.0).toFloat()
//        batch.color = COL_METER_GRAD2
//        Toolkit.fillArea(batch, x.toFloat(), y.toFloat(), BasicDebugInfoWindow.STRIP_W * perc, 14f)
//        batch.color = COL_METER_GRAD
//        Toolkit.fillArea(batch, x.toFloat(), y+14f, BasicDebugInfoWindow.STRIP_W * perc, 2f)

        batch.color = FILTER_NAME_ACTIVE
        App.fontSmallNumbers.draw(batch, "FL:${HP.cutoff.toInt()}", x+3f, y+1f)
        App.fontSmallNumbers.draw(batch, "FH:${LP.cutoff.toInt()}", x+3f, y+17f)
    }

    override val debugViewHeight = 32

    override fun copyParamsFrom(other: TerrarumAudioFilter) {
        if (other is Bandpass) {
            other.LP.setCutoff(this.LP.cutoff)
            other.HP.setCutoff(this.HP.cutoff)
        }
    }
}