package net.torvald.terrarum.audio

import com.badlogic.gdx.utils.Queue
import net.torvald.reflection.forceInvoke
import org.apache.commons.math3.special.Erf.erf
import kotlin.math.absoluteValue
import kotlin.math.tanh

/**
 * Created by minjaesong on 2023-11-17.
 */
class MixerTrackProcessor(val bufferSize: Int, val rate: Int, val track: TerrarumAudioMixerTrack): Runnable {

    companion object {
        val BACK_BUF_COUNT = 1
    }

    @Volatile var running = true; private set
    @Volatile var paused = false; private set
    private val pauseLock = java.lang.Object()


    private val emptyBuf = FloatArray(bufferSize / 4)


    internal val streamBuf = AudioProcessBuf(bufferSize)
    internal val sideChainBufs = Array(track.sidechainInputs.size) { AudioProcessBuf(bufferSize) }

    private var fout0 = listOf(emptyBuf, emptyBuf)
    private var fout1 = listOf(emptyBuf, emptyBuf)

    var maxSigLevel = arrayOf(0.0, 0.0); private set
    var hasClipping = arrayOf(false, false); private set

    private var breakBomb = false

    private fun printdbg(msg: Any) {
        if (true) println("[AudioAdapter ${track.name}] $msg")
    }
    override fun run() {
//        while (running) { // uncomment to multithread
            /*synchronized(pauseLock) { // uncomment to multithread
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

            if (breakBomb) break*/ // uncomment to multithread
            // Your code here

            // fetch deviceBufferSize amount of sample from the disk
            if (!track.isMaster && !track.isBus && track.streamPlaying) {
                streamBuf.fetchBytes {
                    val bytesRead = track.currentTrack?.gdxMusic?.forceInvoke<Int>("read", arrayOf(it))
                    if (bytesRead == null || bytesRead <= 0) { // some class (namely Mp3) may return 0 instead of negative value
//                        printdbg("Finished reading audio stream")
                        track.currentTrack?.gdxMusic?.forceInvoke<Int>("reset", arrayOf())
                        track.streamPlaying = false
                        track.fireSongFinishHook()
                    }

                }
            }

            var samplesL0: FloatArray? = null
            var samplesR0: FloatArray? = null
            var samplesL1: FloatArray? = null
            var samplesR1: FloatArray? = null

            var bufEmpty = false

            // get samples and apply the fader
            if (track.isMaster || track.isBus) {
                // combine all the inputs
                samplesL0 = FloatArray(bufferSize / 4)
                samplesR0 = FloatArray(bufferSize / 4)
                samplesL1 = FloatArray(bufferSize / 4)
                samplesR1 = FloatArray(bufferSize / 4)

                val sidechains = track.sidechainInputs.filterNotNull()
                // add all up
                sidechains.forEach { (side, mix) ->
                    for (i in samplesL0!!.indices) {
                        samplesL0!![i] += side.processor.fout0[0][i] * (mix * track.volume).toFloat()
                        samplesR0!![i] += side.processor.fout0[1][i] * (mix * track.volume).toFloat()
                        samplesL1!![i] += side.processor.fout1[0][i] * (mix * track.volume).toFloat()
                        samplesR1!![i] += side.processor.fout1[1][i] * (mix * track.volume).toFloat()
                    }
                }
                // de-clip using sigmoid function
                for (i in samplesL0.indices) {
                    samplesL0[i] = erf(samplesL0[i].toDouble()).toFloat()
                    samplesR0[i] = erf(samplesR0[i].toDouble()).toFloat()
                    samplesL1[i] = erf(samplesL1[i].toDouble()).toFloat()
                    samplesR1[i] = erf(samplesR1[i].toDouble()).toFloat()
                }

                /*track.sidechainInputs[TerrarumAudioMixerTrack.INDEX_BGM]?.let { (side, mix) ->
                    samplesL0 = side.processor.fout0[0].applyVolume((mix * track.volume).toFloat()) // must not applyVolumeInline
                    samplesR0 = side.processor.fout0[1].applyVolume((mix * track.volume).toFloat())
                    samplesL1 = side.processor.fout1[0].applyVolume((mix * track.volume).toFloat())
                    samplesR1 = side.processor.fout1[1].applyVolume((mix * track.volume).toFloat())
                }*/
            }
            // source channel: skip processing if there's no active input
//            else if (track.getSidechains().any { it != null && !it.isBus && !it.isMaster && !it.streamPlaying } && !track.streamPlaying) {
            else if (!track.streamPlaying) {
                samplesL0 = null
                samplesR0 = null
                samplesL1 = null
                samplesR1 = null
            }
            else {
                samplesL0 = streamBuf.getL0(track.volume)
                samplesR0 = streamBuf.getR0(track.volume)
                samplesL1 = streamBuf.getL1(track.volume)
                samplesR1 = streamBuf.getR1(track.volume)
            }

            if (samplesL0 != null /*&& samplesL1 != null && samplesR0 != null && samplesR1 != null*/) {
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


                // scan the finished sample for mapping signal level and clipping detection
                fout1.map { it.maxOf { it.absoluteValue } }.forEachIndexed { index, fl ->
                    maxSigLevel[index] = fl.toDouble()
                }
                hasClipping.fill(false)
                fout1.forEachIndexed { index, floats ->
                    var lastSample = floats[0]
                    for (i in 1 until floats.size) {
                        val currentSample = floats[i]
                        if (lastSample * currentSample > 0.0 && lastSample.absoluteValue >= 1.0 && currentSample.absoluteValue >= 1.0) {
                            hasClipping[index] = true
                            break
                        }
                        lastSample = currentSample
                    }
                }
            }
            else {
                maxSigLevel.fill(0.0)
                hasClipping.fill(false)
            }


            // by this time, the output buffer is filled with processed results, pause the execution
            if (!track.isMaster) {
                this.pause()
            }
            else {
                if (samplesL0 != null /*&& samplesL1 != null && samplesR0 != null && samplesR1 != null*/) {

                    // spin until queue is sufficiently empty
                    /*while (track.pcmQueue.size >= BACK_BUF_COUNT && running) { // uncomment to multithread
                        Thread.sleep(1)
                    }*/

//                    printdbg("PUSHE; Queue size: ${track.pcmQueue.size}")
                    track.pcmQueue.addLast(fout1)
                }

                // spin
//                Thread.sleep(((1000*bufferSize) / 8L / rate).coerceAtLeast(1L)) // uncomment to multithread

                // wake sidechain processors
                resumeSidechainsRecursively(track, track.name)
            }
//        } // uncomment to multithread
    }

    private fun FloatArray.applyVolume(volume: Float) = FloatArray(this.size) { (this[it] * volume) }
    /*private fun FloatArray.applyVolumeInline(volume: Float): FloatArray {
        for (i in this.indices) {
            this[i] *= volume
        }
        return this
    }*/


    private fun resumeSidechainsRecursively(track: TerrarumAudioMixerTrack?, caller: String) {
        track?.getSidechains()?.forEach {
            if (it?.processor?.running == true) {
                it.processor.resume()
                it.getSidechains().forEach {
                    if (it?.processor?.running == true) {
                        it.processor.resume()
                        resumeSidechainsRecursively(it, caller + caller)
                    }
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
//        printdbg("PAUSE")
        // you may want to throw an IllegalStateException if !running
        paused = true
    }

    fun resume() {
//        printdbg("RESUME")
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

    val sleepTime = (1000000000.0 * ((bufferSize / 4.0) / TerrarumAudioMixerTrack.SAMPLING_RATED)).toLong()
    val sleepMS = sleepTime / 1000000
    val sleepNS = (sleepTime % 1000000).toInt()

    private fun printdbg(msg: Any) {
        if (true) println("[AudioAdapter ${track.name}] $msg")
    }
    @Volatile private var exit = false
    override fun run() {
        while (!exit) {

            val writeQueue = track.pcmQueue
            val queueSize = writeQueue.size
            if (queueSize > 0) {
//                printdbg("PULL; Queue size: $queueSize")
                val samples = writeQueue.removeFirst()
                track.adev!!.writeSamples(samples)
            }
//            else {
//                printdbg("QUEUE EMPTY QUEUE EMPTY QUEUE EMPTY ")
//            }

//            Thread.sleep(sleepMS, sleepNS)
        }
    }

    fun stop() {
        exit = true
    }
}
