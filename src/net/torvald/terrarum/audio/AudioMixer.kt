package net.torvald.terrarum.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.lwjgl3.audio.Lwjgl3Audio
import com.badlogic.gdx.utils.Disposable
import com.jme3.math.FastMath
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATE
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATED
import net.torvald.terrarum.concurrent.ThreadExecutor
import net.torvald.terrarum.modulebasegame.MusicContainer
import net.torvald.terrarum.tryDispose
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


    val tracks = Array(8) { TerrarumAudioMixerTrack(
        if (it == 0) "Music"
        else if (it == 1) "Ambient"
        else if (it == 2) "Player"
        else if (it == 3) "GUI"
        else if (it == 4) "\u00F0 \u00E4 \u00F0" // summation
        else if (it == 5) "\u00D9Open\u00D9" // convolution1
        else if (it == 6) "\u00D9Cave\u00D9" // convolution2
        else if (it == 7) "\u00F0 \u00DA \u00F0" // fade
        else "Trk${it+1}", isBus = (it >= 4), maxVolumeFun = {
            when (it) {
                0 -> { musicVolume }
                1 -> { ambientVolume }
                2 -> { sfxVolume }
                3 -> { guiVolume }
                else -> { 1.0 }
            }
        }
    ) }

    val masterTrack = TerrarumAudioMixerTrack("\u00DBMASTER", true) { masterVolume }

    val musicTrack: TerrarumAudioMixerTrack
        get() = tracks[0]
    val ambientTrack: TerrarumAudioMixerTrack
        get() = tracks[1]
    val sfxMixTrack: TerrarumAudioMixerTrack
        get() = tracks[2]
    val guiTrack: TerrarumAudioMixerTrack
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

    private val processingExecutor = ThreadExecutor()
//    private lateinit var processingSubthreads: List<Thread>
    val processingThread = Thread {
        /*while (processing) {
            // process
            tracks.forEach {
                if (!it.processor.paused) {
                    it.processor.run()
                }
            }
            masterTrack.processor.run()

            /*while (masterTrack.pcmQueue.size >= BACK_BUF_COUNT && masterTrack.processor.running && processing) {
                Thread.sleep(1)
            }*/

            while (!masterTrack.pcmQueue.isEmpty) {
                masterTrack.adev!!.writeSamples(masterTrack.pcmQueue.removeFirst()) // it blocks until the queue is consumed
            }
        }*/

        while (processing) {
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
        }
    }

    val parallelProcessingSchedule: Array<Array<TerrarumAudioMixerTrack>>

//    val feeder = FeedSamplesToAdev(BUFFER_SIZE, SAMPLING_RATE, masterTrack)
//    val feedingThread = Thread(feeder)


    init {
        // initialise audio paths //

//        musicTrack.filters[1] = BinoPan(0f)
//        musicTrack.filters[2] = Reverb(36f, 0.92f, 1200f)

        listOf(musicTrack, ambientTrack, sfxMixTrack, guiTrack).forEach {
            it.filters[0] = Gain(1f)
        }

        masterTrack.filters[0] = SoftClp
        masterTrack.filters[1] = Buffer
        masterTrack.filters[2] = Scope()

        listOf(sumBus, convolveBusOpen, convolveBusCave).forEach {
            it.addSidechainInput(musicTrack, 1.0)
            it.addSidechainInput(ambientTrack, 1.0)
            it.addSidechainInput(sfxMixTrack, 1.0)
        }

        convolveBusOpen.filters[0] = Highpass(80f)
        convolveBusOpen.filters[1] = Convolv(ModMgr.getFile("basegame", "audio/convolution/EchoThief - PurgatoryChasm.bin"))
        convolveBusOpen.filters[2] = Gain(decibelsToFullscale(18.0).toFloat()) // don't make it too loud; it'll sound like a shit
        convolveBusOpen.volume = 0.5 // will be controlled by the other updater which surveys the world

        convolveBusCave.filters[0] = Highpass(80f)
        convolveBusCave.filters[1] = Convolv(ModMgr.getFile("basegame", "audio/convolution/EchoThief - WaterplacePark-trimmed.bin"))
        convolveBusCave.filters[2] = Gain(decibelsToFullscale(18.0).toFloat())
        convolveBusCave.volume = 0.5 // will be controlled by the other updater which surveys the world

        fadeBus.addSidechainInput(sumBus, 1.0 / 3.0)
        fadeBus.addSidechainInput(convolveBusOpen, 2.0 / 3.0)
        fadeBus.addSidechainInput(convolveBusCave, 2.0 / 3.0)
        fadeBus.filters[0] = Lowpass(SAMPLING_RATE / 2f)

        masterTrack.addSidechainInput(fadeBus, 1.0)
        masterTrack.addSidechainInput(guiTrack, 1.0)


        parallelProcessingSchedule = arrayOf(
            arrayOf(musicTrack, ambientTrack, sfxMixTrack, guiTrack),
            arrayOf(sumBus, convolveBusOpen, convolveBusCave),
            arrayOf(fadeBus),
            arrayOf(masterTrack)
        )


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
    )

    private val fadeReqs = HashMap<TerrarumAudioMixerTrack, FadeRequest>().also { map ->
        listOf(musicTrack, ambientTrack, sfxMixTrack, guiTrack, fadeBus).forEach {
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
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            convolveBusOpen.volume = (convolveBusOpen.volume + 0.001).coerceIn(0.0, 1.0)
            convolveBusCave.volume = 1.0 - convolveBusOpen.volume
        }
        else if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            convolveBusOpen.volume = (convolveBusOpen.volume - 0.001).coerceIn(0.0, 1.0)
            convolveBusCave.volume = 1.0 - convolveBusOpen.volume
        }



        // the real updates
        (Gdx.audio as? Lwjgl3Audio)?.update()
        masterTrack.volume = masterVolume
        musicTrack.getFilter<Gain>().gain = musicVolume.toFloat()
        ambientTrack.getFilter<Gain>().gain = ambientVolume.toFloat()
        sfxMixTrack.getFilter<Gain>().gain = sfxVolume.toFloat()
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
                    if (req.fadeTarget == 0.0 && track == fadeBus) {
                        musicTrack.currentTrack = null
                        musicTrack.streamPlaying = false
                        ambientTrack.currentTrack = null
                        ambientTrack.streamPlaying = false
                    }
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
            musicTrack.play()
        }

        if (!ambientTrack.isPlaying && ambientTrack.nextTrack != null) {
            ambientTrack.queueNext(null)
            requestFadeIn(ambientTrack, DEFAULT_FADEOUT_LEN * 4, 1.0, 0.00001)
            ambientTrack.play()
        }
    }

    fun startMusic(song: MusicContainer) {
        if (musicTrack.isPlaying == true) {
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

    fun requestFadeOut(track: TerrarumAudioMixerTrack, length: Double, target: Double = 0.0, source: Double? = null) {
        val req = fadeReqs[track]!!
        if (!req.fadeoutFired) {
            req.fadeLength = length.coerceAtLeast(1.0/1024.0)
            req.fadeAkku = 0.0
            req.fadeoutFired = true
            req.fadeTarget = target * track.maxVolume
            req.fadeStart = source ?: fadeBus.volume
        }
    }

    fun requestFadeIn(track: TerrarumAudioMixerTrack, length: Double, target: Double = 1.0, source: Double? = null) {
        val req = fadeReqs[track]!!
        if (!req.fadeinFired) {
            req.fadeLength = length.coerceAtLeast(1.0/1024.0)
            req.fadeAkku = 0.0
            req.fadeinFired = true
            req.fadeTarget = target * track.maxVolume
            req.fadeStart = source ?: fadeBus.volume
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