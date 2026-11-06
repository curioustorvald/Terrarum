package net.torvald.terrarum.audio.taudplay

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sets a Taud song's faders (one per lane) as its lane sheet says, from one number: x, the player's surroundings,
 * 0 = in the open … 1 = enclosed in rock (the balance the game sets between the open and the cave convolvers;
 * see [MusicEnvironment]).
 *
 * A lane's gain is the product of
 * - its group's crossfade gain (`dual`): "open" = cos(x·π/2) and "cave" = sin(x·π/2) under the equal-power law
 *   (1 − x and x under "linear"); any other group ("core") 1;
 * - its role's hint (`hints_db`), in decibels, interpolated between the "open" hint at x = 0 and the "cave" hint
 *   at x = 1;
 * - for a `combinatorial` song, the draw made by [start]: `gain[k]` for every lane of a drawn option, 0 for every
 *   lane no drawn option names.
 *
 * Pure: no player, no clock. [TaudAudioBank] owns the smoothing of x and the timing.
 *
 * @param channelCount the song's lane count (32 or 64): the length of every gain array this returns
 *
 * Created by minjaesong on 2026-11-06.
 */
class LaneDirector(val sheet: BgmLanes, val channelCount: Int, private val random: Random = Random.Default) {

    /** Whether the gains change with x while the song plays (a combinatorial song's never do). */
    val followsSurroundings: Boolean =
        sheet.kind != BgmLanes.Kind.COMBINATORIAL &&
        (sheet.kind == BgmLanes.Kind.DUAL && sheet.groups.keys.any { it == "open" || it == "cave" } || sheet.hintsDb.isNotEmpty())

    /** How long a change of x takes to fade in. */
    val fadeSeconds: Double = when {
        sheet.kind == BgmLanes.Kind.COMBINATORIAL -> (sheet.selection?.fadeMs ?: 0.0) / 1000.0
        else -> (sheet.crossfade?.fadeMs ?: DEFAULT_FADE_MS) / 1000.0
    }

    /** The draw of a combinatorial song: per lane, its gain (1 for every lane of any other kind). */
    private val drawGain = DoubleArray(channelCount) { 1.0 }

    /** The last draw, slot id → the ids of the options drawn (empty for any other kind). */
    var lastDraw: Map<String, List<String>> = emptyMap(); private set

    private val laneRole = arrayOfNulls<String>(channelCount).also { roles ->
        sheet.lanes.forEach { if (it.index in 0 until channelCount) roles[it.index] = it.role }
    }
    private val laneGroup = arrayOfNulls<String>(channelCount).also { groups ->
        sheet.groups.values.forEach { g -> g.lanes.forEach { if (it in 0 until channelCount) groups[it] = g.name } }
    }

    /** Gains for a playback start at surroundings `x`: makes a new draw for a combinatorial song. */
    fun start(x: Double): DoubleArray {
        if (sheet.kind == BgmLanes.Kind.COMBINATORIAL) draw()
        return gainsAt(x)
    }

    /** Every lane's gain at surroundings `x` (the current draw kept). */
    fun gainsAt(x: Double): DoubleArray {
        val x = x.coerceIn(0.0, 1.0)
        val dual = sheet.kind == BgmLanes.Kind.DUAL
        val linear = sheet.crossfade?.law == "linear"
        val open = sheet.hintsDb["open"]
        val cave = sheet.hintsDb["cave"]
        return DoubleArray(channelCount) { lane ->
            var g = drawGain[lane]
            if (dual) {
                g *= when (laneGroup[lane]) {
                    "open" -> if (linear) 1.0 - x else cos(x * PI / 2)
                    "cave" -> if (linear) x else sin(x * PI / 2)
                    else -> 1.0
                }
            }
            val role = laneRole[lane]
            if (role != null && (open != null || cave != null)) {
                val db = (1.0 - x) * (open?.get(role) ?: 0.0) + x * (cave?.get(role) ?: 0.0)
                if (db != 0.0) g *= 10.0.pow(db / 20.0)
            }
            g.coerceIn(0.0, 1.0)
        }
    }

    /** The draw (`selection.rule`): for each slot in order, k from `count` by weight, then k different options
     * by weight without replacement; every lane of a drawn option to `gain[k]` (1 without one), every other lane of
     * the song to 0. */
    private fun draw() {
        drawGain.fill(0.0)
        val made = LinkedHashMap<String, List<String>>()
        for (slot in sheet.slots) {
            if (slot.count.isEmpty()) continue
            val k = slot.count[pick(slot.count.map { it.second })].first
            val gain = slot.gain[k] ?: 1.0
            val pool = slot.options.toMutableList()
            val drawn = ArrayList<String>()
            repeat(minOf(k, pool.size)) {
                val option = pool.removeAt(pick(pool.map { it.weight }))
                option.lanes.forEach { if (it in 0 until channelCount) drawGain[it] = gain }
                drawn.add(option.id)
            }
            made[slot.id] = drawn
        }
        lastDraw = made
    }

    /** An index by weight: an item's chance is its weight over the sum of the weights. */
    private fun pick(weights: List<Double>): Int {
        val sum = weights.sumOf { maxOf(it, 0.0) }
        if (sum <= 0.0) return random.nextInt(weights.size)
        var r = random.nextDouble() * sum
        weights.forEachIndexed { i, w ->
            r -= maxOf(w, 0.0)
            if (r < 0.0) return i
        }
        return weights.indexOfLast { it > 0.0 }
    }

    companion object {
        const val DEFAULT_FADE_MS = 1500.0
    }
}
