package com.torvald.terrarum.mapdrawer

import com.torvald.terrarum.gameactors.ActorWithBody
import com.torvald.terrarum.gameactors.Luminous
import com.torvald.terrarum.gamemap.WorldTime
import com.torvald.terrarum.Terrarum
import com.torvald.terrarum.tileproperties.TilePropCodex
import com.jme3.math.FastMath
import com.torvald.terrarum.tileproperties.TileNameCode
import org.newdawn.slick.Color
import org.newdawn.slick.Graphics
import java.util.*

/**
 * Created by minjaesong on 16-01-25.
 */

object LightmapRenderer {
    /**
     * 8-Bit RGB values
     */
    @Volatile private var lightmap: Array<IntArray> = Array(Terrarum.game.map.height) { IntArray(Terrarum.game.map.width) }
    private var lightMapInitialised = false

    private val AIR = 0
    private val SUNSTONE = 41 // TODO add sunstone: emits same light as Map.GL. Goes dark at night


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

    fun getLight(x: Int, y: Int): Int? =
            if (x !in 0..Terrarum.game.map.width - 1 || y !in 0..Terrarum.game.map.height - 1)
                // if out of range then
                null
            else
                lightmap[y][x]

    fun setLight(x: Int, y: Int, colour: Int) {
        lightmap[y][x] = colour
    }

    fun renderLightMap() {
        val for_x_start = div16(MapCamera.cameraX) - 1 // fix for premature lightmap rendering
        val for_y_start = div16(MapCamera.cameraY) - 1 // on topmost/leftmost side

        val for_x_end = for_x_start + div16(MapCamera.getRenderWidth()) + 3
        val for_y_end = for_y_start + div16(MapCamera.getRenderHeight()) + 2 // same fix as above

        val overscan_open: Int = (256f / (TilePropCodex.getProp(TileNameCode.AIR).opacity and 0xFF).toFloat()).ceil()
        val overscan_opaque: Int = (256f / (TilePropCodex.getProp(TileNameCode.STONE).opacity and 0xFF).toFloat()).ceil()

        /**
         * * true: overscanning is limited to 8 tiles in width (overscan_opaque)
         * * false: overscanning will fully applied to 32 tiles in width (overscan_open)
         */
        val noop_mask = BitSet(2 * (for_x_end - for_x_start + 1) +
                               2 * (for_y_end - for_y_start - 1))

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

        purgePartOfLightmap(for_x_start - overscan_open, for_y_start - overscan_open, for_x_end + overscan_open, for_y_end + overscan_open)

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
                val tileX = Math.round(actor.hitbox!!.pointedX / TSIZE)
                val tileY = Math.round(actor.hitbox!!.pointedY / TSIZE) - 1
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
                        if (!outOfMapBounds(x + xoff, y + yoff)) {
                            nearby = getLight(x + xoff, y + yoff) ?: 0
                        }
                    }
                    else if (xoff != 0 && yoff != 0) {
                        // 'a' tiles
                        if (!outOfMapBounds(x + xoff, y + yoff)) {
                            nearby = darkenUniformInt(getLight(x + xoff, y + yoff) ?: 0, 12) //2 for 40step
                            // mix some to have more 'spreading'
                            // so that light spreads in a shape of an octagon instead of a diamond
                        }
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
        val for_x_start = div16(MapCamera.cameraX) - 1 // fix for premature lightmap rendering
        val for_y_start = div16(MapCamera.cameraY) - 1 // on topmost/leftmost side

        val for_x_end = for_x_start + div16(MapCamera.getRenderWidth()) + 3
        val for_y_end = for_y_start + div16(MapCamera.getRenderHeight()) + 2 // same fix as above


        // draw
        try {
            // loop for "scanlines"
            for (y in for_y_start..for_y_end) {
                // loop x
                var x = for_x_start
                while (x < for_x_end) {
                    // smoothing enabled
                    if (Terrarum.game.screenZoom >= 1
                            && Terrarum.gameConfig.getAsBoolean("smoothlighting") ?: false) {

                        val thisLightLevel = getLight(x, y) ?: 0

                        if (x < for_x_end && thisLightLevel == 0
                            && getLight(x, y - 1) == 0) {
                            try {
                                // coalesce zero intensity blocks to one
                                var zeroLevelCounter = 1
                                while (getLight(x + zeroLevelCounter, y) == 0) {
                                    zeroLevelCounter += 1

                                    if (x + zeroLevelCounter >= for_x_end) break
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

                                if (x + sameLevelCounter >= for_x_end) break
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

    private fun quantise16(x: Int): Int {
        if (x < 0) throw IllegalArgumentException("positive integer only.")
        return x and 0xFFFFFFF0.toInt()
    }

    private fun div16(x: Int): Int {
        if (x < 0) throw IllegalArgumentException("positive integer only.")
        return x and 0x7FFFFFFF shr 4
    }

    private fun mul16(x: Int): Int {
        if (x < 0) throw IllegalArgumentException("positive integer only.")
        return x shl 4
    }

    private fun max(vararg i: Int): Int {
        Arrays.sort(i)
        return i[i.size - 1]
    }

    private fun min(vararg i: Int): Int {
        Arrays.sort(i)
        return i[0]
    }

    private fun outOfBounds(x: Int, y: Int): Boolean =
            x !in 0..Terrarum.game.map.width - 1 || y !in 0..Terrarum.game.map.height - 1

    private fun outOfMapBounds(x: Int, y: Int): Boolean =
            //x !in 0..lightMapMSB!![0].size - 1 || y !in 0..lightMapMSB!!.size - 1
            x !in 0..lightmap[0].size - 1 || y !in 0..lightmap.size - 1

    private fun Int.clampZero() = if (this < 0) 0 else this

    private fun Float.clampZero() = if (this < 0) 0f else this

    private fun Int.clampChannel() = if (this < 0) 0 else if (this > CHANNEL_MAX) CHANNEL_MAX else this

    private fun Float.clampOne() = if (this < 0) 0f else if (this > 1) 1f else this

    fun getValueFromMap(x: Int, y: Int): Int? = getLight(x, y)

    private fun purgePartOfLightmap(x1: Int, y1: Int, x2: Int, y2: Int) {
        try {
            for (y in y1 - 1..y2 + 1) {
                for (x in x1 - 1..x2 + 1) {
                    //if (y == y1 - 1 || y == y2 + 1 || x == x1 - 1 || x == x2 + 1) {
                        // fill the rim with (pre) calculation
                    //    setLight(x, y, preCalculateUpdateGLOnly(x, y))
                    //}
                    //else {
                        setLight(x, y, 0)
                    //}
                }
            }
        }
        catch (e: ArrayIndexOutOfBoundsException) {
        }

    }

    private fun clampWTile(x: Int): Int {
        if (x < 0) {
            return 0
        }
        else if (x > Terrarum.game.map.width) {
            return Terrarum.game.map.width
        }
        else {
            return x
        }
    }

    private fun clampHTile(x: Int): Int {
        if (x < 0) {
            return 0
        }
        else if (x > Terrarum.game.map.height) {
            return Terrarum.game.map.height
        }
        else {
            return x
        }
    }

    private fun arithmeticAverage(vararg i: Int): Int {
        var sum = 0
        for (k in i.indices) {
            sum += i[k]
        }
        return Math.round(sum / i.size.toFloat())
    }

    internal data class LightmapLantern(
            var x: Int,
            var y: Int,
            var intensity: Int
    )

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
    fun Float.floor() = FastMath.floor(this)
    fun Float.round(): Int = Math.round(this)
    fun Float.ceil() = FastMath.ceil(this)
}
