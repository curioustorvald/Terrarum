package net.torvald.terrarum

import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.modulebasegame.MusicContainer

/**
 * Created by minjaesong on 2023-11-07.
 */
object AudioManager {

    /** Returns a master volume */
    val masterVolume: Float
        get() = App.getConfigDouble("mastervolume").toFloat()

    /** Returns a (master volume * bgm volume) */
    val musicVolume: Float
        get() = (App.getConfigDouble("bgmvolume") * App.getConfigDouble("mastervolume")).toFloat()

    /** Returns a (master volume * sfx volume */
    val ambientVolume: Float
        get() = (App.getConfigDouble("sfxvolume") * App.getConfigDouble("mastervolume")).toFloat()


    var currentMusic: MusicContainer? = null
    var currentAmbient: MusicContainer? = null

    private var nextMusic: MusicContainer? = null

    private var fadeAkku = 0f
    private var fadeLength = 1f

    private var fadeoutFired = false
    private var fadeinFired = false

    fun update(delta: Float) {
        if (fadeoutFired) {
            fadeAkku += delta
            currentMusic?.gdxMusic?.volume = musicVolume * (1f - (fadeAkku / fadeLength)).coerceIn(0f, 1f)

            printdbg(this, "Fadeout fired - akku: $fadeAkku; volume: ${currentMusic?.gdxMusic?.volume}")

            if (fadeAkku >= fadeLength) {
                fadeoutFired = false
                currentMusic?.gdxMusic?.volume = 0f
                currentMusic?.gdxMusic?.pause()
                currentMusic = null

                printdbg(this, "Fadeout end")
            }
        }
        // process fadein request
        else if (fadeinFired) {
            fadeAkku += delta
            currentMusic?.gdxMusic?.volume = (musicVolume * (fadeAkku / fadeLength)).coerceIn(0f, 1f)

            printdbg(this, "Fadein fired - akku: $fadeAkku; volume: ${currentMusic?.gdxMusic?.volume}")

            if (currentMusic?.gdxMusic?.isPlaying == false) {
                currentMusic?.gdxMusic?.play()
                printdbg(this, "Fadein starting music ${currentMusic?.name}")
            }

            if (fadeAkku >= fadeLength) {
                currentMusic?.gdxMusic?.volume = musicVolume
                fadeinFired = false

                printdbg(this, "Fadein end")
            }
        }


        if (currentMusic?.gdxMusic?.isPlaying != true && nextMusic != null) {
            printdbg(this, "Playing next music: ${nextMusic!!.name}")
            currentMusic = nextMusic
            nextMusic = null
            currentMusic!!.gdxMusic.volume = musicVolume
            currentMusic!!.gdxMusic.play()
        }
    }

    fun startMusic(song: MusicContainer) {
        if (currentMusic?.gdxMusic?.isPlaying == true) {
            requestFadeOut(1f)
        }
        nextMusic = song
    }

    fun stopMusic() {
        requestFadeOut(1f)
    }

    fun requestFadeOut(length: Float) {
        if (!fadeoutFired) {
            fadeLength = length.coerceAtLeast(1f/1024f)
            fadeAkku = 0f
            fadeoutFired = true
        }
    }

    fun requestFadeIn(length: Float) {
        if (!fadeinFired) {
            fadeLength = length.coerceAtLeast(1f/1024f)
            fadeAkku = 0f
            fadeinFired = true
        }
    }

}