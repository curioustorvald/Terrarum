package net.torvald.terrarum.modulebasegame.audio.audiobank

import net.torvald.terrarum.App
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.audio.AudioBank

/**
 * Created by minjaesong on 2024-04-12.
 */
class AudioBankMusicBox(override var songFinishedHook: (AudioBank) -> Unit = {}) : AudioBank() {

    private data class Msg(val tick: Long, val samplesL: FloatArray, val samplesR: FloatArray, var samplesDispatched: Int = 0) // in many cases, samplesL and samplesR will point to the same object

    override val name = "spieluhr"
    override val samplingRate = 48000 // 122880 // use 122880 to make each tick is 2048 samples
    override val channels = 1

    private val getSample = // usage: getSample(noteNum 0..60)
        InstrumentLoader.load("spieluhr", "basegame", "audio/effects/notes/spieluhr.ogg", 29)

    private val SAMPLES_PER_TICK = samplingRate / App.TICK_SPEED // should be 800 on default setting

    override val totalSizeInSamples = getSample(0).first.size.toLong() // length of lowest-pitch note

    private val messageQueue = ArrayList<Msg>()

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
    override fun sendMessage(noteBits: Long) {
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
        messageQueue.add(Msg(tick, buf, buf))
    }

    override fun currentPositionInSamples() = 0L

    override fun readSamples(bufferL: FloatArray, bufferR: FloatArray): Int {
        val tickCount = INGAME.WORLD_UPDATE_TIMER
        val bufferSize = bufferL.size

        // only copy over the past and current messages
        messageQueue.filter { it.tick <= tickCount }.forEach {
            // copy over the samples
            for (i in 0 until minOf(bufferSize, it.samplesL.size - it.samplesDispatched)) {
                bufferL[i] += it.samplesL[i + it.samplesDispatched]
                bufferR[i] += it.samplesR[i + it.samplesDispatched]
            }

            it.samplesDispatched += bufferSize
        }


        // dequeue the finished messages
        val messagesToKill = ArrayList<Msg>(messageQueue.filter { it.samplesDispatched >= it.samplesL.size })
        if (messagesToKill.isNotEmpty()) messagesToKill.forEach {
            messageQueue.remove(it)
        }

        return bufferSize
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

    private fun prel(n1: Int, n2: Int, n3: Int, n4: Int, n5: Int): List<Int> {
        return listOf(
            n1, n2, n3, n4, n5, n3, n4, n5,
            n1, n2, n3, n4, n5, n3, n4, n5
        )
    }

    private val testNotes = prel(0,0,0,0,0)+
            prel(24,28,31,36,40) +
            prel(24, 26,33,28,41) +
            prel(23,26,31,38,41) +
            prel(24,28,31,36,40) +
            prel(24,28,33,40,45) +
            prel(24,26,30,33,38) +
            prel(23,26,31,38,43) +
            prel(23,24,28,31,36) +
            prel(21,24,28,31,36)

}