package net.torvald.terrarum.audio

import com.badlogic.gdx.Gdx
import com.jme3.math.FastMath
import net.torvald.reflection.forceInvoke
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.serialise.toUint
import java.io.File

/**
 * Created by minjaesong on 2024-01-24.
 */
object AudioHelper {

    fun getIR(module: String, path: String): Array<ComplexArray> {
        val id = "convolution$$module.$path"

        if (CommonResourcePool.resourceExists(id)) {
            val fft = CommonResourcePool.getAs<Array<ComplexArray>>(id)

            return fft
        }
        else {
            val ir = ModMgr.getFile(module, path)
            val fft = createIR(ir)

            CommonResourcePool.addToLoadingList(id) { fft }
            CommonResourcePool.loadAll()

            return fft
        }
    }

    private fun createIR(ir: File): Array<ComplexArray> {
        if (!ir.exists()) {
            throw IllegalArgumentException("Impulse Response file '${ir.path}' does not exist.")
        }

        val sampleCount = (ir.length().toInt() / 8)//.coerceAtMost(65536)
        val fftLen = FastMath.nextPowerOfTwo(sampleCount)

        printdbg(this, "IR '${ir.path}' Sample Count = $sampleCount; FFT Length = $fftLen")

        val conv = Array(2) { FloatArray(fftLen) }

        ir.inputStream().let {
            for (i in 0 until sampleCount) {
                val f1 = Float.fromBits(it.read().and(255) or
                        it.read().and(255).shl(8) or
                        it.read().and(255).shl(16) or
                        it.read().and(255).shl(24))
                val f2 = Float.fromBits(it.read().and(255) or
                        it.read().and(255).shl(8) or
                        it.read().and(255).shl(16) or
                        it.read().and(255).shl(24))
                conv[0][i] = f1
                conv[1][i] = f2
            }

            it.close()
        }

        // fourier-transform the 'conv'
        return Array(2) { FFT.fft(conv[it]) }
    }

    fun getAudioInSamples(module: String, path: String): Array<FloatArray> {
        val id = "audiosamplesf32$$module.$path"

        if (CommonResourcePool.resourceExists(id)) {
            return CommonResourcePool.getAs<Array<FloatArray>>(id)
        }
        else {
            val file = ModMgr.getFile(module, path)
            val samples = createAudioInSamples(file)

            CommonResourcePool.addToLoadingList(id) { samples }
            CommonResourcePool.loadAll()

            return samples
        }
    }

    private fun createAudioInSamples(static: File): Array<FloatArray> {
        val music = Gdx.audio.newMusic(Gdx.files.absolute(static.absolutePath))
        val readbuf = ByteArray(AudioProcessBuf.MP3_CHUNK_SIZE * 4)
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
        val staticSample = arrayOf(FloatArray(bytesRead / 4), FloatArray(bytesRead / 4))
        for (i in staticSample[0].indices) {
            staticSample[0][i] = (outbuf[4*i+0].toUint() or outbuf[4*i+1].toUint().shl(8)).toShort() / 32767f
            staticSample[1][i] = (outbuf[4*i+2].toUint() or outbuf[4*i+3].toUint().shl(8)).toShort() / 32767f
        }
        return staticSample
    }

}