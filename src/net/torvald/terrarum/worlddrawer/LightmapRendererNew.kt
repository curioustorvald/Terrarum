package net.torvald.terrarum.worlddrawer

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import net.torvald.gdx.graphics.Cvec
import net.torvald.gdx.graphics.UnsafeCvecArray
import net.torvald.terrarum.*
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.blockproperties.Fluid
import net.torvald.terrarum.concurrent.ThreadExecutor
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.Luminous
import net.torvald.terrarum.gameworld.BlockAddress
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.IngameRenderer
import net.torvald.terrarum.modulebasegame.ui.abs
import net.torvald.terrarum.realestate.LandUtil
import kotlin.system.exitProcess

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
    private const val TILE_SIZE = CreateTileAtlas.TILE_SIZE

    /** World change is managed by IngameRenderer.setWorld() */
    private var world: GameWorld = GameWorld.makeNullWorld()

    //private lateinit var lightCalcShader: ShaderProgram
    //private val SHADER_LIGHTING = AppLoader.getConfigBoolean("gpulightcalc")

    /** do not call this yourself! Let your game renderer handle this! */
    internal fun internalSetWorld(world: GameWorld) {
        try {
            if (this.world != world) {
                printdbg(this, "World change detected -- old world: ${this.world.hashCode()}, new world: ${world.hashCode()}")

                lightmap.zerofill()
                _mapLightLevelThis.zerofill()
                _mapThisTileOpacity.zerofill()
                _mapThisTileOpacity2.zerofill()
            }
        }
        catch (e: UninitializedPropertyAccessException) {
            // new init, do nothing
        }
        finally {
            this.world = world

            // fireRecalculateEvent()
        }
    }

    const val overscan_open: Int = 40
    const val overscan_opaque: Int = 10

    private var LIGHTMAP_WIDTH: Int = (Terrarum.ingame?.ZOOM_MINIMUM ?: 1f).inv().times(AppLoader.screenW).div(TILE_SIZE).ceilInt() + overscan_open * 2 + 3
    private var LIGHTMAP_HEIGHT: Int = (Terrarum.ingame?.ZOOM_MINIMUM ?: 1f).inv().times(AppLoader.screenH).div(TILE_SIZE).ceilInt() + overscan_open * 2 + 3

    //private val noopMask = HashSet<Point2i>((LIGHTMAP_WIDTH + LIGHTMAP_HEIGHT) * 2)

    private val lanternMap = HashMap<BlockAddress, Cvec>((Terrarum.ingame?.ACTORCONTAINER_INITIAL_SIZE ?: 2) * 4)
    /**
     * Float value, 1.0 for 1023
     *
     * Note: using UnsafeCvecArray does not actually show great performance improvement
     */
    // it utilises alpha channel to determine brightness of "glow" sprites (so that alpha channel works like UV light)
    private var lightmap = UnsafeCvecArray(LIGHTMAP_WIDTH, LIGHTMAP_HEIGHT)
    private var _mapLightLevelThis = UnsafeCvecArray(LIGHTMAP_WIDTH, LIGHTMAP_HEIGHT)
    private var _mapThisTileOpacity = UnsafeCvecArray(LIGHTMAP_WIDTH, LIGHTMAP_HEIGHT)
    private var _mapThisTileOpacity2 = UnsafeCvecArray(LIGHTMAP_WIDTH, LIGHTMAP_HEIGHT)

    init {
        LightmapHDRMap.invoke()
        printdbg(this, "Overscan open: $overscan_open; opaque: $overscan_opaque")
    }

    private const val AIR = Block.AIR

    const val DRAW_TILE_SIZE: Float = CreateTileAtlas.TILE_SIZE / IngameRenderer.lightmapDownsample

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
    internal var for_draw_x_start = 0
    internal var for_draw_y_start = 0
    internal var for_draw_x_end = 0
    internal var for_draw_y_end = 0

    /**
     * @param x world coord
     * @param y world coord
     */
    private fun inBounds(x: Int, y: Int) =
            (y - for_y_start + overscan_open in 0 until LIGHTMAP_HEIGHT &&
             x - for_x_start + overscan_open in 0 until LIGHTMAP_WIDTH)
    /** World coord to array coord */
    private inline fun Int.convX() = this - for_x_start + overscan_open
    /** World coord to array coord */
    private inline fun Int.convY() = this - for_y_start + overscan_open

    /**
     * Conventional level (multiplied by four)
     *
     * @param x world tile coord
     * @param y world tile coord
     */
    internal fun getLight(x: Int, y: Int): Cvec? {
        return if (!inBounds(x, y)) {
            null
        }
        else {
            val x = x.convX()
            val y = y.convY()

            Cvec(
                    lightmap.getR(x, y) * MUL_FLOAT,
                    lightmap.getG(x, y) * MUL_FLOAT,
                    lightmap.getB(x, y) * MUL_FLOAT,
                    lightmap.getA(x, y) * MUL_FLOAT
            )
        }
    }

    internal fun fireRecalculateEvent(vararg actorContainers: List<ActorWithBody>?) {
        try {
            world.getTileFromTerrain(0, 0) // test inquiry
        }
        catch (e: UninitializedPropertyAccessException) {
            return // quit prematurely
        }
        catch (e: NullPointerException) {
            System.err.println("[LightmapRendererNew.fireRecalculateEvent] Attempted to refer destroyed unsafe array " +
                               "(${world.layerTerrain.ptr})")
            e.printStackTrace()
            return // something's wrong but we'll ignore it like a trustful AK
        }

        if (world.worldIndex == -1) return


        for_x_start = WorldCamera.zoomedX / TILE_SIZE // fix for premature lightmap rendering
        for_y_start = WorldCamera.zoomedY / TILE_SIZE // on topmost/leftmost side
        for_draw_x_start = WorldCamera.x / TILE_SIZE
        for_draw_y_start = WorldCamera.y / TILE_SIZE

        if (WorldCamera.x < 0) for_draw_x_start -= 1 // edge case fix that light shift 1 tile to the left when WorldCamera.x < 0
        //if (WorldCamera.x in -(TILE_SIZE - 1)..-1) for_draw_x_start -= 1 // another edge-case fix; we don't need this anymore?

        for_x_end = for_x_start + WorldCamera.zoomedWidth / TILE_SIZE + 3
        for_y_end = for_y_start + WorldCamera.zoomedHeight / TILE_SIZE + 3 // same fix as above
        for_draw_x_end = for_draw_x_start + WorldCamera.width / TILE_SIZE + 3
        for_draw_y_end = for_draw_y_start + WorldCamera.height / TILE_SIZE + 3

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
        /*AppLoader.measureDebugTime("Renderer.LightNoOpMask") {
            noopMask.clear()
            buildNoopMask()
        }*/

        // wipe out lightmap
        AppLoader.measureDebugTime("Renderer.LightPrecalc") {
            // when disabled, light will "decay out" instead of "instantly out", which can have a cool effect
            // but the performance boost is measly 0.1 ms on 6700K
            lightmap.zerofill()
            _mapLightLevelThis.zerofill()
            //lightsourceMap.clear()

            for (y in for_y_start - overscan_open..for_y_end + overscan_open) {
                for (x in for_x_start - overscan_open..for_x_end + overscan_open) {
                    precalculate(x, y)
                }
            }
        }

        // YE OLDE LIGHT UPDATER
        // O((5*9)n where n is a size of the map.
        // Because of inevitable overlaps on the area, it only works with MAX blend
        /*fun or1() {
            // Round 1
            for (y in for_y_start - overscan_open..for_y_end) {
                for (x in for_x_start - overscan_open..for_x_end) {
                    calculateAndAssign(lightmap, x, y)
                }
            }
        }
        fun or2() {
            // Round 2
            for (y in for_y_end + overscan_open downTo for_y_start) {
                for (x in for_x_start - overscan_open..for_x_end) {
                    calculateAndAssign(lightmap, x, y)
                }
            }
        }
        fun or3() {
            // Round 3
            for (y in for_y_end + overscan_open downTo for_y_start) {
                for (x in for_x_end + overscan_open downTo for_x_start) {
                    calculateAndAssign(lightmap, x, y)
                }
            }
        }
        fun or4() {
            // Round 4
            for (y in for_y_start - overscan_open..for_y_end) {
                for (x in for_x_end + overscan_open downTo for_x_start) {
                    calculateAndAssign(lightmap, x, y)
                }
            }
        }*/

        // 'NEWLIGHT2' LIGHT SWIPER
        // O((8*2)n) where n is a size of the map.
        fun r1() {
            // TODO test non-parallel
            swipeDiag = false
            for (line in 1 until LIGHTMAP_HEIGHT - 1) {
                swipeLight(
                        1, line,
                        LIGHTMAP_WIDTH - 2, line,
                        1, 0
                )
            }
        }
        fun r2() {
            // TODO test non-parallel
            swipeDiag = false
            for (line in 1 until LIGHTMAP_WIDTH - 1) {
                swipeLight(
                        line, 1,
                        line, LIGHTMAP_HEIGHT - 2,
                        0, 1
                )
            }
        }
        fun r3() {
            // TODO test non-parallel
            swipeDiag = true
            /* construct indices such that:
                  56789ABC
               4 1       w-2
               3 \---\---+
               2 \\···\··|
               1 \\\···\·|
               0 \\\\···\|
             h-2 \\\\\---\

             0   (1, h-2) -> (1, h-2)
             1   (1, h-2-1) -> (2, h-2)
             2   (1, h-2-2) -> (3, h-2)
             3   (1, h-2-3) -> (4, h-2)
             4   (1, 1) -> (5, h-2)

             5   (2, 1) -> (6, h-2)
             6   (3, 1) -> (7, h-2)
             7   (4, 1) -> (8, h-2)
             8   (5, 1) -> (w-2, h-2)

             9   (6, 1) -> (w-2, h-2-1)
             10  (7, 1) -> (w-2, h-2-2)
             11  (8, 1) -> (w-2, h-2-3)
             12  (w-2, 1) -> (w-2, 1)

             number of indices: internal_width + internal_height - 1
             */
            for (i in 0 until LIGHTMAP_WIDTH + LIGHTMAP_HEIGHT - 5) {
                swipeLight(
                        maxOf(1, i - LIGHTMAP_HEIGHT + 4), maxOf(1, LIGHTMAP_HEIGHT - 2 - i),
                        minOf(LIGHTMAP_WIDTH - 2, i + 1), minOf(LIGHTMAP_HEIGHT - 2, (LIGHTMAP_WIDTH + LIGHTMAP_HEIGHT - 5) - i),
                        1, 1
                )
            }
        }
        fun r4() {
            // TODO test non-parallel
            swipeDiag = true
            /*
                1       w-2
                /////---/
                ////···/|
                ///···/·|
                //···/··|
            h-2 /---/---+
            d:(1,-1)

             0  (1, 1) -> (1, 1)
             1  (1, 2) -> (2, 1)
             2  (1, 3) -> (3, 1)
             3  (1, 4) -> (4, 1)
             4  (1, h-2) -> (5, 1)
             5  (2, h-2) -> (6, 1)
             6  (3, h-2) -> (7, 1)
             7  (4, h-2) -> (8, 1)
             8  (5, h-2) -> (w-2, 1)
             9  (6, h-2) -> (w-2, 2)
            10  (7, h-2) -> (w-2, 3)
            11  (8, h-2) -> (w-2, 4)
            12  (w-2, h-2) -> (w-2, h-2)
             */
            for (i in 0 until LIGHTMAP_WIDTH + LIGHTMAP_HEIGHT - 5) {
                swipeLight(
                        maxOf(1, i - LIGHTMAP_HEIGHT + 4), minOf(LIGHTMAP_HEIGHT - 2, i + 1),
                        minOf(LIGHTMAP_WIDTH - 2, i + 1), maxOf(1, (LIGHTMAP_HEIGHT - 2) - (LIGHTMAP_WIDTH + LIGHTMAP_HEIGHT - 6) + i),
                        1, -1
                )
            }
        }


        // each usually takes 8..12 ms total when not threaded
        // - with direct memory access of world array and pre-calculating things in the start of the frame,
        //      I was able to pull out 3.5..5.5 ms! With abhorrently many occurrences of segfaults I had to track down...
        // - with 'NEWLIGHT2', I was able to pull ~2 ms!
        //
        // multithreading - forget about it; overhead is way too big and for some reason i was not able to
        //      resolve the 'noisy shit' artefact
        AppLoader.measureDebugTime("Renderer.LightRuns") {

            // To save you from pains:
            // - Per-channel light updating is actually slower
            // BELOW NOTES DOES NOT APPLY TO 'NEWLIGHT2' LIGHT SWIPER
            // - It seems 5-pass lighting is needed to resonably eliminate the dark spot (of which I have zero idea
            //      why dark spots appear in the first place)
            // - Multithreading? I have absolutely no idea.
            // - If you naively slice the screen (job area) to multithread, the seam will appear.

            r1();r2();r3();r4()
        }

    }

    private fun buildLanternmap(actorContainers: Array<out List<ActorWithBody>?>) {
        lanternMap.clear()
        actorContainers.forEach { actorContainer ->
            actorContainer?.forEach {
                if (it is Luminous) {
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

                                val normalisedCvec = it.color//.cpy().mul(DIV_FLOAT)

                                lanternMap[LandUtil.getBlockAddr(world, x, y)] = normalisedCvec
                            }
                        }
                    }
                }
            }
        }
    }

    /*private fun buildNoopMask() {
        fun isShaded(x: Int, y: Int) = try {
            BlockCodex[world.getTileFromTerrain(x, y)].isSolid
        }
        catch (e: NullPointerException) {
            System.err.println("[LightmapRendererNew.buildNoopMask] Invalid block id ${world.getTileFromTerrain(x, y)} from coord ($x, $y)")
            e.printStackTrace()

            false
        }

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
    }*/

    // local variables that are made static
    private val sunLight = Cvec(0)
    private var _thisTerrain = 0
    private var _thisFluid = GameWorld.FluidInfo(Fluid.NULL, 0f)
    private var _thisWall = 0
    private val _ambientAccumulator = Cvec(0)
    private val _thisTileOpacity = Cvec(0)
    private val _thisTileOpacity2 = Cvec(0) // thisTileOpacity * sqrt(2)
    private val _fluidAmountToCol = Cvec(0)
    private val _thisTileLuminosity = Cvec(0)

    fun precalculate(rawx: Int, rawy: Int) {
        val lx = rawx.convX(); val ly = rawy.convY()
        val (worldX, worldY) = world.coerceXY(rawx, rawy)

        //printdbg(this, "precalculate ($rawx, $rawy) -> ($lx, $ly) | ($LIGHTMAP_WIDTH, $LIGHTMAP_HEIGHT)")

        if (lx !in 0..LIGHTMAP_WIDTH || ly !in 0..LIGHTMAP_HEIGHT) {
            println("[LightmapRendererNew.precalculate] Out of range: ($lx, $ly) for size ($LIGHTMAP_WIDTH, $LIGHTMAP_HEIGHT)")
            exitProcess(1)
        }


        _thisTerrain = world.getTileFromTerrainRaw(worldX, worldY)
        _thisFluid = world.getFluid(worldX, worldY)
        _thisWall = world.getTileFromWallRaw(worldX, worldY)


        // regarding the issue #26
        // uncomment this and/or run JVM with -ea if you're facing diabolically indescribable bugs
        /*try {
            val fuck = BlockCodex[_thisTerrain].getLumCol(worldX, worldY)
        }
        catch (e: NullPointerException) {
            System.err.println("## NPE -- x: $worldX, y: $worldY, value: $_thisTerrain")
            e.printStackTrace()
            // create shitty minidump
            System.err.println("MINIMINIDUMP START")
            for (xx in worldX - 16 until worldX + 16) {
                val raw = world.getTileFromTerrain(xx, worldY)
                val lsb = raw.and(0xff).toString(16).padStart(2, '0')
                val msb = raw.ushr(8).and(0xff).toString(16).padStart(2, '0')
                System.err.print(lsb)
                System.err.print(msb)
                System.err.print(" ")
            }
            System.err.println("\nMINIMINIDUMP END")

            exitProcess(1)
        }*/


        if (_thisFluid.type != Fluid.NULL) {
            _fluidAmountToCol.set(_thisFluid.amount, _thisFluid.amount, _thisFluid.amount, _thisFluid.amount)

            _thisTileLuminosity.set(BlockCodex[_thisTerrain].getLumCol(worldX, worldY))
            _thisTileLuminosity.maxAndAssign(BlockCodex[_thisFluid.type].getLumCol(worldX, worldY).mul(_fluidAmountToCol)) // already been div by four
            _mapThisTileOpacity.setVec(lx, ly, BlockCodex[_thisTerrain].opacity)
            _mapThisTileOpacity.max(lx, ly, BlockCodex[_thisFluid.type].opacity.mul(_fluidAmountToCol))// already been div by four
        }
        else {
            _thisTileLuminosity.set(BlockCodex[_thisTerrain].getLumCol(worldX, worldY))
            _mapThisTileOpacity.setVec(lx, ly, BlockCodex[_thisTerrain].opacity)
        }

        _mapThisTileOpacity2.setR(lx, ly, _mapThisTileOpacity.getR(lx, ly) * 1.41421356f)
        _mapThisTileOpacity2.setG(lx, ly, _mapThisTileOpacity.getG(lx, ly) * 1.41421356f)
        _mapThisTileOpacity2.setB(lx, ly, _mapThisTileOpacity.getB(lx, ly) * 1.41421356f)
        _mapThisTileOpacity2.setA(lx, ly, _mapThisTileOpacity.getA(lx, ly) * 1.41421356f)


        // open air || luminous tile backed by sunlight
        if ((_thisTerrain == AIR && _thisWall == AIR) || (_thisTileLuminosity.nonZero() && _thisWall == AIR)) {
            _mapLightLevelThis.setVec(lx, ly, sunLight)
        }

        // blend lantern
        _mapLightLevelThis.max(lx, ly, _thisTileLuminosity.maxAndAssign(
                lanternMap[LandUtil.getBlockAddr(world, worldX, worldY)] ?: colourNull
        ))
    }

    /*private val inNoopMaskp = Point2i(0,0)

    private fun inNoopMask(x: Int, y: Int): Boolean {
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
    }*/

    private var swipeX = -1
    private var swipeY = -1
    private var swipeDiag = false
    private fun _swipeTask(x: Int, y: Int, x2: Int, y2: Int) {
        if (x2 < 0 || y2 < 0 || x2 >= LIGHTMAP_WIDTH || y2 >= LIGHTMAP_HEIGHT) return

        _ambientAccumulator.r = _mapLightLevelThis.getR(x, y)
        _ambientAccumulator.g = _mapLightLevelThis.getG(x, y)
        _ambientAccumulator.b = _mapLightLevelThis.getB(x, y)
        _ambientAccumulator.a = _mapLightLevelThis.getA(x, y)

        if (!swipeDiag) {
            _thisTileOpacity.r = _mapThisTileOpacity.getR(x, y)
            _thisTileOpacity.g = _mapThisTileOpacity.getG(x, y)
            _thisTileOpacity.b = _mapThisTileOpacity.getB(x, y)
            _thisTileOpacity.a = _mapThisTileOpacity.getA(x, y)
            _ambientAccumulator.maxAndAssign(darkenColoured(x2, y2, _thisTileOpacity))
        }
        else {
            _thisTileOpacity2.r = _mapThisTileOpacity2.getR(x, y)
            _thisTileOpacity2.g = _mapThisTileOpacity2.getG(x, y)
            _thisTileOpacity2.b = _mapThisTileOpacity2.getB(x, y)
            _thisTileOpacity2.a = _mapThisTileOpacity2.getA(x, y)
            _ambientAccumulator.maxAndAssign(darkenColoured(x2, y2, _thisTileOpacity2))
        }

        _mapLightLevelThis.setVec(x, y, _ambientAccumulator)
        lightmap.setVec(x, y, _ambientAccumulator)
    }
    private fun swipeLight(sx: Int, sy: Int, ex: Int, ey: Int, dx: Int, dy: Int) {
        swipeX = sx; swipeY = sy
        while (swipeX*dx <= ex*dx && swipeY*dy <= ey*dy) {
            // conduct the task #1
            // spread towards the end
            _swipeTask(swipeX, swipeY, swipeX-dx, swipeY-dy)

            swipeX += dx
            swipeY += dy
        }

        swipeX = ex; swipeY = ey
        while (swipeX*dx >= sx*dx && swipeY*dy >= sy*dy) {
            // conduct the task #2
            // spread towards the start
            _swipeTask(swipeX, swipeY, swipeX+dx, swipeY+dy)

            swipeX -= dx
            swipeY -= dy
        }
    }

    /** Another YE OLDE light simulator
     * Calculates the light simulation, using main lightmap as one of the input.
     */
    /*private fun calculateAndAssign(lightmap: UnsafeCvecArray, worldX: Int, worldY: Int) {

        //if (inNoopMask(worldX, worldY)) return

        // O(9n) == O(n) where n is a size of the map

        //getLightsAndShades(worldX, worldY)

        val x = worldX.convX()
        val y = worldY.convY()

        // calculate ambient
        /*  + * +  0 4 1
         *  * @ *  6 @ 7
         *  + * +  2 5 3
         *  sample ambient for eight points and apply attenuation for those
         *  maxblend eight values and use it
         */


        // TODO getLightsAndShades is replaced with precalculate; change following codes accordingly!
        _ambientAccumulator.r = _mapLightLevelThis.getR(x, y)
        _ambientAccumulator.g = _mapLightLevelThis.getG(x, y)
        _ambientAccumulator.b = _mapLightLevelThis.getB(x, y)
        _ambientAccumulator.a = _mapLightLevelThis.getA(x, y)

        _thisTileOpacity.r = _mapThisTileOpacity.getR(x, y)
        _thisTileOpacity.g = _mapThisTileOpacity.getG(x, y)
        _thisTileOpacity.b = _mapThisTileOpacity.getB(x, y)
        _thisTileOpacity.a = _mapThisTileOpacity.getA(x, y)

        _thisTileOpacity2.r = _mapThisTileOpacity2.getR(x, y)
        _thisTileOpacity2.g = _mapThisTileOpacity2.getG(x, y)
        _thisTileOpacity2.b = _mapThisTileOpacity2.getB(x, y)
        _thisTileOpacity2.a = _mapThisTileOpacity2.getA(x, y)

        // will "overwrite" what's there in the lightmap if it's the first pass
        // takes about 2 ms on 6700K
        /* + */_ambientAccumulator.maxAndAssign(darkenColoured(x - 1, y - 1, _thisTileOpacity2))
        /* + */_ambientAccumulator.maxAndAssign(darkenColoured(x + 1, y - 1, _thisTileOpacity2))
        /* + */_ambientAccumulator.maxAndAssign(darkenColoured(x - 1, y + 1, _thisTileOpacity2))
        /* + */_ambientAccumulator.maxAndAssign(darkenColoured(x + 1, y + 1, _thisTileOpacity2))
        /* * */_ambientAccumulator.maxAndAssign(darkenColoured(x, y - 1, _thisTileOpacity))
        /* * */_ambientAccumulator.maxAndAssign(darkenColoured(x, y + 1, _thisTileOpacity))
        /* * */_ambientAccumulator.maxAndAssign(darkenColoured(x - 1, y, _thisTileOpacity))
        /* * */_ambientAccumulator.maxAndAssign(darkenColoured(x + 1, y, _thisTileOpacity))

        lightmap.setVec(x, y, _ambientAccumulator)
    }*/

    private fun isSolid(x: Int, y: Int): Float? { // ...so that they wouldn't appear too dark
        if (!inBounds(x, y)) return null

        // brighten if solid
        return if (BlockCodex[world.getTileFromTerrain(x, y)].isSolid) 1.2f else 1f
    }

    var lightBuffer: Pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)

    private val colourNull = Cvec(0)
    private val gdxColorNull = Color(0)
    const val epsilon = 1f/1024f

    private var _lightBufferAsTex: Texture = Texture(1, 1, Pixmap.Format.RGBA8888)

    internal fun draw(): Texture {

        // when shader is not used: 0.5 ms on 6700K
        AppLoader.measureDebugTime("Renderer.LightToScreen") {

            val this_x_start = for_draw_x_start
            val this_y_start = for_draw_y_start
            val this_x_end = for_draw_x_end
            val this_y_end = for_draw_y_end

            // wipe out beforehand. You DO need this
            lightBuffer.blending = Pixmap.Blending.None // gonna overwrite (remove this line causes the world to go bit darker)
            lightBuffer.setColor(0)
            lightBuffer.fill()


            // write to colour buffer
            for (y in this_y_start..this_y_end) {
                //println("y: $y, this_y_start: $this_y_start")
                //if (y == this_y_start && this_y_start == 0) {
                //    throw Error("Fuck hits again...")
                //}

                for (x in this_x_start..this_x_end) {

                    val solidMultMagic = isSolid(x, y)

                    val arrayX = x.convX()
                    val arrayY = y.convY()

                    val color = if (solidMultMagic == null)
                        gdxColorNull
                    else
                        Color(
                                lightmap.getR(arrayX, arrayY) * solidMultMagic,
                                lightmap.getG(arrayX, arrayY) * solidMultMagic,
                                lightmap.getB(arrayX, arrayY) * solidMultMagic,
                                lightmap.getA(arrayX, arrayY) * solidMultMagic
                        ).normaliseToHDR()

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
        LightmapHDRMap.dispose()
        _lightBufferAsTex.dispose()
        lightBuffer.dispose()

        lightmap.destroy()
        _mapLightLevelThis.destroy()
        _mapThisTileOpacity.destroy()
        _mapThisTileOpacity2.destroy()
    }

    private const val lightScalingMagic = 8f

    /**
     * Subtract each channel's RGB value.
     *
     * @param x array coord
     * @param y array coord
     * @param darken (0-255) per channel
     * @return darkened data (0-255) per channel
     */
    fun darkenColoured(x: Int, y: Int, darken: Cvec): Cvec {
        // use equation with magic number 8.0
        // this function, when done recursively (A_x = darken(A_x-1, C)), draws exponential curve. (R^2 = 1)

        if (x !in 0 until LIGHTMAP_WIDTH || y !in 0 until LIGHTMAP_HEIGHT) return colourNull

        return Cvec(
                lightmap.getR(x, y) * (1f - darken.r * lightScalingMagic),
                lightmap.getG(x, y) * (1f - darken.g * lightScalingMagic),
                lightmap.getB(x, y) * (1f - darken.b * lightScalingMagic),
                lightmap.getA(x, y) * (1f - darken.a * lightScalingMagic)
        )

    }

    /** infix is removed to clarify the association direction */
    private fun Cvec.maxAndAssign(other: Cvec): Cvec {
        // TODO investigate: if I use assignment instead of set(), it blackens like the vector branch.  --Torvald, 2019-06-07
        //                   that was because you forgot 'this.r/g/b/a = ' part, bitch. --Torvald, 2019-06-07
        this.r = if (this.r > other.r) this.r else other.r
        this.g = if (this.g > other.g) this.g else other.g
        this.b = if (this.b > other.b) this.b else other.b
        this.a = if (this.a > other.a) this.a else other.a

        return this
    }

    private fun Float.inv() = 1f / this
    fun Int.even(): Boolean = this and 1 == 0
    fun Int.odd(): Boolean = this and 1 == 1

    // TODO: float LUT lookup using linear interpolation

    // input: 0..1 for int 0..1023
    private fun hdr(intensity: Float): Float {
        val intervalStart = (intensity * CHANNEL_MAX).floorInt()
        val intervalEnd = (intensity * CHANNEL_MAX).floorInt() + 1

        if (intervalStart == intervalEnd) return LightmapHDRMap[intervalStart]

        val intervalPos = (intensity * CHANNEL_MAX) - (intensity * CHANNEL_MAX).toInt()

        val ret = interpolateLinear(
                intervalPos,
                LightmapHDRMap[intervalStart],
                LightmapHDRMap[intervalEnd]
        )

        return ret
    }

    private var _init = false

    fun resize(screenW: Int, screenH: Int) {
        // make sure the BlocksDrawer is resized first!

        // copied from BlocksDrawer, duh!
        // FIXME 'lightBuffer' is not zoomable in this way
        val tilesInHorizontal = (AppLoader.screenWf / TILE_SIZE).ceilInt() + 1
        val tilesInVertical = (AppLoader.screenHf / TILE_SIZE).ceilInt() + 1

        LIGHTMAP_WIDTH = (Terrarum.ingame?.ZOOM_MINIMUM ?: 1f).inv().times(AppLoader.screenW).div(TILE_SIZE).ceilInt() + overscan_open * 2 + 3
        LIGHTMAP_HEIGHT = (Terrarum.ingame?.ZOOM_MINIMUM ?: 1f).inv().times(AppLoader.screenH).div(TILE_SIZE).ceilInt() + overscan_open * 2 + 3

        if (_init) {
            lightBuffer.dispose()
        }
        else {
            _init = true
        }
        lightBuffer = Pixmap(tilesInHorizontal, tilesInVertical, Pixmap.Format.RGBA8888)

        lightmap.destroy()
        _mapLightLevelThis.destroy()
        _mapThisTileOpacity.destroy()
        _mapThisTileOpacity2.destroy()
        lightmap = UnsafeCvecArray(LIGHTMAP_WIDTH, LIGHTMAP_HEIGHT)
        _mapLightLevelThis = UnsafeCvecArray(LIGHTMAP_WIDTH, LIGHTMAP_HEIGHT)
        _mapThisTileOpacity = UnsafeCvecArray(LIGHTMAP_WIDTH, LIGHTMAP_HEIGHT)
        _mapThisTileOpacity2 = UnsafeCvecArray(LIGHTMAP_WIDTH, LIGHTMAP_HEIGHT)

        printdbg(this, "Resize event")
    }


    /** To eliminated visible edge on the gradient when 255/1023 is exceeded */
    fun Color.normaliseToHDR() = Color(
            hdr(this.r.coerceIn(0f, 1f)),
            hdr(this.g.coerceIn(0f, 1f)),
            hdr(this.b.coerceIn(0f, 1f)),
            hdr(this.a.coerceIn(0f, 1f))
    )

    private fun Cvec.nonZero() = this.r.abs() > epsilon ||
                                 this.g.abs() > epsilon ||
                                 this.b.abs() > epsilon ||
                                 this.a.abs() > epsilon

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
                        // TODO
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

fun Cvec.toRGBA() = (255 * r).toInt() shl 24 or ((255 * g).toInt() shl 16) or ((255 * b).toInt() shl 8) or (255 * a).toInt()
fun Color.toRGBA() = (255 * r).toInt() shl 24 or ((255 * g).toInt() shl 16) or ((255 * b).toInt() shl 8) or (255 * a).toInt()

