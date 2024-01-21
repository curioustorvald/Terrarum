package net.torvald.terrarum.audio.dsp

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import java.io.File
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
class LoFi(ir: File, val crossfeed: Float, gain: Float = 1f / 256f): TerrarumAudioFilter(), DspCompressor {
    override val downForce = arrayOf(1.0f, 1.0f)

    internal val convolver = Convolv(ir, crossfeed, gain)

    private val imm = listOf(FloatArray(App.audioBufferSize), FloatArray(App.audioBufferSize))

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        convolver.thru(inbuf, imm)

        for (ch in imm.indices) {
            val inn = imm[ch]
            val out = outbuf[ch]

            for (i in inn.indices) {
                val u = inn[i]
                val v = tanh(u)
                val diff = (v.absoluteValue / u.absoluteValue)
                out[i] = v

                if (!diff.isNaN()) {
                    downForce[ch] = minOf(downForce[ch], diff)
                }
            }
        }
    }

    override fun drawDebugView(batch: SpriteBatch, x: Int, y: Int) {
    }

    override val debugViewHeight = 0

    override fun copyParamsFrom(other: TerrarumAudioFilter) {
        this.convolver.copyParamsFrom((other as LoFi).convolver)
    }
}