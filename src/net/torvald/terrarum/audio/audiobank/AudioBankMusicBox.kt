package net.torvald.terrarum.audio.audiobank

import com.badlogic.gdx.utils.Queue
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.audio.AudioBank

/**
 * Created by minjaesong on 2024-04-12.
 */
class AudioBankMusicBox(override var songFinishedHook: (AudioBank) -> Unit = {}) : AudioBank() {

    override val name = "spieluhr"
    override val samplingRate = 48000
    override val channels = 1

    override val totalSizeInSamples = Long.MAX_VALUE // TODO length of lowest-pitch note

    private val messageQueue = Queue<Pair<Long, Long>>() // pair of: absolute tick count, notes (61 notes polyphony)

    override fun currentPositionInSamples(): Long {
        TODO("Not yet implemented")
    }

    override fun readSamples(bufferL: FloatArray, bufferR: FloatArray): Int {
        val tickCount = INGAME.WORLD_UPDATE_TIMER

        TODO("Not yet implemented")
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

    override fun makeCopy(): AudioBank {
        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }
}