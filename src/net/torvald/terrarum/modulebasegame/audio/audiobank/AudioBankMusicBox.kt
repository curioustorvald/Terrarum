package net.torvald.terrarum.modulebasegame.audio.audiobank

import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.audio.AudioBank

/**
 * Created by minjaesong on 2024-04-12.
 */
class AudioBankMusicBox(override var songFinishedHook: (AudioBank) -> Unit = {}) : AudioBank() {

    override val notCopyable = true

    internal data class Msg(val tick: Long, val notes: IntArray, val maxlen: Int, var samplesDispatched: Int = 0) { // in many cases, samplesL and samplesR will point to the same object
        override fun toString(): String {
            return "Msg(tick=$tick, samplesDispatched=$samplesDispatched)"
        }
    }

    override val name = "spieluhr"
    override var samplingRate = 48000f // 122880 // use 122880 to make each tick is 2048 samples
    override var channels = 1

    private val getSample = // usage: getSample(noteNum 0..60)
        InstrumentLoader.load("spieluhr", "basegame", "audio/effects/notes/spieluhr.ogg", 41)

    private val SAMPLES_PER_TICK = samplingRate / App.TICK_SPEED // should be 800 on default setting

    override var totalSizeInSamples = getSample(0).first.size.toLong() // length of lowest-pitch note

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

    init {
        printdbg(this, "Note 0 length: ${getSample(0).first.size}")
        printdbg(this, "Note 12 length: ${getSample(12).first.size}")
        printdbg(this, "Note 24 length: ${getSample(24).first.size}")
        printdbg(this, "Note 48 length: ${getSample(48).first.size}")
    }

    /**
     * Queues the notes such that they are played on the next tick
     */
    override fun sendMessage(noteBits: Long) {
        queue(INGAME.WORLD_UPDATE_TIMER + 1, noteBits)
    }

    private fun queue(tick: Long, noteBits: Long) {
        if (noteBits == 0L) return

        val notes = findSetBits(noteBits).toIntArray()
        val maxlen = getSample(notes.first()).first.size

        // actually queue it
        val msg = Msg(tick, notes, maxlen)
        messageQueue.add(messageQueue.size, msg)
    }

    override fun currentPositionInSamples() = 0L

    override fun readSamples(bufferL: FloatArray, bufferR: FloatArray): Int {
        val tickCount = INGAME.WORLD_UPDATE_TIMER
        val bufferSize = bufferL.size

        bufferL.fill(0f)
        bufferR.fill(0f)

        // only copy over the past and current messages
        // use cloned version of queue to prevent concurrent modification exception
        messageQueue.toMutableList().filter { it.tick <= tickCount }.forEach {
            // copy over the samples
            it.notes.forEach { note ->
                val noteSamples = getSample(note)
                val start = it.samplesDispatched
                val end = minOf(start + bufferSize, noteSamples.first.size)

                for (i in start until end) {
                    bufferL[i - start] += noteSamples.first[i]
                    bufferR[i - start] += noteSamples.second[i]
                }
            }

            it.samplesDispatched += bufferSize
        }

        // dequeue the finished messages
        var rc = 0
        while (rc < messageQueue.size) {
            val it = messageQueue[rc]
            if (it.samplesDispatched >= it.maxlen) {
                messageQueue.removeAt(rc)
                rc -= 1
            }

            rc += 1
        }

        return bufferSize
    }

    override fun reset() {
        messageQueue.clear()
    }

    override fun makeCopy(): AudioBank {
        throw UnsupportedOperationException()
    }

    override fun dispose() {
    }


}