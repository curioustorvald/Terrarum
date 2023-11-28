package net.torvald.terrarum.audio.dsp

import com.jme3.math.FastMath
import net.torvald.terrarum.audio.AudioMixer
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack
import net.torvald.terrarum.audio.decibelsToFullscale
import kotlin.math.absoluteValue

/**
 * The input audio must be monaural
 *
 * @param pan -1 for far-left, 0 for centre, 1 for far-right
 * @param soundSpeed speed of the sound in meters per seconds
 * @param earDist distance between ears in meters
 */
class BinoPan(var pan: Float, var earDist: Float = 0.18f): TerrarumAudioFilter() {

    private val PANNING_CONST = 3.0 // 3dB panning rule

    private val delayLine = FloatArray(TerrarumAudioMixerTrack.BUFFER_SIZE / 4)

    private fun getFrom(index: Float, buf0: FloatArray, buf1: FloatArray): Float {
        val index = index.toInt() // TODO resampling
        return if (index >= 0) buf1[index]
        else buf0[buf0.size + index]
    }

    private val delays = arrayOf(0f, 0f)
    private val mults = arrayOf(1f, 1f)

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        val angle = pan * 1.5707963f
        val timeDiffMax = earDist / AudioMixer.SPEED_OF_SOUND * TerrarumAudioMixerTrack.SAMPLING_RATEF
        val delayInSamples = (timeDiffMax * FastMath.sin(angle)).absoluteValue
        val volMultDbThis = PANNING_CONST * pan.absoluteValue
        val volMultFsThis = decibelsToFullscale(volMultDbThis).toFloat()
        val volMUltFsOther = 1f / volMultFsThis

        if (pan >= 0) {
            delays[0] = delayInSamples
            delays[1] = 0f
        }
        else {
            delays[0] = 0f
            delays[1] = delayInSamples
        }

        if (pan >= 0) {
            mults[0] = volMUltFsOther
            mults[1] = volMultFsThis
        }
        else {
            mults[0] = volMultFsThis
            mults[1] = volMUltFsOther
        }

        for (ch in 0..1) {
            for (i in 0 until TerrarumAudioMixerTrack.BUFFER_SIZE / 4) {
                outbuf[ch][i] = getFrom(i - delays[ch], delayLine, inbuf[0]) * mults[ch]
            }
        }

        push(inbuf[0], delayLine)
    }
}