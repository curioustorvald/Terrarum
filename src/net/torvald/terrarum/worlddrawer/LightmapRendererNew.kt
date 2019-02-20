package net.torvald.terrarum.worlddrawer

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.jme3.math.FastMath
import net.torvald.terrarum.*
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.blockproperties.Fluid
import net.torvald.terrarum.concurrent.ParallelUtils.sliceEvenly
import net.torvald.terrarum.gameactors.ActorWBMovable
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.Luminous
import net.torvald.terrarum.gameworld.BlockAddress
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.IngameRenderer
import net.torvald.terrarum.realestate.LandUtil

/**
 * Sub-portion of IngameRenderer. You are not supposed to directly deal with this.
 *
 * Created by minjaesong on 2016-01-25.
 */

//typealias RGB10 = Int

// NOTE: no Float16 on this thing: 67 kB of memory footage is totally acceptable

/** This object should not be called by yourself; must be only being used and manipulated by your
 * own ingame renderer
 */
object LightmapRenderer {
    private const val TILE_SIZE = FeaturesDrawer.TILE_SIZE

    private var world: GameWorld = GameWorld.makeNullWorld()
    private lateinit var lightCalcShader: ShaderProgram
    //private val SHADER_LIGHTING = AppLoader.getConfigBoolean("gpulightcalc")

    /** do not call this yourself! Let your game renderer handle this! */
    fun setWorld(world: GameWorld) {
        try {
            if (this.world != world) {
                printdbg(this, "World change detected -- old world: ${this.world.hashCode()}, new world: ${world.hashCode()}")

                /*for (y in 0 until LIGHTMAP_HEIGHT) {
                    for (x in 0 until LIGHTMAP_WIDTH) {
                        lightmap[y][x] = colourNull
                    }
                }*/

                for (i in 0 until lightmap.size) {
                    lightmap[i] = colourNull
                }

                makeUpdateTaskList()
            }
        }
        catch (e: UninitializedPropertyAccessException) {
            // new init, do nothing
        }
        finally {
            this.world = world
        }
    }

    val overscan_open: Int = 40
    val overscan_opaque: Int = 10


    // TODO resize(int, int) -aware

    val LIGHTMAP_WIDTH = (Terrarum.ingame?.ZOOM_MINIMUM ?: 1f).inv().times(Terrarum.WIDTH).div(TILE_SIZE).ceil() + overscan_open * 2 + 3
    val LIGHTMAP_HEIGHT = (Terrarum.ingame?.ZOOM_MINIMUM ?: 1f).inv().times(Terrarum.HEIGHT).div(TILE_SIZE).ceil() + overscan_open * 2 + 3

    val noopMask = HashSet<Point2i>((LIGHTMAP_WIDTH + LIGHTMAP_HEIGHT) * 2)

    /**
     * Float value, 1.0 for 1023
     */
    // it utilises alpha channel to determine brightness of "glow" sprites (so that alpha channel works like UV light)
    //private val lightmap: Array<Array<Color>> = Array(LIGHTMAP_HEIGHT) { Array(LIGHTMAP_WIDTH, { Color(0f,0f,0f,0f) }) } // Can't use framebuffer/pixmap -- this is a fvec4 array, whereas they are ivec4.
    private val lightmap: Array<Color> = Array(LIGHTMAP_WIDTH * LIGHTMAP_HEIGHT) { Color(0f,0f,0f,0f) } // Can't use framebuffer/pixmap -- this is a fvec4 array, whereas they are ivec4.
    private val lanternMap = HashMap<BlockAddress, Color>((Terrarum.ingame?.ACTORCONTAINER_INITIAL_SIZE ?: 2) * 4)

    init {
        printdbg(this, "Overscan open: $overscan_open; opaque: $overscan_opaque")
    }

    private val AIR = Block.AIR

    val DRAW_TILE_SIZE: Float = FeaturesDrawer.TILE_SIZE / IngameRenderer.lightmapDownsample

    // color model related constants
    const val MUL = 1024 // modify this to 1024 to implement 30-bit RGB
    const val CHANNEL_MAX_DECIMAL = 1f
    const val MUL_2 = MUL * MUL
    const val CHANNEL_MAX = MUL - 1
    const val CHANNEL_MAX_FLOAT = CHANNEL_MAX.toFloat()
    const val COLOUR_RANGE_SIZE = MUL * MUL_2
    const val MUL_FLOAT = MUL / 256f
    const val DIV_FLOAT = 256f / MUL

    internal var for_x_start = 0
    internal var for_y_start = 0
    internal var for_x_end = 0
    internal var for_y_end = 0
    internal var for_x = 0
    internal var for_y = 0


    //inline fun getLightRawPos(x: Int, y: Int) = lightmap[y][x]


    /**
     * Conventional level (multiplied by four)
     *
     * @param x world tile coord
     * @param y world tile coord
     */
    internal fun getLight(x: Int, y: Int): Color? {
        val col = getLightInternal(x, y)
        if (col == null) {
            return null
        }
        else {
            return Color(col.r * MUL_FLOAT, col.g * MUL_FLOAT, col.b * MUL_FLOAT, col.a * MUL_FLOAT)
        }
    }

    /**
     * Internal level (0..1)
     *
     * @param x world tile coord
     * @param y world tile coord
     */
    // TODO in regard of "colour math against integers", return Int?
    private fun getLightInternal(x: Int, y: Int): Color? {
        if (y - for_y_start + overscan_open in 0 until LIGHTMAP_HEIGHT &&
            x - for_x_start + overscan_open in 0 until LIGHTMAP_WIDTH) {

            val ypos = y - for_y_start + overscan_open
            val xpos = x - for_x_start + overscan_open

            //return lightmap[ypos][xpos]
            return lightmap[ypos * LIGHTMAP_WIDTH + xpos]
        }

        return null
    }


    /**
     * Converts world coord (x,y) into the lightmap index, and stores the input colour into the given list
     * with given applyFun applied.
     *
     * Default 'applyFun' is simply storing given colour into the array.
     *
     * @param list The lightmap
     * @param x World X coordinate
     * @param y World Y coordinate
     * @param colour Color to write
     * @param applyFun A function ```foo(old_colour, given_colour)```
     */
    private fun setLightOf(list: Array<Color>, x: Int, y: Int, colour: Color, applyFun: (Color, Color) -> Color = { _, c -> c }) {
        if (y - for_y_start + overscan_open in 0 until LIGHTMAP_HEIGHT &&
            x - for_x_start + overscan_open in 0 until LIGHTMAP_WIDTH) {

            val ypos = y - for_y_start + overscan_open
            val xpos = x - for_x_start + overscan_open

            //lightmap[ypos][xpos] = applyFun.invoke(list[ypos][xpos], colour)
            list[ypos * LIGHTMAP_WIDTH + xpos] = applyFun.invoke(list[ypos * LIGHTMAP_WIDTH + xpos], colour)
        }
    }

    internal fun fireRecalculateEvent(vararg actorContainers: List<ActorWithBody>?) {
        try {
            world.getTileFromTerrain(0, 0) // test inquiry
        }
        catch (e: UninitializedPropertyAccessException) {
            return // quit prematurely
        }


        for_x_start = WorldCamera.x / TILE_SIZE // fix for premature lightmap rendering
        for_y_start = WorldCamera.y / TILE_SIZE // on topmost/leftmost side

        if (for_x_start < 0) for_x_start -= 1 // to fix that the light shifts 1 tile to the left when WorldCamera < 0
        //if (for_y_start < 0) for_y_start -= 1 // not needed when we only wrap at x axis

        if (WorldCamera.x in -(TILE_SIZE - 1)..-1) for_x_start -= 1 // another edge-case fix

        for_x_end = for_x_start + WorldCamera.width / TILE_SIZE + 3
        for_y_end = for_y_start + WorldCamera.height / TILE_SIZE + 3 // same fix as above

        for_x = for_x_start + (for_x_end - for_x_start) / 2
        for_y = for_y_start + (for_y_end - for_y_start) / 2

        //println("$for_x_start..$for_x_end, $for_x\t$for_y_start..$for_y_end, $for_y")

        AppLoader.measureDebugTime("Renderer.Lanterns") {
            buildLanternmap(actorContainers)
        } // usually takes 3000 ns

        /*
         * Updating order:
         * ,--------.   ,--+-----.   ,-----+--.   ,--------. -
         * |↘       |   |  |    3|   |3    |  |   |       ↙| ↕︎ overscan_open / overscan_opaque
         * |  ,-----+   |  |  2  |   |  2  |  |   +-----.  | - depending on the noop_mask
         * |  |1    |   |  |1    |   |    1|  |   |    1|  |
         * |  |  2  |   |  `-----+   +-----'  |   |  2  |  |
         * |  |    3|   |↗       |   |       ↖|   |3    |  |
         * `--+-----'   `--------'   `--------'   `-----+--'
         * round:   1            2            3            4
         * for all lightmap[y][x], run in this order: 2-3-4-1
         *     If you run only 4 sets, orthogonal/diagonal artefacts are bound to occur,
         */

        // set sunlight
        sunLight.set(world.globalLight); sunLight.mul(DIV_FLOAT)

        // set no-op mask from solidity of the block
        AppLoader.measureDebugTime("Renderer.LightNoOpMask") {
            noopMask.clear()
            buildNoopMask()
        }

        // wipe out lightmap
        AppLoader.measureDebugTime("Renderer.Light0") {
            for (k in 0 until lightmap.size) lightmap[k] = colourNull
            // when disabled, light will "decay out" instead of "instantly out", which can have a cool effect
            // but the performance boost is measly 0.1 ms on 6700K
        }
        // O((5*9)n) == O(n) where n is a size of the map.
        // Because of inevitable overlaps on the area, it only works with MAX blend


        // each usually takes 8 000 000..12 000 000 miliseconds total when not threaded

        if (!AppLoader.getConfigBoolean("multithreadedlight")) {

            // The skipping is dependent on how you get ambient light,
            // in this case we have 'spillage' due to the fact calculate() samples 3x3 area.

            AppLoader.measureDebugTime("Renderer.LightTotal") {
                // Round 2
                for (y in for_y_end + overscan_open downTo for_y_start) {
                    for (x in for_x_start - overscan_open..for_x_end) {
                        calculateAndAssign(lightmap, x, y)
                    }
                }
                // Round 3
                for (y in for_y_end + overscan_open downTo for_y_start) {
                    for (x in for_x_end + overscan_open downTo for_x_start) {
                        calculateAndAssign(lightmap, x, y)
                    }
                }
                // Round 4
                for (y in for_y_start - overscan_open..for_y_end) {
                    for (x in for_x_end + overscan_open downTo for_x_start) {
                        calculateAndAssign(lightmap, x, y)
                    }
                }
                // Round 1
                for (y in for_y_start - overscan_open..for_y_end) {
                    for (x in for_x_start - overscan_open..for_x_end) {
                        calculateAndAssign(lightmap, x, y)
                    }
                }
                // Round 2 again
                /*for (y in for_y_end + overscan_open downTo for_y_start) {
                    for (x in for_x_start - overscan_open..for_x_end) {
                        calculateAndAssign(lightmap, x, y)
                    }
                }*/
            }
        }
        else if (world.worldIndex != -1) { // to avoid updating on the null world
            TODO()
        }
    }




    // TODO re-init at every resize
    private lateinit var updateMessages: List<Array<ThreadedLightmapUpdateMessage>>

    private fun makeUpdateTaskList() {
        val lightTaskArr = ArrayList<ThreadedLightmapUpdateMessage>()

        val for_x_start = overscan_open
        val for_y_start = overscan_open
        val for_x_end = for_x_start + WorldCamera.width / TILE_SIZE + 3
        val for_y_end = for_y_start + WorldCamera.height / TILE_SIZE + 3 // same fix as above

        // Round 2
        for (y in for_y_end + overscan_open downTo for_y_start) {
            for (x in for_x_start - overscan_open..for_x_end) {
                lightTaskArr.add(ThreadedLightmapUpdateMessage(x, y))
            }
        }

        // Round 3
        for (y in for_y_end + overscan_open downTo for_y_start) {
            for (x in for_x_end + overscan_open downTo for_x_start) {
                lightTaskArr.add(ThreadedLightmapUpdateMessage(x, y))
            }
        }

        // Round 4
        for (y in for_y_start - overscan_open..for_y_end) {
            for (x in for_x_end + overscan_open downTo for_x_start) {
                lightTaskArr.add(ThreadedLightmapUpdateMessage(x, y))
            }
        }

        // Round 1
        for (y in for_y_start - overscan_open..for_y_end) {
            for (x in for_x_start - overscan_open..for_x_end) {
                lightTaskArr.add(ThreadedLightmapUpdateMessage(x, y))
            }
        }

        updateMessages = lightTaskArr.toTypedArray().sliceEvenly(Terrarum.THREADS)
    }

    internal data class ThreadedLightmapUpdateMessage(val x: Int, val y: Int)


    private fun buildLanternmap(actorContainers: Array<out List<ActorWithBody>?>) {
        lanternMap.clear()
        actorContainers.forEach { actorContainer ->
            actorContainer?.forEach {
                if (it is Luminous && it is ActorWBMovable) {
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

                                val normalisedColor = it.color//.cpy().mul(DIV_FLOAT)

                                lanternMap[LandUtil.getBlockAddr(world, x, y)] = normalisedColor
                                //lanternMap[Point2i(x, y)] = normalisedColor
                                // Q&D fix for Roundworld anomaly
                                //lanternMap[Point2i(x + world.width, y)] = normalisedColor
                                //lanternMap[Point2i(x - world.width, y)] = normalisedColor
                            }
                        }
                    }
                }
            }
        }
    }

    private fun buildNoopMask() {
        fun isShaded(x: Int, y: Int) = BlockCodex[world.getTileFromTerrain(x, y) ?: Block.STONE].isSolid

        /*
        update ordering: clockwise snake

         for_x_start
         |
         02468>..............|--for_y_start
         :                   :
         :                   :
         :                   :
         V                   V
         13579>............../--for_y_end
                             |
                     for_x_end

         */

        for (x in for_x_start..for_x_end) {
            if (isShaded(x, for_y_start)) noopMask.add(Point2i(x, for_y_start))
            if (isShaded(x, for_y_end)) noopMask.add(Point2i(x, for_y_end))
        }
        for (y in for_y_start + 1..for_y_end - 1) {
            if (isShaded(for_x_start, y)) noopMask.add(Point2i(for_x_start, y))
            if (isShaded(for_x_end, y)) noopMask.add(Point2i(for_x_end, y))
        }

    }


    //private val ambientAccumulator = Color(0f,0f,0f,0f)
    private val lightLevelThis = Color(0)
    private var thisTerrain = 0
    private var thisFluid = GameWorld.FluidInfo(Fluid.NULL, 0f)
    private val fluidAmountToCol = Color(0)
    private var thisWall = 0
    private val thisTileLuminosity = Color(0)
    private val thisTileOpacity = Color(0)
    private val thisTileOpacity2 = Color(0) // thisTileOpacity * sqrt(2)
    private val sunLight = Color(0)

    /**
     * This function will alter following variables:
     * - lightLevelThis
     * - thisTerrain
     * - thisFluid
     * - thisWall
     * - thisTileLuminosity
     * - thisTileOpacity
     * - thisTileOpacity2
     * - sunlight
     */
    private fun getLightsAndShades(x: Int, y: Int) {
        lightLevelThis.set(colourNull)
        thisTerrain = world.getTileFromTerrain(x, y) ?: Block.STONE
        thisFluid = world.getFluid(x, y)
        thisWall = world.getTileFromWall(x, y) ?: Block.STONE

        if (thisFluid.type != Fluid.NULL) {
            fluidAmountToCol.set(thisFluid.amount, thisFluid.amount, thisFluid.amount, thisFluid.amount)

            thisTileLuminosity.set(BlockCodex[thisTerrain].luminosity)
            thisTileLuminosity.maxAndAssign(BlockCodex[thisFluid.type].luminosity mul fluidAmountToCol) // already been div by four
            thisTileOpacity.set(BlockCodex[thisTerrain].opacity)
            thisTileOpacity.maxAndAssign(BlockCodex[thisFluid.type].opacity mul fluidAmountToCol) // already been div by four
        }
        else {
            thisTileLuminosity.set(BlockCodex[thisTerrain].luminosity)
            thisTileOpacity.set(BlockCodex[thisTerrain].opacity)
        }

        thisTileOpacity2.set(thisTileOpacity); thisTileOpacity2.mul(1.41421356f)
        //sunLight.set(world.globalLight); sunLight.mul(DIV_FLOAT) // moved to fireRecalculateEvent()


        // open air || luminous tile backed by sunlight
        if ((thisTerrain == AIR && thisWall == AIR) || (thisTileLuminosity.nonZero() && thisWall == AIR)) {
            lightLevelThis.set(sunLight)
        }

        // blend lantern
        lightLevelThis.maxAndAssign(thisTileLuminosity).maxAndAssign(lanternMap[LandUtil.getBlockAddr(world, x, y)] ?: colourNull)

    }

    private val inNoopMaskp = Point2i(0,0)

    private fun inNoopMask(x: Int, y: Int): Boolean {

        // TODO: digitise your note of the idea of No-op Mask (date unknown, prob before 2017-03-17)
        if (x in for_x_start..for_x_end) {
            // if it's in the top flange
            inNoopMaskp.set(x, for_y_start)
            if (y < for_y_start - overscan_opaque && noopMask.contains(inNoopMaskp)) return true
            // if it's in the bottom flange
            inNoopMaskp.y = for_y_end
            return (y > for_y_end + overscan_opaque && noopMask.contains(inNoopMaskp))
        }
        else if (y in for_y_start..for_y_end) {
            // if it's in the left flange
            inNoopMaskp.set(for_x_start, y)
            if (x < for_x_start - overscan_opaque && noopMask.contains(inNoopMaskp)) return true
            // if it's in the right flange
            inNoopMaskp.set(for_x_end, y)
            return (x > for_x_end + overscan_opaque && noopMask.contains(inNoopMaskp))
        }
        // top-left corner
        else if (x < for_x_start && y < for_y_start) {
            inNoopMaskp.set(for_x_start, for_y_start)
            return (x < for_x_start - overscan_opaque && y < for_y_start - overscan_opaque && noopMask.contains(inNoopMaskp))
        }
        // top-right corner
        else if (x > for_x_end && y < for_y_start) {
            inNoopMaskp.set(for_x_end, for_y_start)
            return (x > for_x_end + overscan_opaque && y < for_y_start - overscan_opaque && noopMask.contains(inNoopMaskp))
        }
        // bottom-left corner
        else if (x < for_x_start && y > for_y_end) {
            inNoopMaskp.set(for_x_start, for_y_end)
            return (x < for_x_start - overscan_opaque && y > for_y_end + overscan_opaque && noopMask.contains(inNoopMaskp))
        }
        // bottom-right corner
        else if (x > for_x_end && y > for_y_end) {
            inNoopMaskp.set(for_x_end, for_y_end)
            return (x > for_x_end + overscan_opaque && y > for_y_end + overscan_opaque && noopMask.contains(inNoopMaskp))
        }
        else
            return false

        // if your IDE error out that you need return statement, AND it's "fixed" by removing 'else' before 'return false',
        // you're doing it wrong, the IF and return statements must be inclusive.
    }

    /**
     * Calculates the light simulation, using main lightmap as one of the input.
     */
    private fun calculateAndAssign(lightmap: Array<Color>, x: Int, y: Int) {

        if (inNoopMask(x, y)) return

        // O(9n) == O(n) where n is a size of the map

        getLightsAndShades(x, y)

        // calculate ambient
        /*  + * +  0 4 1
         *  * @ *  6 @ 7
         *  + * +  2 5 3
         *  sample ambient for eight points and apply attenuation for those
         *  maxblend eight values and use it
         */

        // will "overwrite" what's there in the lightmap if it's the first pass
        // takes about 2 ms on 6700K
        /* + */lightLevelThis.maxAndAssign(darkenColoured(getLightInternal(x - 1, y - 1) ?: colourNull, thisTileOpacity2))
        /* + */lightLevelThis.maxAndAssign(darkenColoured(getLightInternal(x + 1, y - 1) ?: colourNull, thisTileOpacity2))
        /* + */lightLevelThis.maxAndAssign(darkenColoured(getLightInternal(x - 1, y + 1) ?: colourNull, thisTileOpacity2))
        /* + */lightLevelThis.maxAndAssign(darkenColoured(getLightInternal(x + 1, y + 1) ?: colourNull, thisTileOpacity2))
        /* * */lightLevelThis.maxAndAssign(darkenColoured(getLightInternal(x, y - 1) ?: colourNull, thisTileOpacity))
        /* * */lightLevelThis.maxAndAssign(darkenColoured(getLightInternal(x, y + 1) ?: colourNull, thisTileOpacity))
        /* * */lightLevelThis.maxAndAssign(darkenColoured(getLightInternal(x - 1, y) ?: colourNull, thisTileOpacity))
        /* * */lightLevelThis.maxAndAssign(darkenColoured(getLightInternal(x + 1, y) ?: colourNull, thisTileOpacity))


        //return lightLevelThis.cpy() // it HAS to be a cpy(), otherwise all cells gets the same instance
        setLightOf(lightmap, x, y, lightLevelThis.cpy())
    }

    private fun getLightForOpaque(x: Int, y: Int): Color? { // ...so that they wouldn't appear too dark
        val l = getLightInternal(x, y)
        if (l == null) return null

        // brighten if solid
        if (BlockCodex[world.getTileFromTerrain(x, y)].isSolid) {
            return Color(
                    (l.r * 1.2f),
                    (l.g * 1.2f),
                    (l.b * 1.2f),
                    (l.a * 1.2f)
            )
        }
        else {
            return l
        }
    }

    var lightBuffer: Pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)

    private val colourNull = Color(0)
    private val epsilon = 1f/1024f

    private var _lightBufferAsTex: Texture = Texture(1, 1, Pixmap.Format.RGBA8888)

    internal fun draw(): Texture {

        // when shader is not used: 0.5 ms on 6700K
        AppLoader.measureDebugTime("Renderer.LightToScreen") {

            val this_x_start = for_x_start// + overscan_open
            val this_x_end = for_x_end// + overscan_open
            val this_y_start = for_y_start// + overscan_open
            val this_y_end = for_y_end// + overscan_open

            // wipe out beforehand. You DO need this
            lightBuffer.blending = Pixmap.Blending.None // gonna overwrite (remove this line causes the world to go bit darker)
            lightBuffer.setColor(colourNull)
            lightBuffer.fill()


            // write to colour buffer
            for (y in this_y_start..this_y_end) {
                //println("y: $y, this_y_start: $this_y_start")
                if (y == this_y_start && this_y_start == 0) {
                    //throw Error("Fuck hits again...")
                }

                for (x in this_x_start..this_x_end) {

                    val color = (getLightForOpaque(x, y) ?: Color(0f, 0f, 0f, 0f)).normaliseToHDR()

                    lightBuffer.setColor(color)

                    //lightBuffer.drawPixel(x - this_x_start, y - this_y_start)

                    lightBuffer.drawPixel(x - this_x_start, lightBuffer.height - 1 - y + this_y_start) // flip Y
                }
            }


            // draw to the batch
            _lightBufferAsTex.dispose()
            _lightBufferAsTex = Texture(lightBuffer)
            _lightBufferAsTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

            /*Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0) // so that batch that comes next will bind any tex to it
            //      we might not need shader here...
            //batch.draw(lightBufferAsTex, 0f, 0f, lightBufferAsTex.width.toFloat(), lightBufferAsTex.height.toFloat())
            batch.draw(_lightBufferAsTex, 0f, 0f, _lightBufferAsTex.width * DRAW_TILE_SIZE, _lightBufferAsTex.height * DRAW_TILE_SIZE)
            */
        }

        return _lightBufferAsTex
    }

    fun dispose() {

    }

    val lightScalingMagic = 8f

    /**
     * Subtract each channel's RGB value.
     *
     * @param data Raw channel value (0-255) per channel
     * @param darken (0-255) per channel
     * @return darkened data (0-255) per channel
     */
    fun darkenColoured(data: Color, darken: Color): Color {
        // use equation with magic number 8.0
        // should draw somewhat exponential curve when you plot the propagation of light in-game

        return Color(
                data.r * (1f - darken.r * lightScalingMagic),//.clampZero(),
                data.g * (1f - darken.g * lightScalingMagic),//.clampZero(),
                data.b * (1f - darken.b * lightScalingMagic),//.clampZero(),
                data.a * (1f - darken.a * lightScalingMagic))
    }

    /**
     * Darken each channel by 'darken' argument
     *
     * @param data Raw channel value (0-255) per channel
     * @param darken (0-255)
     * @return
     */
    fun darkenUniformInt(data: Color, darken: Float): Color {
        if (darken < 0 || darken > CHANNEL_MAX)
            throw IllegalArgumentException("darken: out of range ($darken)")

        val darkenColoured = Color(darken, darken, darken, darken)
        return darkenColoured(data, darkenColoured)
    }

    /**
     * Darken or brighten colour by 'brighten' argument
     *
     * @param data Raw channel value (0-255) per channel
     * @param brighten (-1.0 - 1.0) negative means darkening
     * @return processed colour
     */
    fun alterBrightnessUniform(data: Color, brighten: Float): Color {
        return Color(
                data.r + brighten,
                data.g + brighten,
                data.b + brighten,
                data.a + brighten
        )
    }

    /** infix is removed to clarify the association direction */
    fun Color.maxAndAssign(other: Color): Color {
        this.set(
                if (this.r > other.r) this.r else other.r,
                if (this.g > other.g) this.g else other.g,
                if (this.b > other.b) this.b else other.b,
                if (this.a > other.a) this.a else other.a
        )

        return this
    }

    private fun Float.inv() = 1f / this
    fun Float.floor() = FastMath.floor(this)
    fun Double.floorInt() = Math.floor(this).toInt()
    fun Float.round(): Int = Math.round(this)
    fun Double.round(): Int = Math.round(this).toInt()
    fun Float.ceil() = FastMath.ceil(this)
    fun Int.even(): Boolean = this and 1 == 0
    fun Int.odd(): Boolean = this and 1 == 1

    // TODO: float LUT lookup using linear interpolation

    // input: 0..1 for int 0..1023
    fun hdr(intensity: Float): Float {
        val intervalStart = (intensity * CHANNEL_MAX).floorInt()
        val intervalEnd = minOf(rgbHDRLookupTable.lastIndex, (intensity * CHANNEL_MAX).floorInt() + 1)

        if (intervalStart == intervalEnd) return rgbHDRLookupTable[intervalStart]

        val intervalPos = (intensity * CHANNEL_MAX) - (intensity * CHANNEL_MAX).toInt()

        val ret = interpolateLinear(
                intervalPos,
                rgbHDRLookupTable[intervalStart],
                rgbHDRLookupTable[intervalEnd]
        )

        return ret
    }

    private var _init = false

    fun resize(screenW: Int, screenH: Int) {
        // make sure the BlocksDrawer is resized first!

        // copied from BlocksDrawer, duh!
        // FIXME 'lightBuffer' is not zoomable in this way
        val tilesInHorizontal = (screenW.toFloat() / TILE_SIZE).ceilInt() + 1
        val tilesInVertical = (screenH.toFloat() / TILE_SIZE).ceilInt() + 1

        if (_init) {
            lightBuffer.dispose()
        }
        else {
            _init = true
        }
        lightBuffer = Pixmap(tilesInHorizontal, tilesInVertical, Pixmap.Format.RGBA8888)


        printdbg(this, "Resize event")
    }


    val rgbHDRLookupTable = floatArrayOf( // polynomial of 6.0   please refer to work_files/HDRcurveBezierLinIntp.kts
            0.0000f,0.0004f,0.0020f,0.0060f,0.0100f,0.0139f,0.0179f,0.0219f,0.0259f,0.0299f,0.0338f,0.0378f,0.0418f,0.0458f,0.0497f,0.0537f,
            0.0577f,0.0617f,0.0656f,0.0696f,0.0736f,0.0776f,0.0816f,0.0855f,0.0895f,0.0935f,0.0975f,0.1014f,0.1054f,0.1094f,0.1134f,0.1173f,
            0.1213f,0.1253f,0.1293f,0.1332f,0.1372f,0.1412f,0.1451f,0.1491f,0.1531f,0.1571f,0.1610f,0.1650f,0.1690f,0.1730f,0.1769f,0.1809f,
            0.1849f,0.1888f,0.1928f,0.1968f,0.2007f,0.2047f,0.2087f,0.2127f,0.2166f,0.2206f,0.2246f,0.2285f,0.2325f,0.2365f,0.2404f,0.2444f,
            0.2484f,0.2523f,0.2563f,0.2602f,0.2642f,0.2682f,0.2721f,0.2761f,0.2800f,0.2840f,0.2880f,0.2919f,0.2959f,0.2998f,0.3038f,0.3078f,
            0.3117f,0.3157f,0.3196f,0.3236f,0.3275f,0.3315f,0.3354f,0.3394f,0.3433f,0.3472f,0.3512f,0.3551f,0.3591f,0.3630f,0.3669f,0.3709f,
            0.3748f,0.3788f,0.3827f,0.3866f,0.3905f,0.3945f,0.3984f,0.4023f,0.4062f,0.4101f,0.4141f,0.4180f,0.4219f,0.4258f,0.4297f,0.4336f,
            0.4375f,0.4414f,0.4453f,0.4491f,0.4530f,0.4569f,0.4608f,0.4647f,0.4685f,0.4724f,0.4762f,0.4801f,0.4839f,0.4878f,0.4916f,0.4954f,
            0.4993f,0.5031f,0.5069f,0.5107f,0.5145f,0.5183f,0.5220f,0.5258f,0.5296f,0.5333f,0.5371f,0.5408f,0.5445f,0.5482f,0.5520f,0.5556f,
            0.5593f,0.5630f,0.5667f,0.5703f,0.5739f,0.5776f,0.5812f,0.5848f,0.5883f,0.5919f,0.5955f,0.5990f,0.6025f,0.6060f,0.6095f,0.6130f,
            0.6164f,0.6199f,0.6233f,0.6267f,0.6300f,0.6334f,0.6367f,0.6401f,0.6433f,0.6466f,0.6499f,0.6531f,0.6563f,0.6595f,0.6627f,0.6658f,
            0.6689f,0.6720f,0.6751f,0.6781f,0.6811f,0.6841f,0.6871f,0.6901f,0.6930f,0.6959f,0.6987f,0.7016f,0.7044f,0.7072f,0.7100f,0.7127f,
            0.7154f,0.7181f,0.7208f,0.7234f,0.7260f,0.7286f,0.7311f,0.7337f,0.7362f,0.7386f,0.7411f,0.7435f,0.7459f,0.7483f,0.7506f,0.7530f,
            0.7553f,0.7575f,0.7598f,0.7620f,0.7642f,0.7664f,0.7685f,0.7706f,0.7727f,0.7748f,0.7769f,0.7789f,0.7809f,0.7829f,0.7849f,0.7868f,
            0.7887f,0.7906f,0.7925f,0.7944f,0.7962f,0.7980f,0.7998f,0.8016f,0.8033f,0.8051f,0.8068f,0.8085f,0.8101f,0.8118f,0.8134f,0.8150f,
            0.8166f,0.8182f,0.8198f,0.8213f,0.8229f,0.8244f,0.8259f,0.8274f,0.8288f,0.8303f,0.8317f,0.8331f,0.8345f,0.8359f,0.8373f,0.8386f,
            0.8400f,0.8413f,0.8426f,0.8439f,0.8452f,0.8465f,0.8477f,0.8490f,0.8502f,0.8514f,0.8526f,0.8538f,0.8550f,0.8562f,0.8573f,0.8585f,
            0.8596f,0.8608f,0.8619f,0.8630f,0.8641f,0.8651f,0.8662f,0.8673f,0.8683f,0.8693f,0.8704f,0.8714f,0.8724f,0.8734f,0.8744f,0.8754f,
            0.8763f,0.8773f,0.8782f,0.8792f,0.8801f,0.8811f,0.8820f,0.8829f,0.8838f,0.8847f,0.8856f,0.8864f,0.8873f,0.8882f,0.8890f,0.8899f,
            0.8907f,0.8915f,0.8923f,0.8932f,0.8940f,0.8948f,0.8956f,0.8963f,0.8971f,0.8979f,0.8987f,0.8994f,0.9002f,0.9009f,0.9017f,0.9024f,
            0.9031f,0.9039f,0.9046f,0.9053f,0.9060f,0.9067f,0.9074f,0.9081f,0.9087f,0.9094f,0.9101f,0.9108f,0.9114f,0.9121f,0.9127f,0.9134f,
            0.9140f,0.9146f,0.9153f,0.9159f,0.9165f,0.9171f,0.9177f,0.9184f,0.9190f,0.9195f,0.9201f,0.9207f,0.9213f,0.9219f,0.9225f,0.9230f,
            0.9236f,0.9242f,0.9247f,0.9253f,0.9258f,0.9264f,0.9269f,0.9274f,0.9280f,0.9285f,0.9290f,0.9296f,0.9301f,0.9306f,0.9311f,0.9316f,
            0.9321f,0.9326f,0.9331f,0.9336f,0.9341f,0.9346f,0.9351f,0.9355f,0.9360f,0.9365f,0.9370f,0.9374f,0.9379f,0.9383f,0.9388f,0.9393f,
            0.9397f,0.9402f,0.9406f,0.9410f,0.9415f,0.9419f,0.9423f,0.9428f,0.9432f,0.9436f,0.9440f,0.9445f,0.9449f,0.9453f,0.9457f,0.9461f,
            0.9465f,0.9469f,0.9473f,0.9477f,0.9481f,0.9485f,0.9489f,0.9493f,0.9497f,0.9501f,0.9504f,0.9508f,0.9512f,0.9516f,0.9519f,0.9523f,
            0.9527f,0.9530f,0.9534f,0.9537f,0.9541f,0.9545f,0.9548f,0.9552f,0.9555f,0.9559f,0.9562f,0.9565f,0.9569f,0.9572f,0.9576f,0.9579f,
            0.9582f,0.9586f,0.9589f,0.9592f,0.9595f,0.9599f,0.9602f,0.9605f,0.9608f,0.9611f,0.9614f,0.9617f,0.9621f,0.9624f,0.9627f,0.9630f,
            0.9633f,0.9636f,0.9639f,0.9642f,0.9645f,0.9648f,0.9650f,0.9653f,0.9656f,0.9659f,0.9662f,0.9665f,0.9668f,0.9670f,0.9673f,0.9676f,
            0.9679f,0.9681f,0.9684f,0.9687f,0.9690f,0.9692f,0.9695f,0.9697f,0.9700f,0.9703f,0.9705f,0.9708f,0.9711f,0.9713f,0.9716f,0.9718f,
            0.9721f,0.9723f,0.9726f,0.9728f,0.9731f,0.9733f,0.9735f,0.9738f,0.9740f,0.9743f,0.9745f,0.9747f,0.9750f,0.9752f,0.9754f,0.9757f,
            0.9759f,0.9761f,0.9764f,0.9766f,0.9768f,0.9770f,0.9773f,0.9775f,0.9777f,0.9779f,0.9781f,0.9784f,0.9786f,0.9788f,0.9790f,0.9792f,
            0.9794f,0.9796f,0.9799f,0.9801f,0.9803f,0.9805f,0.9807f,0.9809f,0.9811f,0.9813f,0.9815f,0.9817f,0.9819f,0.9821f,0.9823f,0.9825f,
            0.9827f,0.9829f,0.9831f,0.9832f,0.9834f,0.9836f,0.9838f,0.9840f,0.9842f,0.9844f,0.9846f,0.9847f,0.9849f,0.9851f,0.9853f,0.9855f,
            0.9856f,0.9858f,0.9860f,0.9862f,0.9864f,0.9865f,0.9867f,0.9869f,0.9870f,0.9872f,0.9874f,0.9876f,0.9877f,0.9879f,0.9881f,0.9882f,
            0.9884f,0.9886f,0.9887f,0.9889f,0.9890f,0.9892f,0.9894f,0.9895f,0.9897f,0.9898f,0.9900f,0.9901f,0.9903f,0.9905f,0.9906f,0.9908f,
            0.9909f,0.9911f,0.9912f,0.9914f,0.9915f,0.9917f,0.9918f,0.9920f,0.9921f,0.9922f,0.9924f,0.9925f,0.9927f,0.9928f,0.9930f,0.9931f,
            0.9932f,0.9934f,0.9935f,0.9937f,0.9938f,0.9939f,0.9941f,0.9942f,0.9943f,0.9945f,0.9946f,0.9947f,0.9949f,0.9950f,0.9951f,0.9953f,
            0.9954f,0.9955f,0.9957f,0.9958f,0.9959f,0.9960f,0.9962f,0.9963f,0.9964f,0.9965f,0.9967f,0.9968f,0.9969f,0.9970f,0.9971f,0.9973f,
            0.9974f,0.9975f,0.9976f,0.9977f,0.9978f,0.9980f,0.9981f,0.9982f,0.9983f,0.9984f,0.9985f,0.9987f,0.9988f,0.9989f,0.9990f,0.9991f,
            0.9992f,0.9993f,0.9994f,0.9995f,0.9996f,0.9997f,0.9999f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,
            1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,
            1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,
            1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,
            1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,
            1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,
            1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,
            1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,
            1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,
            1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,
            1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,
            1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,
            1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,
            1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,
            1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,
            1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,
            1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,
            1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,
            1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,
            1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,
            1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,
            1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,
            1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,
            1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f,1.0000f  // isn't it beautiful?
    )
    /** To eliminated visible edge on the gradient when 255/1023 is exceeded */
    internal fun Color.normaliseToHDR() = Color(
            hdr(this.r.coerceIn(0f,1f)),
            hdr(this.g.coerceIn(0f,1f)),
            hdr(this.b.coerceIn(0f,1f)),
            hdr(this.a.coerceIn(0f,1f))
    )

    private fun Color.nonZero() = this.r + this.g + this.b + this.a > epsilon

    val histogram: Histogram
        get() {
            val reds = IntArray(MUL) // reds[intensity] ← counts
            val greens = IntArray(MUL) // do.
            val blues = IntArray(MUL) // do.
            val uvs = IntArray(MUL)
            val render_width = for_x_end - for_x_start
            val render_height = for_y_end - for_y_start
            // excluiding overscans; only reckon echo lights
            for (y in overscan_open..render_height + overscan_open + 1) {
                for (x in overscan_open..render_width + overscan_open + 1) {
                    try {
                        //val colour = lightmap[y][x]
                        val colour = lightmap[y * LIGHTMAP_WIDTH + x]
                        reds[minOf(CHANNEL_MAX, colour.r.times(MUL).floorInt())] += 1
                        greens[minOf(CHANNEL_MAX, colour.g.times(MUL).floorInt())] += 1
                        blues[minOf(CHANNEL_MAX, colour.b.times(MUL).floorInt())] += 1
                        uvs[minOf(CHANNEL_MAX, colour.a.times(MUL).floorInt())] += 1
                    }
                    catch (e: ArrayIndexOutOfBoundsException) { }
                }
            }
            return Histogram(reds, greens, blues, uvs)
        }

    class Histogram(val reds: IntArray, val greens: IntArray, val blues: IntArray, val uvs: IntArray) {

        val RED = 0
        val GREEN = 1
        val BLUE = 2
        val UV = 3

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
                UV    -> uvs
                else  -> throw IllegalArgumentException()
            }
        }
    }

    fun interpolateLinear(scale: Float, startValue: Float, endValue: Float): Float {
        if (startValue == endValue) {
            return startValue
        }
        if (scale <= 0f) {
            return startValue
        }
        if (scale >= 1f) {
            return endValue
        }
        return (1f - scale) * startValue + scale * endValue
    }
}

fun Color.toRGBA() = (255 * r).toInt() shl 24 or ((255 * g).toInt() shl 16) or ((255 * b).toInt() shl 8) or (255 * a).toInt()

