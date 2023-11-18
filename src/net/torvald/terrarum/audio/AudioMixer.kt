package net.torvald.terrarum.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.audio.Lwjgl3Audio
import com.badlogic.gdx.utils.Disposable
import com.jme3.math.FastMath
import net.torvald.terrarum.App
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATED
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATEF
import net.torvald.terrarum.modulebasegame.MusicContainer
import net.torvald.terrarum.tryDispose
import kotlin.math.log
import kotlin.math.log10
import kotlin.math.pow

/**
 * Any audio reference fed into this manager will get lost; you must manually store and dispose of them on your own.
 *
 * Created by minjaesong on 2023-11-07.
 */
object AudioMixer: Disposable {
    const val DEFAULT_FADEOUT_LEN = 2.4

    /** Returns a master volume */
    val masterVolume: Double
        get() = App.getConfigDouble("mastervolume")

    /** Returns a (master volume * bgm volume) */
    val musicVolume: Double
        get() = (App.getConfigDouble("bgmvolume") * App.getConfigDouble("mastervolume"))

    /** Returns a (master volume * sfx volume */
    val ambientVolume: Double
        get() = (App.getConfigDouble("sfxvolume") * App.getConfigDouble("mastervolume"))


    val tracks = Array(10) { TerrarumAudioMixerTrack(
        if (it == 0) "BGM Track"
        else if (it == 1) "AMB Track"
        else "Audio Track #${it+1}"
    ) }

    val masterTrack = TerrarumAudioMixerTrack("Master", true).also { master ->
        tracks.forEach { master.addSidechainInput(it, 1.0) }
    }

    val musicTrack: TerrarumAudioMixerTrack
        get() = tracks[0]
    val ambientTrack: TerrarumAudioMixerTrack
        get() = tracks[1]

    init {
        musicTrack.filters[0] = Lowpass(48000f, TerrarumAudioMixerTrack.SAMPLING_RATE)
        ambientTrack.filters[0] = Lowpass(48000f, TerrarumAudioMixerTrack.SAMPLING_RATE)
    }

    private var fadeAkku = 0.0
    private var fadeLength = DEFAULT_FADEOUT_LEN
    private var fadeoutFired = false
    private var fadeinFired = false

    private var lpAkku = 0.0
    private var lpLength = 0.4
    private var lpOutFired = false
    private var lpInFired = false

    // TODO make sidechaining work
    // TODO master volume controls the master track
    // TODO fadein/out controls the master track

    fun update(delta: Float) {
        (Gdx.audio as? Lwjgl3Audio)?.update()


        if (fadeoutFired) {
            fadeAkku += delta
            musicTrack.volume = (musicVolume * (1.0 - (fadeAkku / fadeLength))).coerceIn(0.0, 1.0)

            if (fadeAkku >= fadeLength) {
                fadeoutFired = false
                musicTrack.volume = 0.0
                musicTrack.currentTrack = null
            }
        }
        else if (fadeinFired) {
            fadeAkku += delta
            musicTrack.volume = (musicVolume * (fadeAkku / fadeLength)).coerceIn(0.0, 1.0)

            if (musicTrack.isPlaying == false) {
                musicTrack.play()
            }

            if (fadeAkku >= fadeLength) {
                musicTrack.volume = musicVolume
                fadeinFired = false
            }
        }


        if (lpOutFired) {
            lpAkku += delta
            val x = (lpAkku / lpLength).coerceIn(0.0, 1.0)
            val q = 400.0
            val step = (q.pow(x) - 1) / (q - 1) // https://www.desmos.com/calculator/sttaq2qhzm
            val cutoff = FastMath.interpolateLinear(step, SAMPLING_RATED / 100.0, SAMPLING_RATED)
            (musicTrack.filters[0] as Lowpass).setCutoff(cutoff)

            if (lpAkku >= lpLength) {
                lpOutFired = false
                (musicTrack.filters[0] as Lowpass).setCutoff(SAMPLING_RATEF)
            }
        }
        else if (lpInFired) {
            lpAkku += delta
            val x = (lpAkku / lpLength).coerceIn(0.0, 1.0)
            val q = 400.0
            val step = log((q-1) * x + 1.0, q) // https://www.desmos.com/calculator/sttaq2qhzm
            val cutoff = FastMath.interpolateLinear(step, SAMPLING_RATED, SAMPLING_RATED / 100.0)
            (musicTrack.filters[0] as Lowpass).setCutoff(cutoff)

            if (lpAkku >= lpLength) {
                (musicTrack.filters[0] as Lowpass).setCutoff(SAMPLING_RATEF / 100.0)
                lpInFired = false
            }
        }


        if (musicTrack.isPlaying != true && musicTrack.nextTrack != null) {
//            printdbg(this, "Playing next music: ${nextMusic!!.name}")
            musicTrack.queueNext(null)
            musicTrack.volume = musicVolume
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

    fun requestFadeOut(length: Double) {
        if (!fadeoutFired) {
            fadeLength = length.coerceAtLeast(1.0/1024.0)
            fadeAkku = 0.0
            fadeoutFired = true
        }
    }

    fun requestFadeIn(length: Double) {
        if (!fadeinFired) {
            fadeLength = length.coerceAtLeast(1.0/1024.0)
            fadeAkku = 0.0
            fadeinFired = true
        }
    }


    fun requestLowpassOut(length: Double) {
        if (!lpOutFired) {
            lpLength = length.coerceAtLeast(1.0/1024.0)
            lpAkku = 0.0
            lpOutFired = true
        }
    }

    fun requestLowpassIn(length: Double) {
        if (!lpInFired) {
            lpLength = length.coerceAtLeast(1.0/1024.0)
            lpAkku = 0.0
            lpInFired = true
        }
    }


    override fun dispose() {
        tracks.forEach { it.tryDispose() }
        masterTrack.tryDispose()
    }
}