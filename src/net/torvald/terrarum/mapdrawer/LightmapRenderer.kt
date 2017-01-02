package net.torvald.terrarum.mapdrawer

import net.torvald.terrarum.gameactors.Luminous
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.tileproperties.TileCodex
import com.jme3.math.FastMath
import net.torvald.colourutil.RGB
import net.torvald.colourutil.CIELuvUtil.additiveLuv
import net.torvald.terrarum.concurrent.ThreadParallel
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.abs
import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.tileproperties.Tile
import net.torvald.terrarum.tileproperties.TilePropUtil
import org.newdawn.slick.Color
import org.newdawn.slick.Graphics
import java.util.*
import java.util.concurrent.locks.ReentrantLock

/**
 * Created by minjaesong on 16-01-25.
 */

object LightmapRenderer {
    private val world: GameWorld = Terrarum.ingame.world

    val overscan_open: Int = Math.min(32, 256f.div(TileCodex[Tile.AIR].opacity and 0xFF).ceil())
    val overscan_opaque: Int = Math.min(8, 256f.div(TileCodex[Tile.STONE].opacity and 0xFF).ceil())

    private val LIGHTMAP_WIDTH = Terrarum.ingame.ZOOM_MIN.inv().times(Terrarum.WIDTH)
            .div(FeaturesDrawer.TILE_SIZE).ceil() + overscan_open * 2 + 3
    private val LIGHTMAP_HEIGHT = Terrarum.ingame.ZOOM_MIN.inv().times(Terrarum.HEIGHT)
            .div(FeaturesDrawer.TILE_SIZE).ceil() + overscan_open * 2 + 3

    /**
     * 8-Bit RGB values
     */
    private val lightmap: Array<IntArray> = Array(LIGHTMAP_HEIGHT) { IntArray(LIGHTMAP_WIDTH) }
    private val lanternMap = ArrayList<Lantern>(Terrarum.ingame.ACTORCONTAINER_INITIAL_SIZE * 4)

    private val AIR = Tile.AIR

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


    fun getLightRawPos(x: Int, y: Int) = lightmap[y][x]

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
        for_x_start = MapCamera.x / TILE_SIZE - 1 // fix for premature lightmap rendering
        for_y_start = MapCamera.y / TILE_SIZE - 1 // on topmost/leftmost side

        for_x_end = for_x_start + MapCamera.width / TILE_SIZE + 3
        for_y_end = for_y_start + MapCamera.height / TILE_SIZE + 2 // same fix as above

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
            val tile = Terrarum.ingame.world.getTileFromTerrain(point.first, point.second) ?: Tile.NULL
            val isSolid = TileCodex[tile].isSolid

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
        Terrarum.ingame.actorContainer.forEach { it ->
            if (it is Luminous && it is ActorWithBody) {
                // put lanterns to the area the luminantBox is occupying
                for (lightBox in it.lightBoxList) {
                    val lightBoxX = it.hitbox.posX + lightBox.posX
                    val lightBoxY = it.hitbox.posY + lightBox.posY
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
        val thisTerrain = Terrarum.ingame.world.getTileFromTerrain(x, y)
        val thisWall = Terrarum.ingame.world.getTileFromWall(x, y)
        val thisTileLuminosity = TileCodex[thisTerrain].luminosity
        val thisTileOpacity = TileCodex[thisTerrain].opacity
        val sunLight = Terrarum.ingame.world.globalLight

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

        // mix luminous actor
        /*for ((posX, posY, luminosity) in lanternMap) {
            if (posX == x && posY == y)
                lightLevelThis = lightLevelThis maxBlend luminosity // maximise to not exceed 1.0 with normal (<= 1.0) light
        }*/
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
                    if (Terrarum.gameConfig.getAsBoolean("smoothlighting") ?: false &&
                        Terrarum.ingame.screenZoom >= 0.75) {

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
                            val a = thisLightLevel maxBlend (getLight(x, y - 1) ?: thisLightLevel)
                            val d = thisLightLevel maxBlend (getLight(x, y + 1) ?: thisLightLevel)
                            val b = thisLightLevel maxBlend (getLight(x - 1, y) ?: thisLightLevel)
                            val c = thisLightLevel maxBlend (getLight(x + 1, y) ?: thisLightLevel)

                            val colourMapItoL = IntArray(4)
                            val colMean = (a linMix d) linMix (b linMix c)
                            val colDelta = thisLightLevel colSub colMean

                            colourMapItoL[0] = a linMix b colAdd colDelta
                            colourMapItoL[1] = a linMix c colAdd colDelta
                            colourMapItoL[2] = b linMix d colAdd colDelta
                            colourMapItoL[3] = c linMix d colAdd colDelta

                            for (iy in 0..1) {
                                for (ix in 0..1) {
                                    g.color = colourMapItoL[iy * 2 + ix].normaliseToColour()

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
                            val thisLightLevel = getLight(x, y)

                            // coalesce identical intensity blocks to one
                            var sameLevelCounter = 1
                            while (getLight(x + sameLevelCounter, y) == thisLightLevel) {
                                sameLevelCounter += 1

                                if (x + sameLevelCounter >= this_x_end) break
                            }

                            g.color = (getLight(x, y) ?: 0).normaliseToColour()
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

        // use equation with magic number 6.5:
        // =>> val r = data.r() * (1f + brighten.r() * 6.5f) <<=
        // gives 8-visible-tile penetration of sunlight, fairly smooth,
        // DOES NOT GO BELOW (2,2,2)

        val r = data.r() * (1f - darken.r() * 6.5f)
        val g = data.g() * (1f - darken.g() * 6.5f)
        val b = data.b() * (1f - darken.b() * 6.5f)
        //val r = data.r() - darken.r()
        //val g = data.g() - darken.g()
        //val b = data.b() - darken.b()

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

        val r = data.r() * (1f + brighten.r() * 6.5f)
        val g = data.g() * (1f + brighten.g() * 6.5f)
        val b = data.b() * (1f + brighten.b() * 6.5f)
        //val r = data.r() + brighten.r()
        //val g = data.g() + brighten.g()
        //val b = data.b() + brighten.b()

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

    /** Get each channel from two RGB values, return new RGB that has max value of each channel
     * @param rgb
     * @param rgb2
     * @return
     */
    private infix fun Int.maxBlend(other: Int): Int {
        val r1 = this.rawR(); val r2 = other.rawR(); val newR = if (r1 > r2) r1 else r2
        val g1 = this.rawG(); val g2 = other.rawG(); val newG = if (g1 > g2) g1 else g2
        val b1 = this.rawB(); val b2 = other.rawB(); val newB = if (b1 > b2) b1 else b2

        return constructRGBFromInt(newR, newG, newB)
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

    private infix fun Int.colSub(other: Int) = constructRGBFromInt(
            (this.rawR() - other.rawR()).clampChannel() ,
            (this.rawG() - other.rawG()).clampChannel() ,
            (this.rawB() - other.rawB()).clampChannel()
    )

    private infix fun Int.colAdd(other: Int) = constructRGBFromInt(
            (this.rawR() + other.rawR()).clampChannel() ,
            (this.rawG() + other.rawG()).clampChannel() ,
            (this.rawB() + other.rawB()).clampChannel()
    )

    fun Int.rawR() = this / MUL_2
    fun Int.rawG() = this % MUL_2 / MUL
    fun Int.rawB() = this % MUL

    /** 0.0 - 1.0 for 0-1023 (0.0 - 0.25 for 0-255) */
    fun Int.r(): Float = this.rawR() / CHANNEL_MAX_FLOAT
    fun Int.g(): Float = this.rawG() / CHANNEL_MAX_FLOAT
    fun Int.b(): Float = this.rawB() / CHANNEL_MAX_FLOAT

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

    fun constructRGBFromInt(r: Int, g: Int, b: Int): Int {
        if (r !in 0..CHANNEL_MAX) throw IllegalArgumentException("Red: out of range ($r)")
        if (g !in 0..CHANNEL_MAX) throw IllegalArgumentException("Green: out of range ($g)")
        if (b !in 0..CHANNEL_MAX) throw IllegalArgumentException("Blue: out of range ($b)")
        return r * MUL_2 + g * MUL + b
    }

    fun constructRGBFromFloat(r: Float, g: Float, b: Float): Int {
        if (r < 0 || r > CHANNEL_MAX_DECIMAL) throw IllegalArgumentException("Red: out of range ($r)")
        if (g < 0 || g > CHANNEL_MAX_DECIMAL) throw IllegalArgumentException("Green: out of range ($g)")
        if (b < 0 || b > CHANNEL_MAX_DECIMAL) throw IllegalArgumentException("Blue: out of range ($b)")

        val intR = (r * CHANNEL_MAX).round()
        val intG = (g * CHANNEL_MAX).round()
        val intB = (b * CHANNEL_MAX).round()

        return constructRGBFromInt(intR, intG, intB)
    }

    private infix fun Int.linMix(other: Int): Int {
        val r = (this.rawR() + other.rawR()) ushr 1
        val g = (this.rawG() + other.rawG()) ushr 1
        val b = (this.rawB() + other.rawB()) ushr 1
        return constructRGBFromInt(r, g, b)
    }

    private fun Int.clampZero() = if (this < 0) 0 else this

    private fun Float.clampZero() = if (this < 0) 0f else this

    private fun Int.clampChannel() = if (this < 0) 0 else if (this > CHANNEL_MAX) CHANNEL_MAX else this

    private fun Float.clampOne() = if (this < 0) 0f else if (this > 1) 1f else this

    private fun Float.clampChannel() = if (this > CHANNEL_MAX_DECIMAL) CHANNEL_MAX_DECIMAL else this

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

    infix fun Float.powerOf(f: Float) = FastMath.pow(this, f)
    private fun Float.sqr() = this * this
    private fun Float.sqrt() = FastMath.sqrt(this)
    private fun Float.inv() = 1f / this
    fun Float.floor() = FastMath.floor(this)
    fun Double.floorInt() = Math.floor(this).toInt()
    fun Float.round(): Int = Math.round(this)
    fun Double.round(): Int = Math.round(this).toInt()
    fun Float.ceil() = FastMath.ceil(this)
    fun Int.even(): Boolean = this and 1 == 0
    fun Int.odd(): Boolean = this and 1 == 1
    fun Int.normaliseToColour(): Color = Color(
            Math.min(this.rawR(), 256),
            Math.min(this.rawG(), 256),
            Math.min(this.rawB(), 256)
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
