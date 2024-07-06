package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.utils.GdxRuntimeException
import com.jme3.math.FastMath
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.audio.audiobank.MusicContainer

import net.torvald.terrarum.gameworld.WorldTime.Companion.DAY_LENGTH

class TerrarumMusicAndAmbientStreamer : MusicStreamer() {
    private val STATE_INIT = 0
    private val STATE_FIREPLAY = 1
    private val STATE_PLAYING = 2
    private val STATE_INTERMISSION = 3


    init {
    }

    private var playlist: List<MusicContainer> = emptyList()
    var playlistName = ""; private set
    /** canonicalPath with path separators converted to forward slash */
    var playlistSource = "" ; private set
    private var musicBin: ArrayList<MusicContainer> = ArrayList()
    private var shuffled = true
    private var diskJockeyingMode = "intermittent" // intermittent, continuous



    private fun restockMusicBin() {
        musicBin = ArrayList(if (shuffled) playlist.shuffled() else playlist.slice(playlist.indices))
    }

    /**
     * Adds a song to the head of the internal playlist (`musicBin`)
     */
    fun xxxqueueMusicToPlayNext(music: MusicContainer) {
        musicBin.add(0, music)
    }

    /**
     * Unshifts an internal playlist (`musicBin`). The `music` argument must be the song that exists on the `songs`.
     */
    fun xxxunshiftPlaylist(music: MusicContainer) {
        val indexAtMusicBin = playlist.indexOf(music)
        if (indexAtMusicBin < 0) throw IllegalArgumentException("The music does not exist on the internal songs list ($music)")

        // rewrite musicBin
        val newMusicBin = if (shuffled) playlist.shuffled().toTypedArray().also {
            // if shuffled,
            // 1. create a shuffled version of songlist
            // 2. swap two songs such that the songs[indexAtMusicBin] comes first
            val swapTo = it.indexOf(playlist[indexAtMusicBin])
            val tmp = it[swapTo]
            it[swapTo] = it[0]
            it[0] = tmp
        }
        else Array(playlist.size - indexAtMusicBin) { offset ->
            val k = offset + indexAtMusicBin
            playlist[k]
        }

        musicBin = ArrayList(newMusicBin.toList())
    }

    fun xxxqueueIndexFromPlaylist(indexAtMusicBin: Int) {
        if (indexAtMusicBin !in playlist.indices) throw IndexOutOfBoundsException("The index is outside of the internal songs list ($indexAtMusicBin/${playlist.size})")

        // rewrite musicBin
        val newMusicBin =  if (shuffled) playlist.shuffled().toTypedArray().also {
            // if shuffled,
            // 1. create a shuffled version of songlist
            // 2. swap two songs such that the songs[indexAtMusicBin] comes first
            val swapTo = it.indexOf(playlist[indexAtMusicBin])
            val tmp = it[swapTo]
            it[swapTo] = it[0]
            it[0] = tmp
        }
        else Array(playlist.size - indexAtMusicBin) { offset ->
            val k = offset + indexAtMusicBin
            playlist[k]
        }

        musicBin = ArrayList(newMusicBin.toList())
    }

    private val ambients: HashMap<String, HashSet<MusicContainer>> =
        HashMap(Terrarum.audioCodex.audio.filter { it.key.startsWith("ambient.") }.map { it.key to it.value.mapNotNull { fileHandle ->
            try {
                MusicContainer(
                    fileHandle.nameWithoutExtension().replace('_', ' ').split(" ").map { it.capitalize() }.joinToString(" "),
                    fileHandle.file(),
                    looping = true,
                )
            }
            catch (e: GdxRuntimeException) {
                e.printStackTrace()
                null
            }
        }.toHashSet() }.toMap())

    private val musicStartHooks = ArrayList<(MusicContainer) -> Unit>()
    private val musicStopHooks = ArrayList<(MusicContainer) -> Unit>()

    init {
        // TODO queue and play the default playlist
        // TerrarumMusicPlaylist.fromDirectory(App.defaultMusicDir, true, "intermittent")
    }


    fun addMusicStartHook(f: (MusicContainer) -> Unit) {
        musicStartHooks.add(f)
    }

    fun addMusicStopHook(f: (MusicContainer) -> Unit) {
        musicStopHooks.add(f)
    }

    init {
        playlist.forEach {
            App.disposables.add(it)
        }
        ambients.forEach { (k, v) ->
            printdbg(this, "Ambients: $k -> $v")

            v.forEach {
                App.disposables.add(it)
            }
        }
    }


    private var warningPrinted = false



    protected var ambState = 0
    protected var ambFired = false

    fun getRandomMusicInterval() = MusicService.getRandomMusicInterval()

    // call MusicService to fade out
    // pauses playlist update
    // called by MusicPlayerControl
    fun stopMusicPlayback() {

    }

    // resumes playlist update
    // called by MusicPlayerControl
    fun resumeMusicPlayback() {

    }

    private fun stopAmbient() {
//        if (::currentAmbientTrack.isInitialized)
//            App.audioMixer.ambientTrack.nextTrack = currentAmbientTrack
    }

    private fun startAmbient1(song: MusicContainer) {
        App.audioMixer.startAmb1(song)
        printdbg(this, "startAmbient1 Now playing: $song")
        ambState = STATE_PLAYING
    }
    private fun startAmbient2(song: MusicContainer) {
        App.audioMixer.startAmb2(song)
        printdbg(this, "startAmbient2 Now playing: $song")
        ambState = STATE_PLAYING
    }

    private fun queueAmbientForce1(song: MusicContainer) {
        App.audioMixer.ambientTrack1.let {
            it.nextTrack = song
            it.stop()
        }
        printdbg(this, "startAmbient1 Now playing: $song")
        ambState = STATE_PLAYING
    }
    private fun queueAmbientForce2(song: MusicContainer) {
        App.audioMixer.ambientTrack2.let {
            it.nextTrack = song
            it.stop()
        }
        printdbg(this, "startAmbient2 Now playing: $song")
        ambState = STATE_PLAYING
    }

    var firstTime = true

    override fun update(ingame: IngameInstance, delta: Float) {
        val timeNow = System.currentTimeMillis()

        // start the song queueing if there is one to play
        if (firstTime) {
            firstTime = false
            if (ambients.isNotEmpty()) ambState = STATE_INTERMISSION
        }

        val season = ingame.world.worldTime.ecologicalSeason
        val isAM = (ingame.world.worldTime.todaySeconds < DAY_LENGTH / 2) // 0 until DAY_LENGTH (86400)
        val solarElevDeg = ingame.world.worldTime.solarElevationDeg
        val isSunUp = (solarElevDeg >= 0)
        val seasonName = when (season) {
            in 0f..2f -> "autumn"
            in 2f..3f -> "summer"
            in 3f..5f -> "autumn"
            else -> "winter"
        }

        when (ambState) {
            STATE_FIREPLAY -> {
                if (!ambFired) {
                    ambFired = true

                    // ambient track 1: diurnal/nocturnal
                    // ambient track 2: crepuscular/matutinal
                    val track1 = if (isSunUp)
                        ambients["ambient.season.diurnal_$seasonName"]!!.random() // mad respect to Klankbeeld
                    else
                        ambients["ambient.season.nocturnal"]!!.random() // as it turns out ambient recordings of a wild place AT NIGHT is quite rare

                    val track2 = if (isAM)
                        ambients["ambient.season.matutinal"]!!.random()
                    else
                        ambients["ambient.season.crepuscular"]!!.random()

                    startAmbient1(track1)
                    startAmbient2(track2)
                }
            }
            STATE_PLAYING -> {
                // mix ambient tracks

                // queue up nocturnal
                if (!isSunUp && oldWorldSolarElev >= 0)
                    queueAmbientForce1(ambients["ambient.season.nocturnal"]!!.random()) // as it turns out ambient recordings of a wild place AT NIGHT is quite rare
                // queue up diurnal
                else if (isSunUp && oldWorldSolarElev < 0)
                    queueAmbientForce1(ambients["ambient.season.diurnal_$seasonName"]!!.random()) // mad respect to Klankbeeld

                // queue up crepuscular
                if (!isAM && oldWorldTime < DAY_LENGTH / 2)
                    queueAmbientForce2(ambients["ambient.season.crepuscular"]!!.random())
                else if (isAM && oldWorldTime >= DAY_LENGTH / 2)
                    queueAmbientForce2(ambients["ambient.season.matutinal"]!!.random())

                // play around the fader
                val track2vol = if (isAM)
                    when (solarElevDeg) {
                        in TRACK2_DAWN_ELEV_UP_MIN..TRACK2_DAWN_ELEV_UP_MAX ->
                            FastMath.interpolateLinear(
                                (solarElevDeg - TRACK2_DAWN_ELEV_UP_MIN) / (TRACK2_DAWN_ELEV_UP_MAX - TRACK2_DAWN_ELEV_UP_MIN),
                                1.0, 0.0
                            )
                        in TRACK2_DAWN_ELEV_DN_MAX..TRACK2_DAWN_ELEV_DN_MIN ->
                            FastMath.interpolateLinear(
                                (solarElevDeg - TRACK2_DAWN_ELEV_DN_MIN) / (TRACK2_DAWN_ELEV_DN_MAX - TRACK2_DAWN_ELEV_DN_MIN),
                                1.0, 0.0
                            )
                        in TRACK2_DAWN_ELEV_DN_MIN..TRACK2_DAWN_ELEV_UP_MIN -> 1.0
                        else -> 0.0
                    }
                else 
                    when (solarElevDeg) {
                        in TRACK2_DUSK_ELEV_UP_MIN..TRACK2_DUSK_ELEV_UP_MAX ->
                            FastMath.interpolateLinear(
                                (solarElevDeg - TRACK2_DUSK_ELEV_UP_MIN) / (TRACK2_DUSK_ELEV_UP_MAX - TRACK2_DUSK_ELEV_UP_MIN),
                                1.0, 0.0
                            )
                        in TRACK2_DUSK_ELEV_DN_MAX..TRACK2_DUSK_ELEV_DN_MIN ->
                            FastMath.interpolateLinear(
                                (solarElevDeg - TRACK2_DUSK_ELEV_DN_MIN) / (TRACK2_DUSK_ELEV_DN_MAX - TRACK2_DUSK_ELEV_DN_MIN),
                                1.0, 0.0
                            )
                        in TRACK2_DUSK_ELEV_DN_MIN..TRACK2_DUSK_ELEV_UP_MIN -> 1.0
                        else -> 0.0
                    }
                val track1vol = 1.0 - track2vol

                App.audioMixer.ambientTrack1.volume = track1vol
                App.audioMixer.ambientTrack2.volume = track2vol
            }
            STATE_INTERMISSION -> {
                ambState = STATE_FIREPLAY
            }
        }

        oldWorldSolarElev = solarElevDeg
        oldWorldTime = ingame.world.worldTime.todaySeconds
    }

    private var oldWorldSolarElev = Terrarum.ingame?.world?.worldTime?.solarElevationDeg ?: 0.0
    private var oldWorldTime = Terrarum.ingame?.world?.worldTime?.todaySeconds ?: 0

    private val TRACK2_DUSK_ELEV_UP_MAX = 15.0
    private val TRACK2_DUSK_ELEV_UP_MIN = 0.1
    private val TRACK2_DUSK_ELEV_DN_MIN = -0.1
    private val TRACK2_DUSK_ELEV_DN_MAX = -15.0

    private val TRACK2_DAWN_ELEV_UP_MAX = 20.0
    private val TRACK2_DAWN_ELEV_UP_MIN = 5.0
    private val TRACK2_DAWN_ELEV_DN_MIN = 5.0
    private val TRACK2_DAWN_ELEV_DN_MAX = -10.0

    override fun dispose() {
        stopAmbient()
    }
}
