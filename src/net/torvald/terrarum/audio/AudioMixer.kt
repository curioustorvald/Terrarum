package net.torvald.terrarum.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.audio.Lwjgl3Audio
import com.badlogic.gdx.utils.Disposable
import com.jme3.math.FastMath
import net.torvald.terrarum.App
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.INDEX_AMB
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.INDEX_BGM
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATED
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATEF
import net.torvald.terrarum.modulebasegame.MusicContainer
import net.torvald.terrarum.tryDispose
import kotlin.math.*

/**
 * Any audio reference fed into this manager will get lost; you must manually store and dispose of them on your own.
 *
 * Created by minjaesong on 2023-11-07.
 */
object AudioMixer: Disposable {
    const val DEFAULT_FADEOUT_LEN = 1.8

    /** Returns a master volume */
    val masterVolume: Double
        get() = App.getConfigDouble("mastervolume")

    /** Returns a (master volume * bgm volume) */
    val musicVolume: Double
        get() = App.getConfigDouble("bgmvolume")

    /** Returns a (master volume * sfx volume */
    val ambientVolume: Double
        get() = App.getConfigDouble("sfxvolume")


    val tracks = Array(4) { TerrarumAudioMixerTrack(
        if (it == 0) "BGM"
        else if (it == 1) "AMB"
        else if (it == 2) "Sfx Mix"
        else if (it == 3) "GUI"
        else "Trk${it+1}"
    ) }

    val masterTrack = TerrarumAudioMixerTrack("Master", true).also { master ->
        master.volume = masterVolume
        master.filters[0] = Buffer
        tracks.forEachIndexed { i, it -> master.addSidechainInput(it, if (i == INDEX_BGM) musicVolume else if (i == INDEX_AMB) ambientVolume else 1.0) }
    }

    val musicTrack: TerrarumAudioMixerTrack
        get() = tracks[0]
    val ambientTrack: TerrarumAudioMixerTrack
        get() = tracks[1]
    val sfxMixTrack: TerrarumAudioMixerTrack
        get() = tracks[2]
    val guiTrack: TerrarumAudioMixerTrack
        get() = tracks[3]


    init {
        musicTrack.filters[0] = Lowpass(48000f, TerrarumAudioMixerTrack.SAMPLING_RATE)
        ambientTrack.filters[0] = Lowpass(48000f, TerrarumAudioMixerTrack.SAMPLING_RATE)
        sfxMixTrack.filters[0] = Lowpass(48000f, TerrarumAudioMixerTrack.SAMPLING_RATE)
    }

    val faderTrack = arrayOf(musicTrack, ambientTrack, sfxMixTrack)

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
        (Gdx.audio as? Lwjgl3Audio)?.update()


        if (fadeoutFired) {
            fadeAkku += delta
            val step = fadeAkku / fadeLength
            faderTrack.forEach { it.volume = FastMath.interpolateLinear(step, fadeStart, fadeTarget) }

            if (fadeAkku >= fadeLength) {
                fadeoutFired = false
                musicTrack.volume = fadeTarget
                faderTrack.forEach { it.volume = fadeTarget }

                if (fadeTarget == 0.0) {
                    musicTrack.currentTrack = null
                    ambientTrack.currentTrack = null
                }
            }
        }
        else if (fadeinFired) {
            fadeAkku += delta
            val step = fadeAkku / fadeLength
            faderTrack.forEach { it.volume = FastMath.interpolateLinear(step, fadeStart, fadeTarget) }

//            if (musicTrack.isPlaying == false) {
//                musicTrack.play()
//            }

            if (fadeAkku >= fadeLength) {
                faderTrack.forEach { it.volume = fadeTarget }
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
            faderTrack.forEach { (it.filters[0] as Lowpass).setCutoff(cutoff) }


            if (lpAkku >= lpLength) {
                lpOutFired = false
                faderTrack.forEach { (it.filters[0] as Lowpass).setCutoff(lpTarget) }
            }
        }
        else if (lpInFired) {
            lpAkku += delta
            // https://www.desmos.com/calculator/dmhve2awxm
            val t = (lpAkku / lpLength).coerceIn(0.0, 1.0)
            val b = ln(lpStart / lpTarget) / -1.0
            val a = lpStart
            val cutoff = a * exp(b * t)
            faderTrack.forEach { (it.filters[0] as Lowpass).setCutoff(cutoff) }

            if (lpAkku >= lpLength) {
                faderTrack.forEach { (it.filters[0] as Lowpass).setCutoff(lpTarget) }
                lpInFired = false
            }
        }


        if (musicTrack.isPlaying != true && musicTrack.nextTrack != null) {
//            printdbg(this, "Playing next music: ${nextMusic!!.name}")
            musicTrack.queueNext(null)
            musicTrack.volume = 1.0
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
            fadeStart = musicTrack.volume
        }
    }

    fun requestFadeIn(length: Double, target: Double = 1.0) {
        if (!fadeinFired) {
            fadeLength = length.coerceAtLeast(1.0/1024.0)
            fadeAkku = 0.0
            fadeinFired = true
            fadeTarget = target
            fadeStart = musicTrack.volume
        }
    }


    fun requestLowpassOut(length: Double) {
        if (!lpOutFired) {
            lpLength = length.coerceAtLeast(1.0/1024.0)
            lpAkku = 0.0
            lpOutFired = true
            lpStart = (musicTrack.filters[0] as Lowpass).cutoff
            lpTarget = SAMPLING_RATED / 2.0
        }
    }

    fun requestLowpassIn(length: Double) {
        if (!lpInFired) {
            lpLength = length.coerceAtLeast(1.0/1024.0)
            lpAkku = 0.0
            lpInFired = true
            lpStart = (musicTrack.filters[0] as Lowpass).cutoff
            lpTarget = SAMPLING_RATED / 100.0
        }
    }


    override fun dispose() {
        tracks.forEach { it.tryDispose() }
        masterTrack.tryDispose()
    }
}