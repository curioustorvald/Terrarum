package net.torvald.terrarum.audio.dsp

import com.jme3.math.FastMath
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack

class Highpass(cutoff0: Float): TerrarumAudioFilter() {

    var cutoff = cutoff0.toDouble(); private set
    private var alpha: Float = 0f

    val in0 = FloatArray(2)
    val out0 = FloatArray(2)

    init {
        setCutoff(cutoff0)
    }

    fun setCutoff(cutoff: Float) {
//        println("LP Cutoff: $cutoff")
        val RC: Float = 1f / (cutoff * FastMath.TWO_PI)
        val dt: Float = 1f / TerrarumAudioMixerTrack.SAMPLING_RATEF
        alpha = RC / (RC + dt)
        this.cutoff = cutoff.toDouble()
    }

    fun setCutoff(cutoff: Double) {
//        println("LP Cutoff: $cutoff")
        val RC: Double = 1.0 / (cutoff * Math.PI * 2.0)
        val dt: Double = 1.0 / TerrarumAudioMixerTrack.SAMPLING_RATEF
        alpha = (RC / (RC + dt)).toFloat()
        this.cutoff = cutoff
    }

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        for (ch in outbuf.indices) {
            val out = outbuf[ch]
            val inn = inbuf[ch]

            out[0] = alpha * (out0[ch] + inn[0] - in0[ch])

            for (i in 1 until outbuf[ch].size) {
                out[i] = alpha * (out[i-1] + inn[i] - inn[i-1])
            }

            out0[ch] = outbuf[ch].last()
            in0[ch] = inbuf[ch].last()
        }
    }

}