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

    // TODO don't store samples (1MB each!), store numbers instead and synthesize on readSamples()
    internal data class Msg(val tick: Long, val samplesL: FloatArray, val samplesR: FloatArray, var samplesDispatched: Int = 0) { // in many cases, samplesL and samplesR will point to the same object
        override fun toString(): String {
            return "Msg(tick=$tick, samplesDispatched=$samplesDispatched)"
        }
    }

    override val name = "spieluhr"
    override val samplingRate = 48000 // 122880 // use 122880 to make each tick is 2048 samples
    override val channels = 1

    private val getSample = // usage: getSample(noteNum 0..60)
        InstrumentLoader.load("spieluhr", "basegame", "audio/effects/notes/spieluhr.ogg", 41)

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

        val notes = findSetBits(noteBits)

        val buf = FloatArray(getSample(0).first.size)

        // combine all those samples
        notes.forEach { note ->
            getSample(note).first.forEachIndexed { index, fl ->
                buf[index] += fl
            }
        }

        // actually queue it
        val msg = Msg(tick, buf, buf)
        messageQueue.add(messageQueue.size, msg)
    }

    override fun currentPositionInSamples() = 0L

    override fun readSamples(bufferL: FloatArray, bufferR: FloatArray): Int {
        val tickCount = INGAME.WORLD_UPDATE_TIMER
        val bufferSize = bufferL.size

        bufferL.fill(0f)
        bufferR.fill(0f)

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
        var rc = 0
        while (rc < messageQueue.size) {
            val it = messageQueue[rc]
            if (it.samplesDispatched >= it.samplesL.size) {
                messageQueue.removeAt(rc)
                rc -= 1
            }

            rc += 1
        }

        printdbg(this, "Queuelen: ${messageQueue.size}")

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