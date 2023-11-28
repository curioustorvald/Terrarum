package net.torvald.terrarum.audio.dsp

object Buffer : TerrarumAudioFilter() {
    init {
        bypass = true
    }

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        bypass = true
    }
}