package net.torvald.terrarum.mapdrawer

import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.Luminous
import net.torvald.terrarum.gamemap.WorldTime
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.tileproperties.TilePropCodex
import com.jme3.math.FastMath
import net.torvald.terrarum.tileproperties.TileNameCode
import org.newdawn.slick.Color
import org.newdawn.slick.Graphics
import java.util.*

/**
 * Created by minjaesong on 16-01-25.
 */

object LightmapRenderer {
    val overscan_open: Int = Math.min(32, 256f.div(TilePropCodex.getProp(TileNameCode.AIR).opacity and 0xFF).toFloat().ceil())
    val overscan_opaque: Int = Math.min(8, 256f.div(TilePropCodex.getProp(TileNameCode.STONE).opacity and 0xFF).toFloat().ceil())

    private val LIGHTMAP_WIDTH = Terrarum.game.ZOOM_MIN.inv().times(Terrarum.WIDTH)
            .div(MapDrawer.TILE_SIZE).ceil() + overscan_open * 2 + 3
    private val LIGHTMAP_HEIGHT = Terrarum.game.ZOOM_MIN.inv().times(Terrarum.HEIGHT)
            .div(MapDrawer.TILE_SIZE).ceil() + overscan_open * 2 + 3

    /**
     * 8-Bit RGB values
     */
    //private val lightmap: Array<IntArray> = Array(Terrarum.game.map.height) { IntArray(Terrarum.game.map.width) }
    private val lightmap: Array<IntArray> = Array(LIGHTMAP_HEIGHT) { IntArray(LIGHTMAP_WIDTH) }

    private val AIR = TileNameCode.AIR

    private val OFFSET_R = 2
    private val OFFSET_G = 1
    private val OFFSET_B = 0

    private const val TSIZE = MapDrawer.TILE_SIZE

    // color model related constants
    const val MUL = 1024 // modify this to 1024 to implement 30-bit RGB
    const val MUL_2 = MUL * MUL
    const val CHANNEL_MAX = MUL - 1
    const val CHANNEL_MAX_FLOAT = CHANNEL_MAX.toFloat()
    const val COLOUR_RANGE_SIZE = MUL * MUL_2

    internal var for_x_start: Int = 0
    internal var for_y_start: Int = 0
    internal var for_x_end: Int = 0
    internal var for_y_end: Int = 0

    fun getLight(x: Int, y: Int): Int? {
        /*if (x !in 0..Terrarum.game.map.width - 1 || y !in 0..Terrarum.game.map.height - 1)
                // if out of range then
                null
            else
                lightmap[y][x]*/
        try {
            return lightmap[y - for_y_start + overscan_open][x - for_x_start + overscan_open]
        }
        catch (e: ArrayIndexOutOfBoundsException) {
            return null
        }
    }

    fun setLight(x: Int, y: Int, colour: Int) {
        //lightmap[y][x] = colour
        try {
            lightmap[y - for_y_start + overscan_open][x - for_x_start + overscan_open] = colour
        }
        catch (e: ArrayIndexOutOfBoundsException) {
        }
    }

    fun renderLightMap() {
        for_x_start = MapCamera.cameraX / TSIZE - 1 // fix for premature lightmap rendering
        for_y_start = MapCamera.cameraY / TSIZE - 1 // on topmost/leftmost side

        for_x_end = for_x_start + MapCamera.getRenderWidth() / TSIZE + 3
        for_y_end = for_y_start + MapCamera.getRenderHeight() / TSIZE + 2 // same fix as above

        /**
         * * true: overscanning is limited to 8 tiles in width (overscan_opaque)
         * * false: overscanning will fully applied to 32 tiles in width (overscan_open)
         */
        /*val rect_width = for_x_end - for_x_start
        val rect_height_rem_hbars = for_y_end - for_y_start - 2
        val noop_mask = BitSet(2 * (rect_width) +
                               2 * (rect_height_rem_hbars))
        val rect_size = noop_mask.size()

        // get No-op mask
        fun edgeToMaskNum(i: Int): Pair<Int, Int> {
            if (i > rect_size) throw IllegalArgumentException()

            if (i < rect_width) // top edge horizontal
                return Pair(for_x_start + i, for_y_start)
            else if (i >= rect_size - rect_width) // bottom edge horizontal
                return Pair(
                        for_x_start + i.minus(rect_size - rect_width),
                        for_y_end
                )
            else { // vertical edges without horizontal edge pair
                return Pair(
                        if ((rect_width.even() && i.even()) || (rect_width.odd() && i.odd()))
                            // if the index is on the left side of the box
                             for_x_start
                        else for_x_end ,
                        (i - rect_width).div(2) + for_y_start + 1
                )
            }
        }

        fun posToMaskNum(x: Int, y: Int): Int? {
            if (x in for_x_start + 1..for_x_end - 1 && y in for_y_start + 1..for_y_end - 1) {
                return null // inside of this imaginary box
            }
            else if (y <= for_y_start) { // upper edge
                if (x < for_x_start) return 0
                else if (x > for_x_end) return rect_width - 1
                else return x - for_x_start
            }
            else if (y >= for_y_end) { // lower edge
                if (x < for_x_start) return rect_size - rect_width
                else if (x > for_x_end) return rect_size - 1
                else return x - for_x_start + (rect_size - rect_width)
            }
            else { // between two edges
                if (x < for_x_start)    return (y - for_y_start - 1) * 2 + rect_width
                else if (x > for_x_end) return (y - for_y_start - 1) * 2 + rect_width + 1
                else return null
            }
        }

        fun isNoop(x: Int, y: Int): Boolean =
                if (posToMaskNum(x, y) == null)
                    false
                else if (!(x in for_x_start - overscan_opaque..for_x_end + overscan_opaque &&
                             x in for_y_start - overscan_opaque..for_y_end + overscan_opaque))
                     // point is within the range of overscan_open but not overscan_opaque
                    noop_mask.get(posToMaskNum(x, y)!!)
                else // point within the overscan_opaque must be rendered, so no no-op
                    false

        // build noop map
        for (i in 0..rect_size) {
            val point = edgeToMaskNum(i)
            val tile = Terrarum.game.map.getTileFromTerrain(point.first, point.second) ?: TileNameCode.NULL
            val isSolid = TilePropCodex.getProp(tile).isSolid

            noop_mask.set(i, isSolid)
        }*/

        /**
         * Updating order:
         * +--------+   +--+-----+   +-----+--+   +--------+ -
         * |↘       |   |  |    3|   |3    |  |   |       ↙| ↕︎ overscan_open / overscan_opaque
         * |  +-----+   |  |  2  |   |  2  |  |   +-----+  | - depending on the noop_mask
         * |  |1    | → |  |1    | → |    1|  | → |    1|  |
         * |  |  2  |   |  +-----+   +-----+  |   |  2  |  |
         * |  |    3|   |↗       |   |       ↖|   |3    |  |
         * +--+-----+   +--------+   +--------+   +-----+--+
         * round:   1            2            3            4
         * for all staticLightMap[y][x]
         */

        //purgePartOfLightmap(for_x_start - overscan_open, for_y_start - overscan_open, for_x_end + overscan_open, for_y_end + overscan_open)
        purgeLightmap()

        try {
            // Round 1
            for (y in for_y_start - overscan_open..for_y_end) {
                for (x in for_x_start - overscan_open..for_x_end) {
                    setLight(x, y, calculate(x, y))
                }
            }

            // Round 2
            for (y in for_y_end + overscan_open downTo for_y_start) {
                for (x in for_x_start - overscan_open..for_x_end) {
                    setLight(x, y, calculate(x, y))
                }
            }

            // Round 3
            for (y in for_y_end + overscan_open downTo for_y_start) {
                for (x in for_x_end + overscan_open downTo for_x_start) {
                    setLight(x, y, calculate(x, y))
                }
            }

            // Round 4
            for (y in for_y_start - overscan_open..for_y_end) {
                for (x in for_x_end + overscan_open downTo for_x_start) {
                    setLight(x, y, calculate(x, y))
                }
            }
        }
        catch (e: ArrayIndexOutOfBoundsException) {
        }

    }

    private fun calculate(x: Int, y: Int): Int = calculate(x, y, false)

    private fun calculate(x: Int, y: Int, doNotCalculateAmbient: Boolean): Int {
        var lightLevelThis: Int = 0
        val thisTerrain = Terrarum.game.map.getTileFromTerrain(x, y)
        val thisWall = Terrarum.game.map.getTileFromWall(x, y)
        val thisTileLuminosity = TilePropCodex.getProp(thisTerrain).luminosity
        val thisTileOpacity = TilePropCodex.getProp(thisTerrain).opacity
        val sunLight = Terrarum.game.map.globalLight

        // MIX TILE
        // open air
        if (thisTerrain == AIR && thisWall == AIR) {
            lightLevelThis = sunLight
        }
        // luminous tile on top of air
        else if (thisWall == AIR && thisTileLuminosity.toInt() > 0) {
            val darkenSunlight = darkenColoured(sunLight, thisTileOpacity)
            lightLevelThis = maximiseRGB(darkenSunlight, thisTileLuminosity) // maximise to not exceed 1.0 with normal (<= 1.0) light
        }
        // opaque wall and luminous tile
        else if (thisWall != AIR && thisTileLuminosity.toInt() > 0) {
            lightLevelThis = thisTileLuminosity
        }
        // END MIX TILE

        // mix luminous actor
        for (actor in Terrarum.game.actorContainer) {
            if (actor is Luminous && actor is ActorWithBody) {
                val tileX = Math.round(actor.hitbox.pointedX / TSIZE)
                val tileY = Math.round(actor.hitbox.pointedY / TSIZE) - 1
                val actorLuminosity = actor.luminosity
                if (x == tileX && y == tileY) {
                    lightLevelThis = maximiseRGB(lightLevelThis, actorLuminosity) // maximise to not exceed 1.0 with normal (<= 1.0) light
                    break
                }
            }
        }


        if (!doNotCalculateAmbient) {
            // calculate ambient
            var ambient: Int = 0
            var nearby: Int = 0
            for (yoff in -1..1) {
                for (xoff in -1..1) {
                    /**
                     * filter for 'v's as:
                     * +-+-+-+
                     * |a|v|a|
                     * +-+-+-+
                     * |v| |v|
                     * +-+-+-+
                     * |a|v|a|
                     * +-+-+-+
                     */
                    if (xoff != yoff && -xoff != yoff) {
                        // 'v' tiles
                        nearby = getLight(x + xoff, y + yoff) ?: 0
                    }
                    else if (xoff != 0 && yoff != 0) {
                        // 'a' tiles
                        nearby = darkenUniformInt(getLight(x + xoff, y + yoff) ?: 0, 12) //2 for 40step
                        // mix some to have more 'spreading'
                        // so that light spreads in a shape of an octagon instead of a diamond
                    }
                    else {
                        nearby = 0 // exclude 'me' tile
                    }

                    ambient = maximiseRGB(ambient, nearby) // keep base value as brightest nearby
                }
            }

            ambient = darkenColoured(ambient,
                    thisTileOpacity) // get real ambient by appling opacity value

            // mix and return lightlevel and ambient
            return maximiseRGB(lightLevelThis, ambient)
        }
        else {
            return lightLevelThis
        }
    }

    fun draw(g: Graphics) {
        val this_x_start = for_x_start// + overscan_open
        val this_x_end = for_x_end// + overscan_open
        val this_y_start = for_y_start// + overscan_open
        val this_y_end = for_y_end// + overscan_open

        // draw
        try {
            // loop for "scanlines"
            for (y in this_y_start..this_y_end) {
                // loop x
                var x = this_x_start
                while (x < this_x_end) {
                    // smoothing enabled
                    if (Terrarum.game.screenZoom >= 1
                            && Terrarum.gameConfig.getAsBoolean("smoothlighting") ?: false) {

                        val thisLightLevel = getLight(x, y) ?: 0

                        if (x < this_x_end && thisLightLevel == 0
                            && getLight(x, y - 1) == 0) {
                            try {
                                // coalesce zero intensity blocks to one
                                var zeroLevelCounter = 1
                                while (getLight(x + zeroLevelCounter, y) == 0) {
                                    zeroLevelCounter += 1

                                    if (x + zeroLevelCounter >= this_x_end) break
                                }

                                g.color = Color(0)
                                g.fillRect(
                                        (x.toFloat() * TSIZE.toFloat() * Terrarum.game.screenZoom).round().toFloat(),
                                        (y.toFloat() * TSIZE.toFloat() * Terrarum.game.screenZoom).round().toFloat(),
                                        ((TSIZE * Terrarum.game.screenZoom).ceil() * zeroLevelCounter).toFloat(),
                                        (TSIZE * Terrarum.game.screenZoom).ceil().toFloat()
                                )

                                x += zeroLevelCounter - 1
                            }
                            catch (e: ArrayIndexOutOfBoundsException) {
                                // do nothing
                            }

                        }
                        else {
                            /**    a
                             *   +-+-+
                             *   |i|j|
                             * b +-+-+ c
                             *   |k|l|
                             *   +-+-+
                             *     d
                             */
                            val a = maximiseRGB(
                                    thisLightLevel,
                                    getLight(x, y - 1) ?: thisLightLevel
                            )
                            val d = maximiseRGB(
                                    thisLightLevel,
                                    getLight(x, y + 1) ?: thisLightLevel
                            )
                            val b = maximiseRGB(
                                    thisLightLevel,
                                    getLight(x - 1, y) ?: thisLightLevel
                            )
                            val c = maximiseRGB(
                                    thisLightLevel,
                                    getLight(x + 1, y) ?: thisLightLevel
                            )
                            val colourMapItoL = IntArray(4)
                            colourMapItoL[0] = colourLinearMix(a, b)
                            colourMapItoL[1] = colourLinearMix(a, c)
                            colourMapItoL[2] = colourLinearMix(b, d)
                            colourMapItoL[3] = colourLinearMix(c, d)

                            for (iy in 0..1) {
                                for (ix in 0..1) {
                                    g.color = Color(colourMapItoL[iy * 2 + ix].rgb30ClampTo24())

                                    g.fillRect(
                                            (x.toFloat() * TSIZE.toFloat() * Terrarum.game.screenZoom).round()
                                                    + ix * TSIZE / 2 * Terrarum.game.screenZoom,
                                            (y.toFloat() * TSIZE.toFloat() * Terrarum.game.screenZoom).round()
                                                    + iy * TSIZE / 2 * Terrarum.game.screenZoom,
                                            (TSIZE * Terrarum.game.screenZoom / 2).ceil().toFloat(),
                                            (TSIZE * Terrarum.game.screenZoom / 2).ceil().toFloat()
                                    )
                                }
                            }
                        }
                    }
                    // smoothing disabled
                    else {
                        try {
                            val thisLightLevel = getLight(x, y)

                            // coalesce identical intensity blocks to one
                            var sameLevelCounter = 1
                            while (getLight(x + sameLevelCounter, y) == thisLightLevel) {
                                sameLevelCounter += 1

                                if (x + sameLevelCounter >= this_x_end) break
                            }

                            g.color = Color((getLight(x, y) ?: 0).rgb30ClampTo24())
                            g.fillRect(
                                    (x.toFloat() * TSIZE.toFloat() * Terrarum.game.screenZoom).round().toFloat(),
                                    (y.toFloat() * TSIZE.toFloat() * Terrarum.game.screenZoom).round().toFloat(),
                                    ((TSIZE * Terrarum.game.screenZoom).ceil() * sameLevelCounter).toFloat(),
                                    (TSIZE * Terrarum.game.screenZoom).ceil().toFloat()
                            )

                            x += sameLevelCounter - 1
                        }
                        catch (e: ArrayIndexOutOfBoundsException) {
                            // do nothing
                        }

                    }

                    x++
                }
            }
        }
        catch (e: ArrayIndexOutOfBoundsException) {
        }

    }

    /**
     * Subtract each channel's RGB value.
     *
     * It works like:
     *
     * f(data, darken) = RGB(data.r - darken.r, data.g - darken.g, data.b - darken.b)
     *
     * @param data Raw channel value (0-39) per channel
     * @param darken (0-39) per channel
     * @return darkened data (0-39) per channel
     */
    fun darkenColoured(data: Int, darken: Int): Int {
        if (darken.toInt() < 0 || darken.toInt() >= COLOUR_RANGE_SIZE)
            throw IllegalArgumentException("darken: out of range ($darken)")

        var r = data.r() * (1f - darken.r() * 6) // 6: Arbitrary value
        var g = data.g() * (1f - darken.g() * 6) // TODO gamma correction?
        var b = data.b() * (1f - darken.b() * 6)

        return constructRGBFromFloat(r.clampZero(), g.clampZero(), b.clampZero())
    }

    /**
     * Darken each channel by 'darken' argument
     *
     * It works like:
     *
     * f(data, darken) = RGB(data.r - darken, data.g - darken, data.b - darken)
     * @param data (0-39) per channel
     * @param darken (0-39)
     * @return
     */
    fun darkenUniformInt(data: Int, darken: Int): Int {
        if (darken < 0 || darken > CHANNEL_MAX)
            throw IllegalArgumentException("darken: out of range ($darken)")

        val darkenColoured = constructRGBFromInt(darken, darken, darken)
        return darkenColoured(data, darkenColoured)
    }

    /** Get each channel from two RGB values, return new RGB that has max value of each channel
     * @param rgb
     * *
     * @param rgb2
     * *
     * @return
     */
    private fun maximiseRGB(rgb: Int, rgb2: Int): Int {
        val r1 = rgb.rawR()
        val r2 = rgb2.rawR()
        val newR = if (r1 > r2) r1 else r2
        val g1 = rgb.rawG()
        val g2 = rgb2.rawG()
        val newG = if (g1 > g2) g1 else g2
        val b1 = rgb.rawB()
        val b2 = rgb2.rawB()
        val newB = if (b1 > b2) b1 else b2

        return constructRGBFromInt(newR, newG, newB)
    }
    
    private fun screenBlend(rgb: Int, rgb2: Int): Int {
        val r1 = rgb.r()
        val r2 = rgb2.r()
        val newR = 1 - (1 - r1) * (1 - r2)
        val g1 = rgb.g()
        val g2 = rgb2.g()
        val newG = 1 - (1 - g1) * (1 - g2)
        val b1 = rgb.b()
        val b2 = rgb2.b()
        val newB = 1 - (1 - b1) * (1 - b2)

        return constructRGBFromFloat(newR, newG, newB)
    }

    fun Int.rawR() = this / MUL_2
    fun Int.rawG() = this % MUL_2 / MUL
    fun Int.rawB() = this % MUL

    fun Int.r(): Float = this.rawR() / CHANNEL_MAX_FLOAT
    fun Int.g(): Float = this.rawG() / CHANNEL_MAX_FLOAT
    fun Int.b(): Float = this.rawB() / CHANNEL_MAX_FLOAT

    /**

     * @param RGB
     * *
     * @param offset 2 = R, 1 = G, 0 = B
     * *
     * @return
     */
    fun getRaw(RGB: Int, offset: Int): Int {
        if      (offset == OFFSET_R) return RGB.rawR()
        else if (offset == OFFSET_G) return RGB.rawG()
        else if (offset == OFFSET_B) return RGB.rawB()
        else throw IllegalArgumentException("Channel offset out of range")
    }

    private fun addRaw(rgb1: Int, rgb2: Int): Int {
        val newR = (rgb1.rawR() + rgb2.rawR()).clampChannel()
        val newG = (rgb1.rawG() + rgb2.rawG()).clampChannel()
        val newB = (rgb1.rawB() + rgb2.rawB()).clampChannel()

        return constructRGBFromInt(newR, newG, newB)
    }

    fun constructRGBFromInt(r: Int, g: Int, b: Int): Int {
        if (r !in 0..CHANNEL_MAX) throw IllegalArgumentException("Red: out of range ($r)")
        if (g !in 0..CHANNEL_MAX) throw IllegalArgumentException("Green: out of range ($g)")
        if (b !in 0..CHANNEL_MAX) throw IllegalArgumentException("Blue: out of range ($b)")
        return (r * MUL_2 + g * MUL + b)
    }

    fun constructRGBFromFloat(r: Float, g: Float, b: Float): Int {
        if (r < 0 || r > 1.0f) throw IllegalArgumentException("Red: out of range ($r)")
        if (g < 0 || g > 1.0f) throw IllegalArgumentException("Green: out of range ($g)")
        if (b < 0 || b > 1.0f) throw IllegalArgumentException("Blue: out of range ($b)")

        val intR = (r * CHANNEL_MAX).floor()
        val intG = (g * CHANNEL_MAX).floor()
        val intB = (b * CHANNEL_MAX).floor()

        return constructRGBFromInt(intR, intG, intB)
    }

    private fun colourLinearMix(colA: Int, colB: Int): Int {
        val r = (colA.rawR() + colB.rawR()) ushr 1
        val g = (colA.rawG() + colB.rawG()) ushr 1
        val b = (colA.rawB() + colB.rawB()) ushr 1
        return constructRGBFromInt(r, g, b)
    }

    private fun Int.clampZero() = if (this < 0) 0 else this

    private fun Float.clampZero() = if (this < 0) 0f else this

    private fun Int.clampChannel() = if (this < 0) 0 else if (this > CHANNEL_MAX) CHANNEL_MAX else this

    private fun Float.clampOne() = if (this < 0) 0f else if (this > 1) 1f else this

    fun getValueFromMap(x: Int, y: Int): Int? = getLight(x, y)

    private fun purgeLightmap() {
        for (y in 0..LIGHTMAP_HEIGHT - 1) {
            for (x in 0..LIGHTMAP_WIDTH - 1) {
                lightmap[y][x] = 0
            }
        }
    }

    private fun arithmeticAverage(vararg i: Int): Int {
        var sum = 0
        for (k in i.indices) {
            sum += i[k]
        }
        return Math.round(sum / i.size.toFloat())
    }

    private fun Int.clamp256() = if (this > 255) 255 else this

    fun Int.rgb30ClampTo24(): Int {
        val r = this.rawR().clamp256()
        val g = this.rawG().clamp256()
        val b = this.rawB().clamp256()

        return r.shl(16) or g.shl(8) or b
    }

    infix fun Float.powerOf(f: Float) = FastMath.pow(this, f)
    private fun Float.sqr() = this * this
    private fun Float.sqrt() = FastMath.sqrt(this)
    private fun Float.inv() = 1f / this
    fun Float.floor() = FastMath.floor(this)
    fun Float.round(): Int = Math.round(this)
    fun Float.ceil() = FastMath.ceil(this)
    fun Int.even(): Boolean = this and 1 == 0
    fun Int.odd(): Boolean = this and 1 == 1

    val histogram: Histogram
        get() {
            var reds = IntArray(MUL) // reds[intensity] ← counts
            var greens = IntArray(MUL) // do.
            var blues = IntArray(MUL) // do.
            val render_width = for_x_end - for_x_start
            val render_height = for_y_end - for_y_start
            // excluiding overscans; only reckon visible lights
            for (y in overscan_open..render_height + overscan_open + 1) {
                for (x in overscan_open..render_width + overscan_open + 1) {
                    reds[lightmap[y][x].rawR()] += 1
                    greens[lightmap[y][x].rawG()] += 1
                    blues[lightmap[y][x].rawB()] += 1
                }
            }
            return Histogram(reds, greens, blues)
        }

    class Histogram(val reds: IntArray, val greens: IntArray, val blues: IntArray) {

        val RED = 0
        val GREEN = 1
        val BLUE = 2

        val screen_tiles: Int
            get() = (for_x_end - for_x_start + 2) * (for_y_end - for_y_start + 2)

        val brightest: Int
            get() {
                for (i in CHANNEL_MAX downTo 1) {
                    if (reds[i] > 0 || greens[i] > 0 || blues[i] > 0)
                        return i
                }
                return 0
            }

        val brightest8Bit: Int
            get() { val b = brightest
                return if (brightest > 255) 255 else b
            }

        val dimmest: Int
            get() {
                for (i in 0..CHANNEL_MAX) {
                    if (reds[i] > 0 || greens[i] > 0 || blues[i] > 0)
                        return i
                }
                return CHANNEL_MAX
            }

        val histogramMax: Int
            get() {
                var max = 0
                for (c in 0..2) {
                    for (i in 0..CHANNEL_MAX) {
                        val value = get(c)[i]
                        if (value > max) max = value
                    }
                }
                return max
            }

        val range: Int
            get() = reds.size

        fun get(index: Int): IntArray {
            return when (index) {
                RED -> reds
                GREEN -> greens
                BLUE -> blues
                else -> throw IllegalArgumentException()
            }
        }
    }
}
