package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.utils.GdxRuntimeException
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.unicode.EMDASH
import java.io.File

data class MusicContainer(
    val name: String,
    val file: File,
    val gdxMusic: Music
) {
    override fun toString() = if (name.isEmpty()) file.nameWithoutExtension else name
}

class TerrarumMusicGovernor : MusicGovernor() {

    private val songs: List<MusicContainer> =
        File(App.customMusicDir).listFiles()?.mapNotNull {
            printdbg(this, "Music: ${it.absolutePath}")
            try {
                MusicContainer(
                    it.nameWithoutExtension.replace('_', ' ').split(" ").map { it.capitalize() }.joinToString(" "),
                    it,
                    Gdx.audio.newMusic(Gdx.files.absolute(it.absolutePath))
                )
            }
            catch (e: GdxRuntimeException) {
                e.printStackTrace()
                null
            }
        } ?: emptyList() // TODO test code

    private var musicBin: ArrayList<Int> = ArrayList(songs.indices.toList().shuffled())

    private var currentMusic: MusicContainer? = null
    private var currentAmbient: MusicContainer? = null



    private var warningPrinted = false

    private val musicVolume: Float
        get() = (App.getConfigDouble("bgmvolume") * App.getConfigDouble("mastervolume")).toFloat()
    private val ambientVolume: Float
        get() = (App.getConfigDouble("sfxvolume") * App.getConfigDouble("mastervolume")).toFloat()




    private fun stopMusic() {
        printdbg(this, "Now stopping: $currentMusic")
        state = 2
        intermissionAkku = 0f
        intermissionLength = 30f + 60f * Math.random().toFloat() // 30s-90m
        musicFired = false
        currentMusic = null
        fadeoutFired = false
        printdbg(this, "Intermission: $intermissionLength seconds")
    }

    private fun startMusic(song: MusicContainer) {
        song.gdxMusic.volume = musicVolume
        song.gdxMusic.play()
        printdbg(this, "Now playing: $song")

        INGAME.sendNotification("Now Playing $EMDASH ${song.name}")

        currentMusic = song
    }


    override fun update(ingame: IngameInstance, delta: Float) {
        if (songs.isEmpty()) {
            if (!warningPrinted) {
                warningPrinted = true
                printdbg(this, "Warning: songs list is empty")
            }
            return
        }

        val ingame = ingame as TerrarumIngame
        if (state == 0) state = 2


        when (state) {
            1 -> {
                if (!musicFired) {
                    musicFired = true

                    val song = songs[musicBin.removeAt(0)]
                    // prevent same song to play twice
                    if (musicBin.isEmpty()) {
                        musicBin = ArrayList(songs.indices.toList().shuffled())
                    }

                    startMusic(song)

                    // process fadeout request
                    if (fadeoutFired) {
                        fadeoutAkku += delta
                        currentMusic?.gdxMusic?.volume = 1f - musicVolume * (fadeoutAkku / fadeoutLength)

                        if (fadeoutAkku >= fadeoutLength) {
                            currentMusic?.gdxMusic?.pause()
                        }
                    }
                    // process fadein request
                    else if (fadeinFired) {
                        if (currentMusic?.gdxMusic?.isPlaying == false) {
                            currentMusic?.gdxMusic?.play()
                        }
                        fadeoutAkku += delta
                        currentMusic?.gdxMusic?.volume = musicVolume * (fadeoutAkku / fadeoutLength)
                    }
                }
                else {
                    if (currentMusic?.gdxMusic?.isPlaying == false) {
                        stopMusic()
                    }
                }
            }
            2 -> {
                intermissionAkku += delta

                if (intermissionAkku >= intermissionLength) {
                    intermissionAkku = 0f
                    state = 1
                }
            }
        }

    }

    override fun dispose() {
        currentMusic?.gdxMusic?.stop()
        stopMusic()
        songs.forEach { it.gdxMusic.tryDispose() }
    }
}
