package net.torvald.terrarum.audio

import net.torvald.terrarum.*
import net.torvald.terrarum.audio.AudioMixer.Companion.DS_FLTIDX_LOW
import net.torvald.terrarum.audio.AudioMixer.Companion.DS_FLTIDX_PAN
import net.torvald.terrarum.audio.AudioMixer.Companion.SPEED_OF_SOUND_AIR
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATE
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATED
import net.torvald.terrarum.audio.dsp.BinoPan
import net.torvald.terrarum.audio.dsp.Lowpass
import net.torvald.terrarum.audio.dsp.NullFilter
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.ActorWithBody.Companion.GAME_TO_SI_VELO
import org.dyn4j.geometry.Vector2
import kotlin.math.absoluteValue
import kotlin.math.cosh
import kotlin.math.sqrt

/**
 * Created by minjaesong on 2023-11-17.
 */
class MixerTrackProcessor(bufferSize: Int, val rate: Int, val track: TerrarumAudioMixerTrack): Runnable {

    private var buffertaille = bufferSize

    companion object {
        fun getVolFun(x: Double): Double {
            // https://www.desmos.com/calculator/blcd4s69gl
//        val K = 1.225
//        fun q(x: Double) = if (x >= 1.0) 0.5 else (K*x - K).pow(2.0) + 0.5
//        val x2 = x.pow(q(x))

            // method 1.
            // https://www.desmos.com/calculator/uzbjw10lna
//        val K = 512.0
//        return K.pow(-sqrt(1.0+x.sqr())) * K


            // method 2.
            // https://www.desmos.com/calculator/3xsac66rsp


            // method 3.
            // comparison with method 1.
            // https://www.desmos.com/calculator/rbteowef8v
            val Q = 2.0
            return 1.0 / cosh(Q * x).sqr()
        }
    }

    @Volatile var running = true; private set
    @Volatile var paused = false; private set
    private val pauseLock = java.lang.Object()


    private var emptyBuf = FloatArray(buffertaille)


    internal var streamBuf: AudioProcessBuf? = null

//    internal var jitterMode = 0
//    internal var jitterIntensity = 0f

    private var fout1 = listOf(emptyBuf, emptyBuf)

    val maxSigLevel = arrayOf(0.0, 0.0)
    val maxRMS = arrayOf(0.0, 0.0)
    val hasClipping = arrayOf(false, false)

    internal fun purgeBuffer() {
        fout1.forEach { it.fill(0f) }
        purgeStreamBuf()
    }

    private fun purgeStreamBuf() {
        track.stop()
        streamBuf = null
        printdbg("StreamBuf is now null")
    }

    private var breakBomb = false

    private val distFalloff = 1600.0

    private fun printdbg(msg: Any) {
        if (true) App.printdbg("MixerTrackProcessor ${track.name}", msg)
    }

    private fun allocateStreamBuf(track: TerrarumAudioMixerTrack) {
        printdbg("Allocating a StreamBuf with rate ${track.currentTrack!!.samplingRate}")
        streamBuf = AudioProcessBuf(track.currentTrack!!.samplingRate, { buffer ->
            var bytesRead = track.currentTrack?.readBytes(buffer) ?: 0

            // do gapless fetch if there is space in the buffer
            if (track.doGaplessPlayback && bytesRead < buffer.size) {
                track.currentTrack?.reset()
                track.pullNextTrack()

                bytesRead += read0(buffer, bytesRead)
            }
            // if isLooping=true, do gapless sampleRead but reads from itself
            else if (track.currentTrack?.looping == true && bytesRead < buffer.size) {
                track.currentTrack?.reset()

                bytesRead += read0(buffer, bytesRead)
            }

            bytesRead
        }, { purgeStreamBuf() }).also {
//            it.jitterMode = jitterMode
//            it.jitterIntensity = jitterIntensity
        }
    }

    private fun read0(buffer: ByteArray, bytesRead: Int): Int {
        val tmpBuf = ByteArray(buffer.size - bytesRead)
        val newRead = track.currentTrack?.readBytes(tmpBuf) ?: 0

        System.arraycopy(tmpBuf, 0, buffer, bytesRead, tmpBuf.size)

        return newRead
    }

    var bufEmpty = true; private set

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
                (track.filters[DS_FLTIDX_PAN] as BinoPan).earDist = App.audioMixer.listenerHeadSize

                if (App.audioMixer.actorNowPlaying != null) {
                    val trackingTarget = track.trackingTarget
                    if (trackingTarget == null || trackingTarget == App.audioMixer.actorNowPlaying) {
                        // "reset" the track
                        track.volume = track.maxVolume
                        (track.filters[DS_FLTIDX_PAN] as BinoPan).pan = 0f
                        (track.filters[DS_FLTIDX_LOW] as Lowpass).setCutoff(SAMPLING_RATE / 2f)
                    }
                    else if (trackingTarget is ActorWithBody) {
                        val relativeXpos = relativeXposition(App.audioMixer.actorNowPlaying!!, trackingTarget as ActorWithBody)
                        val distFromActor = distBetweenActors(App.audioMixer.actorNowPlaying!!, trackingTarget as ActorWithBody)
                        val vol = track.maxVolume * getVolFun(distFromActor / distFalloff).coerceAtLeast(0.0)
                        track.volume = vol
                        (track.filters[DS_FLTIDX_PAN] as BinoPan).pan = (1.3f * relativeXpos / distFalloff).toFloat()
                        (track.filters[DS_FLTIDX_LOW] as Lowpass).setCutoff(
                            (SAMPLING_RATED*0.5) / (24.0 * (distFromActor / distFalloff).sqr() + 1.0)
                        )

                        val sourceVec = (trackingTarget as ActorWithBody).let { it.externalV + (it.controllerV ?: Vector2()) }
                        val listenerVec = App.audioMixer.actorNowPlaying!!.let { it.externalV + (it.controllerV ?: Vector2()) }
                        val distFromActorNext = distBetweenPoints(
                            App.audioMixer.actorNowPlaying!!.centrePosVector + listenerVec,
                            (trackingTarget as ActorWithBody).centrePosVector + sourceVec
                        )
                        val isApproaching = if (distFromActorNext <= distFromActor) 1.0 else -1.0
                        val relativeSpeed = (sourceVec - listenerVec).magnitude * GAME_TO_SI_VELO * isApproaching
                        val soundSpeed = SPEED_OF_SOUND_AIR * 4f // using an arbitrary value for "gamification"
                        val dopplerFactor = (soundSpeed + relativeSpeed) / soundSpeed // >1: speedup, <1: speeddown

                        track.processor.streamBuf?.playbackSpeed = dopplerFactor.toFloat()

//                        printdbg("dist=$distFromActor\tdopplerFactor=$dopplerFactor")
                    }
                }
                else {
                    // "reset" the track
                    track.volume = track.maxVolume
                    (track.filters[DS_FLTIDX_PAN] as BinoPan).pan = 0f
                    (track.filters[DS_FLTIDX_LOW] as Lowpass).setCutoff(SAMPLING_RATE / 2f)
                }
            }


            // fetch deviceBufferSize amount of sample from the disk
            if (track.playRequested.get()) {
                track.play()
            }

            if (track.trackType != TrackType.MASTER && track.trackType != TrackType.BUS && track.streamPlaying.get()) {
                if (streamBuf == null && track.currentTrack != null) {
                    allocateStreamBuf(track)
                }

                streamBuf!!.fetchBytes()
            }

            var samplesL1: FloatArray
            var samplesR1: FloatArray

            bufEmpty = false

            // get samples and apply the fader
            if (track.trackType == TrackType.MASTER || track.trackType == TrackType.BUS) {
                // combine all the inputs
                samplesL1 = FloatArray(buffertaille)
                samplesR1 = FloatArray(buffertaille)

                val sidechains = track.sidechainInputs
                // add all up
                sidechains.forEach { (side, mix) ->
                    for (i in samplesL1.indices) {
                        samplesL1[i] += side.processor.fout1[0][i] * mix.toFloat()
                        samplesR1[i] += side.processor.fout1[1][i] * mix.toFloat()
                    }
                }
            }
            // source channel: skip processing if there's no active input
//            else if (track.getSidechains().any { it != null && !it.isBus && !it.isMaster && !it.streamPlaying } && !track.streamPlaying) {
            else if (!track.streamPlaying.get() || streamBuf == null || streamBuf!!.validSamplesInBuf < App.audioBufferSize) {
                samplesL1 = emptyBuf
                samplesR1 = emptyBuf

                bufEmpty = true
            }
            else {
                streamBuf!!.getLR().let {
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
                    fout1 = listOf(FloatArray(buffertaille), FloatArray(buffertaille))

                    filterStack.forEachIndexed { index, it ->
                        it(fin1, fout1)
                        fin1 = fout1
                        if (index < filterStack.lastIndex) {
                            fout1 = listOf(FloatArray(buffertaille), FloatArray(buffertaille))
                        }
                    }
                }


                // apply fader at post
                fout1.forEach { ch ->
                    ch.forEachIndexed { index, sample ->
                        ch[index] = (sample * track.volume).toFloat()
                    }
                }


                // scan the finished sample for mapping signal level and clipping detection
                fout1.map { it.maxOf { it.absoluteValue } }.forEachIndexed { index, fl ->
                    maxSigLevel[index] = fl.toDouble()
                }
                fout1.map { it.sumOf { it.sqr().toDouble() } }.forEachIndexed { index, fl ->
                    maxRMS[index] = sqrt(fl / (buffertaille))
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
