package net.torvald.terrarum.audio

import com.badlogic.gdx.utils.Disposable

/**
 * Created by minjaesong on 2024-04-05.
 */
abstract class AudioBank : Disposable {

    open val notCopyable: Boolean = false

    protected val hash = System.nanoTime()

    abstract fun makeCopy(): AudioBank

    abstract val name: String

    abstract val samplingRate: Int
    abstract val channels: Int
    abstract val totalSizeInSamples: Long
    abstract fun currentPositionInSamples(): Long

    open fun sendMessage(msg: String) {}
    open fun sendMessage(bits: Long) {}

    abstract fun readSamples(bufferL: FloatArray, bufferR: FloatArray): Int
    abstract fun reset()

    abstract val songFinishedHook: (AudioBank) -> Unit
}