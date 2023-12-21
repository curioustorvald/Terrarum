package net.torvald.terrarum.audio.dsp

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import kotlin.math.absoluteValue
import kotlin.math.tanh

object SoftClp : TerrarumAudioFilter() {
    val downForce = arrayOf(1.0f, 1.0f)

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        downForce.fill(1.0f)

        for (ch in inbuf.indices) {
            val inn = inbuf[ch]
            val out = outbuf[ch]

            for (i in inn.indices) {
                val u = inn[i] * 0.95f
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
}