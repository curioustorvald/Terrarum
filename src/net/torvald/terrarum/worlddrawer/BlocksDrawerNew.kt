package net.torvald.terrarum.worlddrawer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.PairedMapLayer
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.ceilInt
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.itemproperties.ItemCodex.ITEM_TILES
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream


/**
 * Note: You can't just hamburger the three jobs; there's actor draw calls in-between the three jobs, like:
 *
```
    BlocksDrawer.renderWall(batch) // JOB #0
    actorsRenderBehind.forEach { it.drawBody(batch) }
    particlesContainer.forEach { it.drawBody(batch) }
    BlocksDrawer.renderTerrain(batch) // JOB #1

    /////////////////
    // draw actors //
    /////////////////
    actorsRenderMiddle.forEach { it.drawBody(batch) }
    actorsRenderMidTop.forEach { it.drawBody(batch) }
    player.drawBody(batch)
    actorsRenderFront.forEach { it.drawBody(batch) }
    // --> Change of blend mode <-- introduced by childs of ActorWithBody //


    /////////////////////////////
    // draw map related stuffs //
    /////////////////////////////

    BlocksDrawer.renderFront(batch, false) // JOB #2
 ```
 *
 * Created by minjaesong on 2016-01-19.
 */
object BlocksDrawer {
    lateinit var world: GameWorld


    private val TILE_SIZE = FeaturesDrawer.TILE_SIZE
    private val TILE_SIZEF = FeaturesDrawer.TILE_SIZE.toFloat()

    val tilesTerrain: TextureRegionPack
    val tilesWire: TextureRegionPack
    val tileItemWall: TextureRegionPack

    //val tileItemWall = Image(TILE_SIZE * 16, TILE_SIZE * GameWorld.TILES_SUPPORTED / 16) // 4 MB


    val wallOverlayColour = Color(2f/3f, 2f/3f, 2f/3f, 1f)

    val breakAnimSteps = 10

    val WALL = GameWorld.WALL
    val TERRAIN = GameWorld.TERRAIN
    val WIRE = GameWorld.WIRE

    private val NEARBY_TILE_KEY_UP = 0
    private val NEARBY_TILE_KEY_RIGHT = 1
    private val NEARBY_TILE_KEY_DOWN = 2
    private val NEARBY_TILE_KEY_LEFT = 3

    private val NEARBY_TILE_CODE_UP = 1
    private val NEARBY_TILE_CODE_RIGHT = 2
    private val NEARBY_TILE_CODE_DOWN = 4
    private val NEARBY_TILE_CODE_LEFT = 8


    private val GZIP_READBUF_SIZE = 8192


    private lateinit var terrainTilesBuffer: Array<IntArray>
    private lateinit var wallTilesBuffer: Array<IntArray>
    private lateinit var wireTilesBuffer: Array<IntArray>
    private lateinit var tilesBuffer: Pixmap


    private lateinit var tilesQuad: Mesh
    private val shader = ShaderProgram(Gdx.files.internal("assets/4096.vert"), Gdx.files.internal("assets/tiling.frag"))

    init {
        // hard-coded as tga.gz
        val gzFileList = listOf("blocks/terrain.tga.gz", "blocks/wire.tga.gz")
        val gzTmpFName = listOf("tmp_terrain.tga", "tmp_wire.tga")
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

        val terrainPixMap = Pixmap(Gdx.files.internal(gzTmpFName[0]))
        val wirePixMap = Pixmap(Gdx.files.internal(gzTmpFName[1]))

        // delete temp files
        gzTmpFName.forEach { File(it).delete() }

        tilesTerrain = TextureRegionPack(Texture(terrainPixMap), TILE_SIZE, TILE_SIZE)
        tilesTerrain.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        tilesWire = TextureRegionPack(Texture(wirePixMap), TILE_SIZE, TILE_SIZE)
        tilesWire.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

        // also dispose unused temp files
        //terrainPixMap.dispose() // commented: tileItemWall needs it
        wirePixMap.dispose()




        // create item_wall images
        // --> make pixmap
        val tileItemImgPixMap = Pixmap(TILE_SIZE * 16, TILE_SIZE * GameWorld.TILES_SUPPORTED / 16, Pixmap.Format.RGBA8888)
        tileItemImgPixMap.pixels.rewind()

        for (tileID in ITEM_TILES) {

            val tileX = (tileID % 16) * 16
            val tileY = tileID / 16
            val tile = tilesTerrain.get(tileX, tileY)


            // slow memory copy :\  I'm afraid I can't random-access bytebuffer...
            for (scanline in 0 until tileItemImgPixMap.height) {
                for (x in 0 until TILE_SIZE) {
                    val pixel = terrainPixMap.getPixel(tileX + x, scanline)
                    tileItemImgPixMap.drawPixel(x + TILE_SIZE * (tileID % 16), scanline, pixel)
                }
            }
        }
        tileItemImgPixMap.pixels.rewind()
        // turn pixmap into texture
        tileItemWall = TextureRegionPack(Texture(tileItemImgPixMap), TILE_SIZE, TILE_SIZE)



        tileItemImgPixMap.dispose()
        terrainPixMap.dispose() // finally
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
            Block.ILLUMINATOR_YELLOW,
            Block.DAYLIGHT_CAPACITOR
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
            Block.ORE_COPPER,
            Block.ORE_IRON,
            Block.ORE_GOLD,
            Block.ORE_SILVER,
            Block.ORE_ILMENITE,
            Block.ORE_AURICHALCUM,

            Block.SANDSTONE,
            Block.SANDSTONE_BLACK,
            Block.SANDSTONE_DESERT,
            Block.SANDSTONE_RED,
            Block.SANDSTONE_WHITE,
            Block.SANDSTONE_GREEN,

            Block.WATER,
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
            Block.LAVA_15
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
            Block.LAVA_15
    )

    /**
     * To interact with external modules
     */
    @JvmStatic fun addBlendMul(blockID: Int): Boolean {
        return TILES_BLEND_MUL.add(blockID)
    }


    ///////////////////////////////////////////
    // NO draw lightmap using colour filter, actors must also be hidden behind the darkness
    ///////////////////////////////////////////

    fun renderWall(batch: SpriteBatch) {
        // blend normal
        Gdx.gl.glEnable(GL20.GL_TEXTURE_2D)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        drawTiles(WALL, false)
        renderUsingBuffer(WALL, batch.projectionMatrix)
    }

    fun renderTerrain(batch: SpriteBatch) {
        // blend normal
        Gdx.gl.glEnable(GL20.GL_TEXTURE_2D)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        drawTiles(TERRAIN, false) // regular tiles
        renderUsingBuffer(TERRAIN, batch.projectionMatrix)
    }

    fun renderFront(batch: SpriteBatch, drawWires: Boolean) {
        // blend mul
        Gdx.gl.glEnable(GL20.GL_TEXTURE_2D)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_DST_COLOR, GL20.GL_ONE_MINUS_SRC_ALPHA)


        drawTiles(TERRAIN, true) // blendmul tiles
        renderUsingBuffer(TERRAIN, batch.projectionMatrix)




        // blend normal
        Gdx.gl.glEnable(GL20.GL_TEXTURE_2D)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        if (drawWires) {
            drawTiles(WIRE, false)
            renderUsingBuffer(WIRE, batch.projectionMatrix)
        }
    }

    private val tileDrawLightThreshold = 2f / LightmapRenderer.MUL

    private fun canIHazRender(mode: Int, x: Int, y: Int) =
            //(world.getTileFrom(mode, x, y) != 0) // not an air tile
            //&&

            // for WALLs; else: ret true
            if (mode == WALL) { // DRAW WHEN it is visible and 'is a lip'
                ( BlockCodex[world.getTileFromTerrain(x, y) ?: 0].isClear ||
                  !
                  ((!BlockCodex[world.getTileFromTerrain(x, y - 1) ?: 0].isClear && !BlockCodex[world.getTileFromTerrain(x, y + 1) ?: 0].isClear)
                   &&
                   (!BlockCodex[world.getTileFromTerrain(x - 1, y) ?: 0].isClear && !BlockCodex[world.getTileFromTerrain(x + 1, y + 1) ?: 0].isClear)
                  )
                )
            }
            else
                true

    // end

    private fun hasLightNearby(x: Int, y: Int) = ( // check if light level of nearby or this tile is illuminated
            LightmapRenderer.getHighestRGB(x, y) ?: 0f >= tileDrawLightThreshold ||
            LightmapRenderer.getHighestRGB(x - 1, y) ?: 0f >= tileDrawLightThreshold ||
            LightmapRenderer.getHighestRGB(x + 1, y) ?: 0f >= tileDrawLightThreshold ||
            LightmapRenderer.getHighestRGB(x, y - 1) ?: 0f >= tileDrawLightThreshold ||
            LightmapRenderer.getHighestRGB(x, y + 1) ?: 0f >= tileDrawLightThreshold ||
            LightmapRenderer.getHighestRGB(x - 1, y - 1) ?: 0f >= tileDrawLightThreshold ||
            LightmapRenderer.getHighestRGB(x + 1, y + 1) ?: 0f >= tileDrawLightThreshold ||
            LightmapRenderer.getHighestRGB(x + 1, y - 1) ?: 0f >= tileDrawLightThreshold ||
            LightmapRenderer.getHighestRGB(x - 1, y + 1) ?: 0f >= tileDrawLightThreshold
                                                 )

    /**
     * Writes to buffer. Actual draw code must be called after this operation.
     */
    private fun drawTiles(mode: Int, drawModeTilesBlendMul: Boolean) {
        val for_y_start = WorldCamera.y / TILE_SIZE
        val for_y_end = for_y_start + tilesBuffer.height - 1//clampHTile(for_y_start + (WorldCamera.height / TILE_SIZE) + 2)

        val for_x_start = WorldCamera.x / TILE_SIZE
        val for_x_end = for_x_start + tilesBuffer.width - 1//for_x_start + (WorldCamera.width / TILE_SIZE) + 3

        // loop
        for (y in for_y_start..for_y_end) {
            for (x in for_x_start..for_x_end) {

                val thisTile: Int?
                if (mode % 3 == WALL)
                    thisTile = world.getTileFromWall(x, y)
                else if (mode % 3 == TERRAIN)
                    thisTile = world.getTileFromTerrain(x, y)
                else if (mode % 3 == WIRE)
                    thisTile = world.getTileFromWire(x, y)
                else
                    throw IllegalArgumentException()

                val noDamageLayer = mode % 3 == WIRE

                // draw a tile, but only when illuminated
                try {
                    //if (canIHazRender(mode, x, y)) {

                        //if (!hasLightNearby(x, y)) {
                        //    // draw black patch
                        //    if (thisTile == 0)
                        //        writeToBuffer(mode, x - for_x_start, y - for_y_start, 0, 0)
                        //    else
                        //        writeToBuffer(mode, x - for_x_start, y - for_y_start, 2, 0)
                        //}
                        //else {

                            val nearbyTilesInfo: Int
                            if (isPlatform(thisTile)) {
                                nearbyTilesInfo = getNearbyTilesInfoPlatform(x, y)
                            }
                            else if (isWallSticker(thisTile)) {
                                nearbyTilesInfo = getNearbyTilesInfoWallSticker(x, y)
                            }
                            else if (isConnectMutual(thisTile)) {
                                nearbyTilesInfo = getNearbyTilesInfoNonSolid(x, y, mode)
                            }
                            else if (isConnectSelf(thisTile)) {
                                nearbyTilesInfo = getNearbyTilesInfo(x, y, mode, thisTile)
                            }
                            else {
                                nearbyTilesInfo = 0
                            }


                            val thisTileX = if (!noDamageLayer)
                                PairedMapLayer.RANGE * ((thisTile ?: 0) % PairedMapLayer.RANGE) + nearbyTilesInfo
                            else
                                nearbyTilesInfo

                            val thisTileY = (thisTile ?: 0) / PairedMapLayer.RANGE


                            // draw a tile
                            if (drawModeTilesBlendMul) {
                                if (isBlendMul(thisTile)) {
                                    writeToBuffer(mode, x - for_x_start, y - for_y_start, thisTileX, thisTileY)
                                }
                                else {
                                    writeToBuffer(mode, x - for_x_start, y - for_y_start, 0, 0)
                                }
                            }
                            else {
                                // do NOT add "if (!isBlendMul(thisTile))"!
                                // or else they will not look like they should be when backed with wall
                                writeToBuffer(mode, x - for_x_start, y - for_y_start, thisTileX, thisTileY)
                            }

                            // draw a breakage
                            /*if (mode == TERRAIN || mode == WALL) {
                                val breakage = if (mode == TERRAIN) world.getTerrainDamage(x, y) else world.getWallDamage(x, y)
                                val maxHealth = BlockCodex[world.getTileFromTerrain(x, y)].strength
                                val stage = (breakage / maxHealth).times(breakAnimSteps).roundInt()
                                // actual drawing
                                if (stage > 0) {
                                    writeToBuffer(mode, x - for_x_start, y - for_y_start, 5 + stage, 0)
                                }
                            }*/


                        //} // end if (is illuminated)
                    //} // end if (not an air)
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
    fun getNearbyTilesInfo(x: Int, y: Int, mode: Int, mark: Int?): Int {
        val nearbyTiles = IntArray(4)
        nearbyTiles[NEARBY_TILE_KEY_LEFT] = world.getTileFrom(mode, x - 1, y) ?: Block.NULL
        nearbyTiles[NEARBY_TILE_KEY_RIGHT] = world.getTileFrom(mode, x + 1, y) ?: Block.NULL
        nearbyTiles[NEARBY_TILE_KEY_UP] = world.getTileFrom(mode, x    , y - 1) ?: 4906
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

    fun getNearbyTilesInfoNonSolid(x: Int, y: Int, mode: Int): Int {
        val nearbyTiles = IntArray(4)
        nearbyTiles[NEARBY_TILE_KEY_LEFT] = world.getTileFrom(mode, x - 1, y) ?: Block.NULL
        nearbyTiles[NEARBY_TILE_KEY_RIGHT] = world.getTileFrom(mode, x + 1, y) ?: Block.NULL
        nearbyTiles[NEARBY_TILE_KEY_UP] = world.getTileFrom(mode, x    , y - 1) ?: 4906
        nearbyTiles[NEARBY_TILE_KEY_DOWN] = world.getTileFrom(mode, x    , y + 1) ?: Block.NULL

        // try for
        var ret = 0
        for (i in 0..3) {
            try {
                if (!BlockCodex[nearbyTiles[i]].isSolid &&
                    !BlockCodex[nearbyTiles[i]].isFluid) {
                    ret += (1 shl i) // add 1, 2, 4, 8 for i = 0, 1, 2, 3
                }
            } catch (e: ArrayIndexOutOfBoundsException) {
            }

        }

        return ret
    }

    fun getNearbyTilesInfoWallSticker(x: Int, y: Int): Int {
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

    fun getNearbyTilesInfoPlatform(x: Int, y: Int): Int {
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
    private fun sheetXYToTilemapColour(mode: Int, sheetX: Int, sheetY: Int): Int = when (mode) {
        TERRAIN, WALL -> (tilesTerrain.horizontalCount * sheetY + sheetX).shl(8) or 255
        WIRE -> (tilesWire.horizontalCount * sheetY + sheetX).shl(8) or 255
        else -> throw IllegalArgumentException()
    }

    private fun writeToBuffer(mode: Int, bufferPosX: Int, bufferPosY: Int, sheetX: Int, sheetY: Int) {
        val sourceBuffer = when(mode) {
            TERRAIN -> terrainTilesBuffer
            WALL -> wallTilesBuffer
            WIRE -> wireTilesBuffer
            else -> throw IllegalArgumentException()
        }


        sourceBuffer[bufferPosY][bufferPosX] = sheetXYToTilemapColour(mode, sheetX, sheetY)
    }

    private fun renderUsingBuffer(mode: Int, projectionMatrix: Matrix4) {
        //Gdx.gl.glClearColor(.094f, .094f, .094f, 0f)
        //Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)


        //val tilesInHorizontal = tilesBuffer.width
        //val tilesInVertical =   tilesBuffer.height


        val tileAtlas = when (mode) {
            TERRAIN, WALL -> tilesTerrain
            WIRE -> tilesWire
            else -> throw IllegalArgumentException()
        }
        val sourceBuffer = when(mode) {
            TERRAIN -> terrainTilesBuffer
            WALL -> wallTilesBuffer
            WIRE -> wireTilesBuffer
            else -> throw IllegalArgumentException()
        }
        val vertexColour = when (mode) {
            TERRAIN, WIRE -> Color.WHITE
            WALL -> wallOverlayColour
            else -> throw IllegalArgumentException()
        }


        // write to colour buffer
        for (y in 0 until tilesBuffer.height) {
            for (x in 0 until tilesBuffer.width) {
                val color = sourceBuffer[y][x]
                tilesBuffer.setColor(color)
                tilesBuffer.drawPixel(x, y)
            }
        }


        val tilesBufferAsTex = Texture(tilesBuffer)
        tilesBufferAsTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        tilesBufferAsTex.bind(1) // trying 1 and 0...
        tileAtlas.texture.bind(0) // for some fuck reason, it must be bound as last

        shader.begin()
        shader.setUniformMatrix("u_projTrans", projectionMatrix)//camera.combined)
        shader.setUniformf("colourFilter", vertexColour)
        shader.setUniformf("screenDimension", Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        shader.setUniformi("tilesAtlas", 0)
        shader.setUniformi("tilemap", 1)
        shader.setUniformi("tilemapDimension", tilesBuffer.width, tilesBuffer.height)
        shader.setUniformf("tilesInAxes", tilesInHorizontal.toFloat(), tilesInVertical.toFloat())
        shader.setUniformi("cameraTranslation", WorldCamera.x fmod TILE_SIZE, WorldCamera.y fmod TILE_SIZE)
        shader.setUniformi("tileSizeInPx", TILE_SIZE)
        shader.setUniformi("tilesInAtlas", tileAtlas.horizontalCount, tileAtlas.verticalCount) //depends on the tile atlas
        shader.setUniformi("atlasTexSize", tileAtlas.texture.width, tileAtlas.texture.height) //depends on the tile atlas
        tilesQuad.render(shader, GL20.GL_TRIANGLES)
        shader.end()

        tilesBufferAsTex.dispose()
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

            tilesBuffer = Pixmap(tilesInHorizontal, tilesInVertical, Pixmap.Format.RGB888)
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


        println("[BlocksDrawerNew] Resize event")

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
