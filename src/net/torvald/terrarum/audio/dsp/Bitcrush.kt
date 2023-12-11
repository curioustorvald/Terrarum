package net.torvald.terrarum.audio.dsp

import net.torvald.terrarum.audio.TerrarumAudioMixerTrack
import net.torvald.terrarum.roundToFloat

class Bitcrush(var steps: Int, var inputGain: Float = 1f): TerrarumAudioFilter() {
    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        for (ch in outbuf.indices) {
            for (i in 0 until TerrarumAudioMixerTrack.AUDIO_BUFFER_SIZE) {
                val inn = ((inbuf[ch][i] * inputGain).coerceIn(-1f, 1f) + 1f) / 2f // 0f..1f
                val stepped = (inn * (steps - 1)).roundToFloat() / (steps - 1)
                val out = (stepped * 2f) - 1f // -1f..1f
                outbuf[ch][i] = out
            }
        }
    }
}