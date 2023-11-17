package net.torvald.terrarum.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.audio.Lwjgl3Audio
import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarum.App
import net.torvald.terrarum.modulebasegame.MusicContainer
import net.torvald.terrarum.tryDispose

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


    private val tracks = Array(10) { TerrarumAudioMixerTrack("Audio Track #${it+1}") }

    private val masterTrack = TerrarumAudioMixerTrack("Master", true).also { master ->
        tracks.forEach { master.addSidechainInput(it, 1.0) }
        master.filters[0] = Lowpass(240, TerrarumAudioMixerTrack.SAMPLING_RATE)
    }

    private val musicTrack: TerrarumAudioMixerTrack
        get() = tracks[0]
    private val ambientTrack: TerrarumAudioMixerTrack
        get() = tracks[1]

    private var fadeAkku = 0.0
    private var fadeLength = DEFAULT_FADEOUT_LEN

    private var fadeoutFired = false
    private var fadeinFired = false

    // TODO make sidechaining work
    // TODO master volume controls the master track
    // TODO fadein/out controls the master track

    fun update(delta: Float) {
        (Gdx.audio as? Lwjgl3Audio)?.update()


        if (fadeoutFired) {
            fadeAkku += delta
            musicTrack.volume = (musicVolume * (1.0 - (fadeAkku / fadeLength))).coerceIn(0.0, 1.0)

//            printdbg(this, "Fadeout fired - akku: $fadeAkku; volume: ${currentMusic?.gdxMusic?.volume}")

            if (fadeAkku >= fadeLength) {
                fadeoutFired = false
                musicTrack.volume = 0.0
//                currentMusic?.gdxMusic?.pause()
                musicTrack.currentTrack = null

//                printdbg(this, "Fadeout end")
            }
        }
        // process fadein request
        else if (fadeinFired) {
            fadeAkku += delta
            musicTrack.volume = (musicVolume * (fadeAkku / fadeLength)).coerceIn(0.0, 1.0)

//            printdbg(this, "Fadein fired - akku: $fadeAkku; volume: ${currentMusic?.gdxMusic?.volume}")

            if (musicTrack.isPlaying == false) {
                musicTrack.play()
//                printdbg(this, "Fadein starting music ${currentMusic?.name}")
            }

            if (fadeAkku >= fadeLength) {
                musicTrack.volume = musicVolume
                fadeinFired = false

//                printdbg(this, "Fadein end")
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


    override fun dispose() {
        tracks.forEach { it.tryDispose() }
        masterTrack.tryDispose()
    }
}