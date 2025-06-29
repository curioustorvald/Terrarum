package net.torvald.terrarum.audio

import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarum.App
import org.jtransforms.fft.FloatFFT_1D

private val RE0 = 0
private val IM0 = 1

private val RE1 = -1
private val IM1 = 0

private val mulBuf = FloatArray(2)
@JvmInline value class ComplexArray(val reim: FloatArray) {


    val indices: IntProgression
        get() = 0 until size
    val size: Int
        get() = reim.size / 2

    operator fun times(other: ComplexArray): ComplexArray {
        val out = FloatArray(size * 2) {
            if (it % 2 == 0)
                reim[it+RE0] * other.reim[it+RE0] - reim[it+IM0] * other.reim[it+IM0]
            else
                reim[it+RE1] *other.reim[it+IM1] + reim[it+IM1] * other.reim[it+RE1]

        }

        return ComplexArray(out)
    }

    fun mult(other: ComplexArray, out: ComplexArray) {
        for (it in 0 until size * 2) {
            out.reim[it] =
            if (it % 2 == 0)
                reim[it+RE0] * other.reim[it+RE0] - reim[it+IM0] * other.reim[it+IM0]
            else
                reim[it+RE1] *other.reim[it+IM1] + reim[it+IM1] * other.reim[it+RE1]

        }
    }

    // this is actually slower that having a separate array for mult results
    /*fun inlineMult(other: ComplexArray) {
        for (it in 0 until size * 2) {
            mulBuf[it % 2] = if (it % 2 == 0)
                reim[it+RE0] * other.reim[it+RE0] - reim[it+IM0] * other.reim[it+IM0]
            else
                reim[it+RE1] *other.reim[it+IM1] + reim[it+IM1] * other.reim[it+RE1]

            if (it % 2 == 1) {
                reim[it+RE1] = mulBuf[0]
                reim[it+IM1] = mulBuf[1]
            }
        }
    }*/

    fun getReal(): FloatArray {
        return FloatArray(size) { reim[it * 2] }
    }
}

/**
 * Helper object to call JTransforms
 *
 * Created by minjaesong on 2023-11-25.
 */
object FFT: Disposable {

    private val ffts = hashMapOf(
        128 to FloatFFT_1D(128),
        256 to FloatFFT_1D(256),
        512 to FloatFFT_1D(512),
        1024 to FloatFFT_1D(1024),
        2048 to FloatFFT_1D(2048),
        4096 to FloatFFT_1D(4096),
        8192 to FloatFFT_1D(8192),
        16384 to FloatFFT_1D(16384),
        32768 to FloatFFT_1D(32768),
        65536 to FloatFFT_1D(65536),
    )


    init {
        App.disposables.add(this)
    }

    override fun dispose() {
    }

    fun fft(signal0: FloatArray): ComplexArray {
        val signal = FloatArray(signal0.size * 2) { if (it % 2 == 0) signal0[it / 2] else 0f }
        ffts[signal0.size]!!.complexForward(signal)
        return ComplexArray(signal)
    }

    fun fft(signal0: ComplexArray) {
        ffts[signal0.size]!!.complexForward(signal0.reim)
    }

    fun fftInto(signal0: ComplexArray, out: ComplexArray) {
        System.arraycopy(signal0.reim, 0, out.reim, 0, signal0.reim.size)
        ffts[signal0.size]!!.complexForward(out.reim)
    }

    fun ifftAndGetReal(signal0: ComplexArray): FloatArray {
        ffts[signal0.size]!!.complexInverse(signal0.reim, true)
        return signal0.getReal()
    }

    fun ifftAndGetReal(signal0: ComplexArray, output: FloatArray) {
        ffts[signal0.size]!!.complexInverse(signal0.reim, true)
        for (i in 0 until signal0.size) {
            output[i] = signal0.reim[i * 2]
        }
    }
}