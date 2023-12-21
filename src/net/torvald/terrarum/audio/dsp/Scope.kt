package net.torvald.terrarum.audio.dsp

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.STRIP_W
import net.torvald.terrarum.ui.Toolkit
import kotlin.math.roundToInt

class Scope : TerrarumAudioFilter() {
    val backbufL = Array((4096f / TerrarumAudioMixerTrack.AUDIO_BUFFER_SIZE).roundToInt().coerceAtLeast(1)) { FloatArray(
        TerrarumAudioMixerTrack.AUDIO_BUFFER_SIZE) }
    val backbufR = Array((4096f / TerrarumAudioMixerTrack.AUDIO_BUFFER_SIZE).roundToInt().coerceAtLeast(1)) { FloatArray(
        TerrarumAudioMixerTrack.AUDIO_BUFFER_SIZE) }

    private val sqrt2p = 0.7071067811865475

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        // shift buffer
        for (i in backbufL.lastIndex downTo 1) {
            backbufL[i] = backbufL[i - 1]
            backbufR[i] = backbufR[i - 1]
        }
        backbufL[0] = FloatArray(TerrarumAudioMixerTrack.AUDIO_BUFFER_SIZE)
        backbufR[0] = FloatArray(TerrarumAudioMixerTrack.AUDIO_BUFFER_SIZE)

        // plot dots
        for (i in 0 until TerrarumAudioMixerTrack.AUDIO_BUFFER_SIZE) {
            val y0 = inbuf[0][i] * 0.7
            val x0 = -inbuf[1][i] * 0.7 // rotate the domain by -90 deg

            val x = (+x0*sqrt2p -y0*sqrt2p) * 1.414
            val y = (-x0*sqrt2p -y0*sqrt2p) * 1.414 // further rotate by -45 deg then flip along the y axis

            backbufL[0][i] = x.toFloat()
            backbufR[0][i] = y.toFloat()
        }

        // copy samples over
        outbuf.forEachIndexed { index, outTrack ->
            System.arraycopy(inbuf[index], 0, outTrack, 0, outTrack.size)
        }
    }

    private val halfStripW = STRIP_W / 2
    private val scopePlotCol = Color(0x61b3df_33)

    override fun drawDebugView(batch: SpriteBatch, x: Int, y: Int) {
        batch.color = scopePlotCol
        val xxs = backbufR
        val yys = backbufL
        for (t in xxs.lastIndex downTo 0) {
            val xs = xxs[t]
            val ys = yys[t]

            for (i in xs.indices.reversed()) {
                val px = xs[i] * halfStripW + halfStripW
                val py = ys[i] * halfStripW + halfStripW
                Toolkit.fillArea(batch, x + px, y + py, 1f, 1f)
            }

        }
    }

    override val debugViewHeight = STRIP_W
}