package net.torvald.terrarum.worlddrawer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.math.Matrix4
import net.torvald.terrarum.*
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.PairedMapLayer
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.modulebasegame.gameworld.GameWorldExtension
import net.torvald.terrarum.modulebasegame.gameworld.WorldSimulator
import net.torvald.terrarum.modulebasegame.gameworld.WorldTime
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream
import kotlin.math.roundToInt


/**
 * Sub-portion of IngameRenderer. You are not supposed to directly deal with this.
 *
 * The terrain texture atlas is HARD CODED as "4096x4096, on which 256x256 tiles are contained"
 * in the shader (tiling.frag). This will not be a problem in the base game, but if you are modifying
 * this engine for your project, you must edit the shader program accordingly.
 *
 * Created by minjaesong on 2016-01-19.
 */
internal object BlocksDrawer {

    var world: GameWorld = GameWorld.makeNullWorld()

    private val TILE_SIZE = FeaturesDrawer.TILE_SIZE
    private val TILE_SIZEF = FeaturesDrawer.TILE_SIZE.toFloat()

    /**
     * Widths of the tile atlases must have exactly the same width (height doesn't matter)
     * If not, the engine will choose wrong tile for a number you provided.
     */

    /** Index zero: spring */
    val weatherTerrains: Array<TextureRegionPack>
    lateinit var tilesTerrain: TextureRegionPack; private set
    lateinit var tilesTerrainBlend: TextureRegionPack; private set
    val tilesWire: TextureRegionPack
    val tileItemWall: TextureRegionPack
    val tilesFluid: TextureRegionPack

    //val tileItemWall = Image(TILE_SIZE * 16, TILE_SIZE * GameWorld.TILES_SUPPORTED / 16) // 4 MB


    val wallOverlayColour = Color(5f/9f,5f/9f,5f/9f,1f)

    const val BREAKAGE_STEPS = 10
    const val TILES_PER_BLOCK = PairedMapLayer.RANGE
    const val BLOCKS_IN_ATLAS_X = 16  // assuming atlas size of 4096x4096 px
    const val BLOCKS_IN_ATLAS_Y = 256 // assuming atlas size of 4096x4096 px

    val WALL = GameWorld.WALL
    val TERRAIN = GameWorld.TERRAIN
    val WIRE = GameWorld.WIRE
    val FLUID = -2

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
    private var tilesBuffer: Pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)


    private lateinit var tilesQuad: Mesh
    private val shader = AppLoader.loadShader("assets/4096.vert", "assets/tiling.frag")

    init {
        printdbg(this, "Unpacking textures...")

        // PNG still doesn't work right.
        // The thing is, pixel with alpha 0 must have RGB of also 0, which PNG does not guarantee it.
        // (pixels of RGB = 255, A = 0 -- white transparent -- causes 'glow')
        // with TGA, you have a complete control over this, with the expense of added hassle on your side.
        // -- Torvald, 2018-12-19

        // hard-coded as tga.gz
        val gzFileList = listOf("blocks/wire.tga.gz", "blocks/fluids.tga.gz", "blocks/terrain_spring.tga.gz", "blocks/terrain.tga.gz", "blocks/terrain_autumn.tga.gz", "blocks/terrain_winter.tga.gz")
        val gzTmpFName = listOf("tmp_wire.tga", "tmp_fluids.tga", "tmp_vor.tga", "tmp_sumar.tga", "tmp_haust.tga", "tmp_vetter.tga")
        // unzip GZIP temporarily
        gzFileList.forEachIndexed { index, filename ->
            val terrainTexFile = ModMgr.getGdxFile("basegame", filename)
            val gzi = GZIPInputStream(terrainTexFile.read(GZIP_READBUF_SIZE))
            val wholeFile = gzi.readBytes()
            gzi.close()
            val fos = BufferedOutputStream(FileOutputStream(gzTmpFName[index]))
            fos.write(wholeFile)
            fos.flush()
            fos.close()
        }

        val _wirePixMap = Pixmap(Gdx.files.internal(gzTmpFName[0]))
        val _fluidPixMap = Pixmap(Gdx.files.internal(gzTmpFName[1]))

        val _terrainPixMap = Array(4) { Pixmap(Gdx.files.internal(gzTmpFName[it + 2])) }

        // delete temp files
        gzTmpFName.forEach { File(it).delete() }


        weatherTerrains = Array(4) {
            val t = TextureRegionPack(Texture(_terrainPixMap[it]), TILE_SIZE, TILE_SIZE)
            t.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
            t
        }

        tilesWire = TextureRegionPack(Texture(_wirePixMap), TILE_SIZE, TILE_SIZE)
        tilesWire.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        tilesFluid = TextureRegionPack(Texture(_fluidPixMap), TILE_SIZE, TILE_SIZE)
        tilesFluid.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

        // also dispose unused temp files
        //terrainPixMap.dispose() // commented: tileItemWall needs it
        _wirePixMap.dispose()
        _fluidPixMap.dispose()



        printdbg(this, "Making wall item textures...")

        // create item_wall images
        // because some limitation, we'll use summer image
        // CPU render shits (lots of magic numbers xD)
        val itemWallPixmap = Pixmap(_terrainPixMap[1].width / 16, _terrainPixMap[1].height, Pixmap.Format.RGBA8888)
        for (blocksCol in 0 until itemWallPixmap.width / TILE_SIZE) { // 0..15
            val pxColStart = blocksCol * 256 // 0, 256, 512, 768, ...
            for (pxCol in pxColStart until pxColStart + TILE_SIZE) {
                val x = blocksCol * TILE_SIZE + (pxCol fmod TILE_SIZE)

                for (y in 0 until itemWallPixmap.height) {
                    val colour = Color(_terrainPixMap[1].getPixel(pxCol, y)) mulAndAssign wallOverlayColour
                    itemWallPixmap.drawPixel(x, y, colour.toRGBA())
                }
            }
        }

        // test print
        //PixmapIO2.writeTGA(Gdx.files.local("wallitem.tga"), itemWallPixmap, false)

        val itemWallTexture = Texture(itemWallPixmap)
        itemWallPixmap.dispose()
        tileItemWall = TextureRegionPack(itemWallTexture, TILE_SIZE, TILE_SIZE)



        // finally
        _terrainPixMap.forEach { it.dispose() }
        tilesTerrain = weatherTerrains[1]


        printdbg(this, "init() exit")
    }

    /**
     * Connectivity group 01 : artificial tiles
     * It holds different shading rule to discriminate with group 02, index 0 is single tile.
     * These are the tiles that only connects to itself, will not connect to colour variants
     */
    private val TILES_CONNECT_SELF = hashSetOf(
            Block.GLASS_CRUDE,
            Block.GLASS_CLEAN,
            Block.ILLUMINATOR_BLACK,
            Block.ILLUMINATOR_BLUE,
            Block.ILLUMINATOR_BROWN,
            Block.ILLUMINATOR_CYAN,
            Block.ILLUMINATOR_FUCHSIA,
            Block.ILLUMINATOR_GREEN,
            Block.ILLUMINATOR_GREEN_DARK,
            Block.ILLUMINATOR_GREY_DARK,
            Block.ILLUMINATOR_GREY_LIGHT,
            Block.ILLUMINATOR_GREY_MED,
            Block.ILLUMINATOR_ORANGE,
            Block.ILLUMINATOR_PURPLE,
            Block.ILLUMINATOR_RED,
            Block.ILLUMINATOR_TAN,
            Block.ILLUMINATOR_WHITE,
            Block.ILLUMINATOR_YELLOW,
            Block.ILLUMINATOR_BLACK_OFF,
            Block.ILLUMINATOR_BLUE_OFF,
            Block.ILLUMINATOR_BROWN_OFF,
            Block.ILLUMINATOR_CYAN_OFF,
            Block.ILLUMINATOR_FUCHSIA_OFF,
            Block.ILLUMINATOR_GREEN_OFF,
            Block.ILLUMINATOR_GREEN_DARK_OFF,
            Block.ILLUMINATOR_GREY_DARK_OFF,
            Block.ILLUMINATOR_GREY_LIGHT_OFF,
            Block.ILLUMINATOR_GREY_MED_OFF,
            Block.ILLUMINATOR_ORANGE_OFF,
            Block.ILLUMINATOR_PURPLE_OFF,
            Block.ILLUMINATOR_RED_OFF,
            Block.ILLUMINATOR_TAN_OFF,
            Block.ILLUMINATOR_WHITE_OFF,
            Block.ILLUMINATOR_YELLOW_OFF,
            Block.DAYLIGHT_CAPACITOR,

            Block.ORE_COPPER,
            Block.ORE_IRON,
            Block.ORE_GOLD,
            Block.ORE_SILVER,
            Block.ORE_ILMENITE,
            Block.ORE_AURICHALCUM
    )

    /**
     * To interact with external modules
     */
    @JvmStatic fun addConnectSelf(blockID: Int): Boolean {
        return TILES_CONNECT_SELF.add(blockID)
    }

    /**
     * Connectivity group 02 : natural tiles
     * It holds different shading rule to discriminate with group 01, index 0 is middle tile.
     */
    private val TILES_CONNECT_MUTUAL = hashSetOf(
            Block.STONE,
            Block.STONE_QUARRIED,
            Block.STONE_TILE_WHITE,
            Block.STONE_BRICKS,
            Block.DIRT,
            Block.GRASS,
            Block.GRASSWALL,
            Block.PLANK_BIRCH,
            Block.PLANK_BLOODROSE,
            Block.PLANK_EBONY,
            Block.PLANK_NORMAL,
            Block.SAND,
            Block.SAND_WHITE,
            Block.SAND_RED,
            Block.SAND_DESERT,
            Block.SAND_BLACK,
            Block.SAND_GREEN,
            Block.GRAVEL,
            Block.GRAVEL_GREY,
            Block.SNOW,
            Block.ICE_NATURAL,
            Block.ICE_MAGICAL,

            Block.SANDSTONE,
            Block.SANDSTONE_BLACK,
            Block.SANDSTONE_DESERT,
            Block.SANDSTONE_RED,
            Block.SANDSTONE_WHITE,
            Block.SANDSTONE_GREEN

            /*lock.WATER,
            Block.WATER_1,
            Block.WATER_2,
            Block.WATER_3,
            Block.WATER_4,
            Block.WATER_5,
            Block.WATER_6,
            Block.WATER_7,
            Block.WATER_8,
            Block.WATER_9,
            Block.WATER_10,
            Block.WATER_11,
            Block.WATER_12,
            Block.WATER_13,
            Block.WATER_14,
            Block.WATER_15,
            Block.LAVA,
            Block.LAVA_1,
            Block.LAVA_2,
            Block.LAVA_3,
            Block.LAVA_4,
            Block.LAVA_5,
            Block.LAVA_6,
            Block.LAVA_7,
            Block.LAVA_8,
            Block.LAVA_9,
            Block.LAVA_10,
            Block.LAVA_11,
            Block.LAVA_12,
            Block.LAVA_13,
            Block.LAVA_14,
            Block.LAVA_15*/
    )

    /**
     * To interact with external modules
     */
    @JvmStatic fun addConnectMutual(blockID: Int): Boolean {
        return TILES_CONNECT_MUTUAL.add(blockID)
    }

    /**
     * Torches, levers, switches, ...
     */
    private val TILES_WALL_STICKER = hashSetOf(
            Block.TORCH,
            Block.TORCH_FROST,
            Block.TORCH_OFF,
            Block.TORCH_FROST_OFF
    )

    /**
     * To interact with external modules
     */
    @JvmStatic fun addWallSticker(blockID: Int): Boolean {
        return TILES_WALL_STICKER.add(blockID)
    }

    /**
     * platforms, ...
     */
    private val TILES_WALL_STICKER_CONNECT_SELF = hashSetOf(
            Block.PLATFORM_BIRCH,
            Block.PLATFORM_BLOODROSE,
            Block.PLATFORM_EBONY,
            Block.PLATFORM_STONE,
            Block.PLATFORM_WOODEN
    )

    /**
     * To interact with external modules
     */
    @JvmStatic fun addWallStickerConnectSelf(blockID: Int): Boolean {
        return TILES_WALL_STICKER_CONNECT_SELF.add(blockID)
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
    @JvmStatic fun addBlendMul(blockID: Int): Boolean {
        return TILES_BLEND_MUL.add(blockID)
    }

    private var drawTIME_T = 0L
    private val SECONDS_IN_MONTH = WorldTime.MONTH_LENGTH * WorldTime.DAY_LENGTH.toLong()

    ///////////////////////////////////////////
    // NO draw lightmap using colour filter, actors must also be hidden behind the darkness
    ///////////////////////////////////////////

    internal fun renderData() {

        try {
            drawTIME_T = (world as GameWorldExtension).time.TIME_T - (WorldTime.DAY_LENGTH * 15) // offset by -15 days
            val seasonalMonth = (drawTIME_T.div(WorldTime.DAY_LENGTH) fmod WorldTime.YEAR_DAYS.toLong()).toInt() / WorldTime.MONTH_LENGTH + 1

            tilesTerrain = weatherTerrains[seasonalMonth - 1]
            tilesTerrainBlend = weatherTerrains[seasonalMonth fmod 4]
        }
        catch (e: ClassCastException) { }

        drawTiles(WALL)
        drawTiles(TERRAIN) // regular tiles
        drawTiles(WIRE)
        drawTiles(FLUID)
    }

    internal fun drawWall(projectionMatrix: Matrix4) {
        gdxSetBlendNormal()

        renderUsingBuffer(WALL, projectionMatrix)
    }

    internal fun drawTerrain(projectionMatrix: Matrix4) {
        gdxSetBlendNormal()

        renderUsingBuffer(TERRAIN, projectionMatrix)
        renderUsingBuffer(FLUID, projectionMatrix)
    }

    internal fun drawFront(projectionMatrix: Matrix4, drawWires: Boolean) {
        // blend mul
        Gdx.gl.glEnable(GL20.GL_TEXTURE_2D)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_DST_COLOR, GL20.GL_ONE_MINUS_SRC_ALPHA)

        // let's just not MUL on terrain, make it FLUID only...
        renderUsingBuffer(FLUID, projectionMatrix)



        gdxSetBlendNormal()

        if (drawWires) {
            renderUsingBuffer(WIRE, projectionMatrix)
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
            val fluidLevel = this.amount.coerceIn(0f,1f).times(PairedMapLayer.RANGE - 1).roundToInt()

            return fluidLevel * BLOCKS_IN_ATLAS_X + fluidNum
        }
        else {
            return 0
        }
    }

    private val tileDrawLightThreshold = 2f / LightmapRenderer.MUL

    /**
     * Writes to buffer. Actual draw code must be called after this operation.
     *
     * @param drawModeTilesBlendMul If current drawing mode is MULTIPLY. Doesn't matter if mode is FLUID.
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

                val thisTile = when (mode) {
                    WALL -> world.getTileFromWall(x, y)
                    TERRAIN -> world.getTileFromTerrain(x, y)
                    WIRE -> world.getTileFromWire(x, y)
                    FLUID -> world.getFluid(x, y).toTileInFluidAtlas()
                    else -> throw IllegalArgumentException()
                }


                // draw a tile, but only when illuminated
                try {
                    val nearbyTilesInfo = if (mode == FLUID) {
                        getNearbyTilesInfoFluids(x, y)
                    }
                    else if (isPlatform(thisTile)) {
                        getNearbyTilesInfoPlatform(x, y)
                    }
                    else if (isWallSticker(thisTile)) {
                        getNearbyTilesInfoWallSticker(x, y)
                    }
                    else if (isConnectMutual(thisTile)) {
                        getNearbyTilesInfoNonSolid(x, y, mode)
                    }
                    else if (isConnectSelf(thisTile)) {
                        getNearbyTilesInfo(x, y, mode, thisTile)
                    }
                    else {
                        0
                    }


                    val thisTileX = TILES_PER_BLOCK * ((thisTile ?: 0) % BLOCKS_IN_ATLAS_X) + nearbyTilesInfo
                    val thisTileY = (thisTile ?: 0) / BLOCKS_IN_ATLAS_X

                    val breakage = if (mode == TERRAIN) world.getTerrainDamage(x, y) else world.getWallDamage(x, y)
                    val maxHealth = BlockCodex[world.getTileFromTerrain(x, y)].strength
                    val breakingStage = (breakage / maxHealth).times(BREAKAGE_STEPS).roundInt()



                    // draw a tile

                    if (mode == FLUID) {
                        writeToBuffer(mode, bufferX, bufferY, thisTileX, thisTileY, 0)
                    }
                    else {
                        writeToBuffer(mode, bufferX, bufferY, thisTileX, thisTileY, breakingStage)
                    }
                } catch (e: NullPointerException) {
                    // do nothing. WARNING: This exception handling may hide erratic behaviour completely.
                }
            }
        }
    }

    /**

     * @param x
     * *
     * @param y
     * *
     * @return binary [0-15] 1: up, 2: right, 4: down, 8: left
     */
    internal fun getNearbyTilesInfo(x: Int, y: Int, mode: Int, mark: Int?): Int {
        val nearbyTiles = IntArray(4)
        nearbyTiles[NEARBY_TILE_KEY_LEFT] = world.getTileFrom(mode, x - 1, y) ?: Block.NULL
        nearbyTiles[NEARBY_TILE_KEY_RIGHT] = world.getTileFrom(mode, x + 1, y) ?: Block.NULL
        nearbyTiles[NEARBY_TILE_KEY_UP] = world.getTileFrom(mode, x    , y - 1) ?: Block.NULL
        nearbyTiles[NEARBY_TILE_KEY_DOWN] = world.getTileFrom(mode, x    , y + 1) ?: Block.NULL

        // try for
        var ret = 0
        for (i in 0..3) {
            if (nearbyTiles[i] == mark) {
                ret += 1 shl i // add 1, 2, 4, 8 for i = 0, 1, 2, 3
            }
        }

        return ret
    }

    internal fun getNearbyTilesInfoNonSolid(x: Int, y: Int, mode: Int): Int {
        val nearbyTiles = IntArray(4)
        nearbyTiles[NEARBY_TILE_KEY_LEFT] = world.getTileFrom(mode, x - 1, y) ?: Block.NULL
        nearbyTiles[NEARBY_TILE_KEY_RIGHT] = world.getTileFrom(mode, x + 1, y) ?: Block.NULL
        nearbyTiles[NEARBY_TILE_KEY_UP] = world.getTileFrom(mode, x    , y - 1) ?: Block.NULL
        nearbyTiles[NEARBY_TILE_KEY_DOWN] = world.getTileFrom(mode, x    , y + 1) ?: Block.NULL

        // try for
        var ret = 0
        for (i in 0..3) {
            try {
                if (!BlockCodex[nearbyTiles[i]].isSolid) {
                    ret += (1 shl i) // add 1, 2, 4, 8 for i = 0, 1, 2, 3
                }
            } catch (e: ArrayIndexOutOfBoundsException) {
            }

        }

        return 15 - ret
    }

    /**
     * Basically getNearbyTilesInfoNonSolid() but connects mutually with all the fluids
     */
    internal fun getNearbyTilesInfoFluids(x: Int, y: Int): Int {
        val nearbyTiles = IntArray(4)
        nearbyTiles[NEARBY_TILE_KEY_UP] =    world.getTileFromTerrain(x    , y - 1) ?: Block.NULL
        nearbyTiles[NEARBY_TILE_KEY_RIGHT] = world.getTileFromTerrain(x + 1, y) ?: Block.NULL
        nearbyTiles[NEARBY_TILE_KEY_DOWN] =  world.getTileFromTerrain(x    , y + 1) ?: Block.NULL
        nearbyTiles[NEARBY_TILE_KEY_LEFT] =  world.getTileFromTerrain(x - 1, y) ?: Block.NULL

        val nearX = intArrayOf(x, x+1, x, x-1)
        val nearY = intArrayOf(y-1, y, y+1, y)

        // try for
        var ret = 0
        for (i in 0..3) {
            try {
                if (!world.getFluid(nearX[i], nearY[i]).isFluid() && // is not a fluid and...
                    !BlockCodex[nearbyTiles[i]].isSolid) {
                    ret += (1 shl i) // add 1, 2, 4, 8 for i = 0, 1, 2, 3
                }
            } catch (e: ArrayIndexOutOfBoundsException) {
            }

        }

        //return ret
        
        return if (ret == 15 || ret == 10)
            ret
        else if (world.getFluid(x, y-1).isFluid()) // if tile above is a fluid
            0
        else
            1
    }

    internal fun getNearbyTilesInfoWallSticker(x: Int, y: Int): Int {
        val nearbyTiles = IntArray(4)
        val NEARBY_TILE_KEY_BACK = NEARBY_TILE_KEY_UP
        nearbyTiles[NEARBY_TILE_KEY_LEFT] =  world.getTileFrom(TERRAIN, x - 1, y) ?: Block.NULL
        nearbyTiles[NEARBY_TILE_KEY_RIGHT] = world.getTileFrom(TERRAIN, x + 1, y) ?: Block.NULL
        nearbyTiles[NEARBY_TILE_KEY_DOWN] =  world.getTileFrom(TERRAIN, x    , y + 1) ?: Block.NULL
        nearbyTiles[NEARBY_TILE_KEY_BACK] =  world.getTileFrom(WALL,    x    , y) ?: Block.NULL

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

    internal fun getNearbyTilesInfoPlatform(x: Int, y: Int): Int {
        val nearbyTiles = IntArray(4)
        nearbyTiles[NEARBY_TILE_KEY_LEFT] = world.getTileFrom(TERRAIN, x - 1, y) ?: Block.NULL
        nearbyTiles[NEARBY_TILE_KEY_RIGHT] = world.getTileFrom(TERRAIN, x + 1, y) ?: Block.NULL

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
            else -> throw IllegalArgumentException()
        }


        sourceBuffer[bufferPosY][bufferPosX] = sheetXYToTilemapColour(mode, sheetX, sheetY, breakage)
    }

    private var _tilesBufferAsTex: Texture = Texture(1, 1, Pixmap.Format.RGBA8888)

    private fun renderUsingBuffer(mode: Int, projectionMatrix: Matrix4) {
        //Gdx.gl.glClearColor(.094f, .094f, .094f, 0f)
        //Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)


        //val tilesInHorizontal = tilesBuffer.width
        //val tilesInVertical =   tilesBuffer.height


        val tileAtlas = when (mode) {
            TERRAIN, WALL -> tilesTerrain
            WIRE -> tilesWire
            FLUID -> tilesFluid
            else -> throw IllegalArgumentException()
        }
        val sourceBuffer = when(mode) {
            TERRAIN -> terrainTilesBuffer
            WALL -> wallTilesBuffer
            WIRE -> wireTilesBuffer
            FLUID -> fluidTilesBuffer
            else -> throw IllegalArgumentException()
        }
        val vertexColour = when (mode) {
            TERRAIN, WIRE, FLUID -> Color.WHITE
            WALL -> wallOverlayColour
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
        tilesTerrainBlend.texture.bind(2)
        _tilesBufferAsTex.bind(1) // trying 1 and 0...
        tileAtlas.texture.bind(0) // for some fuck reason, it must be bound as last

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
        /*shader hard-code*/shader.setUniformi("tilesInAtlas", tileAtlas.horizontalCount, tileAtlas.verticalCount) //depends on the tile atlas
        /*shader hard-code*/shader.setUniformi("atlasTexSize", tileAtlas.texture.width, tileAtlas.texture.height) //depends on the tile atlas
        // set the blend value as world's time progresses, in linear fashion
        shader.setUniformf("tilesBlend", if (world is GameWorldExtension && (mode == TERRAIN || mode == WALL))
            drawTIME_T.fmod(SECONDS_IN_MONTH) / SECONDS_IN_MONTH.toFloat()
        else
            0f
        )
        tilesQuad.render(shader, GL20.GL_TRIANGLES)
        shader.end()

        //tilesBufferAsTex.dispose()
    }

    private var oldScreenW = 0
    private var oldScreenH = 0

    var tilesInHorizontal = -1; private set
    var tilesInVertical = -1; private set

    fun resize(screenW: Int, screenH: Int) {
        tilesInHorizontal = (screenW.toFloat() / TILE_SIZE).ceilInt() + 1
        tilesInVertical = (screenH.toFloat() / TILE_SIZE).ceilInt() + 1

        val oldTH = (oldScreenW.toFloat() / TILE_SIZE).ceilInt() + 1
        val oldTV = (oldScreenH.toFloat() / TILE_SIZE).ceilInt() + 1

        // only update if it's really necessary
        if (oldTH != tilesInHorizontal || oldTV != tilesInVertical) {
            terrainTilesBuffer = Array<IntArray>(tilesInVertical, { kotlin.IntArray(tilesInHorizontal) })
            wallTilesBuffer = Array<IntArray>(tilesInVertical, { kotlin.IntArray(tilesInHorizontal) })
            wireTilesBuffer = Array<IntArray>(tilesInVertical, { kotlin.IntArray(tilesInHorizontal) })
            fluidTilesBuffer = Array<IntArray>(tilesInVertical, { kotlin.IntArray(tilesInHorizontal) })

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
                    screenW.toFloat(), 0f, 0f, 1f, 1f, 1f, 1f, 1f, 0f,
                    screenW.toFloat(), screenH.toFloat(), 0f, 1f, 1f, 1f, 1f, 1f, 1f,
                    0f, screenH.toFloat(), 0f, 1f, 1f, 1f, 1f, 0f, 1f
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
        Thread.currentThread().stackTrace.forEach {
            printdbg(this, it)
        }

        weatherTerrains.forEach { it.dispose() }
        tilesWire.dispose()
        tileItemWall.dispose()
        tilesFluid.dispose()
    }

    fun getRenderStartX(): Int = WorldCamera.x / TILE_SIZE
    fun getRenderStartY(): Int = WorldCamera.y / TILE_SIZE

    fun getRenderEndX(): Int = clampWTile(getRenderStartX() + (WorldCamera.width / TILE_SIZE) + 2)
    fun getRenderEndY(): Int = clampHTile(getRenderStartY() + (WorldCamera.height / TILE_SIZE) + 2)

    fun isConnectSelf(b: Int?): Boolean = TILES_CONNECT_SELF.contains(b)
    fun isConnectMutual(b: Int?): Boolean = TILES_CONNECT_MUTUAL.contains(b)
    fun isWallSticker(b: Int?): Boolean = TILES_WALL_STICKER.contains(b)
    fun isPlatform(b: Int?): Boolean = TILES_WALL_STICKER_CONNECT_SELF.contains(b)
    fun isBlendMul(b: Int?): Boolean = TILES_BLEND_MUL.contains(b)

    fun tileInCamera(x: Int, y: Int) =
            x >= WorldCamera.x.div(TILE_SIZE) && y >= WorldCamera.y.div(TILE_SIZE) &&
            x <= WorldCamera.x.plus(WorldCamera.width).div(TILE_SIZE) && y <= WorldCamera.y.plus(WorldCamera.width).div(TILE_SIZE)
}
