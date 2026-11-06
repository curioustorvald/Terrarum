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

    /**
     * Extra gain the mixer puts on the music track's [net.torvald.terrarum.audio.dsp.PreGain] while this audio is
     * its current track, on top of the music volume setting: e.g. a music pack's `global_dsp_request`. 1 = as is.
     */
    open val preGain: Float = 1f

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