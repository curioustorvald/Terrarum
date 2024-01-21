package net.torvald.terrarum.audio.dsp

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.FILTER_NAME_ACTIVE

/**
 * Created by minjaesong on 2023-11-18.
 */
object Buffer : TerrarumAudioFilter() {
    init {
        bypass = true
    }

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        bypass = true
    }

    override fun drawDebugView(batch: SpriteBatch, x: Int, y: Int) {
        batch.color = FILTER_NAME_ACTIVE
        App.fontSmallNumbers.draw(batch, "Bs:${App.audioBufferSize}", x+3f, y+1f)
    }

    override val debugViewHeight = 16

    override fun copyParamsFrom(other: TerrarumAudioFilter) {
    }
}