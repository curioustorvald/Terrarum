package net.torvald.terrarum.audio.dsp

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.reflection.forceInvoke
import net.torvald.terrarum.App
import net.torvald.terrarum.audio.AudioProcessBuf.Companion.MP3_CHUNK_SIZE
import net.torvald.terrarum.serialise.toUint
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
open class LoFi(static: File, ir: File, val crossfeed: Float, gain: Float = 1f / 256f): TerrarumAudioFilter(), DspCompressor {
    override val downForce = arrayOf(1.0f, 1.0f)

    internal val staticSample: List<FloatArray>
    private var staticSamplePlayCursor = 0

    init {
        val music = Gdx.audio.newMusic(Gdx.files.absolute(static.absolutePath))
        val readbuf = ByteArray(MP3_CHUNK_SIZE * 4)
        val OUTBUF_BLOCK_SIZE_IN_BYTES = (48000 * 60) * 2 * 2
        var outbuf = ByteArray(OUTBUF_BLOCK_SIZE_IN_BYTES)
        var bytesRead = 0

        fun expandOutbuf() {
            val newOutBuf = ByteArray(outbuf.size + OUTBUF_BLOCK_SIZE_IN_BYTES)
            System.arraycopy(outbuf, 0, newOutBuf, 0, outbuf.size)
            outbuf = newOutBuf
        }

        while (true) {
            val readSize = music.forceInvoke<Int>("read", arrayOf(readbuf))!!
            if (readSize <= 0) break

            // check if outbuf has room
            if (bytesRead + readSize > outbuf.size) expandOutbuf()

            // actually copy the bytes
            System.arraycopy(readbuf, 0, outbuf, bytesRead, readSize)

            bytesRead += readSize
        }

        // convert bytes to float samples
        staticSample = listOf(FloatArray(bytesRead / 4), FloatArray(bytesRead / 4))
        for (i in staticSample[0].indices) {
            staticSample[0][i] = (outbuf[4*i+0].toUint() or outbuf[4*i+1].toUint().shl(8)).toShort() / 32767f
            staticSample[1][i] = (outbuf[4*i+2].toUint() or outbuf[4*i+3].toUint().shl(8)).toShort() / 32767f
        }

    }

    internal val convolver = Convolv(ir, crossfeed, gain)

    private val immAfterStaticMix = listOf(FloatArray(App.audioBufferSize), FloatArray(App.audioBufferSize))
    private val immAfterConvolv = listOf(FloatArray(App.audioBufferSize), FloatArray(App.audioBufferSize))

    override fun thru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        staticMixThru(inbuf, immAfterStaticMix)
        convolver.thru(immAfterStaticMix, immAfterConvolv)
        saturatorThru(immAfterConvolv, outbuf)
    }

    private fun staticMixThru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
        for (h in 0 until App.audioBufferSize) {
            outbuf[0][h] = inbuf[0][h] + staticSample[0][staticSamplePlayCursor]
            outbuf[1][h] = inbuf[1][h] + staticSample[1][staticSamplePlayCursor]
            staticSamplePlayCursor = (staticSamplePlayCursor + 1) % staticSample[0].size
        }
    }

    private fun saturatorThru(inbuf: List<FloatArray>, outbuf: List<FloatArray>) {
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
        return tanh(v)
    }

    override fun drawDebugView(batch: SpriteBatch, x: Int, y: Int) {
    }

    override val debugViewHeight = 0

    override fun copyParamsFrom(other: TerrarumAudioFilter) {
        this.convolver.copyParamsFrom((other as LoFi).convolver)
    }
}