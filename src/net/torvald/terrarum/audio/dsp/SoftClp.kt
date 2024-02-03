package net.torvald.terrarum.audio.dsp

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * This filter compresses the input signal using a fixed artificial Voltage Transfer Characteristic curve,
 * where the curve is linear on the intensity lower than -3dB, then up to +3.5dB the curve flattens
 * (compression ratio steadily reaches infinity) gradually, using simple quadratic curve, then above
 * +3.5dB the curve is completely flat and the compression ratio is infinity.
 *
 * Created by minjaesong on 2023-11-20.
 */
object SoftClp : TerrarumAudioFilter(), DspCompressor {
    override val downForce = arrayOf(1.0f, 1.0f)

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        downForce.fill(1.0f)

        for (ch in inbuf.indices) {
            val inn = inbuf[ch]
            val out = outbuf[ch]

            for (i in inn.indices) {
                val u = inn[i]
                val v = clipfun0(u / 2.0).toFloat() * 2f
                val diff = (v.absoluteValue / u.absoluteValue)
                out[i] = v

                if (!diff.isNaN()) {
                    downForce[ch] = minOf(downForce[ch], diff)
                }
            }
        }
    }

//    private const val clip_p = 0.277777f // knee of around -1.94dB
    private const val clip_p = 0.349f // knee of around -3.01dB
//    private const val clip_p = 0.44444f // knee of around -6.02dB
    private val clip_p1 = sqrt(1.0f - 2.0f * clip_p)
    private val clip_lim = 1.0f / (1.0f + clip_p1)

    /**
     * https://www.desmos.com/calculator/syqd1byzzl
     * @param x0 -0.5..0.5 ish
     * @return -0.5..0.5
     */
    private fun clipfun0(x0: Double): Double {
        val x = x0 * (1.0f + clip_p1) / 2.0f
        val t = 0.5f * clip_p1

        if (x0.absoluteValue >= clip_lim) return 0.5f * sign(x0)

        val y0 = if (x < -t)
            (x*x + x + 0.25f) / clip_p - 0.5f
        else if (x > t)
            -(x*x - x + 0.25f) / clip_p + 0.5f
        else
            x * 2.0f * clip_lim

        return y0
    }

    override fun drawDebugView(batch: SpriteBatch, x: Int, y: Int) {
    }

    override val debugViewHeight = 0

    override fun copyParamsFrom(other: TerrarumAudioFilter) {
    }
}