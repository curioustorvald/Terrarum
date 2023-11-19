package net.torvald.terrarum.audio

import com.jme3.math.FastMath

abstract class TerrarumAudioFilter {
    var bypass = false
    protected abstract fun thru(inbuf0: List<FloatArray>, inbuf1: List<FloatArray>, outbuf0: List<FloatArray>, outbuf1: List<FloatArray>)
    operator fun invoke(inbuf0: List<FloatArray>, inbuf1: List<FloatArray>, outbuf0: List<FloatArray>, outbuf1: List<FloatArray>) {
        if (bypass) {
            outbuf1.forEachIndexed { index, outTrack ->
                System.arraycopy(inbuf1[index], 0, outTrack, 0, outTrack.size)
            }
        }
        else thru(inbuf0, inbuf1, outbuf0, outbuf1)
    }
}

object NullFilter : TerrarumAudioFilter() {
    override fun thru(inbuf0: List<FloatArray>, inbuf1: List<FloatArray>, outbuf0: List<FloatArray>, outbuf1: List<FloatArray>) {
        outbuf1.forEachIndexed { index, outTrack ->
            System.arraycopy(inbuf1[index], 0, outTrack, 0, outTrack.size)
        }
    }
}


class Lowpass(cutoff0: Float, val rate: Int): TerrarumAudioFilter() {

    var cutoff = cutoff0.toDouble(); private set
    private var alpha: Float = 0f

    init {
        setCutoff(cutoff0)
    }

    fun setCutoff(cutoff: Float) {
//        println("LP Cutoff: $cutoff")
        val RC: Float = 1f / (cutoff * FastMath.TWO_PI)
        val dt: Float = 1f / rate
        alpha = dt / (RC + dt)
        this.cutoff = cutoff.toDouble()
    }

    fun setCutoff(cutoff: Double) {
//        println("LP Cutoff: $cutoff")
        val RC: Double = 1.0 / (cutoff * Math.PI * 2.0)
        val dt: Double = 1.0 / rate
        alpha = (dt / (RC + dt)).toFloat()
        this.cutoff = cutoff
    }

    override fun thru(inbuf0: List<FloatArray>, inbuf1: List<FloatArray>, outbuf0: List<FloatArray>, outbuf1: List<FloatArray>) {
        for (ch in outbuf1.indices) {
            val out = outbuf1[ch]
            val inn = inbuf1[ch]

            out[0] = outbuf0[ch].last()

            for (i in 1 until outbuf1[ch].size) {
                out[i] = out[i-1] + alpha * (inn[i] - out[i-1])
            }
        }
    }

}


class Highpass(cutoff0: Float, val rate: Int): TerrarumAudioFilter() {

    var cutoff = cutoff0.toDouble(); private set
    private var alpha: Float = 0f

    init {
        setCutoff(cutoff0)
    }

    fun setCutoff(cutoff: Float) {
//        println("LP Cutoff: $cutoff")
        val RC: Float = 1f / (cutoff * FastMath.TWO_PI)
        val dt: Float = 1f / rate
        alpha = dt / (RC + dt)
        this.cutoff = cutoff.toDouble()
    }

    fun setCutoff(cutoff: Double) {
//        println("LP Cutoff: $cutoff")
        val RC: Double = 1.0 / (cutoff * Math.PI * 2.0)
        val dt: Double = 1.0 / rate
        alpha = (RC / (RC + dt)).toFloat()
        this.cutoff = cutoff
    }

    override fun thru(inbuf0: List<FloatArray>, inbuf1: List<FloatArray>, outbuf0: List<FloatArray>, outbuf1: List<FloatArray>) {
        for (ch in outbuf1.indices) {
            val out = outbuf1[ch]
            val inn = inbuf1[ch]

            out[0] = outbuf0[ch].last()

            for (i in 1 until outbuf1[ch].size) {
                out[i] = alpha * (out[i-1] + inn[i] - inn[i-1])
            }
        }
    }

}


object Buffer : TerrarumAudioFilter() {
    init {
        bypass = true
    }

    override fun thru(inbuf0: List<FloatArray>, inbuf1: List<FloatArray>, outbuf0: List<FloatArray>, outbuf1: List<FloatArray>) {
        bypass = true
    }
}