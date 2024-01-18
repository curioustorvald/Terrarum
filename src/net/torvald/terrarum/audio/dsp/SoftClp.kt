package net.torvald.terrarum.audio.dsp

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sqrt

object SoftClp : TerrarumAudioFilter() {
    val downForce = arrayOf(1.0f, 1.0f)

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

    /**
     * https://www.desmos.com/calculator/syqd1byzzl
     * @param x0 -0.5..0.5 ish
     * @return -0.5..0.5
     */
    private fun clipfun0(x0: Double): Double {
//        val p = 0.277777 // knee of around -1.94dB
        val p = 0.349 // knee of around -3.01dB
//        val p = 0.44444 // knee of around -6.02dB
        val p1 = sqrt(1.0 - 2.0 * p)

        val x = x0 * (1.0 + p1) / 2.0
        val t = 0.5 * p1

        val lim = 1.0 / (1.0 + p1)

        if (x0 >= lim) return 0.5
        if (x0 <= -lim) return -0.5

        val y0 = if (x < -t)
            (1.0 / p) * (x + 0.5).pow(2) - 0.5
        else if (x > t)
            -(1.0 / p) * (x - 0.5).pow(2) + 0.5
        else
            x * 2.0 * lim

        return y0
    }

    override fun drawDebugView(batch: SpriteBatch, x: Int, y: Int) {
    }

    override val debugViewHeight = 0

    override fun copyParamsFrom(other: TerrarumAudioFilter) {
    }
}