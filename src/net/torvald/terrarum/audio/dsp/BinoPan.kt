package net.torvald.terrarum.audio.dsp

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.jme3.math.FastMath
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.audio.AudioMixer
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.AUDIO_BUFFER_SIZE
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATEF
import net.torvald.terrarum.audio.decibelsToFullscale
import net.torvald.terrarum.ceilToInt
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.COL_METER_GRAD
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.COL_METER_GRAD2
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.FILTER_NAME_ACTIVE
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.STRIP_W
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.toIntAndFrac
import net.torvald.terrarum.ui.Toolkit
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * @param pan -1 for far-left, 0 for centre, 1 for far-right
 * @param soundSpeed speed of the sound in meters per seconds
 * @param earDist distance between ears in meters. Maximum: 16.0
 */
class BinoPan(var pan: Float, var earDist: Float = EARDIST_DEFAULT): TerrarumAudioFilter() {

    private val MAX_DELAY = (EARDIST_MAX / AudioMixer.SPEED_OF_SOUND * SAMPLING_RATEF).ceilToInt()

    private val delayLineL = FloatArray(MAX_DELAY)
    private val delayLineR = FloatArray(MAX_DELAY)


    private fun getFrom(index: Float, buf0: FloatArray, buf1: FloatArray): Float {
        val index = index.toInt() // TODO resampling
        return if (index >= 0) buf1[index]
        else buf0[buf0.size + index]
    }

    private val delays = arrayOf(0f, 0f)
    private val mults = arrayOf(1f, 1f)

    private val outLs = Array(2) { FloatArray(AUDIO_BUFFER_SIZE) }
    private val outRs = Array(2) { FloatArray(AUDIO_BUFFER_SIZE) }


    companion object {
        const val EARDIST_DEFAULT = 0.18f
        const val EARDIST_MAX = 16f

        private val PANNING_CONST = 6.0 / 2.0
        private const val L = 0
        private const val R = 1

        private val HALF_PI = (Math.PI / 2.0).toFloat()
    }

    /**
     * @param intensity -inf to +inf
     */
    private fun panningFieldMap(intensity: Float): Float {
        // https://www.desmos.com/calculator/0c6ivoqr52
        return tanh(sqrt(2f) * intensity)
    }


    /**
     * @param angleOffset must be smaller than ±90 deg
     */
    private fun thru(sym: String, angleOffset: Float, inbuf: FloatArray, sumbuf: Array<FloatArray>, delayLine: FloatArray) {
        val pan = panningFieldMap(pan + angleOffset / 90f)

        val timeDiffMax = earDist.coerceAtMost(EARDIST_MAX) / AudioMixer.SPEED_OF_SOUND * SAMPLING_RATEF
        val angle = pan * HALF_PI
        val delayInSamples = if (App.getConfigString("audio_speaker_setup") == "headphone")
                (timeDiffMax * FastMath.sin(angle)).absoluteValue
        else 0f
        val volMultDbThis = PANNING_CONST * pan.absoluteValue
        val volMultFsThis = decibelsToFullscale(volMultDbThis).toFloat()

        val volMultFsOther = if (App.getConfigString("audio_speaker_setup") == "headphone")
            1f / volMultFsThis
        else
            (1f - pan.absoluteValue).coerceIn(0f, 1f)

        if (pan >= 0) {
            delays[L] = delayInSamples
            delays[R] = 0f
        }
        else {
            delays[L] = 0f
            delays[R] = delayInSamples
        }

        if (pan >= 0) {
            mults[L] = volMultFsOther
            mults[R] = volMultFsThis
        }
        else {
            mults[L] = volMultFsThis
            mults[R] = volMultFsOther
        }

        for (i in 0 until AUDIO_BUFFER_SIZE) {
            sumbuf[L][i] = mults[L] * getFrom(i - delays[L], delayLine, inbuf)
            sumbuf[R][i] = mults[R] * getFrom(i - delays[R], delayLine, inbuf)
        }

//        printdbg(this, "$sym\tpan=$pan, mults=${mults[L]}\t${mults[R]}")
    }
    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        thru("L", -50f, inbuf[L], outLs, delayLineL) // 50 will become 59.036 on panningFieldMap
        thru("R", +50f, inbuf[R], outRs, delayLineR)

        for (i in 0 until AUDIO_BUFFER_SIZE) {
            val outL = (outLs[L][i] + outRs[L][i]) / 2f
            val outR = (outLs[R][i] + outRs[R][i]) / 2f
            outbuf[L][i] = outL
            outbuf[R][i] = outR
        }

        push(inbuf[L], delayLineL)
        push(inbuf[R], delayLineR)
    }

    override fun drawDebugView(batch: SpriteBatch, x: Int, y: Int) {
        val w = pan * 0.5f * STRIP_W
        batch.color = COL_METER_GRAD2
        Toolkit.fillArea(batch, x + STRIP_W / 2f, y.toFloat(), w, 14f)
        batch.color = COL_METER_GRAD
        Toolkit.fillArea(batch, x + STRIP_W / 2f, y+14f, w, 2f)

        batch.color = FILTER_NAME_ACTIVE
        val panLabel = if (pan == 0f) "<C>"
        else if (pan < 0) "L${pan.absoluteValue.times(100).toIntAndFrac(3,1)}"
        else "R${pan.absoluteValue.times(100).toIntAndFrac(3,1)}"
        App.fontSmallNumbers.draw(batch, panLabel, x+3f, y+1f)

        App.fontSmallNumbers.draw(batch, "AS:${AudioMixer.SPEED_OF_SOUND.roundToInt()}", x+3f, y+17f)
    }

    override val debugViewHeight = 32
}