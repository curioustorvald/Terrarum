package net.torvald.terrarum.audio

import com.jme3.math.FastMath

interface TerrarumAudioFilter {
    fun thru(inbuf0: List<FloatArray>, inbuf1: List<FloatArray>, outbuf0: List<FloatArray>, outbuf1: List<FloatArray>)
}

object NullFilter: TerrarumAudioFilter {
    override fun thru(inbuf0: List<FloatArray>, inbuf1: List<FloatArray>, outbuf0: List<FloatArray>, outbuf1: List<FloatArray>) {
        outbuf1.forEachIndexed { index, outTrack ->
            System.arraycopy(inbuf1[index], 0, outTrack, 0, outTrack.size)
        }
    }
}


class Lowpass(cutoff: Int, rate: Int): TerrarumAudioFilter {

    val alpha: Float
    init {
        val RC: Float = 1f / (cutoff.toFloat() * FastMath.TWO_PI)
        val dt: Float = 1f / rate
        alpha = dt / (RC + dt)
    }

    override fun thru(inbuf0: List<FloatArray>, inbuf1: List<FloatArray>, outbuf0: List<FloatArray>, outbuf1: List<FloatArray>) {
        for (ch in outbuf1.indices) {
            val out = outbuf1[ch]
            val inn = inbuf1[ch]

            out[0] = outbuf0[ch].last()

            for (i in 1 until outbuf1[ch].size) {
                out[i] = out[i-1] + (alpha * (inn[i] - out[i-1]))
            }
        }
    }

}