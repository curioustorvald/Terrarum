package net.torvald.terrarum.worlddrawer

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.jme3.math.FastMath
import net.torvald.gdx.graphics.Cvec
import net.torvald.gdx.graphics.UnsafeCvecArray
import net.torvald.spriteanimation.AssembledSpriteAnimation
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.blockproperties.*
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.Hitbox
import net.torvald.terrarum.gameactors.Lightbox
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.isBlock
import net.torvald.terrarum.gameworld.BlockAddress
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.IngameRenderer
import net.torvald.terrarum.modulebasegame.gameactors.Pocketed
import net.torvald.terrarum.modulebasegame.ui.abs
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.spriteassembler.ADPropertyObject
import java.util.*
import kotlin.math.*

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
        }
    }

    const val overscan_open: Int = 40
    const val overscan_opaque: Int = 10
    const val LIGHTMAP_OVERRENDER = 10

    private var LIGHTMAP_WIDTH: Int = (Terrarum.ingame?.ZOOM_MINIMUM ?: 1f).inv().times(App.scr.width).div(TILE_SIZE).ceilToInt() + overscan_open * 2 + 3
    private var LIGHTMAP_HEIGHT: Int = (Terrarum.ingame?.ZOOM_MINIMUM ?: 1f).inv().times(App.scr.height).div(TILE_SIZE).ceilToInt() + overscan_open * 2 + 3

    //private val noopMask = HashSet<Point2i>((LIGHTMAP_WIDTH + LIGHTMAP_HEIGHT) * 2)

    private val lanternMap = HashMap<BlockAddress, Cvec>((Terrarum.ingame?.ACTORCONTAINER_INITIAL_SIZE ?: 2) * 4)
    private val shadowMap = HashMap<BlockAddress, Cvec>((Terrarum.ingame?.ACTORCONTAINER_INITIAL_SIZE ?: 2) * 4)
//    private val giMap = HashMap<BlockAddress, Cvec>((Terrarum.ingame?.ACTORCONTAINER_INITIAL_SIZE ?: 2) * 4)
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

    const val DRAW_TILE_SIZE: Float = TILE_SIZE / IngameRenderer.lightmapDownsample

    internal var for_x_start = 0
    internal var for_y_start = 0
    internal var for_x_end = 0
    internal var for_y_end = 0
    internal var for_draw_x_start = 0
    internal var for_draw_y_start = 0
    internal var for_draw_x_end = 0
    internal var for_draw_y_end = 0
    internal var camX = 0
    internal var camY = 0

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
        val x = if (for_x_start - overscan_open + LIGHTMAP_WIDTH >= world.width && x - for_x_start + overscan_open < 0)
            x + world.width
        else if (for_x_start - overscan_open + LIGHTMAP_WIDTH < 0 && x - for_x_start + overscan_open >= world.width)
            x - world.width else x

        return if (!inBounds(x, y)) {
            null
        }
        else {
            lightmap.getVec(x.convX(), y.convY())
        }
    }

    init {
        printdbg(this, "Overscan open: $overscan_open; opaque: $overscan_opaque")
    }

    fun recalculate(actorContainer: List<ActorWithBody>) {
        if (!world.layerTerrain.ptrDestroyed) _recalculate(actorContainer, lightmap)
    }

    private fun _recalculate(actorContainer: List<ActorWithBody>, lightmap: UnsafeCvecArray) {
        try {
            world.getTileFromTerrain(0, 0) // test inquiry
        }
        catch (e: UninitializedPropertyAccessException) {
            return // quit prematurely
        }
        catch (e: NullPointerException) {
            System.err.println("[LightmapRendererNew.recalculate] Attempted to refer destroyed unsafe array " +
                               "(${world.layerTerrain.ptr})")
            e.printStackTrace()
            return // something's wrong but we'll ignore it like a trustful AK
        }

        if (world.worldIndex == UUID(0L,0L)) return


        for_x_start = WorldCamera.zoomedX / TILE_SIZE // fix for premature lightmap rendering
        for_y_start = WorldCamera.zoomedY / TILE_SIZE // on topmost/leftmost side
        for_draw_x_start = WorldCamera.x / TILE_SIZE - LIGHTMAP_OVERRENDER
        for_draw_y_start = WorldCamera.y / TILE_SIZE - LIGHTMAP_OVERRENDER

        if (WorldCamera.x < 0) for_draw_x_start -= 1 // edge case fix that light shift 1 tile to the left when WorldCamera.x < 0
        //if (WorldCamera.x in -(TILE_SIZE - 1)..-1) for_draw_x_start -= 1 // another edge-case fix; we don't need this anymore?

        for_x_end = for_x_start + WorldCamera.zoomedWidth / TILE_SIZE + 1
        for_y_end = for_y_start + WorldCamera.zoomedHeight / TILE_SIZE + 1
        for_draw_x_end = for_draw_x_start + WorldCamera.width / TILE_SIZE + 1 + 2*LIGHTMAP_OVERRENDER
        for_draw_y_end = for_draw_y_start + WorldCamera.height / TILE_SIZE + 1 + 2*LIGHTMAP_OVERRENDER

        camX = WorldCamera.x / TILE_SIZE
        camY = WorldCamera.y / TILE_SIZE

        //println("$for_x_start..$for_x_end, $for_x\t$for_y_start..$for_y_end, $for_y")

        App.measureDebugTime("Renderer.Lanterns") {
            buildLanternAndShadowMap(actorContainer)
        } // usually takes 3000 ns

        // copy current world's globalLight into this
        sunLight.set(world.globalLight)

        // set no-op mask from solidity of the block
        /*AppLoader.measureDebugTime("Renderer.LightNoOpMask") {
            noopMask.clear()
            buildNoopMask()
        }*/

        // wipe out lightmap
        App.measureDebugTime("Renderer.Precalculate1") {
            // when disabled, light will "decay out" instead of "instantly out", which can have a cool effect
            // but the performance boost is measly 0.1 ms on 6700K

//            giMap.clear()
//            _mapLightLevelThis.zerofill()

            for (y in for_y_start - overscan_open..for_y_end + overscan_open) {
                for (x in for_x_start - overscan_open..for_x_end + overscan_open) {
                    precalculate(x, y)
                }
            }
        }

        // 'NEWLIGHT2' LIGHT SWIPER
        // O((8*2)n) where n is a size of the map.
        /* - */fun r1(lightmap: UnsafeCvecArray) {
            for (line in 1 until LIGHTMAP_HEIGHT - 1) {
                swipeLight(
                        1, line,
                        LIGHTMAP_WIDTH - 2, line,
                        1, 0,
                        lightmap, false
                )
            }
        }
        /* | */fun r2(lightmap: UnsafeCvecArray) {
            for (line in 1 until LIGHTMAP_WIDTH - 1) {
                swipeLight(
                        line, 1,
                        line, LIGHTMAP_HEIGHT - 2,
                        0, 1,
                        lightmap, false
                )
            }
        }
        /* \ */fun r3(lightmap: UnsafeCvecArray) {
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
                        max(1, i - LIGHTMAP_HEIGHT + 4), max(1, LIGHTMAP_HEIGHT - 2 - i),
                        min(LIGHTMAP_WIDTH - 2, i + 1), min(LIGHTMAP_HEIGHT - 2, (LIGHTMAP_WIDTH + LIGHTMAP_HEIGHT - 5) - i),
                        1, 1,
                        lightmap, true
                )
            }
        }
        /* / */fun r4(lightmap: UnsafeCvecArray) {
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
                        max(1, i - LIGHTMAP_HEIGHT + 4), min(LIGHTMAP_HEIGHT - 2, i + 1),
                        min(LIGHTMAP_WIDTH - 2, i + 1), max(1, (LIGHTMAP_HEIGHT - 2) - (LIGHTMAP_WIDTH + LIGHTMAP_HEIGHT - 6) + i),
                        1, -1,
                        lightmap, true
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
        App.measureDebugTime("Renderer.LightRuns1") {

            // To save you from pains:
            // - Per-channel light updating is actually slower
            // BELOW NOTES DOES NOT APPLY TO 'NEWLIGHT2' LIGHT SWIPER
            // - It seems 5-pass lighting is needed to resonably eliminate the dark spot (of which I have zero idea
            //      why dark spots appear in the first place)
            // - Multithreading? I have absolutely no idea.
            // - If you naively slice the screen (job area) to multithread, the seam will appear.
            r1(lightmap);r2(lightmap);r3(lightmap);r4(lightmap)
        }

        App.measureDebugTime("Renderer.Precalculate2") {
            // populate GImap and perform precalculation again
            for (y in for_y_start - overscan_open + 1..for_y_end + overscan_open - 1) {
                for (x in for_x_start - overscan_open + 1..for_x_end + overscan_open - 1) {
                    precalculate2(lightmap, x, y)
                }
            }
        }

        App.measureDebugTime("Renderer.LightRuns2") {
            // no rendering trickery will eliminate the need of 2nd pass, even the "decay out"
            r1(lightmap);r2(lightmap);r3(lightmap);r4(lightmap)
        }

        App.getConfigInt("lightpasses").let { passcnt ->
            if (passcnt > 2) {
                for (pass in 3..passcnt) {
                    App.measureDebugTime("Renderer.LightRuns$pass") {
                        r1(lightmap);r2(lightmap);r3(lightmap);r4(lightmap)
                    }
                }
            }
        }
    }

    private fun buildLanternAndShadowMap(actorContainer: List<ActorWithBody>) {
        lanternMap.clear()
        shadowMap.clear()
        actorContainer.forEach {
            val lightBoxCopy = it.lightBoxList.subList(0, it.lightBoxList.size) // make copy to prevent ConcurrentModificationException
            val shadeBoxCopy = it.shadeBoxList.subList(0, it.shadeBoxList.size) // make copy to prevent ConcurrentModificationException

            // add lightbox for held item
            val heldItemLightBox = if (it.sprite is AssembledSpriteAnimation && it is Pocketed) {
                val sprite = it.sprite as AssembledSpriteAnimation
                val adp = sprite.adp
                val currentAnimNameRoot = sprite.currentAnimation
                val currentAnimNameFull = "${sprite.currentAnimation}_${1 + sprite.currentFrame}"
                val anim = adp.animations[currentAnimNameRoot]!!
                val HELD_ITEM = "HELD_ITEM"
                val transform = adp.getTransform(currentAnimNameFull)
                val skeleton = anim.skeleton

                val heldItemTransform = transform.firstOrNull { it.joint.name == HELD_ITEM }?.translate ?: ADPropertyObject.Vector2i(0, 0)
                val heldItemJoint = skeleton.joints.firstOrNull { it.name == HELD_ITEM }

                if (heldItemJoint != null) {
                    val relativeHeldItemTopLeftPos =
                        adp.origin + (heldItemJoint.position + heldItemTransform).invertY() + ADPropertyObject.Vector2i(1,0)

                    val tx = -(it.hitboxTranslateX).toDouble()
                    val ty = if (sprite.flipVertical) (it.hitboxTranslateY).toDouble() else -(it.hitboxTranslateY - it.baseHitboxH).toDouble()

                    val heldItem = it.inventory.itemEquipped[GameItem.EquipPosition.HAND_GRIP]

                    val light = ItemCodex[heldItem]?.getLumCol() ?: Cvec(0)


                    Lightbox(Hitbox(
                        relativeHeldItemTopLeftPos.x + tx,
                        relativeHeldItemTopLeftPos.y + ty - TILE_SIZED,
                        TILE_SIZED, TILE_SIZED
                    ), light)
                }
                else
                    Lightbox(Hitbox(0.0, 0.0, 1.0, 1.0), Cvec(0))
            }
            else
                Lightbox(Hitbox(0.0, 0.0, 1.0, 1.0), Cvec(0))


            // put lanterns to the area the lightBox is occupying
            val scale = it.scale
            (lightBoxCopy + heldItemLightBox).forEach { (box, colour) ->
                val boxX = it.hitbox.startX + (box.startX * scale)
                val boxY = it.hitbox.startY + (box.startY * scale)
                val boxW = box.width * scale
                val boxH = box.height * scale

//                println("boxX=$boxX, boxY=$boxY, boxW=$boxW, boxH=$boxH; actor=$it; baseHitboxW=${it.baseHitboxW}; box=$box")

                val x0 = boxX.div(TILE_SIZE).floorToInt()
                val x1 = (boxX + boxW - 1).div(TILE_SIZE).floorToInt()
                val y0 = boxY.div(TILE_SIZE).floorToInt()
                val y1 = (boxY + boxH - 1).div(TILE_SIZE).floorToInt()

                for (y in y0 .. y1) {
                    for (x in x0 .. x1) {

                        val oldLight = lanternMap[LandUtil.getBlockAddr(world, x, y)] ?: Cvec(0) // if two or more luminous actors share the same block, mix the light
                        val actorLight = colour

                        lanternMap[LandUtil.getBlockAddr(world, x, y)] = oldLight.maxAndAssign(actorLight)
                    }
                }
            }

            // put shades to the area the shadeBox is occupying
            shadeBoxCopy.forEach { (box, colour) ->
                val boxX = it.hitbox.startX + (box.startX * scale)
                val boxY = it.hitbox.startY + (box.startY * scale)
                val boxW = box.width * scale
                val boxH = box.height * scale

                val x0 = boxX.div(TILE_SIZE).floorToInt()
                val x1 = (boxX + boxW - 1).div(TILE_SIZE).floorToInt()
                val y0 = boxY.div(TILE_SIZE).floorToInt()
                val y1 = (boxY + boxH - 1).div(TILE_SIZE).floorToInt()

                for (y in y0 .. y1) {
                    for (x in x0 .. x1) {

                        val oldLight = shadowMap[LandUtil.getBlockAddr(world, x, y)] ?: Cvec(0) // if two or more luminous actors share the same block, mix the light
                        val actorLight = colour

                        shadowMap[LandUtil.getBlockAddr(world, x, y)] = oldLight.maxAndAssign(actorLight)
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
    private val _reflectanceAccumulator = Cvec(0)
    private val _thisTileOpacity = Cvec(0)
    private val _thisTileOpacity2 = Cvec(0) // thisTileOpacity * sqrt(2)
    private val _fluidAmountToCol = Cvec(0)
    private val _thisTileLuminosity = Cvec(0)
    private var _thisTerrainProp: BlockProp = BlockProp()
    private var _thisWallProp: BlockProp = BlockProp()
    private var _thisFluidProp: FluidProp = FluidProp()

    private fun precalculate(rawx: Int, rawy: Int) {
        val lx = rawx.convX(); val ly = rawy.convY()
        val (worldX, worldY) = world.coerceXY(rawx, rawy)

        //printdbg(this, "precalculate ($rawx, $rawy) -> ($lx, $ly) | ($LIGHTMAP_WIDTH, $LIGHTMAP_HEIGHT)")

//        if (lx !in 0..LIGHTMAP_WIDTH || ly !in 0..LIGHTMAP_HEIGHT) {
//            throw IllegalArgumentException("[LightmapRendererNew.precalculate] Out of range: ($lx, $ly) for size ($LIGHTMAP_WIDTH, $LIGHTMAP_HEIGHT)")
//        }



        _thisTerrain = world.getTileFromTerrainRaw(worldX, worldY)
        _thisTerrainProp = BlockCodex[world.tileNumberToNameMap[_thisTerrain.toLong()]]
        _thisWall = world.getTileFromWallRaw(worldX, worldY)
        _thisWallProp = BlockCodex[world.tileNumberToNameMap[_thisWall.toLong()]]
        _thisFluid = world.getFluid(worldX, worldY)
        _thisFluidProp = FluidCodex[_thisFluid.type]


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
            val fluidAmount = _thisFluid.amount.coerceIn(0f, 1f).pow(0.5f)

            _thisTileLuminosity.set(_thisTerrainProp.getLumCol(worldX, worldY))
            _thisTileLuminosity.maxAndAssign(_thisFluidProp.lumCol.cpy().mul(fluidAmount))
            _mapThisTileOpacity.setVec(lx, ly, _thisTerrainProp.opacity)
            _mapThisTileOpacity.max(lx, ly, _thisFluidProp.opacity.cpy().mul(fluidAmount))
        }
        else {
            _thisTileLuminosity.set(_thisTerrainProp.getLumCol(worldX, worldY))
            _mapThisTileOpacity.setVec(lx, ly, _thisTerrainProp.opacity)
        }

        // blend shade
        _mapThisTileOpacity.max(lx, ly, shadowMap[LandUtil.getBlockAddr(world, worldX, worldY)] ?: colourNull)

//        _mapThisTileOpacity2.setVec(lx, ly, _mapThisTileOpacity.getVec(lx, ly).mul(1.41421356f))
        _mapThisTileOpacity.getAndSetMap(_mapThisTileOpacity2, lx, ly) { it * 1.41421356f }


        // open air || luminous tile backed by sunlight
        if ((!_thisTerrainProp.isSolid && !_thisWallProp.isSolid) ||
            (_thisTileLuminosity.nonZero() && !_thisWallProp.isSolid)) {
            _mapLightLevelThis.setVec(lx, ly, sunLight)
        }
        else {
            _mapLightLevelThis.setScalar(lx, ly, 0f)
        }

        // blend lantern
        _mapLightLevelThis.max(lx, ly, _thisTileLuminosity)
        _mapLightLevelThis.max(lx, ly, lanternMap[LandUtil.getBlockAddr(world, worldX, worldY)] ?: colourNull)
    }

    private fun precalculate2(lightmap: UnsafeCvecArray, rawx: Int, rawy: Int) {
        val lx = rawx.convX(); val ly = rawy.convY()
        val (worldX, worldY) = world.coerceXY(rawx, rawy)

        //printdbg(this, "precalculate2 ($rawx, $rawy) -> ($lx, $ly) | ($LIGHTMAP_WIDTH, $LIGHTMAP_HEIGHT)")

//        if (lx !in 0..LIGHTMAP_WIDTH || ly !in 0..LIGHTMAP_HEIGHT) {
//            throw IllegalArgumentException("[LightmapRendererNew.precalculate2] Out of range: ($lx, $ly) for size ($LIGHTMAP_WIDTH, $LIGHTMAP_HEIGHT)")
//        }

        // blend nearby 4 lights to get intensity
        _ambientAccumulator.set(0)
                .maxAndAssign(lightmap.getVec(lx - 1, ly))
                .maxAndAssign(lightmap.getVec(lx + 1, ly))
                .maxAndAssign(lightmap.getVec(lx, ly - 1))
                .maxAndAssign(lightmap.getVec(lx, ly + 1))

        _thisTerrain = world.getTileFromTerrainRaw(worldX, worldY)
        _thisTerrainProp = BlockCodex[world.tileNumberToNameMap[_thisTerrain.toLong()]]

        _reflectanceAccumulator.set(App.tileMaker.terrainTileColourMap[_thisTerrainProp.id] ?: Cvec())
        _reflectanceAccumulator.a = 0f // TODO temporarily disabled
        _reflectanceAccumulator.mul(_thisTerrainProp.reflectance).mul(giScale)

        _mapLightLevelThis.max(lx, ly, _reflectanceAccumulator)
    }

    private const val giScale = 0.35f
    private fun _swipeTask(x: Int, y: Int, xOld: Int, yOld: Int, lightmap: UnsafeCvecArray, swipeDiag: Boolean) {//, distFromLightSrc: Ivec4) {
        if (xOld < 0 || yOld < 0 || xOld >= LIGHTMAP_WIDTH || yOld >= LIGHTMAP_HEIGHT) return

        _mapLightLevelThis.getAndSet(_ambientAccumulator, x, y)

        if (!swipeDiag) {
            _mapThisTileOpacity.getAndSet(_thisTileOpacity, x, y)
            _ambientAccumulator.maxAndAssign(darkenColoured(xOld, yOld, _thisTileOpacity, lightmap))
        }
        else {
            _mapThisTileOpacity2.getAndSet(_thisTileOpacity2, x, y)
            _ambientAccumulator.maxAndAssign(darkenColoured(xOld, yOld, _thisTileOpacity2, lightmap))
        }

        _mapLightLevelThis.setVec(x, y, _ambientAccumulator)
        lightmap.setVec(x, y, _ambientAccumulator)
    }
    private fun swipeLight(sx: Int, sy: Int, ex: Int, ey: Int, dx: Int, dy: Int, lightmap: UnsafeCvecArray, swipeDiag: Boolean) {
        var swipeX = sx
        var swipeY = sy
        while (swipeX*dx <= ex*dx && swipeY*dy <= ey*dy) {
            // conduct the task #1
            // spread towards the end
            _swipeTask(swipeX, swipeY, swipeX-dx, swipeY-dy, lightmap, swipeDiag)

            swipeX += dx
            swipeY += dy
        }

        swipeX = ex; swipeY = ey
        while (swipeX*dx >= sx*dx && swipeY*dy >= sy*dy) {
            // conduct the task #2
            // spread towards the start
            _swipeTask(swipeX, swipeY, swipeX+dx, swipeY+dy, lightmap, swipeDiag)

            swipeX -= dx
            swipeY -= dy
        }
    }

    private fun isSolid(x: Int, y: Int): Float? { // ...so that they wouldn't appear too dark
        if (!inBounds(x, y)) return null

        // brighten if solid
        return if (BlockCodex[world.getTileFromTerrain(x, y)].isSolid) 1.2f else 1f
    }

    var lightBuffer: Pixmap = Pixmap(64, 64, Pixmap.Format.RGBA8888) // must not be too small

    private val colourNull = Cvec(0)
    private val gdxColorNull = Color(0)
    const val epsilon = 1f/1024f

    private var _lightBufferAsTex: Texture = Texture(64, 64, Pixmap.Format.RGBA8888) // must not be too small

    internal fun draw(): Texture {

        if (!world.layerTerrain.ptrDestroyed) {
            // when shader is not used: 0.5 ms on 6700K
            App.measureDebugTime("Renderer.LightToScreen") {

                val this_x_start = for_draw_x_start
                val this_y_start = for_draw_y_start
                val this_x_end = for_draw_x_end
                val this_y_end = for_draw_y_end

                // wipe out beforehand. You DO need this
                lightBuffer.blending =
                    Pixmap.Blending.None // gonna overwrite (remove this line causes the world to go bit darker)
                lightBuffer.setColor(0)
                lightBuffer.fill()


                // write to colour buffer
                for (y in this_y_start..this_y_end) {
                    //println("y: $y, this_y_start: $this_y_start")
                    //if (y == this_y_start && this_y_start == 0) {
                    //    throw Error("Fuck hits again...")
                    //}

                    for (x in this_x_start..this_x_end) {

                        val solidMultMagic = isSolid(x, y) // one of {1.2f, 1f, null}

                        val arrayX = x.convX()
                        val arrayY = y.convY()

                        val (red, grn, blu, uvl) = lightmap.getVec(arrayX, arrayY)
//                        val redw = (grn.sqrt() - 1f) * (1f / 53f)
//                        val grnw = (red.sqrt() - 1f) * (1f / 39f)
//                        val bluw = (blu.sqrt() - 1f) * (1f / 159f)
//                        val bluwv = (blu.sqrt() - 1f) * (1f / 39f)
//                        val uvlwr = (uvl.sqrt() - 1f) * (1f / 947f)
//                        val uvlwg = (uvl.sqrt() - 1f) * (1f / 1279f)
//                        val uvlwb = (uvl.sqrt() - 1f) * (1f / 319f)

                        if (red.isNaN() || grn.isNaN() || blu.isNaN() || uvl.isNaN()) throw IllegalArgumentException("Light vector contains NaN ($red,$grn,$blu,$uvl)")

                        if (solidMultMagic == null)
                            lightBuffer.drawPixel(
                                x - this_x_start,
                                lightBuffer.height - 1 - y + this_y_start, // flip Y
                                0
                            )
                        else
                            /*lightBuffer.drawPixel(
                                x - this_x_start,
                                lightBuffer.height - 1 - y + this_y_start, // flip Y
                                    (maxOf(red,grnw,bluw,uvlwr) * solidMultMagic).tonemap().times(255f).roundToInt().shl(24) or
                                            (maxOf(redw,grn,bluw,uvlwg) * solidMultMagic).tonemap().times(255f).roundToInt().shl(16) or
                                            (maxOf(redw,grnw,blu,uvlwb) * solidMultMagic).tonemap().times(255f).roundToInt().shl(8) or
                                            (maxOf(bluwv,uvl) * solidMultMagic).tonemap().times(255f).roundToInt()
                            )*/
                            lightBuffer.drawPixel(
                                x - this_x_start,
                                lightBuffer.height - 1 - y + this_y_start, // flip Y
                                (red * solidMultMagic).tonemap().times(255f).roundToInt().shl(24) or
                                        (grn * solidMultMagic).tonemap().times(255f).roundToInt().shl(16) or
                                        (blu * solidMultMagic).tonemap().times(255f).roundToInt().shl(8) or
                                        (uvl * solidMultMagic).tonemap().times(255f).roundToInt()
                            )
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
        }

        return _lightBufferAsTex
    }

    fun dispose() {
        _lightBufferAsTex.dispose()
        lightBuffer.dispose()

        lightmap.destroy()
        _mapLightLevelThis.destroy()
        _mapThisTileOpacity.destroy()
        _mapThisTileOpacity2.destroy()
    }

    private const val lightScalingMagic = 2f

    private fun lerpBetween(x: Float, xStart: Float, xEnd: Float, yStart: Float, yEnd: Float): Float {
        val scale = (x - xStart) / (xEnd - xStart)
        return FastMath.interpolateLinear(scale, yStart, yEnd)
    }


    private fun darkenConv(it: Float) =
            if (it < 0f) 0f
            else if (it < 0.25f) lerpBetween(it, 0f, 0.25f, 0f, 1.33f)
            else if (it < 0.5f) lerpBetween(it, 0.25f, 0.5f, 1.33f, 3.14f)
            else if (it < 0.63f) lerpBetween(it, 0.5f, 0.63f, 3.14f, 5.6f)
            else if (it < 0.71f) lerpBetween(it, 0.63f, 0.71f, 5.6f, 8.8f)
            else if (it < 0.76f) lerpBetween(it, 0.71f, 0.76f, 8.8f, 12.8f)
            else if (it < 0.796f) lerpBetween(it, 0.76f, 0.796f, 12.8f, 17.5f)
            else if (it < 0.82f) lerpBetween(it, 0.796f, 0.82f, 17.5f, 23f)
            else if (it < 0.84f) lerpBetween(it, 0.82f, 0.84f, 23f, 29f)
            else if (it < 0.858f) lerpBetween(it, 0.84f, 0.858f, 29f, 36f)
            else if (it < 0.87f) lerpBetween(it, 0.858f, 0.87f, 36f, 43f)
            else if (it < 0.88f) lerpBetween(it, 0.87f, 0.88f, 43f, 51f)
            else if (it < 0.89f) lerpBetween(it, 0.88f, 0.89f, 51f, 60f)
            else if (it < 0.9f) lerpBetween(it, 0.89f, 0.9f, 60f, 71f)
            else if (it < 0.906f) lerpBetween(it, 0.9f, 0.906f, 71f, 81f)
            else lerpBetween(it, 0.906f, 0.911f, 81f, 92f)

    /**
     * Subtract each channel's RGB value.
     *
     * @param x array coord
     * @param y array coord
     * @param darken (0-255) per channel
     * @return darkened data (0-255) per channel
     */
    internal fun darkenColoured(x: Int, y: Int, darken: Cvec, lightmap: UnsafeCvecArray): Cvec {//, distFromLightSrc: Ivec4 = Ivec4()): Cvec {
        // use equation with magic number 8.0
        // this function, when done recursively (A_x = darken(A_x-1, C)), draws exponential curve. (R^2 = 1)

        if (x !in 0 until LIGHTMAP_WIDTH || y !in 0 until LIGHTMAP_HEIGHT) return colourNull

//        if (App.getConfigBoolean("fx_newlight")) {
//            val newDarken: Cvec = darken.lanewise { it, ch ->
//                darkenConv(1f - it * lightScalingMagic)
//            }
//
//            return lightmap.getVec(x, y).lanewise { it, ch ->
//                (it * ((newDarken.lane(ch) - distFromLightSrc.lane(ch)) / newDarken.lane(ch))).coerceAtLeast(0f)
//            }
//        }
//        else {
            return lightmap.getVec(x, y).lanewise { it, ch ->
                it * (1f - darken.lane(ch) * lightScalingMagic)
            }
//        }
    }

    /** infix is removed to clarify the association direction */
    private fun Cvec.maxAndAssign(other: Cvec): Cvec {
        // TODO investigate: if I use assignment instead of set(), it blackens like the vector branch.  --Torvald, 2019-06-07
        //                   that was because you forgot 'this.r/g/b/a = ' part, bitch. --Torvald, 2019-06-07
        this.r = max(this.r, other.r)
        this.g = max(this.g, other.g)
        this.b = max(this.b, other.b)
        this.a = max(this.a, other.a)

        return this
    }

    private fun Float.inv() = 1f / this
    fun Int.even(): Boolean = this and 1 == 0
    fun Int.odd(): Boolean = this and 1 == 1

    private var _init = false

    fun resize(screenW: Int, screenH: Int) {
        // make sure the BlocksDrawer is resized first!

        // copied from BlocksDrawer, duh!
        // FIXME 'lightBuffer' is not zoomable in this way

        val tilesInHorizontal = (App.scr.wf / TILE_SIZE).ceilToInt() + 1 + LIGHTMAP_OVERRENDER * 2
        val tilesInVertical = (App.scr.hf / TILE_SIZE).ceilToInt() + 1 + LIGHTMAP_OVERRENDER * 2

        LIGHTMAP_WIDTH = (Terrarum.ingame?.ZOOM_MINIMUM ?: 1f).inv().times(App.scr.width).div(TILE_SIZE).ceilToInt() + overscan_open * 2 + 3
        LIGHTMAP_HEIGHT = (Terrarum.ingame?.ZOOM_MINIMUM ?: 1f).inv().times(App.scr.height).div(TILE_SIZE).ceilToInt() + overscan_open * 2 + 3

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
    
    private const val clip_p = 0.44444f // knee of around -6.02dB
    private val clip_p1 = sqrt(1.0f - 2.0f * clip_p)
    private val clip_lim = 1.0f / (1.0f + clip_p1)
    /**
     * https://www.desmos.com/calculator/syqd1byzzl
     * @param x0 -0.5..0.5 ish
     * @return -0.5..0.5
     */
    private fun clipfun0(x0: Float): Float {
        if (x0.isNaN()) throw IllegalArgumentException("Cannot clip NaN value.")

        val x = x0 * (1.0f + clip_p1) / 2.0f
        val t = 0.5f * clip_p1

        if (x0.absoluteValue >= clip_lim) return 0.5f * sign(x0)

        val y0 = if (x < -t)
             (x*x + x + 0.25f) / clip_p - 0.5f
        else if (x > t)
            -(x*x - x + 0.25f) / clip_p + 0.5f
        else
            x * 2.0f * clip_lim

        return y0
    }
    
    fun Float.tonemap() = clipfun0(this / 2f) * 2f

    private fun Cvec.nonZero() = this.r.abs() > epsilon ||
                                 this.g.abs() > epsilon ||
                                 this.b.abs() > epsilon ||
                                 this.a.abs() > epsilon

    val histogram: Histogram
        get() {
            val reds = IntArray(256) // reds[intensity] ← counts
            val greens = IntArray(256) // do.
            val blues = IntArray(256) // do.
            val uvs = IntArray(256)
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
                for (i in 255 downTo 1) {
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
                for (i in 0..255) {
                    if (reds[i] > 0 || greens[i] > 0 || blues[i] > 0)
                        return i
                }
                return 255
            }

        val range: Int = 255

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

