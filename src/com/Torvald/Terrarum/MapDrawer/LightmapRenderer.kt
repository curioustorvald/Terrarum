package com.torvald.terrarum.mapdrawer

import com.torvald.terrarum.gameactors.ActorWithBody
import com.torvald.terrarum.gameactors.Luminous
import com.torvald.terrarum.gamemap.WorldTime
import com.torvald.terrarum.Terrarum
import com.torvald.terrarum.tileproperties.TilePropCodex
import com.jme3.math.FastMath
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
    @Volatile private var lightMapMSB: Array<CharArray>? = null
    @Volatile private var lightMapLSB: Array<ByteArray>? = null // modify this to CharArray to implement 30-bit RGB
    private var lightMapInitialised = false

    /**
     * For entities that emits light (e.g. Player with shine potion)
     */
    private val lanterns = ArrayList<LightmapLantern>()

    private val AIR = 0
    private val SUNSTONE = 41 // TODO add sunstone: emits same light as Map.GL. Goes dark at night


    private val OFFSET_R = 2
    private val OFFSET_G = 1
    private val OFFSET_B = 0

    private const val TSIZE = MapDrawer.TILE_SIZE

    // color model related constants
    const val MUL = 256 // modify this to 1024 to implement 30-bit RGB
    const val MUL_2 = MUL * MUL
    const val CHANNEL_MAX = MUL - 1
    const val CHANNEL_MAX_FLOAT = CHANNEL_MAX.toFloat()
    const val COLOUR_RANGE_SIZE = MUL * MUL_2

    private const val deprecatedFeatureDebatable = "The usage of this feature is debatable. Do not use it yet."


    @Deprecated(deprecatedFeatureDebatable)
    fun addLantern(x: Int, y: Int, intensity: Int) {
        val thisLantern = LightmapLantern(x, y, intensity)

        for (i in lanterns.indices.reversed()) {
            val lanternInList = lanterns[i]
            // found duplicates
            if (lanternInList.x == x && lanternInList.y == y) {
                // add colour
                val addedL = addRaw(intensity, lanternInList.intensity)
                lanternInList.intensity = addedL
                return
            }
        }
        //else
        lanterns.add(thisLantern)
    }

    @Deprecated(deprecatedFeatureDebatable)
    fun removeLantern(x: Int, y: Int) {
        for (i in lanterns.indices.reversed()) {
            val lantern = lanterns[i]
            if (lantern.x == x && lantern.y == y) {
                lanterns.removeAt(i)
            }
        }
    }

    fun getLight(x: Int, y: Int): Int? =
            if (x !in 0..Terrarum.game.map.width - 1 || y !in 0..Terrarum.game.map.height - 1)
                // if out of range then
                null
            else
                java.lang.Byte.toUnsignedInt(lightMapLSB!![y][x]) or (lightMapMSB!![y][x].toInt() shl 8)


    fun setLight(x: Int, y: Int, colour: Int) {
        lightMapLSB!![y][x] = (colour and 0xFF).toByte()
        lightMapMSB!![y][x] = (colour ushr 8).toChar()
    }

    fun renderLightMap() {
        if (lightMapMSB == null && lightMapLSB == null) {
            lightMapMSB = Array(Terrarum.game.map.height) { CharArray(Terrarum.game.map.width) }
            lightMapLSB = Array(Terrarum.game.map.height) { ByteArray(Terrarum.game.map.width) }

            if (lightMapInitialised) {
                throw RuntimeException("Attempting to re-initialise 'staticLightMap'")
            }

            lightMapInitialised = true
        }


        val for_x_start = div16(MapCamera.cameraX) - 1 // fix for premature lightmap rendering
        val for_y_start = div16(MapCamera.cameraY) - 1 // on topmost/leftmost side

        val for_x_end = clampWTile(for_x_start + div16(MapCamera.getRenderWidth()) + 2) + 1
        val for_y_end = clampHTile(for_y_start + div16(MapCamera.getRenderHeight()) + 2) + 1 // same fix as above

        /**
         * Updating order:
         * +-----+   +-----+   +-----+   +-----+
         * |1    |   |    1|   |3    |   |    3|
         * |  2  | > |  2  | > |  2  | > |  2  |
         * |    3|   |3    |   |    1|   |1    |
         * +-----+   +-----+   +-----+   +-----+
         * round: 1         2         3         4
         * for all staticLightMap[y][x]
         */

        purgePartOfLightmap(for_x_start, for_y_start, for_x_end, for_y_end)
        // if wider purge were not applied, GL changing (sunset, sunrise) will behave incorrectly
        // ("leakage" of not updated sunlight)

        try {
            // Round 1
            for (y in for_y_start..for_y_end - 1) {
                for (x in for_x_start..for_x_end - 1) {
                    setLight(x, y, calculate(x, y))
                }
            }

            // Round 4
            for (y in for_y_end - 1 downTo for_y_start + 1) {
                for (x in for_x_start..for_x_end - 1) {
                    setLight(x, y, calculate(x, y))
                }
            }

            // Round 3
            for (y in for_y_end - 1 downTo for_y_start + 1) {
                for (x in for_x_end - 1 downTo for_x_start) {
                    setLight(x, y, calculate(x, y))
                }
            }

            // Round 2
            for (y in for_y_start..for_y_end - 1) {
                for (x in for_x_end - 1 downTo for_x_start) {
                    setLight(x, y, calculate(x, y))
                }
            }
        }
        catch (e: ArrayIndexOutOfBoundsException) {
        }

    }

    fun draw(g: Graphics) {
        val for_x_start = MapCamera.getRenderStartX() - 1
        val for_y_start = MapCamera.getRenderStartY() - 1
        val for_x_end = MapCamera.getRenderEndX()
        val for_y_end = MapCamera.getRenderEndY()

        // draw
        try {
            for (y in for_y_start..for_y_end - 1) {
                var x = for_x_start
                while (x < for_x_end) {
                    // smooth
                    if (Terrarum.game.screenZoom >= 1 && Terrarum.gameConfig.getAsBoolean("smoothlighting") ?: false) {
                        val thisLightLevel = getLight(x, y) ?: 0
                        if (y > 0 && x < for_x_end && thisLightLevel == 0
                            && getLight(x, y - 1) == 0) {
                            try {
                                // coalesce zero intensity blocks to one
                                var zeroLevelCounter = 1
                                while (getLight(x + zeroLevelCounter, y) == 0
                                       && getLight(x + zeroLevelCounter, y - 1) == 0) {
                                    zeroLevelCounter += 1

                                    if (x + zeroLevelCounter >= for_x_end) break
                                }

                                g.color = Color(0)
                                g.fillRect(
                                        Math.round(x.toFloat() * TSIZE.toFloat() * Terrarum.game.screenZoom).toFloat(), Math.round(y.toFloat() * TSIZE.toFloat() * Terrarum.game.screenZoom).toFloat(), (FastMath.ceil(
                                        TSIZE * Terrarum.game.screenZoom) * zeroLevelCounter).toFloat(), FastMath.ceil(TSIZE * Terrarum.game.screenZoom).toFloat())

                                x += zeroLevelCounter - 1
                            }
                            catch (e: ArrayIndexOutOfBoundsException) {
                                // do nothing
                            }

                        }
                        else {
                            /**    a
                             * +-+-+
                             * |i|j|
                             * b +-+-+ c
                             * |k|l|
                             * +-+-+
                             * d
                             */
                            val a = if (y == 0)
                                thisLightLevel
                            else if (y == Terrarum.game.map.height - 1)
                                thisLightLevel
                            else
                                maximiseRGB(
                                        getLight(x, y) ?: 0,
                                        getLight(x, y - 1) ?: 0)
                            val d = if (y == 0)
                                thisLightLevel
                            else if (y == Terrarum.game.map.height - 1)
                                thisLightLevel
                            else
                                maximiseRGB(
                                        getLight(x, y) ?: 0,
                                        getLight(x, y + 1) ?: 0)
                            val b = if (x == 0)
                                thisLightLevel
                            else if (x == Terrarum.game.map.width - 1)
                                thisLightLevel
                            else
                                maximiseRGB(
                                        getLight(x, y) ?: 0,
                                        getLight(x - 1, y) ?: 0)
                            val c = if (x == 0)
                                thisLightLevel
                            else if (x == Terrarum.game.map.width - 1)
                                thisLightLevel
                            else
                                maximiseRGB(
                                        getLight(x, y) ?: 0,
                                        getLight(x + 1, y) ?: 0)
                            val colourMapItoL = IntArray(4)
                            colourMapItoL[0] = colourLinearMix(a, b)
                            colourMapItoL[1] = colourLinearMix(a, c)
                            colourMapItoL[2] = colourLinearMix(b, d)
                            colourMapItoL[3] = colourLinearMix(c, d)

                            for (iy in 0..1) {
                                for (ix in 0..1) {
                                    g.color = Color(colourMapItoL[iy * 2 + ix])

                                    g.fillRect(
                                            Math.round(
                                                    x.toFloat() * TSIZE.toFloat() * Terrarum.game.screenZoom) + ix * TSIZE / 2 * Terrarum.game.screenZoom, Math.round(
                                            y.toFloat() * TSIZE.toFloat() * Terrarum.game.screenZoom) + iy * TSIZE / 2 * Terrarum.game.screenZoom, FastMath.ceil(TSIZE * Terrarum.game.screenZoom / 2).toFloat(), FastMath.ceil(TSIZE * Terrarum.game.screenZoom / 2).toFloat())
                                }
                            }
                        }
                    }
                    else {
                        try {
                            val thisLightLevel = getLight(x, y)

                            // coalesce identical intensity blocks to one
                            var sameLevelCounter = 1
                            while (getLight(x + sameLevelCounter, y) == thisLightLevel) {
                                sameLevelCounter += 1

                                if (x + sameLevelCounter >= for_x_end) break
                            }

                            g.color = Color(getLight(x, y) ?: 0)
                            g.fillRect(
                                    Math.round(x.toFloat() * TSIZE.toFloat() * Terrarum.game.screenZoom).toFloat(), Math.round(y.toFloat() * TSIZE.toFloat() * Terrarum.game.screenZoom).toFloat(), (FastMath.ceil(
                                    TSIZE * Terrarum.game.screenZoom) * sameLevelCounter).toFloat(), FastMath.ceil(TSIZE * Terrarum.game.screenZoom).toFloat())

                            x += sameLevelCounter - 1
                        }
                        catch (e: ArrayIndexOutOfBoundsException) {
                            // do nothing
                        }

                    }// Retro
                    x++
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
            lightLevelThis = maximiseRGB(darkenSunlight, thisTileLuminosity)
        }
        // opaque wall and luminous tile
        else if (thisWall != AIR && thisTileLuminosity.toInt() > 0) {
            lightLevelThis = thisTileLuminosity
        }
        // END MIX TILE

        // mix lantern
        for (lantern in lanterns) {
            if (lantern.x == x && lantern.y == y) {
                lightLevelThis = screenBlend(lightLevelThis, lantern.intensity)
                break
            }
        }

        // mix luminous actor
        for (actor in Terrarum.game.actorContainer) {
            if (actor is Luminous && actor is ActorWithBody) {
                val tileX = Math.round(actor.hitbox!!.pointedX / TSIZE)
                val tileY = Math.round(actor.hitbox!!.pointedY / TSIZE) - 1
                val actorLuminosity = actor.luminosity
                if (x == tileX && y == tileY) {
                    lightLevelThis = screenBlend(lightLevelThis, actorLuminosity)
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

        val r = clampZero(getR(data) - getR(darken))
        val g = clampZero(getG(data) - getG(darken))
        val b = clampZero(getB(data) - getB(darken))

        return constructRGBFromFloat(r, g, b)
    }

    /**
     * Darken each channel by 'darken' argument
     *
     * It works like:
     *
     * f(data, darken) = RGB(data.r - darken, data.g - darken, data.b - darken)
     *
     * @param data (0-39) per channel
     * @param darken (0-1)
     * @return
     */
    fun darkenUniformFloat(data: Int, darken: Float): Int {
        if (darken < 0 || darken > 1f)
            throw IllegalArgumentException("darken: out of range ($darken)")

        val r = clampZero(getR(data) - darken)
        val g = clampZero(getG(data) - darken)
        val b = clampZero(getB(data) - darken)

        return constructRGBFromFloat(r, g, b)
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

        val r = clampZero(getRawR(data) - darken)
        val g = clampZero(getRawG(data) - darken)
        val b = clampZero(getRawB(data) - darken)

        return constructRGBFromInt(r, g, b)
    }


    /**
     * Add each channel's RGB value.
     *
     * It works like:
     *
     * f(data, brighten) = RGB(data.r + brighten.r, data.g + brighten.g, data.b + brighten.b)
     * @param data Raw channel value [0-39] per channel
     * *
     * @param brighten [0-39] per channel
     * *
     * @return brightened data [0-39] per channel
     */
    private fun brightenColoured(data: Int, brighten: Int): Int {
        if (brighten.toInt() < 0 || brighten.toInt() >= COLOUR_RANGE_SIZE)
            throw IllegalArgumentException("brighten: out of range ($brighten)")

        val r = clampFloat(getR(data) + getR(brighten))
        val g = clampFloat(getG(data) + getG(brighten))
        val b = clampFloat(getB(data) + getB(brighten))

        return constructRGBFromFloat(r, g, b)
    }

    /** Get each channel from two RGB values, return new RGB that has max value of each channel
     * @param rgb
     * *
     * @param rgb2
     * *
     * @return
     */
    private fun maximiseRGB(rgb: Int, rgb2: Int): Int {
        val r1 = getRawR(rgb)
        val r2 = getRawR(rgb2)
        val newR = if (r1 > r2) r1 else r2
        val g1 = getRawG(rgb)
        val g2 = getRawG(rgb2)
        val newG = if (g1 > g2) g1 else g2
        val b1 = getRawB(rgb)
        val b2 = getRawB(rgb2)
        val newB = if (b1 > b2) b1 else b2

        return constructRGBFromInt(newR, newG, newB)
    }

    private fun screenBlend(rgb: Int, rgb2: Int): Int {
        val r1 = getR(rgb)
        val r2 = getR(rgb2)
        val newR = 1 - (1 - r1) * (1 - r2)
        val g1 = getG(rgb)
        val g2 = getG(rgb2)
        val newG = 1 - (1 - g1) * (1 - g2)
        val b1 = getB(rgb)
        val b2 = getB(rgb2)
        val newB = 1 - (1 - b1) * (1 - b2)

        return constructRGBFromFloat(newR, newG, newB)
    }

    fun getRawR(RGB: Int): Int {
        return RGB.toInt() / MUL_2
    }

    fun getRawG(RGB: Int): Int {
        return RGB.toInt() % MUL_2 / MUL
    }

    fun getRawB(RGB: Int): Int {
        return RGB.toInt() % MUL
    }

    /**

     * @param RGB
     * *
     * @param offset 2 = R, 1 = G, 0 = B
     * *
     * @return
     */
    fun getRaw(RGB: Int, offset: Int): Int {
        if      (offset == OFFSET_R) return getRawR(RGB)
        else if (offset == OFFSET_G) return getRawG(RGB)
        else if (offset == OFFSET_B) return getRawB(RGB)
        else throw IllegalArgumentException("Channel offset out of range")
    }

    private fun getR(rgb: Int): Float {
        return getRawR(rgb) / CHANNEL_MAX_FLOAT
    }

    private fun getG(rgb: Int): Float {
        return getRawG(rgb) / CHANNEL_MAX_FLOAT
    }

    private fun getB(rgb: Int): Float {
        return getRawB(rgb) / CHANNEL_MAX_FLOAT
    }

    private fun addRaw(rgb1: Int, rgb2: Int): Int {
        val newR = clampByte(getRawR(rgb1) + getRawB(rgb2))
        val newG = clampByte(getRawG(rgb1) + getRawG(rgb2))
        val newB = clampByte(getRawB(rgb1) + getRawB(rgb2))

        return constructRGBFromInt(newR, newG, newB)
    }

    fun constructRGBFromInt(r: Int, g: Int, b: Int): Int {
        if (r !in 0..CHANNEL_MAX) throw IllegalArgumentException("Red: out of range")
        if (g !in 0..CHANNEL_MAX) throw IllegalArgumentException("Green: out of range")
        if (b !in 0..CHANNEL_MAX) throw IllegalArgumentException("Blue: out of range")
        return (r * MUL_2 + g * MUL + b)
    }

    fun constructRGBFromFloat(r: Float, g: Float, b: Float): Int {
        if (r < 0 || r > 1.0f) throw IllegalArgumentException("Red: out of range")
        if (g < 0 || g > 1.0f) throw IllegalArgumentException("Green: out of range")
        if (b < 0 || b > 1.0f) throw IllegalArgumentException("Blue: out of range")

        val intR = Math.round(r * CHANNEL_MAX)
        val intG = Math.round(g * CHANNEL_MAX)
        val intB = Math.round(b * CHANNEL_MAX)

        return constructRGBFromInt(intR, intG, intB)
    }

    private fun colourLinearMix(colA: Int, colB: Int): Int {
        val r = getRawR(colA) + getRawR(colB) shr 1
        val g = getRawG(colA) + getRawG(colB) shr 1
        val b = getRawB(colA) + getRawB(colB) shr 1
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
            x !in 0..lightMapMSB!![0].size - 1 || y !in 0..lightMapMSB!!.size - 1

    private fun clampZero(i: Int): Int = if (i < 0) 0 else i

    private fun clampZero(i: Float): Float = if (i < 0) 0f else i

    private fun clampByte(i: Int): Int = if (i < 0) 0 else if (i > CHANNEL_MAX) CHANNEL_MAX else i

    private fun clampFloat(i: Float): Float = if (i < 0) 0f else if (i > 1) 1f else i

    fun getValueFromMap(x: Int, y: Int): Int? = getLight(x, y)

    private fun purgePartOfLightmap(x1: Int, y1: Int, x2: Int, y2: Int) {
        try {
            for (y in y1 - 1..y2 + 1) {
                for (x in x1 - 1..x2 + 1) {
                    if (y == y1 - 1 || y == y2 + 1 || x == x1 - 1 || x == x2 + 1) {
                        // fill the rim with (pre) calculation
                        setLight(x, y, preCalculateUpdateGLOnly(x, y))
                    }
                    else {
                        setLight(x, y, 0)
                    }
                }
            }
        }
        catch (e: ArrayIndexOutOfBoundsException) {
        }

    }

    private fun preCalculateUpdateGLOnly(x: Int, y: Int): Int {
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
            lightLevelThis = maximiseRGB(darkenSunlight, thisTileLuminosity)
        }
        // opaque wall and luminous tile
        else if (thisWall != AIR && thisTileLuminosity.toInt() > 0) {
            lightLevelThis = thisTileLuminosity
        }
        // END MIX TILE

        // mix lantern
        for (lantern in lanterns) {
            if (lantern.x == x && lantern.y == y) {
                lightLevelThis = screenBlend(lightLevelThis, lantern.intensity)
                break
            }
        }

        // mix luminous actor
        for (actor in Terrarum.game.actorContainer) {
            if (actor is Luminous && actor is ActorWithBody) {
                val tileX = Math.round(actor.hitbox!!.pointedX / TSIZE)
                val tileY = Math.round(actor.hitbox!!.pointedY / TSIZE) - 1
                val actorLuminosity = actor.luminosity
                if (x == tileX && y == tileY) {
                    lightLevelThis = screenBlend(lightLevelThis, actorLuminosity)
                }
            }
        }

        return lightLevelThis
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
}
