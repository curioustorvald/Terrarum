package net.torvald.terrarum.audio

import com.badlogic.gdx.utils.Queue
import net.torvald.reflection.forceInvoke
import net.torvald.terrarum.App
import net.torvald.terrarum.audio.dsp.BinoPan
import net.torvald.terrarum.audio.dsp.NullFilter
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.relativeXposition
import net.torvald.terrarum.sqr
import kotlin.math.*

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


    internal var streamBuf: AudioProcessBuf? = null

    private var fout1 = listOf(emptyBuf, emptyBuf)

    val maxSigLevel = arrayOf(0.0, 0.0)
    val maxRMS = arrayOf(0.0, 0.0)
    val hasClipping = arrayOf(false, false)

    private var breakBomb = false

    private val distFalloff = 2048.0

    private fun printdbg(msg: Any) {
        if (true) App.printdbg("AudioAdapter ${track.name}", msg)
    }

    private fun allocateStreamBuf(track: TerrarumAudioMixerTrack) {
        streamBuf = AudioProcessBuf(track.currentTrack!!.samplingRate, {
            track.currentTrack?.gdxMusic?.forceInvoke<Int>("read", arrayOf(it))
        }, {
            track.stop()
            this.streamBuf = null
        })
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


            // update panning and shits
            if (track.trackType == TrackType.DYNAMIC_SOURCE && track.isPlaying) {
                if (AudioMixer.actorNowPlaying != null) {
                    if (track.trackingTarget == null || track.trackingTarget == AudioMixer.actorNowPlaying) {
                        track.volume = track.maxVolume
                        (track.filters[0] as BinoPan).pan = 0f
                    }
                    else if (track.trackingTarget is ActorWithBody) {
                        val relativeXpos = relativeXposition(AudioMixer.actorNowPlaying!!, track.trackingTarget as ActorWithBody)
                        track.volume = track.maxVolume * (1.0 - relativeXpos.absoluteValue.pow(0.5) / distFalloff)
                        (track.filters[0] as BinoPan).pan = ((2*asin(relativeXpos / distFalloff)) / Math.PI).toFloat()
                    }
                }
            }


            // fetch deviceBufferSize amount of sample from the disk
            if (track.trackType != TrackType.MASTER && track.trackType != TrackType.BUS && track.streamPlaying) {
                if (streamBuf == null && track.currentTrack != null) allocateStreamBuf(track)
                streamBuf!!.fetchBytes()
            }

            var samplesL1: FloatArray
            var samplesR1: FloatArray

            var bufEmpty = false

            // get samples and apply the fader
            if (track.trackType == TrackType.MASTER || track.trackType == TrackType.BUS) {
                // combine all the inputs
                samplesL1 = FloatArray(bufferSize / 4)
                samplesR1 = FloatArray(bufferSize / 4)

                val sidechains = track.sidechainInputs
                // add all up
                sidechains.forEach { (side, mix) ->
                    for (i in samplesL1.indices) {
                        samplesL1[i] += side.processor.fout1[0][i] * (mix * track.volume).toFloat()
                        samplesR1[i] += side.processor.fout1[1][i] * (mix * track.volume).toFloat()
                    }
                }
            }
            // source channel: skip processing if there's no active input
//            else if (track.getSidechains().any { it != null && !it.isBus && !it.isMaster && !it.streamPlaying } && !track.streamPlaying) {
            else if (!track.streamPlaying || streamBuf == null) {
                samplesL1 = emptyBuf
                samplesR1 = emptyBuf

                bufEmpty = true
            }
            else {
                streamBuf!!.getLR(track.volume).let {
                    samplesL1 = it.first
                    samplesR1 = it.second
                }
            }

            if (!bufEmpty) {
                // run the input through the stack of filters
                val filterStack = track.filters.filter { !it.bypass && it !is NullFilter }

                if (filterStack.isEmpty()) {
                    fout1 = listOf(samplesL1, samplesR1)
                }
                else {
                    var fin1 = listOf(samplesL1, samplesR1)
                    fout1 = listOf(FloatArray(bufferSize / 4), FloatArray(bufferSize / 4))

                    filterStack.forEachIndexed { index, it ->
                        it(fin1, fout1)
                        fin1 = fout1
                        if (index < filterStack.lastIndex) {
                            fout1 = listOf(FloatArray(bufferSize / 4), FloatArray(bufferSize / 4))
                        }
                    }
                }


                // scan the finished sample for mapping signal level and clipping detection
                fout1.map { it.maxOf { it.absoluteValue } }.forEachIndexed { index, fl ->
                    maxSigLevel[index] = fl.toDouble()
                }
                fout1.map { it.sumOf { it.sqr().toDouble() } }.forEachIndexed { index, fl ->
                    maxRMS[index] = sqrt(fl / (bufferSize / 4))
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
                fout1 = listOf(samplesL1, samplesR1) // keep pass the so that long-delay filters can empty out its buffer
                maxSigLevel.fill(0.0)
                maxRMS.fill(0.0)
                hasClipping.fill(false)
            }


            // by this time, the output buffer is filled with processed results, pause the execution
            if (track.trackType != TrackType.MASTER) {
                this.pause()
            }
            else {

                // spin until queue is sufficiently empty
                /*while (track.pcmQueue.size >= BACK_BUF_COUNT && running) { // uncomment to multithread
                    Thread.sleep(1)
                }*/

//                printdbg("PUSHE; Queue size: ${track.pcmQueue.size}")
                track.pcmQueue.addLast(fout1)

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
        if (track.trackType != TrackType.MASTER) throw IllegalArgumentException("Track is not master")
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
