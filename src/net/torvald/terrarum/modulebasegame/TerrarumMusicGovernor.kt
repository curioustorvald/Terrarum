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
import net.torvald.reflection.forceInvoke
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.audio.AudioMixer
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack
import net.torvald.terrarum.audio.TerrarumAudioMixerTrack.Companion.SAMPLING_RATE
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

    fun reset() {
        samplesRead = 0L
        gdxMusic.forceInvoke<Int>("reset", arrayOf())
    }

    override fun equals(other: Any?) = this.file.path == (other as MusicContainer).file.path
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
    var playlistName = ""; private set
    /** canonicalPath with path separators converted to forward slash */
    var playlistSource = "" ; private set
    private var musicBin: ArrayList<MusicContainer> = ArrayList()
    private var shuffled = true
    private var diskJockeyingMode = "intermittent" // intermittent, continuous

    private fun registerSongsFromDir(musicDir: String, fileToName: ((String) -> String)?) {
        val musicDir = musicDir.replace('\\', '/')
        playlistSource = musicDir
        printdbg(this, "registerSongsFromDir $musicDir")

        val fileToName = if (fileToName == null) {
            { name: String -> name.substringBeforeLast('.').replace('_', ' ').split(" ").map { it.capitalize() }.joinToString(" ") }
        }
        else fileToName

        playlistName = musicDir.substringAfterLast('/')

        songs = File(musicDir).listFiles()?.sortedBy { it.name }?.mapNotNull {
            printdbg(this, "Music: ${it.absolutePath}")
            try {
                MusicContainer(
                    fileToName(it.name),
                    it,
                    Gdx.audio.newMusic(Gdx.files.absolute(it.absolutePath))
                ).also { muscon ->

                    printdbg(this, "MusicTitle: ${muscon.name}")

                    muscon.songFinishedHook =  { stopMusic(muscon) }
                }
            }
            catch (e: GdxRuntimeException) {
                e.printStackTrace()
                null
            }
        } ?: emptyList() // TODO test code
    }

    private fun restockMusicBin() {
        musicBin = ArrayList(if (shuffled) songs.shuffled() else songs.slice(songs.indices))
    }

    /**
     * @param musicDir where the music files are. Absolute path.
     * @param shuffled if the tracks are to be shuffled
     * @param diskJockeyingMode `intermittent` to give random gap between tracks, `continuous` for continuous playback
     */
    fun queueDirectory(musicDir: String, shuffled: Boolean, diskJockeyingMode: String, fileToName: ((String) -> String)? = null) {
        if (musicState != STATE_INIT && musicState != STATE_INTERMISSION) {
            App.audioMixer.requestFadeOut(App.audioMixer.fadeBus, AudioMixer.DEFAULT_FADEOUT_LEN) // explicit call for fade-out when the game instance quits
            stopMusic(App.audioMixer.musicTrack.currentTrack)
        }

        songs.forEach { it.gdxMusic.tryDispose() }
        registerSongsFromDir(musicDir, fileToName)

        this.shuffled = shuffled
        this.diskJockeyingMode = diskJockeyingMode

        restockMusicBin()
    }

    /**
     * Adds a song to the head of the internal playlist (`musicBin`)
     */
    fun queueMusicToPlayNext(music: MusicContainer) {
        musicBin.add(0, music)
    }

    /**
     * Unshifts an internal playlist (`musicBin`). The `music` argument must be the song that exists on the `songs`.
     */
    fun unshiftPlaylist(music: MusicContainer) {
        val indexAtMusicBin = songs.indexOf(music)
        if (indexAtMusicBin < 0) throw IllegalArgumentException("The music does not exist on the internal songs list ($music)")

        // rewrite musicBin
        val newMusicBin = if (shuffled) songs.shuffled().toTypedArray().also {
            // if shuffled,
            // 1. create a shuffled version of songlist
            // 2. swap two songs such that the songs[indexAtMusicBin] comes first
            val swapTo = it.indexOf(songs[indexAtMusicBin])
            val tmp = it[swapTo]
            it[swapTo] = it[0]
            it[0] = tmp
        }
        else Array(songs.size - indexAtMusicBin) { offset ->
            val k = offset + indexAtMusicBin
            songs[k]
        }

        musicBin = ArrayList(newMusicBin.toList())
    }

    fun queueIndexFromPlaylist(indexAtMusicBin: Int) {
        if (indexAtMusicBin !in songs.indices) throw IndexOutOfBoundsException("The index is outside of the internal songs list ($indexAtMusicBin/${songs.size})")

        // rewrite musicBin
        val newMusicBin =  if (shuffled) songs.shuffled().toTypedArray().also {
            // if shuffled,
            // 1. create a shuffled version of songlist
            // 2. swap two songs such that the songs[indexAtMusicBin] comes first
            val swapTo = it.indexOf(songs[indexAtMusicBin])
            val tmp = it[swapTo]
            it[swapTo] = it[0]
            it[0] = tmp
        }
        else Array(songs.size - indexAtMusicBin) { offset ->
            val k = offset + indexAtMusicBin
            songs[k]
        }

        musicBin = ArrayList(newMusicBin.toList())
    }

    private val ambients: HashMap<String, HashSet<MusicContainer>> =
        HashMap(Terrarum.audioCodex.audio.filter { it.key.startsWith("ambient.") }.map { it.key to it.value.mapNotNull { fileHandle ->
            try {
                MusicContainer(
                    fileHandle.nameWithoutExtension().replace('_', ' ').split(" ").map { it.capitalize() }.joinToString(" "),
                    fileHandle.file(),
                    Gdx.audio.newMusic(fileHandle).also {
                        it.isLooping = true
                    }
                ) { stopAmbient() }
            }
            catch (e: GdxRuntimeException) {
                e.printStackTrace()
                null
            }
        }.toHashSet() }.toMap())

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
        ambients.forEach { (k, v) ->
            printdbg(this, "Ambients: $k -> $v")

            v.forEach {
                App.disposables.add(it.gdxMusic)
            }
        }
    }


    private var warningPrinted = false



    protected var ambState = 0
    protected var ambFired = false

    private fun stopMusic(song: MusicContainer?, callStopMusicHook: Boolean = true) {
        if (intermissionLength < Float.POSITIVE_INFINITY) {
            musicState = STATE_INTERMISSION
            intermissionAkku = 0f
            intermissionLength =
                if (diskJockeyingMode == "intermittent") 30f + 30f * Math.random().toFloat() else 0f // 30s-60s
            musicFired = false
            if (callStopMusicHook && musicStopHooks.isNotEmpty()) musicStopHooks.forEach {
                if (song != null) {
                    it(song)
                }
            }
            printdbg(this, "StopMusic Intermission: $intermissionLength seconds")
        }
    }

    fun stopMusic(callStopMusicHook: Boolean = true, pauseLen: Float = Float.POSITIVE_INFINITY) {
        stopMusic(App.audioMixer.musicTrack.currentTrack, callStopMusicHook)
        intermissionLength = pauseLen
//        printdbg(this, "StopMusic Intermission2: $intermissionLength seconds")
    }

    fun startMusic() {
        startMusic(pullNextMusicTrack())
    }

    private fun startMusic(song: MusicContainer) {
        App.audioMixer.startMusic(song)
        printdbg(this, "startMusic Now playing: ${song.name}")
//        INGAME.sendNotification("Now Playing $EMDASH ${song.name}")
        if (musicStartHooks.isNotEmpty()) musicStartHooks.forEach { it(song) }
        musicState = STATE_PLAYING
        intermissionLength = 42.42424f
    }

    // MixerTrackProcessor will call this function externally to make gapless playback work
    fun pullNextMusicTrack(callNextMusicHook: Boolean = false): MusicContainer {
//        printStackTrace(this)

        // prevent same song to play twice in row (for the most time)
        if (musicBin.isEmpty()) {
            restockMusicBin()
        }
        return musicBin.removeAt(0).also { mus ->
            if (callNextMusicHook && musicStartHooks.isNotEmpty()) musicStartHooks.forEach { it(mus) }
        }
    }

    private fun stopAmbient() {
//        if (::currentAmbientTrack.isInitialized)
//            App.audioMixer.ambientTrack.nextTrack = currentAmbientTrack
    }

    private fun startAmbient(song: MusicContainer) {
        currentAmbientTrack = song
        App.audioMixer.startAmb(song)
        printdbg(this, "startAmbient Now playing: $song")
//        INGAME.sendNotification("Now Playing $EMDASH ${song.name}")
        ambState = STATE_PLAYING
    }

    private lateinit var currentAmbientTrack: MusicContainer

    override fun update(ingame: IngameInstance, delta: Float) {
        // start the song queueing if there is one to play
        if (songs.isNotEmpty() && musicState == 0) musicState = STATE_INTERMISSION
        if (ambients.isNotEmpty() && ambState == 0) ambState = STATE_INTERMISSION


        when (musicState) {
            STATE_FIREPLAY -> {
                if (!musicFired) {
                    musicFired = true
                    startMusic(pullNextMusicTrack())
                }
            }
            STATE_PLAYING -> {
                // stopMusic() will be called when the music finishes; it's on the setOnCompletionListener
            }
            STATE_INTERMISSION -> {
                intermissionAkku += delta

                if (intermissionAkku >= intermissionLength && songs.isNotEmpty()) {
                    intermissionAkku = 0f
                    musicState = STATE_FIREPLAY
                }
            }
        }

        when (ambState) {
            STATE_FIREPLAY -> {
                if (!ambFired) {
                    ambFired = true

                    val season = ingame.world.worldTime.ecologicalSeason
                    val seasonName = when (season) {
                        in 0f..2f -> "autumn"
                        in 2f..3f -> "summer"
                        in 3f..5f -> "autumn"
                        else -> "winter"
                    }

                    val track = ambients["ambient.season.$seasonName"]!!.random()
                    startAmbient(track)
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
        App.audioMixer.requestFadeOut(App.audioMixer.fadeBus, AudioMixer.DEFAULT_FADEOUT_LEN) // explicit call for fade-out when the game instance quits
        stopMusic(App.audioMixer.musicTrack.currentTrack)
        stopAmbient()
    }
}
