package net.torvald.terrarum.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.audio.Lwjgl3Audio
import com.badlogic.gdx.utils.Disposable
import com.jme3.math.FastMath
import net.torvald.terrarum.App
import net.torvald.terrarum.audio.MixerTrackProcessor.Companion.BACK_BUF_COUNT
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.BUFFER_SIZE
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.INDEX_AMB
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.INDEX_BGM
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATE
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATED
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATEF
import net.torvald.terrarum.modulebasegame.MusicContainer
import net.torvald.terrarum.tryDispose
import java.lang.Thread.MAX_PRIORITY
import kotlin.math.*

/**
 * Any audio reference fed into this manager will get lost; you must manually store and dispose of them on your own.
 *
 * Created by minjaesong on 2023-11-07.
 */
object AudioMixer: Disposable {
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


    val tracks = Array(5) { TerrarumAudioMixerTrack(
        if (it == 0) "BGM"
        else if (it == 1) "AMB"
        else if (it == 2) "Sfx Mix"
        else if (it == 3) "GUI"
        else if (it == 4) "BUS1"
        else "Trk${it+1}", isBus = (it == 4)
    ) }

    val masterTrack = TerrarumAudioMixerTrack("Master", true)

    val musicTrack: TerrarumAudioMixerTrack
        get() = tracks[0]
    val ambientTrack: TerrarumAudioMixerTrack
        get() = tracks[1]
    val sfxMixTrack: TerrarumAudioMixerTrack
        get() = tracks[2]
    val guiTrack: TerrarumAudioMixerTrack
        get() = tracks[3]

    val fadeBus: TerrarumAudioMixerTrack
        get() = tracks[4]

    var processing = true

    val processingThread = Thread {
        while (processing) {
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
        }
    }

//    val feeder = FeedSamplesToAdev(BUFFER_SIZE, SAMPLING_RATE, masterTrack)
//    val feedingThread = Thread(feeder)


    init {
        masterTrack.filters[0] = Buffer

        fadeBus.addSidechainInput(musicTrack, 1.0)
        fadeBus.addSidechainInput(ambientTrack, 1.0)
        fadeBus.addSidechainInput(sfxMixTrack, 1.0)
        fadeBus.filters[0] = Lowpass(SAMPLING_RATE / 2f, SAMPLING_RATE)

        masterTrack.addSidechainInput(fadeBus, 1.0)
        masterTrack.addSidechainInput(guiTrack, 1.0)


        processingThread.priority = MAX_PRIORITY // higher = more predictable; audio delay is very noticeable so it gets high priority
        processingThread.start()
//        feedingThread.priority = MAX_PRIORITY
//        feedingThread.start()
    }


    private var fadeAkku = 0.0
    private var fadeLength = DEFAULT_FADEOUT_LEN
    private var fadeoutFired = false
    private var fadeinFired = false
    private var fadeTarget = 0.0
    private var fadeStart = 0.0

    private var lpAkku = 0.0
    private var lpLength = 0.4
    private var lpOutFired = false
    private var lpInFired = false
    private var lpStart = SAMPLING_RATED / 2.0
    private var lpTarget = SAMPLING_RATED / 2.0

    // TODO make sidechaining work
    // TODO master volume controls the master track
    // TODO fadein/out controls the master track

    fun update(delta: Float) {
        // the real updates
        (Gdx.audio as? Lwjgl3Audio)?.update()
        masterTrack.volume = masterVolume
        musicTrack.volume = musicVolume
        ambientTrack.volume = ambientVolume
        sfxMixTrack.volume = sfxVolume
        guiTrack.volume = guiVolume


        // process fades
        if (fadeoutFired) {
            fadeAkku += delta
            val step = fadeAkku / fadeLength
            fadeBus.volume = FastMath.interpolateLinear(step, fadeStart, fadeTarget)

            if (fadeAkku >= fadeLength) {
                fadeoutFired = false
                fadeBus.volume = fadeTarget
                fadeBus.volume = fadeTarget

                if (fadeTarget == 0.0) {
                    musicTrack.currentTrack = null
                    ambientTrack.currentTrack = null
                }
            }
        }
        else if (fadeinFired) {
            fadeAkku += delta
            val step = fadeAkku / fadeLength
            fadeBus.volume = FastMath.interpolateLinear(step, fadeStart, fadeTarget)

//            if (musicTrack.isPlaying == false) {
//                musicTrack.play()
//            }

            if (fadeAkku >= fadeLength) {
                fadeBus.volume = fadeTarget
                fadeinFired = false
            }
        }


        if (lpOutFired) {
            lpAkku += delta
            // https://www.desmos.com/calculator/dmhve2awxm
            val t = (lpAkku / lpLength).coerceIn(0.0, 1.0)
            val b = ln(lpStart / lpTarget) / -1.0
            val a = lpStart
            val cutoff = a * exp(b * t)
            (fadeBus.filters[0] as Lowpass).setCutoff(cutoff)


            if (lpAkku >= lpLength) {
                lpOutFired = false
                (fadeBus.filters[0] as Lowpass).setCutoff(lpTarget)
            }
        }
        else if (lpInFired) {
            lpAkku += delta
            // https://www.desmos.com/calculator/dmhve2awxm
            val t = (lpAkku / lpLength).coerceIn(0.0, 1.0)
            val b = ln(lpStart / lpTarget) / -1.0
            val a = lpStart
            val cutoff = a * exp(b * t)
            (fadeBus.filters[0] as Lowpass).setCutoff(cutoff)

            if (lpAkku >= lpLength) {
                (fadeBus.filters[0] as Lowpass).setCutoff(lpTarget)
                lpInFired = false
            }
        }


        if (musicTrack.isPlaying != true && musicTrack.nextTrack != null) {
//            printdbg(this, "Playing next music: ${nextMusic!!.name}")
            musicTrack.queueNext(null)
            fadeBus.volume = 1.0
            musicTrack.play()
        }
    }

    fun startMusic(song: MusicContainer) {
        if (musicTrack.isPlaying == true) {
            requestFadeOut(DEFAULT_FADEOUT_LEN)
        }
        musicTrack.nextTrack = song
    }

    fun stopMusic() {
        requestFadeOut(DEFAULT_FADEOUT_LEN)
    }

    fun requestFadeOut(length: Double, target: Double = 0.0) {
        if (!fadeoutFired) {
            fadeLength = length.coerceAtLeast(1.0/1024.0)
            fadeAkku = 0.0
            fadeoutFired = true
            fadeTarget = target
            fadeStart = fadeBus.volume
        }
    }

    fun requestFadeIn(length: Double, target: Double = 1.0) {
        if (!fadeinFired) {
            fadeLength = length.coerceAtLeast(1.0/1024.0)
            fadeAkku = 0.0
            fadeinFired = true
            fadeTarget = target
            fadeStart = fadeBus.volume
        }
    }


    fun requestLowpassOut(length: Double) {
        if (!lpOutFired) {
            lpLength = length.coerceAtLeast(1.0/1024.0)
            lpAkku = 0.0
            lpOutFired = true
            lpStart = (fadeBus.filters[0] as Lowpass).cutoff
            lpTarget = SAMPLING_RATED / 2.0
        }
    }

    fun requestLowpassIn(length: Double) {
        if (!lpInFired) {
            lpLength = length.coerceAtLeast(1.0/1024.0)
            lpAkku = 0.0
            lpInFired = true
            lpStart = (fadeBus.filters[0] as Lowpass).cutoff
            lpTarget = SAMPLING_RATED / 100.0
        }
    }


    override fun dispose() {
        processing = false
        processingThread.join()
//        feeder.stop()
//        feedingThread.join()
        tracks.forEach { it.tryDispose() }
        masterTrack.tryDispose()
    }
}