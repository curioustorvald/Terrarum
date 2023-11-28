package net.torvald.terrarum.audio.dsp

object NullFilter : TerrarumAudioFilter() {
    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        outbuf.forEachIndexed { index, outTrack ->
            System.arraycopy(inbuf[index], 0, outTrack, 0, outTrack.size)
        }
    }
}