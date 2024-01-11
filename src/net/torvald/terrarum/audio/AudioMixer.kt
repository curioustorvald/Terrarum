package net.torvald.terrarum.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.lwjgl3.audio.Lwjgl3Audio
import com.badlogic.gdx.utils.Disposable
import com.jme3.math.FastMath
import net.torvald.terrarum.*
import net.torvald.terrarum.App.THREAD_COUNT
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATE
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATED
import net.torvald.terrarum.audio.dsp.*
import net.torvald.terrarum.concurrent.ThreadExecutor
import net.torvald.terrarum.concurrent.sliceEvenly
import net.torvald.terrarum.modulebasegame.BuildingMaker
import net.torvald.terrarum.modulebasegame.MusicContainer
import java.lang.Thread.MAX_PRIORITY
import java.util.*
import java.util.concurrent.Callable
import kotlin.math.*

/**
 * Any audio reference fed into this manager will get lost; you must manually store and dispose of them on your own.
 *
 * Created by minjaesong on 2023-11-07.
 */
object AudioMixer: Disposable {
    var SPEED_OF_SOUND = 340f

    const val SPEED_OF_SOUND_AIR = 340f
    const val SPEED_OF_SOUND_WATER = 1480f

    const val DEFAULT_FADEOUT_LEN = 1.8

    val masterVolume: Double
        get() = App.getConfigDouble("mastervolume")

    val musicVolume: Double
        get() = App.getConfigDouble("bgmvolume")

    val ambientVolume: Double
        get() = App.getConfigDouble("ambientvolume")

    val sfxVolume: Double
        get() = App.getConfigDouble("sfxvolume")

    val guiVolume: Double
        get() = App.getConfigDouble("guivolume")

    val dynamicSourceCount: Int
        get() = App.getConfigInt("audio_dynamic_source_max")

    val tracks = Array(8) { TerrarumAudioMixerTrack(
        if (it == 0) "Music"
        else if (it == 1) "Ambient"
        else if (it == 2) "GUI"
        else if (it == 3) "\u00E4SFX"
        else if (it == 4) "\u00F0 \u00E4 \u00F0" // summation
        else if (it == 5) "\u00D9Open\u00D9" // convolution1
        else if (it == 6) "\u00D9Cave\u00D9" // convolution2
        else if (it == 7) "\u00F0 \u00DA \u00F0" // fade
        else "Trk${it+1}", trackType = if (it >= 3) TrackType.BUS else TrackType.STATIC_SOURCE, maxVolumeFun = {
            when (it) {
                0 -> { musicVolume }
                1 -> { ambientVolume }
                2 -> { sfxVolume }
                3 -> { guiVolume }
                else -> { 1.0 }
            }
        }
    ) }

    val dynamicTracks = Array(dynamicSourceCount) { TerrarumAudioMixerTrack(
        "DS${(it + 1).toString().padStart(3, '0')}",
        TrackType.DYNAMIC_SOURCE
    ) }

    val masterTrack = TerrarumAudioMixerTrack("\u00DBMASTER", TrackType.MASTER) { masterVolume }

    val musicTrack: TerrarumAudioMixerTrack
        get() = tracks[0]
    val ambientTrack: TerrarumAudioMixerTrack
        get() = tracks[1]
    val guiTrack: TerrarumAudioMixerTrack
        get() = tracks[2]
    val sfxSumBus: TerrarumAudioMixerTrack
        get() = tracks[3]

    val sumBus: TerrarumAudioMixerTrack
        get() = tracks[4]
    val convolveBusOpen: TerrarumAudioMixerTrack
        get() = tracks[5]
    val convolveBusCave: TerrarumAudioMixerTrack
        get() = tracks[6]
    val fadeBus: TerrarumAudioMixerTrack
        get() = tracks[7]

    var processing = true

    var actorNowPlaying = Terrarum.ingame?.actorNowPlaying; private set

    /**
     * Return oldest dynamic track, even if the track is currently playing
     */
    fun getFreeTrackNoMatterWhat(): TerrarumAudioMixerTrack {
        return getFreeTrack() ?: dynamicTracks.minBy { it.playStartedTime }
    }

    /**
     * Return oldest dynamic track that is not playing
     */
    fun getFreeTrack(): TerrarumAudioMixerTrack? {
        return dynamicTracks.filter { it.trackingTarget == null && !it.isPlaying }.minByOrNull { it.playStartedTime }
    }

    private val processingExecutor = ThreadExecutor()
    val processingThread = Thread {
        // serial precessing
        while (processing) {
            actorNowPlaying = Terrarum.ingame?.actorNowPlaying

            // process
            dynamicTracks.forEach {
                if (!it.processor.paused) {
                    try { it.processor.run() }
                    catch (e: Throwable) { e.printStackTrace() }
                }
            }
            tracks.forEach {
                if (!it.processor.paused) {
                    try { it.processor.run() }
                    catch (e: Throwable) { e.printStackTrace() }
                }
            }
            masterTrack.processor.run()

            while (processing && !masterTrack.pcmQueue.isEmpty) {
                masterTrack.adev!!.writeSamples(masterTrack.pcmQueue.removeFirst()) // it blocks until the queue is consumed
            }
        }

        // parallel processing, it seems even on the 7950X this is less efficient than serial processing...
        /*while (processing) {
            actorNowPlaying = Terrarum.ingame?.actorNowPlaying

            for (tracks in parallelProcessingSchedule) {
                if (!processing) break

                val callables = tracks.map { Callable {
                    if (!it.processor.paused) {
                        try { it.processor.run() }
                        catch (_: InterruptedException) {}
                        catch (e: Throwable) { e.printStackTrace() }
                    }
                } }

                try {
                    processingExecutor.renew()
                    processingExecutor.submitAll(callables)
                    processingExecutor.join()
                }
                catch (_: InterruptedException) {}
                catch (e: Throwable) { e.printStackTrace() }
            }

            while (processing && !masterTrack.pcmQueue.isEmpty) {
                masterTrack.adev!!.writeSamples(masterTrack.pcmQueue.removeFirst()) // it blocks until the queue is consumed
            }
        }*/
    }

//    val parallelProcessingSchedule: Array<Array<TerrarumAudioMixerTrack>>


    init {
        // initialise audio paths //

        listOf(musicTrack, ambientTrack, sfxSumBus, guiTrack).forEach {
            it.filters[0] = Gain(1f)
        }

        masterTrack.filters[0] = SoftClp
        masterTrack.filters[1] = Buffer
        masterTrack.filters[2] = Spectro()
        masterTrack.filters[3] = Vecto(2f)

        musicTrack.filters[1] = Vecto()
        musicTrack.filters[2] = Spectro()
        ambientTrack.filters[1] = Vecto()
        ambientTrack.filters[2] = Spectro()

        listOf(sumBus, convolveBusOpen, convolveBusCave).forEach {
            it.addSidechainInput(musicTrack, 1.0)
            it.addSidechainInput(ambientTrack, 1.0)
            it.addSidechainInput(sfxSumBus, 1.0)
        }

        convolveBusOpen.filters[1] = Convolv(ModMgr.getFile("basegame", "audio/convolution/EchoThief - PurgatoryChasm.bin"), decibelsToFullscale(-6.0).toFloat())
        convolveBusOpen.filters[2] = Gain(decibelsToFullscale(17.0).toFloat()) // don't make it too loud; it'll sound like a shit
        convolveBusOpen.volume = 0.5 // will be controlled by the other updater which surveys the world

        convolveBusCave.filters[1] = Convolv(ModMgr.getFile("basegame", "audio/convolution/EchoThief - WaterplacePark-trimmed.bin"), decibelsToFullscale(-3.0).toFloat())
        convolveBusCave.filters[2] = Gain(decibelsToFullscale(16.0).toFloat())
        convolveBusCave.volume = 0.5 // will be controlled by the other updater which surveys the world

        fadeBus.addSidechainInput(sumBus, 1.0 / 3.0)
        fadeBus.addSidechainInput(convolveBusOpen, 2.0 / 3.0)
        fadeBus.addSidechainInput(convolveBusCave, 2.0 / 3.0)
        fadeBus.filters[0] = Lowpass(SAMPLING_RATE / 2f)

        masterTrack.addSidechainInput(fadeBus, 1.0)
        masterTrack.addSidechainInput(guiTrack, 1.0)

        musicTrack.filters[3] = Gain(0.5f)

        dynamicTracks.forEach {
            it.filters[0] = BinoPan(0f)
            it.filters[1] = Lowpass(SAMPLING_RATE / 2f)
            sfxSumBus.addSidechainInput(it, 1.0)
        }

        // unused for now
        /*parallelProcessingSchedule =
            arrayOf(musicTrack, ambientTrack, guiTrack).sliceEvenly(THREAD_COUNT / 2).toTypedArray() +
            dynamicTracks.sliceEvenly(THREAD_COUNT / 2).toTypedArray() +
            arrayOf(sfxSumBus, sumBus, convolveBusOpen, convolveBusCave).sliceEvenly(THREAD_COUNT / 2).toTypedArray() +
            arrayOf(fadeBus) +
            arrayOf(masterTrack)*/


        processingThread.priority = MAX_PRIORITY // higher = more predictable; audio delay is very noticeable so it gets high priority
        processingThread.start()
//        feedingThread.priority = MAX_PRIORITY
//        feedingThread.start()
    }


    data class FadeRequest(
        var fadeAkku: Double = 0.0,
        var fadeLength: Double = DEFAULT_FADEOUT_LEN,
        var fadeoutFired: Boolean = false,
        var fadeinFired: Boolean = false,
        var fadeTarget: Double = 0.0,
        var fadeStart: Double = 0.0,
        var callback: () -> Unit = {},
    )

    private val fadeReqs = HashMap<TerrarumAudioMixerTrack, FadeRequest>().also { map ->
        listOf(musicTrack, ambientTrack, guiTrack, fadeBus).forEach {
            map[it] = FadeRequest()
        }
    }
    private val fadeReqsCol = fadeReqs.entries

    private var lpAkku = 0.0
    private var lpLength = 0.4
    private var lpOutFired = false
    private var lpInFired = false
    private var lpStart = SAMPLING_RATED / 2.0
    private var lpTarget = SAMPLING_RATED / 2.0

    private var testAudioMixRatio = 0.0

    private var muteLatched = false

    fun update(delta: Float) {
        // test the panning
        /*musicTrack.getFilter<BinoPan>().let {
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                it.pan = (it.pan + 0.001f).coerceIn(-1f, 1f)
            }
            else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                it.pan = (it.pan - 0.001f).coerceIn(-1f, 1f)
            }
        }*/
        if (Terrarum.ingame is BuildingMaker) {
            val mixDelta = if (testAudioMixRatio >= 0.0) 0.001 else (0.001 * MaterialCodex["AIIR"].sondrefl).absoluteValue

            if (Gdx.input.isKeyPressed(Input.Keys.UP))
                testAudioMixRatio += mixDelta
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN))
                testAudioMixRatio -= mixDelta
            if (Gdx.input.isKeyPressed(Input.Keys.NUM_1))
                testAudioMixRatio = -1.0
            if (Gdx.input.isKeyPressed(Input.Keys.NUM_2))
                testAudioMixRatio = 0.0
            if (Gdx.input.isKeyPressed(Input.Keys.NUM_3))
                testAudioMixRatio = 1.0
            if (!muteLatched && Gdx.input.isKeyPressed(Input.Keys.NUM_4)) {
                AudioMixer.sumBus.volume = 1.0 - AudioMixer.sumBus.volume
                muteLatched = true
            }
            else if (!Gdx.input.isKeyPressed(Input.Keys.NUM_4))
                muteLatched = false

            testAudioMixRatio = testAudioMixRatio.coerceIn(MaterialCodex["AIIR"].sondrefl.absoluteValue * -1.0, 1.0)

            if (testAudioMixRatio >= 0.0) {
                val ratio1 = testAudioMixRatio.coerceIn(0.0, 1.0)
                AudioMixer.convolveBusCave.volume = ratio1
                AudioMixer.convolveBusOpen.volume = 1.0 - ratio1
            }
            else {
                val ratio1 = (testAudioMixRatio / MaterialCodex["AIIR"].sondrefl).absoluteValue.coerceIn(0.0, 1.0)
                AudioMixer.convolveBusOpen.volume = (1.0 - ratio1).pow(0.75)
                AudioMixer.convolveBusCave.volume = 0.0
            }
        }



        // the real updates
        (Gdx.audio as? Lwjgl3Audio)?.update()
        masterTrack.volume = masterVolume
        musicTrack.getFilter<Gain>().gain = musicVolume.toFloat()
        ambientTrack.getFilter<Gain>().gain = ambientVolume.toFloat()
        sfxSumBus.getFilter<Gain>().gain = sfxVolume.toFloat()
        guiTrack.getFilter<Gain>().gain = guiVolume.toFloat()


        // process fades
        fadeReqsCol.forEach { val track = it.key; val req = it.value
            if (req.fadeoutFired) {
                req.fadeAkku += delta
                val step = req.fadeAkku / req.fadeLength
                track.volume = FastMath.interpolateLinear(step, req.fadeStart, req.fadeTarget)

                if (req.fadeAkku >= req.fadeLength) {
                    req.fadeoutFired = false
                    track.volume = req.fadeTarget

                    // stop streaming if fadeBus is muted
                    if (req.fadeTarget == 0.0 && (track == musicTrack || track == fadeBus)) {
                        musicTrack.stop()
                        musicTrack.currentTrack = null
                    }
                    if (req.fadeTarget == 0.0 && (track == musicTrack || track == fadeBus)) {
                        ambientTrack.stop()
                        ambientTrack.currentTrack = null
                    }

                    req.callback()
                }
            }
            else if (req.fadeinFired) {
                req.fadeAkku += delta
                val step = req.fadeAkku / req.fadeLength
                track.volume = FastMath.interpolateLinear(step, req.fadeStart, req.fadeTarget)

                if (req.fadeAkku >= req.fadeLength) {
                    track.volume = req.fadeTarget
                    req.fadeinFired = false
                }

                req.callback
            }
        }


        if (lpOutFired) {
            lpAkku += delta
            val cutoff = linPercToLog(lpAkku / lpLength, lpStart, lpTarget)
            fadeBus.getFilter<Lowpass>().setCutoff(cutoff)

            if (lpAkku >= lpLength) {
                lpOutFired = false
                fadeBus.getFilter<Lowpass>().setCutoff(lpTarget)
            }
        }
        else if (lpInFired) {
            lpAkku += delta
            val cutoff = linPercToLog(lpAkku / lpLength, lpStart, lpTarget)
            fadeBus.getFilter<Lowpass>().setCutoff(cutoff)

            if (lpAkku >= lpLength) {
                fadeBus.getFilter<Lowpass>().setCutoff(lpTarget)
                lpInFired = false
            }
        }


        if (!musicTrack.isPlaying && musicTrack.nextTrack != null) {
            musicTrack.queueNext(null)
            fadeBus.volume = 1.0
            musicTrack.volume = 1.0
            musicTrack.play()
        }

        if (!ambientTrack.isPlaying && ambientTrack.nextTrack != null) {
            ambientTrack.queueNext(null)
            requestFadeIn(ambientTrack, DEFAULT_FADEOUT_LEN * 4, 1.0, 0.00001)
            ambientTrack.volume = 1.0
            ambientTrack.play()
        }
    }

    fun startMusic(song: MusicContainer) {
        if (musicTrack.isPlaying) {
            requestFadeOut(musicTrack, DEFAULT_FADEOUT_LEN)
        }
        musicTrack.nextTrack = song
    }

    fun stopMusic() {
        requestFadeOut(musicTrack, DEFAULT_FADEOUT_LEN)
    }

    fun startAmb(song: MusicContainer) {
        if (ambientTrack.isPlaying == true) {
            requestFadeOut(musicTrack, DEFAULT_FADEOUT_LEN)
        }
        ambientTrack.nextTrack = song
        // fade will be processed by the update()
    }

    fun stopAmb() {
        requestFadeOut(ambientTrack, DEFAULT_FADEOUT_LEN * 4)
    }

    fun requestFadeOut(track: TerrarumAudioMixerTrack, length: Double = DEFAULT_FADEOUT_LEN, target: Double = 0.0, source: Double? = null, jobAfterFadeout: () -> Unit = {}) {
        val req = fadeReqs[track]!!
        if (!req.fadeoutFired) {
            req.fadeLength = length.coerceAtLeast(1.0/1024.0)
            req.fadeAkku = 0.0
            req.fadeoutFired = true
            req.fadeTarget = target * track.maxVolume
            req.fadeStart = source ?: fadeBus.volume
            req.callback = jobAfterFadeout
        }
    }

    fun requestFadeIn(track: TerrarumAudioMixerTrack, length: Double, target: Double = 1.0, source: Double? = null, jobAfterFadeout: () -> Unit = {}) {
        val req = fadeReqs[track]!!
        if (!req.fadeinFired) {
            req.fadeLength = length.coerceAtLeast(1.0/1024.0)
            req.fadeAkku = 0.0
            req.fadeinFired = true
            req.fadeTarget = target * track.maxVolume
            req.fadeStart = source ?: fadeBus.volume
            req.callback = jobAfterFadeout
        }
    }


    fun requestLowpassOut(length: Double) {
        if (!lpOutFired) {
            lpLength = length.coerceAtLeast(1.0/1024.0)
            lpAkku = 0.0
            lpOutFired = true
            lpStart = fadeBus.getFilter<Lowpass>().cutoff
            lpTarget = SAMPLING_RATED / 2.0
        }
    }

    fun requestLowpassIn(length: Double) {
        if (!lpInFired) {
            lpLength = length.coerceAtLeast(1.0/1024.0)
            lpAkku = 0.0
            lpInFired = true
            lpStart = fadeBus.getFilter<Lowpass>().cutoff
            lpTarget = SAMPLING_RATED / 100.0
        }
    }

    fun reset() {
        dynamicTracks.forEach { it.stop() }
        tracks.filter { it.trackType == TrackType.STATIC_SOURCE }.forEach { it.stop() }
        tracks.forEach {
            it.processor.purgeBuffer()
        }
        fadeBus.getFilter<Lowpass>().setCutoff(TerrarumAudioMixerTrack.SAMPLING_RATEF / 2)
        // give some time for the cave bus to decay before ramping the volume up
        Timer().schedule(object : TimerTask() {
            override fun run() {
                fadeBus.volume = 1.0
            }
        }, 500L)
    }

    override fun dispose() {
        processingExecutor.killAll()
//        processingSubthreads.forEach { it.interrupt() }
        processing = false
        processingThread.interrupt()
//        feeder.stop()
//        feedingThread.join()
        tracks.forEach { it.tryDispose() }
        masterTrack.tryDispose()
    }
}

fun linToLogPerc(value: Double, low: Double, high: Double): Double {
    // https://www.desmos.com/calculator/dmhve2awxm
    val b = -ln(low / high)
    val a = low
    return ln(value / a) / b
}

fun linPercToLog(perc: Double, low: Double, high: Double): Double {
    // https://www.desmos.com/calculator/dmhve2awxm
    val t = perc.coerceIn(0.0, 1.0)
    val b = -ln(low / high)
    val a = low
    return a * exp(b * t)
}