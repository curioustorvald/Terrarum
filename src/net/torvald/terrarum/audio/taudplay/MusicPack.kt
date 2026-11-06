package net.torvald.terrarum.audio.taudplay

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.TerrarumMusicPlaylist
import net.torvald.terrarum.audio.AudioBank
import kotlin.math.pow
import kotlin.random.Random

/**
 * A music pack: a directory of Taud songs, their lane sheets ([BgmLanes]) and a manifest, `_muspackmanifest.json`
 * (JSON with comments):
 *
 * - `manifest`: `format_version` (1), `name`, `pack_version`, `author`, `copyright`, `licence`;
 * - `global_dsp_request`: what the game does to every song of the pack ([DspRequest]): `volume_db`, extra volume in
 *   decibels, and `volume_amp`, extra volume as a multiplier (both may be given; they multiply). Put on the music
 *   track's PreGain while a song of the pack plays ([AudioBank.preGain]);
 * - `themes`: theme name → a list of songs, each a file name or a `[file, lane sheet]` pair;
 * - `probabilities`: a list of `{biome: [...], prefer: [songs], power: [...]}`: while the player is in any of the
 *   biomes, each preferred song is `power` times as likely to be picked (sizes of `prefer` and `power` equal).
 *
 * The themes the game knows: "titlescreen" (the title screen), and for the game itself "generic" and "dual" (any
 * surroundings), "cave" (underground) and "above" (in the open). See [pickWeight] for how a song is picked.
 *
 * Created by minjaesong on 2026-11-06.
 */
class MusicPack(
    val dir: FileHandle,
    val name: String,
    val formatVersion: Int,
    val packVersion: String?,
    val author: String?,
    val copyright: String?,
    val licence: String?,
    val themes: Map<String, List<Song>>,
    val preferences: List<Preference>,
    val dspRequest: DspRequest = DspRequest(),
) {
    /** `global_dsp_request`: applies to every song of the pack. */
    class DspRequest(val volumeDb: Double = 0.0, val volumeAmp: Double = 1.0) {
        /** The extra gain, as a multiplier: `volume_amp` × 10^(`volume_db`/20). */
        val gain: Double = volumeAmp * 10.0.pow(volumeDb / 20.0)
        override fun toString() = "DspRequest(volume_db=$volumeDb, volume_amp=$volumeAmp)"
    }

    /** A song of the pack: its file, and its lane sheet if it has one. */
    class Song(val file: String, val lanesFile: String?, val lanes: BgmLanes?) {
        val title: String get() = lanes?.title?.ifBlank { null } ?: fileToTitle(file)
        override fun toString() = file
    }

    class Preference(val biomes: Set<String>, val prefer: List<Pair<String, Double>>)

    /** canonical path with forward slashes: the playlists' `source` */
    val source: String = dir.path().replace('\\', '/')

    /** Every song of the pack once, in the order the themes name them. */
    val songs: List<Song> get() = themes.values.flatten().distinctBy { it.file }

    /** The themes a song belongs to. */
    fun themesOf(file: String) = themes.filterValues { list -> list.any { it.file == file } }.keys

    /**
     * How likely `song` is to be picked next, relative to the others of the playlist:
     * - a "cave" song × x and an "above" song × (1 − x), x the player's surroundings ([MusicEnvironment.enclosure]:
     *   0 = in the open, 1 = underground); a song of both, or of neither ("generic", "dual"), × 1;
     * - × every `power` the pack's `probabilities` give it for a biome the player is in ([MusicEnvironment.biomes]).
     */
    fun pickWeight(song: Song, x: Double = MusicEnvironment.enclosure, biomes: Set<String> = MusicEnvironment.biomes): Double {
        val t = themesOf(song.file)
        val x = x.coerceIn(0.0, 1.0)
        var w = when {
            "cave" in t && "above" !in t -> x
            "above" in t && "cave" !in t -> 1.0 - x
            else -> 1.0
        }
        preferences.forEach { p ->
            if (p.biomes.any { it in biomes }) p.prefer.forEach { (file, power) -> if (file == song.file) w *= power }
        }
        return w
    }

    /**
     * A playlist of the songs of `themeNames` (each song once), each a [TaudAudioBank] of its own. When it moves on,
     * it picks by [pickWeight] at that moment, never the song that just played while another can be picked.
     */
    fun playlist(
        themeNames: Collection<String>,
        playlistName: String = name,
        diskJockeyingMode: String = "intermittent",
        random: Random = Random.Default,
    ): TerrarumMusicPlaylist {
        val list = themeNames.flatMap { themes[it] ?: emptyList() }.distinctBy { it.file }
        val preGain = dspRequest.gain.toFloat()
        val banks: List<AudioBank> = list.map { TaudAudioBank(it.title, dir.child(it.file), it.lanes, preGain = preGain) }
        return TerrarumMusicPlaylist(banks, playlistName, source, diskJockeyingMode, true) { current ->
            val weights = DoubleArray(list.size) { pickWeight(list[it]) }
            if (current in weights.indices && weights.count { it > 0.0 } > 1) weights[current] = 0.0
            pick(weights, random, current)
        }
    }

    /** The title screen's playlist: the "titlescreen" theme, back to back (each playback a new draw). */
    fun titlePlaylist() = playlist(listOf(THEME_TITLE), "$name (title)", "continuous")

    /** The game's playlist: every theme but the title screen's. */
    fun ingamePlaylist() = playlist(themes.keys.filter { it != THEME_TITLE }, name, "intermittent")

    override fun toString() = "MusicPack($name ${packVersion ?: ""}, ${songs.size} songs, themes ${themes.keys})"

    companion object {
        const val MANIFEST = "_muspackmanifest.json"
        const val THEME_TITLE = "titlescreen"

        /** The game's own music: basegame's `audio/music/ingame`. Null (and the music silent) if it cannot be read. */
        val default: MusicPack? by lazy {
            try {
                load(ModMgr.getGdxFile("basegame", "audio/music/ingame"))
            }
            catch (e: Throwable) {
                e.printStackTrace()
                null
            }
        }

        fun fileToTitle(file: String) = file.substringAfterLast('/').substringBeforeLast('.').removeSuffix("_master")
            .replace('_', ' ')

        /** Read a pack from its directory: the manifest and every lane sheet it names. */
        fun load(dir: FileHandle): MusicPack {
            val json = JsonReader().parse(dir.child(MANIFEST).readString("UTF-8"))
            val sheets = HashMap<String, BgmLanes?>()

            fun sheet(file: String?): BgmLanes? = if (file == null) null else sheets.getOrPut(file) {
                try {
                    BgmLanes.parse(dir.child(file).readString("UTF-8"))
                }
                catch (e: Throwable) {
                    printdbg(this, "Lane sheet $file of ${dir.path()} unreadable, its song plays as written: $e")
                    null
                }
            }

            fun song(v: JsonValue): Song = when {
                v.isString -> Song(v.asString(), null, null)
                v.isArray && v.size >= 1 -> v.getString(0).let { file ->
                    val lanesFile = if (v.size >= 2) v.getString(1) else null
                    Song(file, lanesFile, sheet(lanesFile))
                }
                else -> throw IllegalArgumentException("A song must be a file name or a [file, lane sheet] pair: $v")
            }

            val meta = json.get("manifest")
            val formatVersion = meta?.getInt("format_version", 1) ?: 1
            if (formatVersion != 1) throw IllegalArgumentException("Music pack format $formatVersion is not 1")

            val themes = LinkedHashMap<String, List<Song>>()
            json.get("themes")?.forEach { theme -> themes[theme.name] = theme.map { song(it) } }

            val dsp = json.get("global_dsp_request")?.let { d ->
                d.forEach { if (it.name != "volume_db" && it.name != "volume_amp") printdbg(this, "global_dsp_request of ${dir.path()}: '${it.name}' is not known, ignored") }
                val db = d.getDouble("volume_db", 0.0)
                val amp = d.getDouble("volume_amp", 1.0)
                if (!db.isFinite() || !amp.isFinite() || amp < 0.0)
                    throw IllegalArgumentException("global_dsp_request: volume_db $db, volume_amp $amp")
                DspRequest(db, amp)
            } ?: DspRequest()

            val preferences = json.get("probabilities")?.map { p ->
                val prefer = p.get("prefer")?.map { song(it).file } ?: emptyList()
                val power = p.get("power")?.map { it.asDouble() } ?: emptyList()
                if (prefer.size != power.size)
                    throw IllegalArgumentException("probabilities: 'prefer' has ${prefer.size} songs but 'power' ${power.size} values")
                Preference(p.get("biome")?.map { it.asString() }?.toSet() ?: emptySet(), prefer.zip(power))
            } ?: emptyList()

            return MusicPack(
                dir,
                meta?.getString("name", null) ?: dir.name(),
                formatVersion,
                meta?.getString("pack_version", null),
                meta?.getString("author", null),
                meta?.getString("copyright", null) ?: meta?.getString("Copyright", null),
                meta?.getString("licence", null) ?: meta?.getString("license", null),
                themes,
                preferences,
                dsp,
            ).also { printdbg(this, "Music pack: $it, $dsp") }
        }

        /** An index by weight; uniformly among all but `avoid` when every weight is 0. */
        internal fun pick(weights: DoubleArray, random: Random, avoid: Int = -1): Int {
            val sum = weights.sum()
            if (sum <= 0.0) {
                if (weights.size <= 1) return 0
                val others = weights.indices.filter { it != avoid }
                return others[random.nextInt(others.size)]
            }
            var r = random.nextDouble() * sum
            weights.forEachIndexed { i, w ->
                r -= w
                if (r < 0.0 && w > 0.0) return i
            }
            return weights.indexOfLast { it > 0.0 }
        }
    }
}
