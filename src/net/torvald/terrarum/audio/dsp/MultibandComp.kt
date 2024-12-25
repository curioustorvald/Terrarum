package net.torvald.terrarum.audio.dsp

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App

/**
 * Created by minjaesong on 2024-12-22.
 */
class MultibandComp: TerrarumAudioFilter() {

    private val ratio = 2f

    private val bands = listOf(
        160f, 1100f, 7500f
    )

    private val bandFilters = bands.mapIndexed { index: Int, band: Float ->
        if (index == 0)
            Lowpass(band)
        else
            Bandpass(bands[index-1], band)
    } + Highpass(bands.last())

    private var midbufLen = App.audioBufferSize
    private var midbufs = bandFilters.map {
        listOf(FloatArray(midbufLen), FloatArray(midbufLen))
    }

    private fun resizeMidbufs(newLen: Int) {
        midbufLen = newLen
        midbufs = bandFilters.map {
            listOf(FloatArray(midbufLen), FloatArray(midbufLen))
        }
    }

    private val statelessComp = Comp(-24f, ratio, 6f)

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        (bandFilters zip midbufs).forEach { (filter, buf) ->
            // pass thru the band filters
            filter.thru(inbuf, buf)

            // apply the comp fun
            for (ch in outbuf.indices) {
                for (sample in 0 until midbufLen) {
                    statelessComp.thru(buf, buf)

                    // and add up the results to the outbuf
                    outbuf[ch][sample] += buf[ch][sample]
                }
            }
        }

    }

    override fun drawDebugView(batch: SpriteBatch, x: Int, y: Int) {
    }

    override val debugViewHeight: Int = 16

    override fun copyParamsFrom(other: TerrarumAudioFilter) {
        TODO()
    }
}