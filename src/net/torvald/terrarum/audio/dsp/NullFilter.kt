package net.torvald.terrarum.audio.dsp

import com.badlogic.gdx.graphics.g2d.SpriteBatch

object NullFilter : TerrarumAudioFilter() {
    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        outbuf.forEachIndexed { index, outTrack ->
            System.arraycopy(inbuf[index], 0, outTrack, 0, outTrack.size)
        }
    }

    override fun drawDebugView(batch: SpriteBatch, x: Int, y: Int) {
    }

    override val debugViewHeight = 0
}