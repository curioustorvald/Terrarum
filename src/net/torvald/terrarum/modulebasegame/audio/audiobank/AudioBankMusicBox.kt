package net.torvald.terrarum.modulebasegame.audio.audiobank

import com.badlogic.gdx.utils.Queue
import net.torvald.terrarum.App
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.audio.AudioBank

/**
 * Created by minjaesong on 2024-04-12.
 */
class AudioBankMusicBox(override var songFinishedHook: (AudioBank) -> Unit = {}) : AudioBank() {

    private data class Msg(val tick: Long, val samplesL: FloatArray, val samplesR: FloatArray, var samplesDispatched: Int = 0) // in many cases, samplesL and samplesR will point to the same object

    override val name = "spieluhr"
    override val samplingRate = 48000
    override val channels = 1

    private val getSample = // usage: getSample(noteNum 0..60)
        InstrumentLoader.load("spieluhr", "basegame", "audio/effects/notes/spieluhr.ogg", 29)

    private val SAMEPLES_PER_TICK = samplingRate / App.TICK_SPEED // should be 800 on default setting

    override val totalSizeInSamples = getSample(0).first.size.toLong() // length of lowest-pitch note

    private val messageQueue = Queue<Msg>()

    private fun findSetBits(num: Long): List<Int> {
        val result = mutableListOf<Int>()
        for (i in 0 until 61) {
            if (num and (1L shl i) != 0L) {
                result.add(i)
            }
        }
        return result
    }

    /**
     * Queues the notes such that they are played on the next tick
     */
    fun queuePlay(noteBits: Long) {
        if (noteBits == 0L) return

        val tick = INGAME.WORLD_UPDATE_TIMER + 1
        val notes = findSetBits(noteBits)

        val buf = FloatArray(getSample(notes.first()).first.size)

        // combine all those samples
        notes.forEach { note ->
            getSample(note).first.forEachIndexed { index, fl ->
                buf[index] += fl
            }
        }

        // actually queue it
        messageQueue.addLast(Msg(tick, buf, buf))
    }

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