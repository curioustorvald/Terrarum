package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.utils.GdxRuntimeException
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.audio.AudioMixer
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
                    Gdx.audio.newMusic(Gdx.files.absolute(it.absolutePath)).also {
                        it.setOnCompletionListener {
                            stopMusic()
                        }
                    }
                )
            }
            catch (e: GdxRuntimeException) {
                e.printStackTrace()
                null
            }
        } ?: emptyList() // TODO test code

    private var musicBin: ArrayList<Int> = ArrayList(songs.indices.toList().shuffled())


    init {
        songs.forEach {
            App.disposables.add(it.gdxMusic)
        }
    }


    private var warningPrinted = false


    private val STATE_INIT = 0
    private val STATE_FIREPLAY = 1
    private val STATE_PLAYING = 2
    private val STATE_INTERMISSION = 3


    private fun stopMusic() {
//        AudioManager.stopMusic() // music will stop itself; with this line not commented, the stop-callback from the already disposed musicgovernor will stop the music queued by the new musicgovernor instance
        state = STATE_INTERMISSION
        intermissionAkku = 0f
        intermissionLength = 30f + 30f * Math.random().toFloat() // 30s-60s
        musicFired = false
        printdbg(this, "Intermission: $intermissionLength seconds")
    }

    private fun startMusic(song: MusicContainer) {
        AudioMixer.startMusic(song)
        printdbg(this, "Now playing: $song")
        INGAME.sendNotification("Now Playing $EMDASH ${song.name}")
        state = STATE_PLAYING
    }


    override fun update(ingame: IngameInstance, delta: Float) {
        if (songs.isEmpty()) {
            if (!warningPrinted) {
                warningPrinted = true
                printdbg(this, "Warning: songs list is empty")
            }
            return
        }

//        val ingame = ingame as TerrarumIngame
        if (state == 0) state = STATE_INTERMISSION


        when (state) {
            STATE_FIREPLAY -> {
                if (!musicFired) {
                    musicFired = true

                    val song = songs[musicBin.removeAt(0)]
                    // prevent same song to play twice
                    if (musicBin.isEmpty()) {
                        musicBin = ArrayList(songs.indices.toList().shuffled())
                    }

                    startMusic(song)
                }
            }
            STATE_PLAYING -> {
                // stopMusic() will be called when the music finishes; it's on the setOnCompletionListener
            }
            STATE_INTERMISSION -> {
                intermissionAkku += delta

                if (intermissionAkku >= intermissionLength) {
                    intermissionAkku = 0f
                    state = 1
                }
            }
        }

    }

    override fun dispose() {
        AudioMixer.stopMusic() // explicit call for fade-out when the game instance quits
        stopMusic()
    }
}
