package net.torvald.terrarum.worlddrawer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.math.Matrix4
import com.jme3.math.FastMath
import net.torvald.terrarum.*
import net.torvald.terrarum.AppLoader.measureDebugTime
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.modulebasegame.gameworld.GameWorldExtension
import net.torvald.terrarum.modulebasegame.gameworld.WorldSimulator
import net.torvald.terrarum.modulebasegame.gameworld.WorldTime
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import kotlin.math.roundToInt


/**
 * Sub-portion of IngameRenderer. You are not supposed to directly deal with this.
 *
 * The terrain texture atlas is HARD CODED as "4096x4096, on which 256x256 tiles are contained"
 * in the shader (tiling.frag). This will not be a problem in the base game, but if you are modifying
 * this engine for your project, you must edit the shader program accordingly.
 *
 * To render and draw images, modify the ```selectedWireBitToDraw``` (bitset) property from the IngameRenderer.
 *
 * Created by minjaesong on 2016-01-19.
 */
internal object BlocksDrawer {

    /** World change is managed by IngameRenderer.setWorld() */
    internal var world: GameWorld = GameWorld.makeNullWorld()


    /**
     * Widths of the tile atlases must have exactly the same width (height doesn't matter)
     * If not, the engine will choose wrong tile for a number you provided.
     */

    /** Index zero: spring */
    val weatherTerrains: Array<TextureRegionPack>
    lateinit var tilesTerrain: TextureRegionPack; private set
    lateinit var tilesTerrainBlend: TextureRegionPack; private set
    val tilesWire: TextureRegionPack
    val tileItemTerrain: TextureRegionPack
    val tileItemWall: TextureRegionPack
    val tilesFluid: TextureRegionPack
    val tilesGlow: TextureRegionPack

    //val tileItemWall = Image(TILE_SIZE * 16, TILE_SIZE * GameWorld.TILES_SUPPORTED / 16) // 4 MB

    const val BREAKAGE_STEPS = 10

    val WALL = GameWorld.WALL
    val TERRAIN = GameWorld.TERRAIN
    val WIRE = GameWorld.WIRE
    val FLUID = -2
    val OCCLUSION = 31337

    private const val OCCLUSION_TILE_NUM_BASE = 16

    private const val NEARBY_TILE_KEY_UP = 0
    private const val NEARBY_TILE_KEY_RIGHT = 1
    private const val NEARBY_TILE_KEY_DOWN = 2
    private const val NEARBY_TILE_KEY_LEFT = 3

    private const val NEARBY_TILE_CODE_UP = 1
    private const val NEARBY_TILE_CODE_RIGHT = 2
    private const val NEARBY_TILE_CODE_DOWN = 4
    private const val NEARBY_TILE_CODE_LEFT = 8


    private const val GZIP_READBUF_SIZE = 8192


    private lateinit var terrainTilesBuffer: Array<IntArray>
    private lateinit var wallTilesBuffer: Array<IntArray>
    private lateinit var wireTilesBuffer: Array<IntArray>
    private lateinit var fluidTilesBuffer: Array<IntArray>
    private lateinit var occlusionBuffer: Array<IntArray>
    private var tilesBuffer: Pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)


    private lateinit var tilesQuad: Mesh
    private val shader = AppLoader.loadShaderFromFile("assets/4096.vert", "assets/tiling.frag")

    init {

        // PNG still doesn't work right.
        // The thing is, pixel with alpha 0 must have RGB of also 0, which PNG does not guarantee it.
        // (pixels of RGB = 255, A = 0 -- white transparent -- causes 'glow')
        // with TGA, you have a complete control over this, with the expense of added hassle on your side.
        // -- Torvald, 2018-12-19

        // CreateTileAtlas.invoke() has been moved to the AppLoader.create() //

        // create terrain texture from pixmaps
        weatherTerrains = arrayOf(
                TextureRegionPack(Texture(AppLoader.tileMaker.atlasSpring), TILE_SIZE, TILE_SIZE),
                TextureRegionPack(Texture(AppLoader.tileMaker.atlas), TILE_SIZE, TILE_SIZE),
                TextureRegionPack(Texture(AppLoader.tileMaker.atlasAutumn), TILE_SIZE, TILE_SIZE),
                TextureRegionPack(Texture(AppLoader.tileMaker.atlasWinter), TILE_SIZE, TILE_SIZE)
        )

        //TODO make wire work with the TileAtlas system
        tilesWire = TextureRegionPack(ModMgr.getGdxFile("basegame", "wires/wire.tga"), TILE_SIZE, TILE_SIZE)
        tilesFluid = TextureRegionPack(Texture(AppLoader.tileMaker.atlasFluid), TILE_SIZE, TILE_SIZE)
        tilesGlow = TextureRegionPack(Texture(AppLoader.tileMaker.atlasGlow), TILE_SIZE, TILE_SIZE)


        printdbg(this, "Making terrain and wall item textures...")



        // test print
        //PixmapIO2.writeTGA(Gdx.files.absolute("${AppLoader.defaultDir}/terrainitem.tga"), itemTerrainPixmap, false)

        tileItemTerrain = TextureRegionPack(AppLoader.tileMaker.itemTerrainTexture, TILE_SIZE, TILE_SIZE)
        tileItemWall = TextureRegionPack(AppLoader.tileMaker.itemWallTexture, TILE_SIZE, TILE_SIZE)



        // finally
        tilesTerrain = weatherTerrains[1]


        printdbg(this, "init() exit")
    }

    /**
     * See work_files/dynamic_shape_2_0.psd
     *
     * bits position: (0 being LSB)
     *
     * 5 6 7
     * 4 @ 0
     * 3 2 1
     *
     * size of this LUT must be equal to 256.
     */
    private val connectLut47 = intArrayOf(17,1,17,1,2,3,2,14,17,1,17,1,2,3,2,14,9,7,9,7,4,5,4,35,9,7,9,7,16,37,16,15,17,1,17,1,2,3,2,14,17,1,17,1,2,3,2,14,9,7,9,7,4,5,4,35,9,7,9,7,16,37,16,15,8,10,8,10,0,12,0,43,8,10,8,10,0,12,0,43,11,13,11,13,6,20,6,34,11,13,11,13,36,33,36,46,8,10,8,10,0,12,0,43,8,10,8,10,0,12,0,43,30,42,30,42,38,26,38,18,30,42,30,42,23,45,23,31,17,1,17,1,2,3,2,14,17,1,17,1,2,3,2,14,9,7,9,7,4,5,4,35,9,7,9,7,16,37,16,15,17,1,17,1,2,3,2,14,17,1,17,1,2,3,2,14,9,7,9,7,4,5,4,35,9,7,9,7,16,37,16,15,8,28,8,28,0,41,0,21,8,28,8,28,0,41,0,21,11,44,11,44,6,27,6,40,11,44,11,44,36,19,36,32,8,28,8,28,0,41,0,21,8,28,8,28,0,41,0,21,30,29,30,29,38,39,38,25,30,29,30,29,23,24,23,22)
    private val connectLut16 = intArrayOf(0,2,0,2,4,6,4,6,0,2,0,2,4,6,4,6,8,10,8,10,12,14,12,14,8,10,8,10,12,14,12,14,0,2,0,2,4,6,4,6,0,2,0,2,4,6,4,6,8,10,8,10,12,14,12,14,8,10,8,10,12,14,12,14,1,3,1,3,5,7,5,7,1,3,1,3,5,7,5,7,9,11,9,11,13,15,13,15,9,11,9,11,13,15,13,15,1,3,1,3,5,7,5,7,1,3,1,3,5,7,5,7,9,11,9,11,13,15,13,15,9,11,9,11,13,15,13,15,0,2,0,2,4,6,4,6,0,2,0,2,4,6,4,6,8,10,8,10,12,14,12,14,8,10,8,10,12,14,12,14,0,2,0,2,4,6,4,6,0,2,0,2,4,6,4,6,8,10,8,10,12,14,12,14,8,10,8,10,12,14,12,14,1,3,1,3,5,7,5,7,1,3,1,3,5,7,5,7,9,11,9,11,13,15,13,15,9,11,9,11,13,15,13,15,1,3,1,3,5,7,5,7,1,3,1,3,5,7,5,7,9,11,9,11,13,15,13,15,9,11,9,11,13,15,13,15)

    init {
        assert(256 == connectLut47.size)
        assert(256 == connectLut16.size)
    }

    /**
     * Tiles that half-transparent and has hue
     * will blend colour using colour multiplication
     * i.e. red hues get lost if you dive into the water
     */
    private val TILES_BLEND_MUL = hashSetOf(
            Block.WATER,
            Block.LAVA
    )

    /**
     * To interact with external modules
     */
    @JvmStatic fun addBlendMul(blockID: ItemID): Boolean {
        return TILES_BLEND_MUL.add(blockID)
    }

    private var drawTIME_T = 0L
    private val SECONDS_IN_MONTH = WorldTime.MONTH_LENGTH * WorldTime.DAY_LENGTH.toLong()

    ///////////////////////////////////////////
    // NO draw lightmap using colour filter, actors must also be hidden behind the darkness
    ///////////////////////////////////////////

    /**
     * Which wires should be drawn. Normally this value is set by the wiring item (e.g. wire pieces, wirecutters)
     */
    var selectedWireRenderClass = ""

    internal fun renderData() {

        try {
            drawTIME_T = (world as GameWorldExtension).worldTime.TIME_T - (WorldTime.DAY_LENGTH * 15) // offset by -15 days
            val seasonalMonth = (drawTIME_T.div(WorldTime.DAY_LENGTH) fmod WorldTime.YEAR_DAYS.toLong()).toInt() / WorldTime.MONTH_LENGTH + 1

            tilesTerrain = weatherTerrains[seasonalMonth - 1]
            tilesTerrainBlend = weatherTerrains[seasonalMonth fmod 4]
        }
        catch (e: ClassCastException) { }

        measureDebugTime("Renderer.Tiling") {
            drawTiles(WALL)
            drawTiles(TERRAIN) // regular tiles
            drawTiles(FLUID)
            drawTiles(OCCLUSION)
            //drawTiles(WIRE)
        }
    }

    internal fun drawWall(projectionMatrix: Matrix4, drawGlow: Boolean) {
        gdxSetBlendNormal()
        renderUsingBuffer(WALL, projectionMatrix, drawGlow)

        gdxSetBlendMul()
        renderUsingBuffer(OCCLUSION, projectionMatrix, false)
    }

    internal fun drawTerrain(projectionMatrix: Matrix4, drawGlow: Boolean) {
        gdxSetBlendNormal()

        renderUsingBuffer(TERRAIN, projectionMatrix, drawGlow)
        renderUsingBuffer(FLUID, projectionMatrix, drawGlow)
    }


    internal fun drawFront(projectionMatrix: Matrix4) {
        gdxSetBlendMul()

        // let's just not MUL on terrain, make it FLUID only...
        renderUsingBuffer(FLUID, projectionMatrix, false)



        gdxSetBlendNormal()

        if (selectedWireRenderClass.isNotBlank()) {
            //println("Wires! draw: $drawWires") // use F10 instead
            renderUsingBuffer(WIRE, projectionMatrix, false)
        }
    }

    /**
     * Returns a tile number as if we're addressing tile number in the main atlas. That is, returning int of
     * 18 means will point to the tile (32, 1) of the fluid atlas.
     *
     * This behaviour is to keep compatibility with World.getTile() method, this method need to mimic the World's
     * behaviour to return "starting point" of the tile, so nearby information (int 0..15) can simply be added to
     * the X-position that can be deduced from the tile number.
     *
     * As a consequence, fluids.tga must have the same width as tiles.tga.
     */
    private fun GameWorld.FluidInfo.toTileInFluidAtlas(): Int {
        val fluidNum = this.type.abs()

        if (this.amount >= WorldSimulator.FLUID_MIN_MASS) {
            val fluidLevel = this.amount.coerceIn(0f,1f).times(15).roundToInt()

            return fluidLevel * 16 + fluidNum
        }
        else {
            return 0
        }
    }

    /**
     * Turns bitmask-with-single-bit-set into its bit index. The LSB is counted as 1, and thus the index starts at one.
     * @return 0 -> -1, 1 -> 0, 2 -> 1, 4 -> 2, 8 -> 3, 16 -> 4, ...
     */
    private fun Int.toBitOrd(): Int {
        val k = FastMath.intLog2(this, -1)
        return k
    }

    /**
     * Writes to buffer. Actual draw code must be called after this operation.
     *
     * @param drawModeTilesBlendMul If current drawing mode is MULTIPLY. Doesn't matter if mode is FLUID.
     * @param wire coduitTypes bit that is selected to be drawn. Must be the power of two.
     */
    private fun drawTiles(mode: Int) {
        // can't be "WorldCamera.y / TILE_SIZE":
        //      ( 3 / 16) == 0
        //      (-3 / 16) == -1  <-- We want it to be '-1', not zero
        // using cast and floor instead of IF on ints: the other way causes jitter artefact, which I don't fucking know why

        // TODO the real fluid rendering must use separate function, but its code should be similar to this.
        //      shader's tileAtlas will be fluid.tga, pixels written to the buffer is in accordance with the new
        //      atlas. IngameRenderer must be modified so that fluid-draw call is separated from drawing tiles.
        //      The MUL draw mode can be removed from this (it turns out drawing tinted glass is tricky because of
        //      the window frame which should NOT be MUL'd)


        val for_y_start = (WorldCamera.y.toFloat() / TILE_SIZE).floorInt()
        val for_y_end = for_y_start + tilesBuffer.height - 1

        val for_x_start = (WorldCamera.x.toFloat() / TILE_SIZE).floorInt()
        val for_x_end = for_x_start + tilesBuffer.width - 1

        // loop
        for (y in for_y_start..for_y_end) {
            for (x in for_x_start..for_x_end) {

                val bufferX = x - for_x_start
                val bufferY = y - for_y_start

                val thisTile: ItemID = when (mode) {
                    WALL -> world.getTileFromWall(x, y)
                    TERRAIN -> world.getTileFromTerrain(x, y)
                    WIRE -> "basegame:-1" // TODO need new wire storing format //world.getWiringBlocks(x, y).and(drawWires).toBitOrd() * 16
                    FLUID -> "basegame:-1" // TODO need new wire storing format //world.getFluid(x, y).type.abs()
                    OCCLUSION -> "placeholder_occlusion"
                    else -> throw IllegalArgumentException()
                }


                // draw a tile
                val nearbyTilesInfo = if (mode == OCCLUSION) {
                    getNearbyTilesInfoFakeOcc(x, y)
                }
                else if (mode == FLUID) {
                    getNearbyTilesInfoFluids(x, y)
                }
                else if (mode == WIRE) {
                    getNearbyWiringInfo(x, y, thisTile)
                }
                else if (isPlatform(thisTile)) {
                    getNearbyTilesInfoPlatform(x, y)
                }
                else if (isWallSticker(thisTile)) {
                    getNearbyTilesInfoWallSticker(x, y)
                }
                else if (isConnectMutual(thisTile)) {
                    getNearbyTilesInfoConMutual(x, y, mode)
                }
                else if (isConnectSelf(thisTile)) {
                    getNearbyTilesInfoConSelf(x, y, mode, thisTile)
                }
                else {
                    0
                }

                val renderTag = AppLoader.tileMaker.getRenderTag(thisTile)
                val tileNumberBase =
                        if (mode == OCCLUSION)
                            OCCLUSION_TILE_NUM_BASE
                        else if (mode == FLUID)
                            AppLoader.tileMaker.fluidToTileNumber(world.getFluid(x, y))
                        else if (mode == WIRE)
                            0 // TODO need new wire storing format
                        else
                            renderTag.tileNumber
                val tileNumber = if (mode != WIRE && thisTile == Block.AIR) 0
                // special case: fluids
                else if (mode == FLUID) tileNumberBase + connectLut47[nearbyTilesInfo]
                // special case: wires
                else if (mode == WIRE) tileNumberBase + connectLut16[nearbyTilesInfo]
                // special case: occlusion
                else if (mode == OCCLUSION)
                    tileNumberBase + connectLut47[nearbyTilesInfo]
                // rest of the cases: terrain and walls
                else tileNumberBase + when (renderTag.maskType) {
                        CreateTileAtlas.RenderTag.MASK_NA -> 0
                        CreateTileAtlas.RenderTag.MASK_16 -> connectLut16[nearbyTilesInfo]
                        CreateTileAtlas.RenderTag.MASK_47 -> connectLut47[nearbyTilesInfo]
                        CreateTileAtlas.RenderTag.MASK_TORCH, CreateTileAtlas.RenderTag.MASK_PLATFORM -> nearbyTilesInfo
                        else -> throw IllegalArgumentException("Unknown mask type: ${renderTag.maskType}")
                    }

                var thisTileX = tileNumber % AppLoader.tileMaker.TILES_IN_X
                var thisTileY = tileNumber / AppLoader.tileMaker.TILES_IN_X

                if (mode == FLUID && thisTileX == 22 && thisTileY == 3) {
                    //println("tileNumberBase = $tileNumberBase, tileNumber = $tileNumber, fluid = ${world.getFluid(x, y)}")
                }

                val breakage = if (mode == TERRAIN) world.getTerrainDamage(x, y) else world.getWallDamage(x, y)
                val maxHealth = BlockCodex[world.getTileFromTerrain(x, y)].strength
                val breakingStage = (breakage / maxHealth).times(BREAKAGE_STEPS).roundToInt()



                // draw a tile

                if (mode == WIRE) {
                    // no wire here, draw block id 255 (bottom right)
                    writeToBuffer(mode, bufferX, bufferY, 15, 15, 0)
                }
                else if (mode == OCCLUSION || mode == FLUID) {
                    writeToBuffer(mode, bufferX, bufferY, thisTileX, thisTileY, 0)
                }
                else {
                    writeToBuffer(mode, bufferX, bufferY, thisTileX, thisTileY, breakingStage)
                }

            }
        }
    }

    private fun getNearbyTilesPos(x: Int, y: Int): Array<Point2i> {
        return arrayOf(
                Point2i(x + 1, y),
                Point2i(x + 1, y + 1),
                Point2i(x, y + 1),
                Point2i(x - 1, y + 1),
                Point2i(x - 1, y),
                Point2i(x - 1, y - 1),
                Point2i(x, y - 1),
                Point2i(x + 1, y - 1)
        )
    }

    private fun getNearbyTilesInfoConSelf(x: Int, y: Int, mode: Int, mark: ItemID?): Int {
        val nearbyTiles = getNearbyTilesPos(x, y).map { world.getTileFrom(mode, it.x, it.y) ?: Block.NULL }

        var ret = 0
        for (i in nearbyTiles.indices) {
            if (nearbyTiles[i] == mark) {
                ret += (1 shl i) // add 1, 2, 4, 8 for i = 0, 1, 2, 3
            }
        }

        return ret
    }

    private fun getNearbyTilesInfoFakeOcc(x: Int, y: Int): Int {
        val eligible = BlockCodex[world.getTileFromWall(x, y)].isSolid && !BlockCodex[world.getTileFromTerrain(x, y)].isSolid
        val nearbyTiles = getNearbyTilesPos(x, y).map {
            !BlockCodex[world.getTileFromTerrain(it.x, it.y)].isSolid
        }

        if (!eligible) return 255

        var ret = 0
        for (i in nearbyTiles.indices) {
            if (nearbyTiles[i] == true) {
                ret += (1 shl i) // add 1, 2, 4, 8 for i = 0, 1, 2, 3
            }
        }

        return ret
    }

    /**
     * @param wire -1 for none, 0 for signal red, 1 for untility prototype, 2 for low power, 3 for high power;
     * log of bits defined in [net.torvald.terrarum.blockproperties.Wire]
     *
     * @return offset from the spritesheet's "base" tile number, 0..15.
     */
    private fun getNearbyWiringInfo(x: Int, y: Int, wire: ItemID): Int {
        return 0 // TODO need new wire storing format
        /*val nearbyTiles = getNearbyTilesPos(x, y).map { world.getWiringBlocks(it.x, it.y).and(drawWires).toBitOrd() * 16 }

        var ret = 0
        for (i in nearbyTiles.indices) {
            if (nearbyTiles[i] == wire) {
                ret += (1 shl i) // add 1, 2, 4, 8 for i = 0, 1, 2, 3
            }
        }

        return ret*/
    }

    private fun getNearbyTilesInfoConMutual(x: Int, y: Int, mode: Int): Int {
        val nearbyTiles: List<ItemID> = getNearbyTilesPos(x, y).map { world.getTileFrom(mode, it.x, it.y)!! }

        var ret = 0
        for (i in nearbyTiles.indices) {
            if (BlockCodex[nearbyTiles[i]].isSolid) {
                ret += (1 shl i) // add 1, 2, 4, 8 for i = 0, 1, 2, 3
            }
        }

        return ret
    }

    /**
     * Basically getNearbyTilesInfoConMutual() but connects mutually with all the fluids
     */
    private fun getNearbyTilesInfoFluids(x: Int, y: Int): Int {
        val nearbyPos = getNearbyTilesPos(x, y)
        val nearbyTiles: List<ItemID> = nearbyPos.map { world.getTileFromTerrain(it.x, it.y) }

        var ret = 0
        for (i in nearbyTiles.indices) {
            val fluid = world.getFluid(nearbyPos[i].x, nearbyPos[i].y)
            if (BlockCodex[nearbyTiles[i]].isSolid || (fluid.isFluid() && 0 < AppLoader.tileMaker.fluidFillToTileLevel(fluid.amount))) {
                ret += (1 shl i) // add 1, 2, 4, 8 for i = 0, 1, 2, 3
            }
        }

        return ret
    }

    private fun getNearbyTilesInfoWallSticker(x: Int, y: Int): Int {
        val nearbyTiles = arrayOf(Block.NULL, Block.NULL, Block.NULL, Block.NULL)
        val NEARBY_TILE_KEY_BACK = NEARBY_TILE_KEY_UP
        nearbyTiles[NEARBY_TILE_KEY_LEFT] =  world.getTileFrom(TERRAIN, x - 1, y)
        nearbyTiles[NEARBY_TILE_KEY_RIGHT] = world.getTileFrom(TERRAIN, x + 1, y)
        nearbyTiles[NEARBY_TILE_KEY_DOWN] =  world.getTileFrom(TERRAIN, x    , y + 1)
        nearbyTiles[NEARBY_TILE_KEY_BACK] =  world.getTileFrom(WALL,    x    , y)

        try {
            if (BlockCodex[nearbyTiles[NEARBY_TILE_KEY_DOWN]].isSolid)
            // has tile on the bottom
                return 3
            else if (BlockCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolid
                     && BlockCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolid)
            // has tile on both sides
                return 0
            else if (BlockCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolid)
            // has tile on the right
                return 2
            else if (BlockCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolid)
            // has tile on the left
                return 1
            else if (BlockCodex[nearbyTiles[NEARBY_TILE_KEY_BACK]].isSolid)
            // has tile on the back
                return 0
            else
                return 3
        } catch (e: ArrayIndexOutOfBoundsException) {
            return if (BlockCodex[nearbyTiles[NEARBY_TILE_KEY_DOWN]].isSolid)
            // has tile on the bottom
                3 else 0
        }
    }

    private fun getNearbyTilesInfoPlatform(x: Int, y: Int): Int {
        val nearbyTiles = arrayOf(Block.NULL, Block.NULL, Block.NULL, Block.NULL)
        val NEARBY_TILE_KEY_BACK = NEARBY_TILE_KEY_UP
        nearbyTiles[NEARBY_TILE_KEY_LEFT] =  world.getTileFrom(TERRAIN, x - 1, y)
        nearbyTiles[NEARBY_TILE_KEY_LEFT] = world.getTileFrom(TERRAIN, x - 1, y)
        nearbyTiles[NEARBY_TILE_KEY_RIGHT] = world.getTileFrom(TERRAIN, x + 1, y)

        if ((BlockCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolid &&
             BlockCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolid) ||
            isPlatform(nearbyTiles[NEARBY_TILE_KEY_LEFT]) &&
            isPlatform(nearbyTiles[NEARBY_TILE_KEY_RIGHT])) // LR solid || LR platform
            return 0
        else if (BlockCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolid &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_LEFT]) &&
                 !BlockCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolid &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_RIGHT])) // L solid and not platform && R not solid and not platform
            return 4
        else if (BlockCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolid &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_RIGHT]) &&
                 !BlockCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolid &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_LEFT])) // R solid and not platform && L not solid and nto platform
            return 6
        else if (BlockCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolid &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_LEFT])) // L solid && L not platform
            return 3
        else if (BlockCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolid &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_RIGHT])) // R solid && R not platform
            return 5
        else if ((BlockCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolid ||
                  isPlatform(nearbyTiles[NEARBY_TILE_KEY_LEFT])) &&
                 !BlockCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolid &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_RIGHT])) // L solid or platform && R not solid and not platform
            return 1
        else if ((BlockCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolid ||
                  isPlatform(nearbyTiles[NEARBY_TILE_KEY_RIGHT])) &&
                 !BlockCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolid &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_LEFT])) // R solid or platform && L not solid and not platform
            return 2
        else
            return 7
    }

    /**
     * Raw format of RGBA8888, where RGB portion actually encodes the absolute tile number and A is always 255.
     *
     * @return Raw colour bits in RGBA8888 format
     */
    private fun sheetXYToTilemapColour(mode: Int, sheetX: Int, sheetY: Int, breakage: Int): Int =
            // the tail ".or(255)" is there to write 1.0 to the A channel (remember, return type is RGBA)

            // this code is synced to the tilesTerrain's tile configuration, but everything else is hard-coded
            // right now.
            (tilesTerrain.horizontalCount * sheetY + sheetX).shl(8).or(255) or // the actual tile bits
                breakage.and(15).shl(28) // breakage bits


    private fun writeToBuffer(mode: Int, bufferPosX: Int, bufferPosY: Int, sheetX: Int, sheetY: Int, breakage: Int) {
        val sourceBuffer = when(mode) {
            TERRAIN -> terrainTilesBuffer
            WALL -> wallTilesBuffer
            WIRE -> wireTilesBuffer
            FLUID -> fluidTilesBuffer
            OCCLUSION -> occlusionBuffer
            else -> throw IllegalArgumentException()
        }


        sourceBuffer[bufferPosY][bufferPosX] = sheetXYToTilemapColour(mode, sheetX, sheetY, breakage)
    }

    private var _tilesBufferAsTex: Texture = Texture(1, 1, Pixmap.Format.RGBA8888)
    private val occlusionIntensity = 0.3f

    private fun renderUsingBuffer(mode: Int, projectionMatrix: Matrix4, drawGlow: Boolean) {
        //Gdx.gl.glClearColor(.094f, .094f, .094f, 0f)
        //Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)


        //val tilesInHorizontal = tilesBuffer.width
        //val tilesInVertical =   tilesBuffer.height


        val tileAtlas = when (mode) {
            TERRAIN, WALL, OCCLUSION -> tilesTerrain
            WIRE -> tilesWire
            FLUID -> tilesFluid
            else -> throw IllegalArgumentException()
        }
        val sourceBuffer = when(mode) {
            TERRAIN -> terrainTilesBuffer
            WALL -> wallTilesBuffer
            WIRE -> wireTilesBuffer
            FLUID -> fluidTilesBuffer
            OCCLUSION -> occlusionBuffer
            else -> throw IllegalArgumentException()
        }
        val vertexColour = when (mode) {
            TERRAIN, WIRE, FLUID, OCCLUSION -> Color.WHITE
            WALL -> AppLoader.tileMaker.wallOverlayColour
            else -> throw IllegalArgumentException()
        }


        // write to colour buffer
        // As the texture size is very small, multithreading it would be less effective
        for (y in 0 until tilesBuffer.height) {
            for (x in 0 until tilesBuffer.width) {
                val color = sourceBuffer[y][x]
                tilesBuffer.setColor(color)
                tilesBuffer.drawPixel(x, y)
            }
        }


        _tilesBufferAsTex.dispose()
        _tilesBufferAsTex = Texture(tilesBuffer)
        _tilesBufferAsTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

        if (drawGlow) {
            tilesGlow.texture.bind(2)
            _tilesBufferAsTex.bind(1) // trying 1 and 0...
            tilesGlow.texture.bind(0) // for some fuck reason, it must be bound as last
        }
        else {
            tilesTerrainBlend.texture.bind(2)
            _tilesBufferAsTex.bind(1) // trying 1 and 0...
            tileAtlas.texture.bind(0) // for some fuck reason, it must be bound as last
        }

        shader.begin()
        shader.setUniformMatrix("u_projTrans", projectionMatrix)//camera.combined)
        shader.setUniformf("colourFilter", vertexColour)
        shader.setUniformf("screenDimension", Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        shader.setUniformi("tilesAtlas", 0)
        shader.setUniformi("tilesBlendAtlas", 2)
        shader.setUniformi("tilemap", 1)
        shader.setUniformi("tilemapDimension", tilesBuffer.width, tilesBuffer.height)
        shader.setUniformf("tilesInAxes", tilesInHorizontal.toFloat(), tilesInVertical.toFloat())
        shader.setUniformi("cameraTranslation", WorldCamera.x fmod TILE_SIZE, WorldCamera.y fmod TILE_SIZE) // usage of 'fmod' and '%' were depend on the for_x_start, which I can't just do naive int div
        shader.setUniformf("tilesInAtlas", tileAtlas.horizontalCount.toFloat(), tileAtlas.verticalCount.toFloat()) //depends on the tile atlas
        shader.setUniformf("atlasTexSize", tileAtlas.texture.width.toFloat(), tileAtlas.texture.height.toFloat()) //depends on the tile atlas
        // set the blend value as world's time progresses, in linear fashion
        shader.setUniformf("tilesBlend", if (world is GameWorldExtension && (mode == TERRAIN || mode == WALL))
            drawTIME_T.fmod(SECONDS_IN_MONTH) / SECONDS_IN_MONTH.toFloat()
        else
            0f
        )
        shader.setUniformf("mulBlendIntensity", if (mode == OCCLUSION) occlusionIntensity else 1f)
        //shader.setUniformf("drawBreakage", if (mode == WIRE) 0f else 1f)
        tilesQuad.render(shader, GL20.GL_TRIANGLES)
        shader.end()

        //tilesBufferAsTex.dispose()
    }

    private var oldScreenW = 0
    private var oldScreenH = 0

    var tilesInHorizontal = -1; private set
    var tilesInVertical = -1; private set

    fun resize(screenW: Int, screenH: Int) {
        tilesInHorizontal = (AppLoader.screenSize.screenWf / TILE_SIZE).ceilInt() + 1
        tilesInVertical = (AppLoader.screenSize.screenHf / TILE_SIZE).ceilInt() + 1

        val oldTH = (oldScreenW.toFloat() / TILE_SIZE).ceilInt() + 1
        val oldTV = (oldScreenH.toFloat() / TILE_SIZE).ceilInt() + 1

        // only update if it's really necessary
        if (oldTH != tilesInHorizontal || oldTV != tilesInVertical) {
            terrainTilesBuffer = Array<IntArray>(tilesInVertical, { kotlin.IntArray(tilesInHorizontal) })
            wallTilesBuffer = Array<IntArray>(tilesInVertical, { kotlin.IntArray(tilesInHorizontal) })
            wireTilesBuffer = Array<IntArray>(tilesInVertical, { kotlin.IntArray(tilesInHorizontal) })
            fluidTilesBuffer = Array<IntArray>(tilesInVertical, { kotlin.IntArray(tilesInHorizontal) })
            occlusionBuffer = Array<IntArray>(tilesInVertical, { kotlin.IntArray(tilesInHorizontal) })

            tilesBuffer.dispose()
            tilesBuffer = Pixmap(tilesInHorizontal, tilesInVertical, Pixmap.Format.RGBA8888)
        }

        if (oldScreenW != screenW || oldScreenH != screenH) {
            tilesQuad = Mesh(
                    true, 4, 6,
                    VertexAttribute.Position(),
                    VertexAttribute.ColorUnpacked(),
                    VertexAttribute.TexCoords(0)
            )

            tilesQuad.setVertices(floatArrayOf( // WARNING! not ususal quads; TexCoords of Y is flipped
                    0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 0f,
                    AppLoader.screenSize.screenWf, 0f, 0f, 1f, 1f, 1f, 1f, 1f, 0f,
                    AppLoader.screenSize.screenWf, AppLoader.screenSize.screenHf, 0f, 1f, 1f, 1f, 1f, 1f, 1f,
                    0f, AppLoader.screenSize.screenHf, 0f, 1f, 1f, 1f, 1f, 0f, 1f
            ))
            tilesQuad.setIndices(shortArrayOf(0, 1, 2, 2, 3, 0))
        }

        oldScreenW = screenW
        oldScreenH = screenH


        printdbg(this, "Resize event")

    }

    fun clampH(x: Int): Int {
        if (x < 0) {
            return 0
        } else if (x > world.height * TILE_SIZE) {
            return world.height * TILE_SIZE
        } else {
            return x
        }
    }

    fun clampWTile(x: Int): Int {
        if (x < 0) {
            return 0
        } else if (x > world.width) {
            return world.width
        } else {
            return x
        }
    }

    fun clampHTile(x: Int): Int {
        if (x < 0) {
            return 0
        } else if (x > world.height) {
            return world.height
        } else {
            return x
        }
    }

    fun dispose() {
        printdbg(this, "dispose called by")
        printStackTrace(this)

        weatherTerrains.forEach { it.dispose() }
        tilesGlow.dispose()
        tilesWire.dispose()
        tileItemTerrain.dispose()
        tileItemWall.dispose()
        tilesFluid.dispose()
        tilesBuffer.dispose()
        _tilesBufferAsTex.dispose()
        tilesQuad.dispose()
        shader.dispose()

        AppLoader.tileMaker.dispose()
    }

    fun getRenderStartX(): Int = WorldCamera.x / TILE_SIZE
    fun getRenderStartY(): Int = WorldCamera.y / TILE_SIZE

    fun getRenderEndX(): Int = clampWTile(getRenderStartX() + (WorldCamera.width / TILE_SIZE) + 2)
    fun getRenderEndY(): Int = clampHTile(getRenderStartY() + (WorldCamera.height / TILE_SIZE) + 2)

    fun isConnectSelf(b: ItemID): Boolean = AppLoader.tileMaker.getRenderTag(b).connectionType == CreateTileAtlas.RenderTag.CONNECT_SELF
    fun isConnectMutual(b: ItemID): Boolean = AppLoader.tileMaker.getRenderTag(b).connectionType == CreateTileAtlas.RenderTag.CONNECT_MUTUAL
    fun isWallSticker(b: ItemID): Boolean = AppLoader.tileMaker.getRenderTag(b).connectionType == CreateTileAtlas.RenderTag.CONNECT_WALL_STICKER
    fun isPlatform(b: ItemID): Boolean = AppLoader.tileMaker.getRenderTag(b).connectionType == CreateTileAtlas.RenderTag.CONNECT_WALL_STICKER_CONNECT_SELF
    //fun isBlendMul(b: Int): Boolean = TILES_BLEND_MUL.contains(b)

    fun tileInCamera(x: Int, y: Int) =
            x >= WorldCamera.x.div(TILE_SIZE) && y >= WorldCamera.y.div(TILE_SIZE) &&
            x <= WorldCamera.x.plus(WorldCamera.width).div(TILE_SIZE) && y <= WorldCamera.y.plus(WorldCamera.width).div(TILE_SIZE)
}
