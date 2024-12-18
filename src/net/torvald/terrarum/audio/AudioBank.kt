package net.torvald.terrarum.audio

import com.badlogic.gdx.utils.Disposable

/**
 * Created by minjaesong on 2024-04-05.
 */
abstract class AudioBank : Disposable {

    /**
     * If the audio bank is a virtual instrument, set this property to `true`; if the audio bank reads audio
     * sample directly from the disk, set it to `false`
     */
    open val notCopyable: Boolean = false

    protected val hash = System.nanoTime()

    abstract fun makeCopy(): AudioBank

    abstract val name: String

    abstract var samplingRate: Float
    abstract var channels: Int
    abstract var totalSizeInSamples: Long
    abstract fun currentPositionInSamples(): Long

    open fun sendMessage(msg: String) {}
    open fun sendMessage(bits: Long) {}

    abstract fun readSamples(bufferL: FloatArray, bufferR: FloatArray): Int
    abstract fun reset()

    abstract var songFinishedHook: (AudioBank) -> Unit
}