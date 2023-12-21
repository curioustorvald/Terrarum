package net.torvald.terrarum.audio.dsp

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.AUDIO_BUFFER_SIZE
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.FILTER_NAME_ACTIVE

object Buffer : TerrarumAudioFilter() {
    init {
        bypass = true
    }

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        bypass = true
    }

    override fun drawDebugView(batch: SpriteBatch, x: Int, y: Int) {
        batch.color = FILTER_NAME_ACTIVE
        App.fontSmallNumbers.draw(batch, "Bs:${AUDIO_BUFFER_SIZE}", x+3f, y+1f)
    }

    override val debugViewHeight = 16
}