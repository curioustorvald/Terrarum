package net.torvald.terrarum.audio.dsp

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.audio.AudioHelper
import kotlin.math.absoluteValue
import kotlin.math.tanh

/**
 * Convolver with tanh saturator
 *
 * @param ir Binary file containing MONO IR (containing only two channels)
 * @param crossfeed The amount of channel crossfeeding to simulate the true stereo IR. Fullscale (0.0 - 1.0)
 * @param gain output gain. Fullscale (0.0 - 1.0)
 *
 * Created by minjaesong on 2024-01-21.
 */
open class LoFi(
    staticModule: String, staticPath: String,
    irModule: String, irPath: String,
    val crossfeed: Float, gain: Float = 1f / 256f, saturationLim: Float = 1f
): TerrarumAudioFilter(), DspCompressor {
    override val downForce = arrayOf(1.0f, 1.0f)

    internal val staticSample = AudioHelper.getAudioInSamples(staticModule, staticPath)
    private var staticSamplePlayCursor = 0

    internal val convolver = Convolv(irModule, irPath, crossfeed, gain)

    private val immAfterStaticMix = listOf(FloatArray(App.audioBufferSize), FloatArray(App.audioBufferSize))
    private val immAfterSat = listOf(FloatArray(App.audioBufferSize), FloatArray(App.audioBufferSize))

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        staticMixThru(inbuf, immAfterStaticMix)
        saturatorThru(immAfterStaticMix, immAfterSat)
        convolver.thru(immAfterSat, outbuf)
    }

    private fun staticMixThru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        for (h in 0 until App.audioBufferSize) {
            outbuf[0][h] = inbuf[0][h] + staticSample[0][staticSamplePlayCursor] * 2.512f // +8 dB
            outbuf[1][h] = inbuf[1][h] + staticSample[1][staticSamplePlayCursor] * 2.512f // +8 dB
            staticSamplePlayCursor = (staticSamplePlayCursor + 1) % staticSample[0].size
        }
    }

    private fun saturatorThru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        downForce.fill(1.0f)

        for (ch in inbuf.indices) {
            for (i in inbuf[ch].indices) {
                val u = inbuf[ch][i]
                val v = saturate(u)
                val diff = (v.absoluteValue / u.absoluteValue)
                outbuf[ch][i] = v

                if (!diff.isNaN()) {
                    downForce[ch] = minOf(downForce[ch], diff)
                }
            }
        }
    }

    /**
     * Saturation function aka Voltage Transfer Characteristic Curve.
     * Default function is `tanh(x)`
     */
    open fun saturate(v: Float): Float {
        return K * tanh(v / K)
    }
    private val K = saturationLim.coerceIn(0f, 1f)

    override fun drawDebugView(batch: SpriteBatch, x: Int, y: Int) {
        convolver.drawDebugView(batch, x, y)
    }

    override val debugViewHeight = 16

    override fun copyParamsFrom(other: TerrarumAudioFilter) {
        this.convolver.copyParamsFrom((other as LoFi).convolver)
    }
}