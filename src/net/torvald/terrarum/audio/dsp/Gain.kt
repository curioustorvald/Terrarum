package net.torvald.terrarum.audio.dsp

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack
import net.torvald.terrarum.audio.fullscaleToDecibels
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.FILTER_NAME_ACTIVE
import kotlin.math.roundToInt

class Gain(var gain: Float): TerrarumAudioFilter() {
    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        for (i in 0 until TerrarumAudioMixerTrack.AUDIO_BUFFER_SIZE) {
            outbuf[0][i] = inbuf[0][i] * gain
            outbuf[1][i] = inbuf[1][i] * gain
        }
    }

    override fun drawDebugView(batch: SpriteBatch, x: Int, y: Int) {
        batch.color = FILTER_NAME_ACTIVE
        App.fontSmallNumbers.draw(batch, "G:${fullscaleToDecibels(gain.toDouble()).times(100).roundToInt().div(100f)}", x+3f, y+1f)
    }

    override val debugViewHeight = 16
}