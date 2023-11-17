package net.torvald.terrarum.audio

interface TerrarumAudioFilters {
    fun thru(inbufL: FloatArray, inbufR: FloatArray, outbufL: FloatArray, outbufR: FloatArray)
}

object NullFilter: TerrarumAudioFilters {
    override fun thru(inbufL: FloatArray, inbufR: FloatArray, outbufL: FloatArray, outbufR: FloatArray) {
        System.arraycopy(inbufL, 0, outbufL, 0, inbufL.size)
        System.arraycopy(inbufR, 0, outbufR, 0, inbufL.size)
    }
}
