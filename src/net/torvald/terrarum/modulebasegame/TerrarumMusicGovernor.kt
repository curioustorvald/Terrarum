package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.backends.lwjgl3.audio.Mp3
import com.badlogic.gdx.backends.lwjgl3.audio.Ogg
import com.badlogic.gdx.backends.lwjgl3.audio.OggInputStream
import com.badlogic.gdx.backends.lwjgl3.audio.Wav
import com.badlogic.gdx.backends.lwjgl3.audio.Wav.WavInputStream
import com.badlogic.gdx.utils.GdxRuntimeException
import com.jcraft.jorbis.VorbisFile
import javazoom.jl.decoder.Bitstream
import net.torvald.reflection.extortField
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.audio.AudioMixer
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATE
import net.torvald.unicode.EMDASH
import java.io.File
import javax.sound.sampled.AudioSystem

data class MusicContainer(
    val name: String,
    val file: File,
    val gdxMusic: Music,
    val songFinishedHook: (Music) -> Unit
) {
    val samplingRate: Int
    val codec: String

    var samplesRead = 0L; internal set
    val samplesTotal: Long

    init {
        gdxMusic.setOnCompletionListener(songFinishedHook)

        samplingRate = when (gdxMusic) {
            is Wav.Music -> {
                val rate = gdxMusic.extortField<WavInputStream>("input")!!.sampleRate

                printdbg(this, "music $name is WAV; rate = $rate")
                rate
            }
            is Ogg.Music -> {
                val rate = gdxMusic.extortField<OggInputStream>("input")!!.sampleRate

                printdbg(this, "music $name is OGG; rate = $rate")
                rate
            }
            is Mp3.Music -> {
                val tempMusic = Gdx.audio.newMusic(Gdx.files.absolute(file.absolutePath))
                val bitstream = tempMusic.extortField<Bitstream>("bitstream")!!
                val header = bitstream.readFrame()
                val rate = header.sampleRate
                tempMusic.dispose()

//                val bitstream = gdxMusic.extortField<Bitstream>("bitstream")!!
//                val header = bitstream.readFrame()
//                val rate = header.sampleRate
//                gdxMusic.reset()

                printdbg(this, "music $name is MP3; rate = $rate")
                rate
            }
            else -> {
                printdbg(this, "music $name is ${gdxMusic::class.qualifiedName}; rate = default")
                SAMPLING_RATE
            }
        }

        codec = gdxMusic::class.qualifiedName!!.split('.').let {
            if (it.last() == "Music") it.dropLast(1).last() else it.last()
        }

        samplesTotal = when (gdxMusic) {
            is Wav.Music -> getWavFileSampleCount(file)
            is Ogg.Music -> getOggFileSampleCount(file)
            else -> Long.MAX_VALUE
        }

    }

    private fun getWavFileSampleCount(file: File): Long {
        val ais = AudioSystem.getAudioInputStream(file)
        val r = ais.frameLength
        ais.close()
        return r
    }

    private fun getOggFileSampleCount(file: File): Long {
        try {
            val vorbisFile = VorbisFile(file.absolutePath)
            return vorbisFile.pcm_total(0)
        }
        catch (e: Throwable) {
            return Long.MAX_VALUE
        }
    }

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
                ) { stopMusic() }
            }
            catch (e: GdxRuntimeException) {
                e.printStackTrace()
                null
            }
        } ?: emptyList() // TODO test code

    private var musicBin: ArrayList<Int> = ArrayList(songs.indices.toList().shuffled())

    private val ambients: List<MusicContainer> =
        File(App.customAmbientDir).listFiles()?.mapNotNull {
            printdbg(this, "Ambient: ${it.absolutePath}")
            try {
                MusicContainer(
                    it.nameWithoutExtension.replace('_', ' ').split(" ").map { it.capitalize() }.joinToString(" "),
                    it,
                    Gdx.audio.newMusic(Gdx.files.absolute(it.absolutePath))
                ) { stopAmbient() }
            }
            catch (e: GdxRuntimeException) {
                e.printStackTrace()
                null
            }
        } ?: emptyList() // TODO test code

    private var ambientsBin: ArrayList<Int> = ArrayList(ambients.indices.toList().shuffled())



    init {
        songs.forEach {
            App.disposables.add(it.gdxMusic)
        }
        ambients.forEach {
            App.disposables.add(it.gdxMusic)
        }
    }


    private var warningPrinted = false


    private val STATE_INIT = 0
    private val STATE_FIREPLAY = 1
    private val STATE_PLAYING = 2
    private val STATE_INTERMISSION = 3

    protected var ambState = 0
    protected var ambFired = false

    private fun stopMusic() {
        musicState = STATE_INTERMISSION
        intermissionAkku = 0f
        intermissionLength = 30f + 30f * Math.random().toFloat() // 30s-60s
        musicFired = false
        printdbg(this, "Intermission: $intermissionLength seconds")
    }

    private fun startMusic(song: MusicContainer) {
        AudioMixer.startMusic(song)
        printdbg(this, "Now playing: $song")
        INGAME.sendNotification("Now Playing $EMDASH ${song.name}")
        musicState = STATE_PLAYING
    }


    private fun stopAmbient() {
        ambState = STATE_INTERMISSION
        intermissionAkku = 0f
        intermissionLength = 30f + 30f * Math.random().toFloat() // 30s-60s
        ambFired = false
        printdbg(this, "Intermission: $intermissionLength seconds")
    }

    private fun startAmbient(song: MusicContainer) {
        AudioMixer.startAmb(song)
        printdbg(this, "Now playing: $song")
        INGAME.sendNotification("Now Playing $EMDASH ${song.name}")
        ambState = STATE_PLAYING
    }


    override fun update(ingame: IngameInstance, delta: Float) {
        // start the song queueing if there is one to play
        if (songs.isNotEmpty() && musicState == 0) musicState = STATE_INTERMISSION
        if (ambients.isNotEmpty() && ambState == 0) ambState = STATE_INTERMISSION


        when (musicState) {
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
                    musicState = STATE_FIREPLAY
                }
            }
        }

        when (ambState) {
            STATE_FIREPLAY -> {
                if (!ambFired) {
                    ambFired = true

                    val song = ambients[ambientsBin.removeAt(0)]
                    // prevent same song to play twice
                    if (ambientsBin.isEmpty()) {
                        ambientsBin = ArrayList(ambients.indices.toList().shuffled())
                    }

                    startAmbient(song)
                }
            }
            STATE_PLAYING -> {
                // stopMusic() will be called when the music finishes; it's on the setOnCompletionListener
            }
            STATE_INTERMISSION -> {
                ambState = STATE_FIREPLAY
            }
        }


    }

    override fun dispose() {
        AudioMixer.requestFadeOut(AudioMixer.fadeBus, AudioMixer.DEFAULT_FADEOUT_LEN) // explicit call for fade-out when the game instance quits
        stopMusic()
        stopAmbient()
    }
}
