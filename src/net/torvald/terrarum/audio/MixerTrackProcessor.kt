package net.torvald.terrarum.audio

import com.badlogic.gdx.utils.Queue
import net.torvald.reflection.forceInvoke
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATE

/**
 * Created by minjaesong on 2023-11-17.
 */
class MixerTrackProcessor(val bufferSize: Int, val rate: Int, val track: TerrarumAudioMixerTrack): Runnable {
    @Volatile private var running = true
    @Volatile private var paused = false
    private val pauseLock = java.lang.Object()


    private val emptyBuf = FloatArray(bufferSize / 4)


    internal val streamBuf = AudioProcessBuf(bufferSize)
    internal val sideChainBufs = Array(track.sidechainInputs.size) { AudioProcessBuf(bufferSize) }

    private var fout0 = listOf(emptyBuf, emptyBuf)
    private var fout1 = listOf(emptyBuf, emptyBuf)

    private var breakBomb = false

    private fun printdbg(msg: Any) {
        if (true) println("[AudioAdapter ${track.name}] $msg")
    }
    override fun run() {
        while (running) { synchronized(pauseLock) {
                if (!running) { // may have changed while waiting to
                    // synchronize on pauseLock
                    breakBomb = true
                }
                if (paused) {
                    try {
                        pauseLock.wait() // will cause this Thread to block until
                        // another thread calls pauseLock.notifyAll()
                        // Note that calling wait() will
                        // relinquish the synchronized lock that this
                        // thread holds on pauseLock so another thread
                        // can acquire the lock to call notifyAll()
                        // (link with explanation below this code)
                    }
                    catch (ex: InterruptedException) {
                        breakBomb = true
                    }
                    if (!running) { // running might have changed since we paused
                        breakBomb = true
                    }
                }
            }

            if (breakBomb) break
            // Your code here

            // fetch deviceBufferSize amount of sample from the disk
            if (!track.isMaster && track.streamPlaying) {
                streamBuf.fetchBytes {
                    track.currentTrack?.gdxMusic?.forceInvoke<Int>("read", arrayOf(it))
                }
            }

            // also fetch samples from sidechainInputs
            // TODO

            // combine all the inputs
            // TODO this code just uses streamBuf

            var samplesL0: FloatArray? = null
            var samplesR0: FloatArray? = null
            var samplesL1: FloatArray? = null
            var samplesR1: FloatArray? = null

            var bufEmpty = false

            if (track.isMaster) {
                // TEST CODE must combine all the inputs
                track.sidechainInputs[0]?.let {
                    samplesL0 = it.first.processor.fout0[0]
                    samplesR0 = it.first.processor.fout0[1]
                    samplesL1 = it.first.processor.fout1[0]
                    samplesR1 = it.first.processor.fout1[1]
                }


                /*track.sidechainInputs[0].let {
                    if (it != null) {
                        val f0 = it.first.pcmQueue.removeFirstOrElse {
                            bufEmpty = true
                            listOf(emptyBuf, emptyBuf)
                        }
                        samplesL0 = f0[0]
                        samplesR0 = f0[1]

                        val f1 = it.first.pcmQueue.removeFirstOrElse {
                            bufEmpty = true
                            listOf(emptyBuf, emptyBuf)
                        }
                        samplesL1 = f1[0]
                        samplesR1 = f1[1]
                    }
                    else {
                        samplesL0 = emptyBuf
                        samplesR0 = emptyBuf
                        samplesL1 = emptyBuf
                        samplesR1 = emptyBuf

                        bufEmpty = true
                    }
                }*/

            }
            else {
                samplesL0 = streamBuf.getL0(track.volume)
                samplesR0 = streamBuf.getR0(track.volume)
                samplesL1 = streamBuf.getL1(track.volume)
                samplesR1 = streamBuf.getR1(track.volume)
            }

            if (samplesL0 != null && samplesL1 != null && samplesR0 != null && samplesR1 != null) {
                // run the input through the stack of filters
                val filterStack = track.filters.filter { !it.bypass && it !is NullFilter }

                if (filterStack.isEmpty()) {
                    fout1 = listOf(samplesL1!!, samplesR1!!)
                }
                else {
                    var fin0 = listOf(samplesL0!!, samplesR0!!)
                    var fin1 = listOf(samplesL1!!, samplesR1!!)
                    fout0 = fout1
                    fout1 = listOf(FloatArray(bufferSize / 4), FloatArray(bufferSize / 4))

                    filterStack.forEachIndexed { index, it ->
                        it(fin0, fin1, fout0, fout1)
                        fin0 = fout0
                        fin1 = fout1
                        if (index < filterStack.lastIndex) {
                            fout0 = listOf(FloatArray(bufferSize / 4), FloatArray(bufferSize / 4))
                            fout1 = listOf(FloatArray(bufferSize / 4), FloatArray(bufferSize / 4))
                        }
                    }
                }
            }

            // by this time, the output buffer is filled with processed results, pause the execution
            if (!track.isMaster) {
                this.pause()
            }
            else {
                if (samplesL0 != null && samplesL1 != null && samplesR0 != null && samplesR1 != null) {

                    // spin until queue is sufficiently empty
                    while (track.pcmQueue.size >= 4 && running) {
                        Thread.sleep(1)
                    }

//                    printdbg("Pushing to queue (queue size: ${track.pcmQueue.size})")
                    track.pcmQueue.addLast(fout1)
                }

                // spin
                Thread.sleep((bufferSize / 8L / rate).coerceAtLeast(1L))

                // wake sidechain processors
                track.getSidechains().forEach {
                    if (it?.processor?.running == true)
                        it?.processor?.resume()
                }
            }
        }
    }

    fun stop() {
        running = false
        // you might also want to interrupt() the Thread that is
        // running this Runnable, too, or perhaps call:
        resume()
        // to unblock
    }

    fun pause() {
        // you may want to throw an IllegalStateException if !running
        paused = true
    }

    fun resume() {
        synchronized(pauseLock) {
            paused = false
            pauseLock.notifyAll() // Unblocks thread
        }
    }
}

private fun <T> Queue<T>.removeFirstOrElse(function: () -> T): T {
    return if (this.isEmpty) {
        this.removeFirst()
    }
    else {
        function()
    }
}


class FeedSamplesToAdev(val bufferSize: Int, val rate: Int, val track: TerrarumAudioMixerTrack) : Runnable {
    init {
        if (!track.isMaster) throw IllegalArgumentException("Track is not master")
    }

    private fun printdbg(msg: Any) {
        if (true) println("[AudioAdapter ${track.name}] $msg")
    }
    @Volatile private var exit = false
    override fun run() {
        while (!exit) {

            val writeQueue = track.pcmQueue

            if (writeQueue.notEmpty()) {
//                printdbg("Taking samples from queue (queue size: ${writeQueue.size})")

                val samples = writeQueue.removeFirst()
                track.adev!!.writeSamples(samples)
            }
            else if (writeQueue.isEmpty) {
//                printdbg("!! QUEUE EXHAUSTED !! QUEUE EXHAUSTED !! QUEUE EXHAUSTED !! QUEUE EXHAUSTED !! QUEUE EXHAUSTED !! QUEUE EXHAUSTED ")
            }

            Thread.sleep((bufferSize / 8L / rate).coerceAtLeast(1L))
        }
//        printdbg("FeedSamplesToAdev EXIT")
    }

    fun stop() {
        exit = true
    }
}
