package net.torvald.terrarum.audio.dsp

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.jme3.math.FastMath
import net.torvald.terrarum.App
import net.torvald.terrarum.audio.AudioMixer
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.AUDIO_BUFFER_SIZE
import net.torvald.terrarum.audio.decibelsToFullscale
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.COL_METER_GRAD
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.COL_METER_GRAD2
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.FILTER_NAME_ACTIVE
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.STRIP_W
import net.torvald.terrarum.ui.BasicDebugInfoWindow.Companion.toIntAndFrac
import net.torvald.terrarum.ui.Toolkit
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * @param pan -1 for far-left, 0 for centre, 1 for far-right
 * @param soundSpeed speed of the sound in meters per seconds
 * @param earDist distance between ears in meters
 */
class BinoPan(var pan: Float, var earDist: Float = 0.18f): TerrarumAudioFilter() {

    private val PANNING_CONST = 3.0 // 3dB panning rule

    private val panUp = decibelsToFullscale(PANNING_CONST / 2.0).toFloat()
    private val panDn = decibelsToFullscale(-PANNING_CONST / 2.0).toFloat()

    private val delayLineL = FloatArray(AUDIO_BUFFER_SIZE)
    private val delayLineR = FloatArray(AUDIO_BUFFER_SIZE)

    private fun getFrom(index: Float, buf0: FloatArray, buf1: FloatArray): Float {
        val index = index.toInt() // TODO resampling
        return if (index >= 0) buf1[index]
        else buf0[buf0.size + index]
    }

    private val delays = arrayOf(0f, 0f)
    private val mults = arrayOf(1f, 1f)

    private val outLs = Array(2) { FloatArray(AUDIO_BUFFER_SIZE) }
    private val outRs = Array(2) { FloatArray(AUDIO_BUFFER_SIZE) }


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
            for (i in 0 until AUDIO_BUFFER_SIZE) {
                outLs[ch][i] = getFrom(i - delays[ch], delayLineL, inbuf[0]) * mults[ch]
                outRs[ch][i] = getFrom(i - delays[ch], delayLineR, inbuf[1]) * mults[ch]
            }
        }

        for (i in 0 until AUDIO_BUFFER_SIZE) {
            outbuf[0][i] = (outLs[0][i] * panUp + outLs[1][i] * panDn) / 2f
            outbuf[1][i] = (outRs[0][i] * panDn + outRs[1][i] * panUp) / 2f
        }

        push(inbuf[0], delayLineL)
        push(inbuf[1], delayLineR)
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