package net.torvald.terrarum.audio.taudplay

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue

/**
 * A song's lane sheet, `<Song>_lanes.json` (format `terrarum-bgm-lanes 1`): what each lane of a Taud song does,
 * and how the game may set the song's faders (one per lane; taudplay's `setVoiceGain`).
 *
 * The four kinds:
 * - `generic`: plays as written everywhere; `hints_db` may lean it towards its surroundings.
 * - `biome`: written for one surrounding (`biome`), kept playing in the other; `hints_db` as above.
 * - `dual`: `groups` of lanes crossfaded by the player's surroundings (`crossfade`); the faders ARE the mix.
 * - `combinatorial`: at every playback start, a draw from `slots` decides which lanes play (`selection`).
 *
 * Only what steers the faders is kept as data; the prose (`about`, the roles' descriptions, the lanes' notes) is
 * kept as text for a player UI.
 *
 * Created by minjaesong on 2026-11-06.
 */
class BgmLanes(
    val title: String,
    /** the song file the sheet was written for, as the sheet names it (informative: the music pack names the file) */
    val file: String?,
    val kind: Kind,
    /** `biome` kind: the surrounding the piece was written for ("open" or "cave") */
    val biome: String?,
    val about: String?,
    /** role → what it means in this song */
    val roles: Map<String, String>,
    val lanes: List<Lane>,
    /** `dual` kind: group name → its lanes */
    val groups: Map<String, Group>,
    val crossfade: Crossfade?,
    /** surrounding ("open", "cave") → role → decibels */
    val hintsDb: Map<String, Map<String, Double>>,
    val selection: Selection?,
    val slots: List<Slot>,
) {
    enum class Kind { GENERIC, BIOME, DUAL, COMBINATORIAL }

    class Lane(
        val index: Int,
        val id: String,
        val label: String,
        val role: String?,
        val group: String?,
        val slot: String?,
        val option: String?,
        val note: String?,
    )

    class Group(val name: String, val label: String, val lanes: IntArray)

    /**
     * @param law "equal-power" (open = cos(x·π/2), cave = sin(x·π/2)) or "linear" (open = 1 − x, cave = x); a group
     * named neither "open" nor "cave" (e.g. "core") always stays at 1
     */
    class Crossfade(val law: String, val fadeMs: Double)

    class Selection(val fadeMs: Double)

    class Slot(
        val id: String,
        val label: String,
        val role: String?,
        /** how many options it plays (k) → the weight of k */
        val count: List<Pair<Int, Double>>,
        /** k → the gain of every lane of a drawn option; absent = 1 */
        val gain: Map<Int, Double>,
        val options: List<Option>,
    )

    class Option(val id: String, val label: String, val lanes: IntArray, val weight: Double)

    /** The lanes of a role, in sheet order. */
    fun lanesOfRole(role: String) = lanes.filter { it.role == role }.map { it.index }

    override fun toString() = "BgmLanes($title, ${kind.name.lowercase()}, ${lanes.size} lanes)"

    companion object {
        const val FORMAT = "terrarum-bgm-lanes 1"

        fun parse(text: String): BgmLanes = parse(JsonReader().parse(text))

        fun parse(json: JsonValue): BgmLanes {
            val format = json.getString("format", null)
                ?: throw IllegalArgumentException("Not a lane sheet: no 'format'")
            if (format != FORMAT)
                throw IllegalArgumentException("Lane sheet format '$format' is not '$FORMAT'")

            val kind = when (val k = json.getString("kind", "generic")) {
                "generic" -> Kind.GENERIC
                "biome" -> Kind.BIOME
                "dual" -> Kind.DUAL
                "combinatorial" -> Kind.COMBINATORIAL
                else -> throw IllegalArgumentException("Unknown lane sheet kind '$k'")
            }

            val lanes = json.get("lanes")?.map { l ->
                Lane(
                    l.getInt("index"),
                    l.getString("id", "${l.getInt("index")}"),
                    l.getString("label", l.getString("id", "")),
                    l.getString("role", null),
                    l.getString("group", null),
                    l.getString("slot", null),
                    l.getString("option", null),
                    l.getString("note", null),
                )
            } ?: emptyList()

            val groups = LinkedHashMap<String, Group>()
            json.get("groups")?.forEach { g ->
                groups[g.name] = Group(g.name, g.getString("label", g.name), g.get("lanes")?.asIntArray() ?: IntArray(0))
            }
            // a lane that names its group but is missing from the group's list still belongs to it
            lanes.filter { it.group != null && groups[it.group]?.lanes?.contains(it.index) == false }.forEach { l ->
                groups[l.group!!] = groups[l.group]!!.let { Group(it.name, it.label, it.lanes + l.index) }
            }
            lanes.filter { it.group != null && !groups.containsKey(it.group) }.groupBy { it.group!! }.forEach { (name, ls) ->
                groups[name] = Group(name, name, ls.map { it.index }.toIntArray())
            }

            val crossfade = json.get("crossfade")?.let {
                Crossfade(it.getString("law", "equal-power"), it.getDouble("fade_ms", 1500.0))
            }

            val hints = LinkedHashMap<String, Map<String, Double>>()
            json.get("hints_db")?.forEach { env ->
                hints[env.name] = LinkedHashMap<String, Double>().also { m -> env.forEach { m[it.name] = it.asDouble() } }
            }

            val selection = json.get("selection")?.let { Selection(it.getDouble("fade_ms", 0.0)) }

            val slots = json.get("slots")?.map { s ->
                Slot(
                    s.getString("id"),
                    s.getString("label", s.getString("id")),
                    s.getString("role", null),
                    s.get("count")?.map { it.name.toInt() to it.asDouble() } ?: listOf(1 to 1.0),
                    LinkedHashMap<Int, Double>().also { m -> s.get("gain")?.forEach { m[it.name.toInt()] = it.asDouble() } },
                    s.get("options")?.map { o ->
                        Option(
                            o.getString("id"),
                            o.getString("label", o.getString("id")),
                            o.get("lanes")?.asIntArray() ?: IntArray(0),
                            o.getDouble("weight", 1.0),
                        )
                    } ?: emptyList(),
                )
            } ?: emptyList()

            if (kind == Kind.COMBINATORIAL && slots.isEmpty())
                throw IllegalArgumentException("A combinatorial lane sheet needs 'slots'")

            return BgmLanes(
                json.getString("title", ""),
                json.getString("file", null),
                kind,
                json.getString("biome", null),
                json.getString("about", null),
                LinkedHashMap<String, String>().also { m -> json.get("roles")?.forEach { m[it.name] = it.asString() } },
                lanes,
                groups,
                crossfade,
                hints,
                selection,
                slots,
            )
        }
    }
}
