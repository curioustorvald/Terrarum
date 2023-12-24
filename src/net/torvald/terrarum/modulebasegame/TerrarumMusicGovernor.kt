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
import java.io.FileInputStream
import javax.sound.sampled.AudioSystem

data class MusicContainer(
    val name: String,
    val file: File,
    val gdxMusic: Music,
    internal var songFinishedHook: (Music) -> Unit = {}
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
            is Mp3.Music -> getMp3FileSampleCount(file)
            else -> Long.MAX_VALUE
        }

    }

    private fun getWavFileSampleCount(file: File): Long {
        return try {
            val ais = AudioSystem.getAudioInputStream(file)
            val r = ais.frameLength
            ais.close()
            r
        }
        catch (e: Throwable) {
            Long.MAX_VALUE
        }
    }

    private fun getOggFileSampleCount(file: File): Long {
        return try {
            val vorbisFile = VorbisFile(file.absolutePath)
            vorbisFile.pcm_total(0)
        }
        catch (e: Throwable) {
            Long.MAX_VALUE
        }
    }

    private fun getMp3FileSampleCount(file: File): Long {
        return try {
            val fis = FileInputStream(file)
            val bs = Bitstream(fis)

            var header = bs.readFrame()
            val rate = header.frequency()
            var totalSamples = 0L

            while (header != null) {
                totalSamples += (header.ms_per_frame() * rate / 1000).toLong()
                bs.closeFrame()
                header = bs.readFrame()
            }

            bs.close()
            fis.close()

            totalSamples
        }
        catch (_: Throwable) {
            Long.MAX_VALUE
        }
    }

    override fun toString() = if (name.isEmpty()) file.nameWithoutExtension else name
}

class TerrarumMusicGovernor : MusicGovernor() {
    private val STATE_INIT = 0
    private val STATE_FIREPLAY = 1
    private val STATE_PLAYING = 2
    private val STATE_INTERMISSION = 3


    init {
        musicState = STATE_INTERMISSION
    }

    private var songs: List<MusicContainer> = emptyList()
    private var musicBin: ArrayList<Int> = ArrayList()
    private var shuffled = true
    private var diskJockeyingMode = "intermittent" // intermittent, continuous

    private fun registerSongsFromDir(musicDir: String) {
        songs = File(musicDir).listFiles()?.sortedBy { it.name }?.mapNotNull {
            printdbg(this, "Music: ${it.absolutePath}")
            try {
                MusicContainer(
                    it.nameWithoutExtension.replace('_', ' ').split(" ").map { it.capitalize() }.joinToString(" "),
                    it,
                    Gdx.audio.newMusic(Gdx.files.absolute(it.absolutePath))
                ).also {  muscon ->
                    muscon.songFinishedHook =  { stopMusic(muscon) }
                }
            }
            catch (e: GdxRuntimeException) {
                e.printStackTrace()
                null
            }
        } ?: emptyList() // TODO test code
    }

    private fun restockMUsicBin() {
        musicBin = if (shuffled) ArrayList(songs.indices.toList().shuffled()) else  ArrayList(songs.indices.toList())
    }

    /**
     * @param musicDir where the music files are. Absolute path.
     * @param shuffled if the tracks are to be shuffled
     * @param diskJockeyingMode `intermittent` to give random gap between tracks, `continuous` for continuous playback
     */
    fun queueDirectory(musicDir: String, shuffled: Boolean, diskJockeyingMode: String) {
        if (musicState != STATE_INIT && musicState != STATE_INTERMISSION) {
            AudioMixer.requestFadeOut(AudioMixer.fadeBus, AudioMixer.DEFAULT_FADEOUT_LEN) // explicit call for fade-out when the game instance quits
            stopMusic(AudioMixer.musicTrack.currentTrack)
        }

        songs.forEach { it.gdxMusic.tryDispose() }
        registerSongsFromDir(musicDir)

        this.shuffled = shuffled
        this.diskJockeyingMode = diskJockeyingMode

        restockMUsicBin()
    }

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

    private val musicStartHooks = ArrayList<(MusicContainer) -> Unit>()
    private val musicStopHooks = ArrayList<(MusicContainer) -> Unit>()

    init {
        queueDirectory(App.customMusicDir, true, "intermittent")
    }


    fun addMusicStartHook(f: (MusicContainer) -> Unit) {
        musicStartHooks.add(f)
    }

    fun addMusicStopHook(f: (MusicContainer) -> Unit) {
        musicStopHooks.add(f)
    }

    init {
        songs.forEach {
            App.disposables.add(it.gdxMusic)
        }
        ambients.forEach {
            App.disposables.add(it.gdxMusic)
        }
    }


    private var warningPrinted = false



    protected var ambState = 0
    protected var ambFired = false

    private fun stopMusic(song: MusicContainer?) {
        musicState = STATE_INTERMISSION
        intermissionAkku = 0f
        intermissionLength = if (diskJockeyingMode == "intermittent") 30f + 30f * Math.random().toFloat() else 0f // 30s-60s
        musicFired = false
        if (musicStopHooks.isNotEmpty()) musicStopHooks.forEach { if (song != null) { it(song) } }
        printdbg(this, "StopMusic Intermission: $intermissionLength seconds")
    }

    private fun startMusic(song: MusicContainer) {
        AudioMixer.startMusic(song)
        printdbg(this, "startMusic Now playing: $song")
//        INGAME.sendNotification("Now Playing $EMDASH ${song.name}")
        if (musicStartHooks.isNotEmpty()) musicStartHooks.forEach { it(song) }
        musicState = STATE_PLAYING
    }


    private fun stopAmbient() {
        ambState = STATE_INTERMISSION
        intermissionAkku = 0f
        intermissionLength = 30f + 30f * Math.random().toFloat() // 30s-60s
        ambFired = false
        printdbg(this, "stopAmbient Intermission: $intermissionLength seconds")
    }

    private fun startAmbient(song: MusicContainer) {
        AudioMixer.startAmb(song)
        printdbg(this, "startAmbient Now playing: $song")
//        INGAME.sendNotification("Now Playing $EMDASH ${song.name}")
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
                        restockMUsicBin()
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
        stopMusic(AudioMixer.musicTrack.currentTrack)
        stopAmbient()
    }
}
