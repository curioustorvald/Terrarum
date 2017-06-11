package net.torvald.terrarum.worlddrawer

import net.torvald.terrarum.gameactors.Luminous
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.blockproperties.BlockCodex
import com.jme3.math.FastMath
import net.torvald.terrarum.gameactors.ActorWithPhysics
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.blockproperties.Block
import org.newdawn.slick.Color
import org.newdawn.slick.Graphics
import java.util.*

/**
 * Created by minjaesong on 16-01-25.
 */

object LightmapRenderer {
    private val world: GameWorld = Terrarum.ingame!!.world


    // TODO if (VBO works on BlocksDrawer) THEN overscan of 256, utilise same technique in here

    val overscan_open: Int = Math.min(32, 256f.div(BlockCodex[Block.AIR].opacity and 0xFF).ceil())
    val overscan_opaque: Int = Math.min(8, 256f.div(BlockCodex[Block.STONE].opacity and 0xFF).ceil())

    val LIGHTMAP_WIDTH = Terrarum.ingame!!.ZOOM_MIN.inv().times(Terrarum.WIDTH)
            .div(FeaturesDrawer.TILE_SIZE).ceil() + overscan_open * 2 + 3
    val LIGHTMAP_HEIGHT = Terrarum.ingame!!.ZOOM_MIN.inv().times(Terrarum.HEIGHT)
            .div(FeaturesDrawer.TILE_SIZE).ceil() + overscan_open * 2 + 3

    /**
     * 8-Bit RGB values
     */
    private val lightmap: Array<IntArray> = Array(LIGHTMAP_HEIGHT) { IntArray(LIGHTMAP_WIDTH) }
    private val lanternMap = ArrayList<Lantern>(Terrarum.ingame!!.ACTORCONTAINER_INITIAL_SIZE * 4)

    private val AIR = Block.AIR

    private val OFFSET_R = 2
    private val OFFSET_G = 1
    private val OFFSET_B = 0

    private const val TILE_SIZE = FeaturesDrawer.TILE_SIZE

    // color model related constants
    const val MUL = 1024 // modify this to 1024 to implement 30-bit RGB
    const val CHANNEL_MAX_DECIMAL = 1f
    const val MUL_2 = MUL * MUL
    const val CHANNEL_MAX = MUL - 1
    const val CHANNEL_MAX_FLOAT = CHANNEL_MAX.toFloat()
    const val COLOUR_RANGE_SIZE = MUL * MUL_2

    internal var for_x_start: Int = 0
    internal var for_y_start: Int = 0
    internal var for_x_end: Int = 0
    internal var for_y_end: Int = 0


    //inline fun getLightRawPos(x: Int, y: Int) = lightmap[y][x]

    fun getLight(x: Int, y: Int): Int? {
        if (y - for_y_start + overscan_open in 0..lightmap.lastIndex &&
            x - for_x_start + overscan_open in 0..lightmap[0].lastIndex) {

            return lightmap[y - for_y_start + overscan_open][x - for_x_start + overscan_open]
        }

        return null
    }

    fun setLight(x: Int, y: Int, colour: Int) {
        if (y - for_y_start + overscan_open in 0..lightmap.lastIndex &&
            x - for_x_start + overscan_open in 0..lightmap[0].lastIndex) {

            lightmap[y - for_y_start + overscan_open][x - for_x_start + overscan_open] = colour
        }
    }

    fun renderLightMap() {
        for_x_start = WorldCamera.x / TILE_SIZE - 1 // fix for premature lightmap rendering
        for_y_start = WorldCamera.y / TILE_SIZE - 1 // on topmost/leftmost side

        for_x_end = for_x_start + WorldCamera.width / TILE_SIZE + 3
        for_y_end = for_y_start + WorldCamera.height / TILE_SIZE + 2 // same fix as above

        /**
         * * true: overscanning is limited to 8 tiles in width (overscan_opaque)
         * * false: overscanning will fully applied to 32 tiles in width (overscan_open)
         */
        val rect_width = for_x_end - for_x_start
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
                        else for_x_end,
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
                if (x < for_x_start) return (y - for_y_start - 1) * 2 + rect_width
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
            val tile = Terrarum.ingame!!.world.getTileFromTerrain(point.first, point.second) ?: Block.NULL
            val isSolid = BlockCodex[tile].isSolid

            noop_mask.set(i, isSolid)
        }

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

        purgeLightmap()
        buildLanternmap()

        // O(36n) == O(n) where n is a size of the map.
        // Because of inevitable overlaps on the area, it only works with ADDITIVE blend (aka maxblend)


        // Round 1
        for (y in for_y_start - overscan_open..for_y_end) {
            for (x in for_x_start - overscan_open..for_x_end) {
                setLight(x, y, calculate(x, y, 1))
            }
        }

        // Round 2
        for (y in for_y_end + overscan_open downTo for_y_start) {
            for (x in for_x_start - overscan_open..for_x_end) {
                setLight(x, y, calculate(x, y, 2))
            }
        }

        // Round 3
        for (y in for_y_end + overscan_open downTo for_y_start) {
            for (x in for_x_end + overscan_open downTo for_x_start) {
                setLight(x, y, calculate(x, y, 3))
            }
        }

        // Round 4
        for (y in for_y_start - overscan_open..for_y_end) {
            for (x in for_x_end + overscan_open downTo for_x_start) {
                setLight(x, y, calculate(x, y, 4))
            }
        }
    }

    private fun buildLanternmap() {
        lanternMap.clear()
        Terrarum.ingame!!.actorContainer.forEach { it ->
            if (it is Luminous && it is ActorWithPhysics) {
                // put lanterns to the area the luminantBox is occupying
                for (lightBox in it.lightBoxList) {
                    val lightBoxX = it.hitbox.startX + lightBox.startX
                    val lightBoxY = it.hitbox.startY + lightBox.startY
                    val lightBoxW = lightBox.width
                    val lightBoxH = lightBox.height
                    for (y in lightBoxY.div(TILE_SIZE).floorInt()
                            ..lightBoxY.plus(lightBoxH).div(TILE_SIZE).floorInt()) {
                        for (x in lightBoxX.div(TILE_SIZE).floorInt()
                                ..lightBoxX.plus(lightBoxW).div(TILE_SIZE).floorInt()) {
                            lanternMap.add(Lantern(x, y, it.luminosity))
                            // Q&D fix for Roundworld anomaly
                            lanternMap.add(Lantern(x + world.width, y, it.luminosity))
                            lanternMap.add(Lantern(x - world.width, y, it.luminosity))
                        }
                    }
                }
            }
        }
    }

    private fun calculate(x: Int, y: Int, pass: Int): Int = calculate(x, y, pass, false)

    private fun calculate(x: Int, y: Int, pass: Int, doNotCalculateAmbient: Boolean): Int {
        // O(9n) == O(n) where n is a size of the map
        // TODO devise multithreading on this

        var lightLevelThis: Int = 0
        val thisTerrain = Terrarum.ingame!!.world.getTileFromTerrain(x, y)
        val thisWall = Terrarum.ingame!!.world.getTileFromWall(x, y)
        val thisTileLuminosity = BlockCodex[thisTerrain].luminosity
        val thisTileOpacity = BlockCodex[thisTerrain].opacity
        val sunLight = Terrarum.ingame!!.world.globalLight

        // MIX TILE
        // open air
        if (thisTerrain == AIR && thisWall == AIR) {
            lightLevelThis = sunLight
        }
        // luminous tile on top of air
        else if (thisWall == AIR && thisTileLuminosity > 0) {
            lightLevelThis = sunLight maxBlend thisTileLuminosity // maximise to not exceed 1.0 with normal (<= 1.0) light
        }
        // opaque wall and luminous tile
        else if (thisWall != AIR && thisTileLuminosity > 0) {
            lightLevelThis = thisTileLuminosity
        }
        // END MIX TILE

        for (i in 0..lanternMap.size - 1) {
            val lmap = lanternMap[i]
            if (lmap.posX == x && lmap.posY == y)
                lightLevelThis = lightLevelThis maxBlend lmap.luminosity // maximise to not exceed 1.0 with normal (<= 1.0) light
        }


        if (!doNotCalculateAmbient) {
            // calculate ambient
            /*  + * +
             *  * @ *
             *  + * +
             *  sample ambient for eight points and apply attenuation for those
             *  maxblend eight values and use it
             */
            var ambient = 0
            ambient = ambient maxBlend darkenColoured(getLight(x - 1, y - 1) ?: 0, scaleColour(thisTileOpacity, 1.4142f))
            ambient = ambient maxBlend darkenColoured(getLight(x + 1, y - 1) ?: 0, scaleColour(thisTileOpacity, 1.4142f))
            ambient = ambient maxBlend darkenColoured(getLight(x - 1, y + 1) ?: 0, scaleColour(thisTileOpacity, 1.4142f))
            ambient = ambient maxBlend darkenColoured(getLight(x + 1, y + 1) ?: 0, scaleColour(thisTileOpacity, 1.4142f))
            ambient = ambient maxBlend darkenColoured(getLight(x    , y - 1) ?: 0, thisTileOpacity)
            ambient = ambient maxBlend darkenColoured(getLight(x    , y + 1) ?: 0, thisTileOpacity)
            ambient = ambient maxBlend darkenColoured(getLight(x - 1, y    ) ?: 0, thisTileOpacity)
            ambient = ambient maxBlend darkenColoured(getLight(x + 1, y    ) ?: 0, thisTileOpacity)
            return lightLevelThis maxBlend ambient
        }
        else {
            return lightLevelThis
        }
    }

    private fun getLightSpecial(x: Int, y: Int): Int? {
        val l = getLight(x, y)
        if (l == null) return null

        if (BlockCodex[world.getTileFromTerrain(x, y)].isSolid) {
            return constructRGBFromFloat(
                    (l.r() * 1.25f).clampOne(),
                    (l.g() * 1.25f).clampOne(),
                    (l.b() * 1.25f).clampOne()
            )
        }
        else {
            return l
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
                    // smoothing enabled and zoom is 0.75 or greater
                    // (zoom of 0.5 should not smoothed, for performance)
                    if (Terrarum.getConfigBoolean("smoothlighting") ?: false &&
                        Terrarum.ingame!!.screenZoom >= 0.75) {

                        val thisLightLevel = getLightSpecial(x, y) ?: 0

                        if (x < this_x_end && thisLightLevel == 0
                            && getLightSpecial(x, y - 1) == 0) {
                            try {
                                // coalesce zero intensity blocks to one
                                var zeroLevelCounter = 1
                                while (getLightSpecial(x + zeroLevelCounter, y) == 0) {
                                    zeroLevelCounter += 1

                                    if (x + zeroLevelCounter >= this_x_end) break
                                }

                                g.color = Color(0)
                                g.fillRect(
                                        (x.toFloat() * TILE_SIZE).round().toFloat(),
                                        (y.toFloat() * TILE_SIZE).round().toFloat(),
                                        (TILE_SIZE * zeroLevelCounter).toFloat(),
                                        (TILE_SIZE).toFloat()
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
                            val a = thisLightLevel maxBlend (getLightSpecial(x, y - 1) ?: thisLightLevel)
                            val d = thisLightLevel maxBlend (getLightSpecial(x, y + 1) ?: thisLightLevel)
                            val b = thisLightLevel maxBlend (getLightSpecial(x - 1, y) ?: thisLightLevel)
                            val c = thisLightLevel maxBlend (getLightSpecial(x + 1, y) ?: thisLightLevel)

                            val colourMapItoL = IntArray(4)
                            val colMean = (a linMix d) linMix (b linMix c)
                            val colDelta = thisLightLevel colSub colMean

                            colourMapItoL[0] = a linMix b colAdd colDelta
                            colourMapItoL[1] = a linMix c colAdd colDelta
                            colourMapItoL[2] = b linMix d colAdd colDelta
                            colourMapItoL[3] = c linMix d colAdd colDelta

                            for (iy in 0..1) {
                                for (ix in 0..1) {
                                    g.color = colourMapItoL[iy * 2 + ix].normaliseToColourHDR()


                                    g.fillRect(
                                            (x.toFloat() * TILE_SIZE).round()
                                            + ix * TILE_SIZE / 2f,
                                            (y.toFloat() * TILE_SIZE).round()
                                            + iy * TILE_SIZE / 2f,
                                            (TILE_SIZE / 2f).ceil().toFloat(),
                                            (TILE_SIZE / 2f).ceil().toFloat()
                                    )
                                }
                            }
                        }
                    }
                    // smoothing disabled
                    else {
                        try {
                            val thisLightLevel = getLightSpecial(x, y)

                            // coalesce identical intensity blocks to one
                            var sameLevelCounter = 1
                            while (getLightSpecial(x + sameLevelCounter, y) == thisLightLevel) {
                                sameLevelCounter += 1

                                if (x + sameLevelCounter >= this_x_end) break
                            }

                            g.color = (getLightSpecial(x, y) ?: 0).normaliseToColourHDR()

                            g.fillRect(
                                    (x.toFloat() * TILE_SIZE).round().toFloat(),
                                    (y.toFloat() * TILE_SIZE).round().toFloat(),
                                    (TILE_SIZE.toFloat().ceil() * sameLevelCounter).toFloat(),
                                     TILE_SIZE.toFloat().ceil().toFloat()
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

    val lightScalingMagic = 8f

    /**
     * Subtract each channel's RGB value.
     *
     * @param data Raw channel value (0-255) per channel
     * @param darken (0-255) per channel
     * @return darkened data (0-255) per channel
     */
    fun darkenColoured(data: Int, darken: Int): Int {
        if (darken < 0 || darken >= COLOUR_RANGE_SIZE)
            throw IllegalArgumentException("darken: out of range ($darken)")

        // use equation with magic number 8.0
        // should draw somewhat exponential curve when you plot the propagation of light in-game

        val r = data.r() * (1f - darken.r() * lightScalingMagic)
        val g = data.g() * (1f - darken.g() * lightScalingMagic)
        val b = data.b() * (1f - darken.b() * lightScalingMagic)

        return constructRGBFromFloat(r.clampZero(), g.clampZero(), b.clampZero())
    }

    fun scaleColour(data: Int, scale: Float): Int {
        val r = data.r() * scale
        val g = data.g() * scale
        val b = data.b() * scale

        return constructRGBFromFloat(r.clampOne(), g.clampOne(), b.clampOne())
    }

    /**
     * Add each channel's RGB value.
     *
     * @param data Raw channel value (0-255) per channel
     * @param brighten (0-255) per channel
     * @return brightened data (0-255) per channel
     */
    fun brightenColoured(data: Int, brighten: Int): Int {
        if (brighten < 0 || brighten >= COLOUR_RANGE_SIZE)
            throw IllegalArgumentException("brighten: out of range ($brighten)")

        val r = data.r() * (1f + brighten.r() * lightScalingMagic)
        val g = data.g() * (1f + brighten.g() * lightScalingMagic)
        val b = data.b() * (1f + brighten.b() * lightScalingMagic)

        return constructRGBFromFloat(r.clampChannel(), g.clampChannel(), b.clampChannel())
    }

    /**
     * Darken each channel by 'darken' argument
     *
     * @param data Raw channel value (0-255) per channel
     * @param darken (0-255)
     * @return
     */
     fun darkenUniformInt(data: Int, darken: Int): Int {
        if (darken < 0 || darken > CHANNEL_MAX)
            throw IllegalArgumentException("darken: out of range ($darken)")

        val darkenColoured = constructRGBFromInt(darken, darken, darken)
        return darkenColoured(data, darkenColoured)
     }

    /**
     * Darken or brighten colour by 'brighten' argument
     *
     * @param data Raw channel value (0-255) per channel
     * @param brighten (-1.0 - 1.0) negative means darkening
     * @return processed colour
     */
    fun alterBrightnessUniform(data: Int, brighten: Float): Int {
        val modifier = if (brighten < 0)
            constructRGBFromFloat(-brighten, -brighten, -brighten)
        else
            constructRGBFromFloat(brighten, brighten, brighten)

        return if (brighten < 0)
            darkenColoured(data, modifier)
        else
            brightenColoured(data, modifier)
    }

    /**
     * Deprecated: Fuck it, this vittupää just doesn't want to work
     */
    private infix fun Int.screenBlend(other: Int): Int {
        /*val r1 = this.r(); val r2 = other.r(); val newR = 1 - (1 - r1) * (1 - r2)
        val g1 = this.g(); val g2 = other.g(); val newG = 1 - (1 - g1) * (1 - g2)
        val b1 = this.b(); val b2 = other.b(); val newB = 1 - (1 - b1) * (1 - b2)*/

        val r1 = this.r(); val r2 = other.r()
        val g1 = this.g(); val g2 = other.g()
        val b1 = this.b(); val b2 = other.b()

        var screenR = 1f - (1f - r1).clampZero() * (1f - r2).clampZero()
        var screenG = 1f - (1f - g1).clampZero() * (1f - g2).clampZero()
        var screenB = 1f - (1f - b1).clampZero() * (1f - b2).clampZero()

        // hax.
        val addR = if (r1 > r2) r1 else r2
        val addG = if (g1 > g2) g1 else g2
        val addB = if (b1 > b2) b1 else b2

        val newR = Math.min(screenR, addR)
        val newG = Math.min(screenG, addG)
        val newB = Math.min(screenB, addB)

        return constructRGBFromFloat(newR, newG, newB)
    }

    /** Get each channel from two RGB values, return new RGB that has max value of each channel
     * @param rgb
     * @param rgb2
     * @return
     */
    private inline infix fun Int.maxBlend(other: Int): Int {
        return (if (this.rawR() > other.rawR()) this.rawR() else other.rawR()) * MUL_2 +
               (if (this.rawG() > other.rawG()) this.rawG() else other.rawG()) * MUL +
               (if (this.rawB() > other.rawB()) this.rawB() else other.rawB())
    }

    private inline infix fun Int.linMix(other: Int): Int {
        return ((this.rawR() + other.rawR()) ushr 1) * MUL_2 +
               ((this.rawG() + other.rawG()) ushr 1) * MUL +
               ((this.rawB() + other.rawB()) ushr 1)
    }

    private inline infix fun Int.colSub(other: Int): Int {
        return ((this.rawR() - other.rawR()).clampChannel()) * MUL_2 +
               ((this.rawG() - other.rawG()).clampChannel()) * MUL +
               ((this.rawB() - other.rawB()).clampChannel())
    }

    private inline infix fun Int.colAdd(other: Int): Int {
        return ((this.rawR() + other.rawR()).clampChannel()) * MUL_2 +
               ((this.rawG() + other.rawG()).clampChannel()) * MUL +
               ((this.rawB() + other.rawB()).clampChannel())
    }

    inline fun Int.rawR() = this / MUL_2
    inline fun Int.rawG() = this % MUL_2 / MUL
    inline fun Int.rawB() = this % MUL

    /** 0.0 - 1.0 for 0-1023 (0.0 - 0.25 for 0-255) */
    inline fun Int.r(): Float = this.rawR() / CHANNEL_MAX_FLOAT
    inline fun Int.g(): Float = this.rawG() / CHANNEL_MAX_FLOAT
    inline fun Int.b(): Float = this.rawB() / CHANNEL_MAX_FLOAT

    /**

     * @param RGB
     * @param offset 2 = R, 1 = G, 0 = B
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

    inline fun constructRGBFromInt(r: Int, g: Int, b: Int): Int {
        if (r !in 0..CHANNEL_MAX) throw IllegalArgumentException("Red: out of range ($r)")
        if (g !in 0..CHANNEL_MAX) throw IllegalArgumentException("Green: out of range ($g)")
        if (b !in 0..CHANNEL_MAX) throw IllegalArgumentException("Blue: out of range ($b)")

        return r * MUL_2 +
               g * MUL +
               b
    }

    inline fun constructRGBFromFloat(r: Float, g: Float, b: Float): Int {
        if (r < 0 || r > CHANNEL_MAX_DECIMAL) throw IllegalArgumentException("Red: out of range ($r)")
        if (g < 0 || g > CHANNEL_MAX_DECIMAL) throw IllegalArgumentException("Green: out of range ($g)")
        if (b < 0 || b > CHANNEL_MAX_DECIMAL) throw IllegalArgumentException("Blue: out of range ($b)")

        return (r * CHANNEL_MAX).round() * MUL_2 +
               (g * CHANNEL_MAX).round() * MUL +
               (b * CHANNEL_MAX).round()
    }

    inline fun Int.clampZero() = if (this < 0) 0 else this
    inline fun Float.clampZero() = if (this < 0) 0f else this
    inline fun Int.clampChannel() = if (this < 0) 0 else if (this > CHANNEL_MAX) CHANNEL_MAX else this
    inline fun Float.clampOne() = if (this < 0) 0f else if (this > 1) 1f else this
    inline fun Float.clampChannel() = if (this > CHANNEL_MAX_DECIMAL) CHANNEL_MAX_DECIMAL else this

    inline fun getValueFromMap(x: Int, y: Int): Int? = getLight(x, y)
    fun getHighestRGB(x: Int, y: Int): Int? {
        val value = getLight(x, y)
        if (value == null)
            return null
        else
            return FastMath.max(value.rawR(), value.rawG(), value.rawB())
    }

    private fun purgeLightmap() {
        for (y in 0..LIGHTMAP_HEIGHT - 1) {
            for (x in 0..LIGHTMAP_WIDTH - 1) {
                lightmap[y][x] = 0
            }
        }
    }

    inline infix fun Float.powerOf(f: Float) = FastMath.pow(this, f)
    private inline fun Float.sqr() = this * this
    private inline fun Float.sqrt() = FastMath.sqrt(this)
    private inline fun Float.inv() = 1f / this
    inline fun Float.floor() = FastMath.floor(this)
    inline fun Double.floorInt() = Math.floor(this).toInt()
    inline fun Float.round(): Int = Math.round(this)
    inline fun Double.round(): Int = Math.round(this).toInt()
    inline fun Float.ceil() = FastMath.ceil(this)
    inline fun Int.even(): Boolean = this and 1 == 0
    inline fun Int.odd(): Boolean = this and 1 == 1
    inline fun Int.normaliseToColour(): Color = Color(
            Math.min(this.rawR(), 256),
            Math.min(this.rawG(), 256),
            Math.min(this.rawB(), 256)
    )
    val RGB_HDR_LUT = floatArrayOf( // polymonial of 6.0  please refer to work_files/HDRcurveBezierLinIntp.kts
            0.0f,0.0f,0.0020097627f,0.0059880256f,0.009966114f,0.013944201f,0.01792211f,0.021899965f,0.025877733f,0.029855346f,0.033832956f,0.037810322f,0.041787688f,0.045764867f,0.04974198f,0.053718954f,
            0.05769581f,0.061672557f,0.065649144f,0.06962565f,0.07360197f,0.07757821f,0.08155425f,0.0855302f,0.08950596f,0.0934816f,0.097457066f,0.101432376f,0.10540755f,0.1093825f,0.11335737f,0.11733195f,
            0.12130644f,0.12528068f,0.12925476f,0.13322866f,0.1372023f,0.14117579f,0.14514904f,0.14912204f,0.1530949f,0.15706745f,0.16103975f,0.16501188f,0.1689837f,0.17295523f,0.17692655f,0.1808976f,
            0.18486828f,0.1888387f,0.19280887f,0.1967787f,0.20074815f,0.20471728f,0.2086861f,0.21265456f,0.21662268f,0.22059034f,0.22455761f,0.22852449f,0.23249097f,0.236457f,0.24042259f,0.24438772f,
            0.24835236f,0.2523165f,0.25628012f,0.2602432f,0.26420572f,0.26816767f,0.27212903f,0.27608976f,0.28004986f,0.28400928f,0.28796804f,0.2919261f,0.2958834f,0.2998399f,0.30379558f,0.30775046f,
            0.31170452f,0.31565768f,0.31960982f,0.32356104f,0.3275113f,0.33146045f,0.3354085f,0.3393555f,0.34330124f,0.3472458f,0.35118902f,0.355131f,0.3590715f,0.36301064f,0.36694816f,0.3708842f,
            0.3748186f,0.37875122f,0.38268217f,0.3866112f,0.3905383f,0.39446342f,0.39838645f,0.40230724f,0.40622574f,0.41014186f,0.4140555f,0.41796657f,0.4218749f,0.42578045f,0.42968303f,0.4335825f,
            0.4374788f,0.44137177f,0.44526124f,0.44914708f,0.45302916f,0.45690724f,0.46078113f,0.46465078f,0.468516f,0.47237647f,0.47623205f,0.48008266f,0.4839279f,0.48776764f,0.49160177f,0.49542972f,
            0.49925172f,0.503067f,0.5068758f,0.51067746f,0.51447207f,0.51825887f,0.52203804f,0.52580905f,0.5295716f,0.5333255f,0.5370702f,0.5408056f,0.5445313f,0.54824686f,0.55195194f,0.5556463f,
            0.5593296f,0.5630012f,0.56666094f,0.57030845f,0.5739433f,0.5775651f,0.58117324f,0.5847676f,0.5883477f,0.59191316f,0.5954636f,0.5989982f,0.60251695f,0.6060194f,0.6095051f,0.61297375f,
            0.6164247f,0.6198574f,0.62327176f,0.6266674f,0.63004386f,0.6334008f,0.63673735f,0.6400535f,0.64334905f,0.64662355f,0.6498765f,0.6531072f,0.6563159f,0.65950227f,0.6626658f,0.6658057f,
            0.6689224f,0.67201567f,0.6750844f,0.67812896f,0.6811494f,0.6841446f,0.687115f,0.69006073f,0.69298035f,0.69587505f,0.69874376f,0.70158684f,0.7044041f,0.7071951f,0.7099604f,0.7126992f,
            0.71541196f,0.7180984f,0.7207585f,0.7233927f,0.7259999f,0.7285815f,0.7311365f,0.7336652f,0.73616827f,0.73864496f,0.74109536f,0.74352026f,0.74591964f,0.7482929f,0.75064045f,0.7529628f,
            0.7552601f,0.7575324f,0.7597799f,0.7620023f,0.7641999f,0.7663732f,0.76852244f,0.77064764f,0.77274907f,0.774827f,0.7768816f,0.778913f,0.78092164f,0.7829076f,0.7848708f,0.78681165f,
            0.7887305f,0.7906278f,0.7925037f,0.7943586f,0.79619235f,0.7980046f,0.79979676f,0.801569f,0.8033212f,0.80505276f,0.8067654f,0.80845934f,0.81013274f,0.8117882f,0.8134254f,0.8150433f,
            0.8166442f,0.8182262f,0.8197912f,0.8213388f,0.8228689f,0.82438266f,0.8258789f,0.82735956f,0.8288232f,0.83027136f,0.8317035f,0.8331199f,0.8345216f,0.83590704f,0.83727884f,0.8386347f,
            0.8399765f,0.8413048f,0.84261733f,0.8439169f,0.84520334f,0.84647477f,0.84773356f,0.84897983f,0.85021216f,0.85143167f,0.85263914f,0.8538346f,0.85501665f,0.85618675f,0.85734534f,0.85849243f,
            0.8596281f,0.8607512f,0.8618634f,0.86296463f,0.8640551f,0.86513495f,0.8662042f,0.8672625f,0.8683101f,0.86934763f,0.87037516f,0.8713928f,0.8724007f,0.8733989f,0.8743876f,0.8753669f,
            0.87633693f,0.87729776f,0.8782496f,0.8791924f,0.8801264f,0.88105166f,0.8819684f,0.8828766f,0.8837764f,0.884668f,0.88555145f,0.88642687f,0.88729435f,0.88815403f,0.8890061f,0.8898505f,
            0.8906874f,0.89151704f,0.8923394f,0.8931546f,0.8939628f,0.89476323f,0.8955566f,0.8963433f,0.8971233f,0.8978967f,0.89866376f,0.89942443f,0.90017843f,0.90092534f,0.9016661f,0.9024009f,
            0.9031299f,0.90385306f,0.90456975f,0.90527993f,0.9059846f,0.90668386f,0.9073778f,0.9080655f,0.9087471f,0.9094236f,0.91009516f,0.9107616f,0.91142124f,0.9120762f,0.91272646f,0.91337216f,
            0.91401106f,0.91464555f,0.91527575f,0.915901f,0.91652024f,0.9171354f,0.9177466f,0.9183519f,0.91895264f,0.9195496f,0.9201418f,0.9207286f,0.92131186f,0.9218912f,0.9224646f,0.9230347f,
            0.9236013f,0.9241619f,0.92471933f,0.9252734f,0.92582166f,0.926367f,0.92690873f,0.9274451f,0.9279788f,0.9285084f,0.92903346f,0.929556f,0.9300736f,0.9305878f,0.93109953f,0.9316056f,
            0.9321094f,0.9326094f,0.93310535f,0.9335993f,0.93408775f,0.9345741f,0.93505687f,0.9355357f,0.93601286f,0.9364844f,0.93695444f,0.9374203f,0.93788326f,0.9383437f,0.93879974f,0.9392545f,
            0.939704f,0.94015217f,0.9405962f,0.9410379f,0.94147664f,0.941912f,0.9423454f,0.9427746f,0.9432027f,0.94362587f,0.9440483f,0.944466f,0.94488263f,0.9452952f,0.94570625f,0.9461137f,
            0.9465192f,0.9469216f,0.9473217f,0.9477191f,0.948114f,0.94850636f,0.94889617f,0.9492836f,0.9496684f,0.95005095f,0.950431f,0.95080864f,0.9511839f,0.9515568f,0.95192754f,0.9522956f,
            0.9526619f,0.9530252f,0.9533872f,0.95374584f,0.9541036f,0.9544576f,0.9548113f,0.9551606f,0.95550996f,0.95585513f,0.9561999f,0.9565413f,0.95688146f,0.9572192f,0.9575549f,0.9578891f,
            0.9582203f,0.95855105f,0.9588778f,0.9592046f,0.9595277f,0.95985f,0.9601699f,0.96048796f,0.96080476f,0.96111846f,0.96143216f,0.9617418f,0.9620512f,0.962358f,0.9626632f,0.96296734f,
            0.9632682f,0.96356916f,0.9638666f,0.96416336f,0.9644583f,0.9647509f,0.96504354f,0.96533203f,0.9656206f,0.9659069f,0.96619135f,0.96647555f,0.966756f,0.96703637f,0.9673146f,0.96759105f,
            0.96786743f,0.9681399f,0.9684123f,0.968683f,0.9689515f,0.96922004f,0.96948516f,0.9697498f,0.97001344f,0.9702742f,0.970535f,0.9707934f,0.9710503f,0.9713073f,0.9715606f,0.97181374f,
            0.9720659f,0.9723153f,0.97256464f,0.97281206f,0.9730577f,0.9733033f,0.9735461f,0.973788f,0.9740299f,0.9742682f,0.9745065f,0.974744f,0.9749787f,0.97521335f,0.97544664f,0.9756777f,
            0.97590876f,0.976138f,0.9763655f,0.976593f,0.97681826f,0.97704226f,0.9772662f,0.9774877f,0.97770816f,0.97792864f,0.97814643f,0.97836345f,0.9785805f,0.97879475f,0.9790083f,0.9792219f,
            0.9794328f,0.979643f,0.97985315f,0.9800608f,0.9802676f,0.98047435f,0.9806789f,0.9808824f,0.98108584f,0.9812874f,0.9814875f,0.98168766f,0.9818864f,0.98208326f,0.9822801f,0.9824761f,
            0.9826697f,0.9828633f,0.9830567f,0.9832471f,0.9834375f,0.98362786f,0.98381555f,0.98400277f,0.9841899f,0.98437536f,0.98455936f,0.9847434f,0.98492664f,0.9851075f,0.9852884f,0.9854693f,
            0.9856473f,0.98582506f,0.98600286f,0.986179f,0.9863537f,0.9865284f,0.9867027f,0.98687434f,0.987046f,0.98721766f,0.9873872f,0.98755586f,0.9877245f,0.9878925f,0.98805815f,0.9882238f,
            0.98838943f,0.988553f,0.9887157f,0.98887837f,0.9890407f,0.9892004f,0.98936015f,0.9895199f,0.9896781f,0.9898349f,0.9899918f,0.9901486f,0.9903028f,0.99045676f,0.9906107f,0.99076396f,
            0.9909151f,0.9910662f,0.9912173f,0.9913669f,0.9915152f,0.9916635f,0.9918118f,0.99195784f,0.99210334f,0.99224883f,0.9923943f,0.992537f,0.9926798f,0.99282247f,0.9929647f,0.9931047f,
            0.99324465f,0.99338466f,0.9935238f,0.99366105f,0.9937983f,0.9939356f,0.9940718f,0.9942063f,0.9943409f,0.9944754f,0.9946088f,0.9947407f,0.9948726f,0.99500453f,0.9951353f,0.99526453f,
            0.9953938f,0.99552304f,0.99565125f,0.9957779f,0.99590456f,0.99603117f,0.996157f,0.9962811f,0.9964051f,0.99652916f,0.9966528f,0.99677426f,0.99689573f,0.9970172f,0.9971387f,0.99725765f,
            0.99737656f,0.99749553f,0.99761444f,0.99773145f,0.99784786f,0.99796426f,0.99808073f,0.9981959f,0.9983098f,0.99842376f,0.99853766f,0.99865115f,0.9987626f,0.99887407f,0.9989855f,0.999097f,
            0.9992064f,0.99931544f,0.99942446f,0.99953353f,0.99964154f,0.9997481f,0.99985474f,0.9999613f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
            1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
            1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
            1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
            1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
            1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
            1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
            1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
            1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
            1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
            1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
            1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
            1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
            1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
            1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
            1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
            1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
            1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
            1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
            1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
            1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
            1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
            1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
            1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f
    )
    /** To eliminated visible edge on the gradient when 255/1023 is exceeded */
    inline fun Int.normaliseToColourHDR() = Color(
            RGB_HDR_LUT[this.rawR()],
            RGB_HDR_LUT[this.rawG()],
            RGB_HDR_LUT[this.rawB()]
    )

    data class Lantern(val posX: Int, val posY: Int, val luminosity: Int)

    val histogram: Histogram
        get() {
            var reds = IntArray(MUL) // reds[intensity] ← counts
            var greens = IntArray(MUL) // do.
            var blues = IntArray(MUL) // do.
            val render_width = for_x_end - for_x_start
            val render_height = for_y_end - for_y_start
            // excluiding overscans; only reckon echo lights
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

        val screen_tiles: Int = (for_x_end - for_x_start + 2) * (for_y_end - for_y_start + 2)

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

        val range: Int = CHANNEL_MAX

        fun get(index: Int): IntArray {
            return when (index) {
                RED   -> reds
                GREEN -> greens
                BLUE  -> blues
                else  -> throw IllegalArgumentException()
            }
        }
    }
}
