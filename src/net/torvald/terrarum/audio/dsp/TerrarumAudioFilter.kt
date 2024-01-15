package net.torvald.terrarum.audio.dsp

import com.badlogic.gdx.graphics.g2d.SpriteBatch

abstract class TerrarumAudioFilter {
    var bypass = false
    protected abstract fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>)
    operator fun invoke(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        if (bypass) {
            outbuf.forEachIndexed { index, outTrack ->
                System.arraycopy(inbuf[index], 0, outTrack, 0, outTrack.size)
            }
        }
        else thru(inbuf, outbuf)
    }
    abstract fun drawDebugView(batch: SpriteBatch, x: Int, y: Int)
    abstract val debugViewHeight: Int
}

fun FloatArray.applyGain(gain: Float = 1f) = this.map { it * gain }.toFloatArray()
fun push(samples: FloatArray, buf: FloatArray) {
    if (samples.size >= buf.size) {
        System.arraycopy(samples, samples.size - buf.size, buf, 0, buf.size)
    }
    else {
        System.arraycopy(buf, samples.size, buf, 0, buf.size - samples.size)
        System.arraycopy(samples, 0, buf, buf.size - samples.size, samples.size)
    }
}