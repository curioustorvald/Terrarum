package net.torvald.terrarum.audio

import com.badlogic.gdx.utils.Disposable
import java.io.File

/**
 * Created by minjaesong on 2024-04-05.
 */
abstract class AudioBank : Disposable {

    companion object {
        fun fromMusic(name: String, file: File, looping: Boolean = false, toRAM: Boolean = false, songFinishedHook: (AudioBank) -> Unit = {}): AudioBank {
            return MusicContainer(name, file, looping, toRAM, songFinishedHook)
        }
    }

    protected val hash = System.nanoTime()

    abstract fun makeCopy(): AudioBank

    abstract val name: String

    abstract val samplingRate: Int
    abstract val channels: Int
    abstract val totalSizeInSamples: Long
    abstract fun currentPositionInSamples(): Long

    abstract fun readBytes(buffer: ByteArray): Int
    abstract fun reset()

    abstract val songFinishedHook: (AudioBank) -> Unit
}