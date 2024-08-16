package net.torvald.terrarum.modulebasegame.audio.audiobank

import net.torvald.terrarum.audio.AudioBank
import net.torvald.terrarum.audio.AudioCodex
import net.torvald.terrarum.tryDispose

/**
 * Created by minjaesong on 2024-08-16.
 */
open class AudioBankTemplateRandomSound(
    private val audioCodex: AudioCodex,
    /** Name must be equal to the name in the AudioCodex identifier */
    override val name: String,

    override var songFinishedHook: (AudioBank) -> Unit
) : AudioBank() {

    override var samplingRate: Float = 48000f
    override var channels: Int = 1
    override var totalSizeInSamples: Long = 1152L


    private var currentSample = audioCodex.getRandomAudio(name)!!.makeCopy()
        set(value) {
            field = value
            samplingRate = value.samplingRate
            channels = value.channels
            totalSizeInSamples = value.totalSizeInSamples
        }

    private fun getNextRandomSample() {
        currentSample = audioCodex.getRandomAudio(name)!!
    }

    override fun makeCopy(): AudioBank {
        return AudioBankTemplateRandomSound(audioCodex, name, songFinishedHook)
    }

    override fun currentPositionInSamples() = currentSample.currentPositionInSamples()

    override fun readSamples(bufferL: FloatArray, bufferR: FloatArray): Int {
        return currentSample.readSamples(bufferL, bufferR)
    }

    override fun reset() {
        currentSample.tryDispose()
        getNextRandomSample()
    }

    override fun dispose() {
        currentSample.tryDispose()
    }
}