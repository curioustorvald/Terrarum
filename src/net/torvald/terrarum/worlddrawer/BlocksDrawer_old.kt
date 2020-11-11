/*package net.torvald.terrarum.worlddrawer

import com.badlogic.gdx.Gdx
import Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.PairedMapLayer
import net.torvald.terrarum.itemproperties.ItemCodex.ITEM_TILES
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream


/**
 * Created by minjaesong on 2016-01-19.
 */
object BlocksDrawerOLD {
    lateinit var world: GameWorld


    private val TILE_SIZE = CreateTileAtlas.TILE_SIZE
    private val TILE_SIZEF = CreateTileAtlas.TILE_SIZE.toFloat()

    // TODO modular
    //val tilesTerrain = SpriteSheet(ModMgr.getPath("basegame", "blocks/terrain.tga.gz"), TILE_SIZE, TILE_SIZE) // 64 MB
    //val tilesWire = SpriteSheet(ModMgr.getPath("basegame", "blocks/wire.tga.gz"), TILE_SIZE, TILE_SIZE) // 4 MB

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

            val tile = tilesTerrain.get((tileID % 16) * 16, (tileID / 16))

            // slow memory copy :\  I'm afraid I can't random-access bytebuffer...
            for (y in 0..TILE_SIZE - 1) {
                for (x in 0..TILE_SIZE - 1) {
                    tileItemImgPixMap.pixels.putInt(
                        terrainPixMap.getPixel(
                                tile.regionX + x,
                                tile.regionY + y
                        )
                    )
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
            Block.SANDSTONE_GREEN

            /*Block.WATER,
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
    private val TILES_BLEND_MUL = hashSetOf(-1
            /*Block.WATER,
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
    @JvmStatic fun addBlendMul(blockID: Int): Boolean {
        return TILES_BLEND_MUL.add(blockID)
    }


    ///////////////////////////////////////////
    // NO draw lightmap using colour filter, actors must also be hidden behind the darkness
    ///////////////////////////////////////////

    fun renderWall(batch: SpriteBatch) {
        /**
         * render to camera
         */
        blendNormal()

        drawTiles(batch, WALL, false, wallOverlayColour)
    }

    fun renderTerrain(batch: SpriteBatch) {
        /**
         * render to camera
         */
        blendNormal()

        drawTiles(batch, TERRAIN, false, Color.WHITE) // regular tiles
    }

    fun renderFront(batch: SpriteBatch, drawWires: Boolean) {
        /**
         * render to camera
         */
        blendMul()

        drawTiles(batch, TERRAIN, true, Color.WHITE) // blendmul tiles

        if (drawWires) {
            drawTiles(batch, WIRE, false, Color.WHITE)
        }

        blendNormal()
    }

    private val tileDrawLightThreshold = 2f / LightmapRenderer.MUL

    private fun canIHazRender(mode: Int, x: Int, y: Int) =
            (world.getTileFrom(mode, x, y) != 0) // not an air tile
                &&
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

    private fun drawTiles(batch: SpriteBatch, mode: Int, drawModeTilesBlendMul: Boolean, color: Color) {
        val for_y_start = WorldCamera.y / TILE_SIZE
        val for_y_end = clampHTile(for_y_start + (WorldCamera.height / TILE_SIZE) + 2)

        val for_x_start = WorldCamera.x / TILE_SIZE - 1
        val for_x_end = for_x_start + (WorldCamera.width / TILE_SIZE) + 3

        val originalBatchColour = batch.color.cpy()
        batch.color = color

        // loop
        for (y in for_y_start..for_y_end) {
            var zeroTileCounter = 0

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
                    if (canIHazRender(mode, x, y)) {

                        if (!hasLightNearby(x, y)) {
                            // draw black patch
                            zeroTileCounter += 1 // unused for now

                            // temporary solution; FIXME bad scanlines bug
                            batch.color = Color.BLACK
                            batch.fillRect(x * TILE_SIZEF, y * TILE_SIZEF, TILE_SIZEF, TILE_SIZEF)
                        }
                        else {
                            // commented out; FIXME bad scanlines bug
                            if (zeroTileCounter > 0) {
                                /*batch.color = Color.BLACK
                                batch.fillRect(x * TILE_SIZEF, y * TILE_SIZEF, -zeroTileCounter * TILE_SIZEF, TILE_SIZEF)
                                batch.color = color
                                zeroTileCounter = 0*/
                            }


                            val nearbyTilesInfo: Int
                            if (isPlatform(thisTile)) {
                                nearbyTilesInfo = getNearbyTilesInfoPlatform(x, y)
                            }
                            else if (isWallSticker(thisTile)) {
                                nearbyTilesInfo = getNearbyTilesInfoWallSticker(x, y)
                            }
                            else if (isConnectMutual(thisTile)) {
                                nearbyTilesInfo = getNearbyTilesInfoConMutual(x, y, mode)
                            }
                            else if (isConnectSelf(thisTile)) {
                                nearbyTilesInfo = getNearbyTilesInfoConSelf(x, y, mode, thisTile)
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
                                if (BlocksDrawer.isBlendMul(thisTile)) {
                                    batch.color = color
                                    drawTile(batch, mode, x, y, thisTileX, thisTileY)
                                }
                            }
                            else {
                                // do NOT add "if (!isBlendMul(thisTile))"!
                                // or else they will not look like they should be when backed with wall
                                batch.color = color
                                drawTile(batch, mode, x, y, thisTileX, thisTileY)
                            }

                            // draw a breakage
                            if (mode == TERRAIN || mode == WALL) {
                                val breakage = if (mode == TERRAIN) world.getTerrainDamage(x, y) else world.getWallDamage(x, y)
                                val maxHealth = BlockCodex[world.getTileFromTerrain(x, y)].strength
                                val stage = (breakage / maxHealth).times(breakAnimSteps).roundToInt()
                                // actual drawing
                                if (stage > 0) {
                                    batch.color = color
                                    drawTile(batch, mode, x, y, 5 + stage, 0)
                                }
                            }


                        } // end if (is illuminated)
                    } // end if (not an air)
                } catch (e: NullPointerException) {
                    // do nothing. WARNING: This exception handling may hide erratic behaviour completely.
                }


                // hit the end of the current scanline
                // FIXME bad scanlines bug
                /*if (x == for_x_end) {
                    val x = x + 1 // because current tile is also counted
                    batch.color = Color.BLACK
                    batch.fillRect(x * TILE_SIZEF, y * TILE_SIZEF, -zeroTileCounter * TILE_SIZEF, TILE_SIZEF)
                    batch.color = color
                    zeroTileCounter = 0
                }*/
            }
        }


        batch.color = originalBatchColour
    }

    /**

     * @param x
     * *
     * @param y
     * *
     * @return binary [0-15] 1: up, 2: right, 4: down, 8: left
     */
    fun getNearbyTilesInfoConSelf(x: Int, y: Int, mode: Int, mark: Int?): Int {
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

    fun getNearbyTilesInfoConMutual(x: Int, y: Int, mode: Int): Int {
        val nearbyTiles = IntArray(4)
        nearbyTiles[NEARBY_TILE_KEY_LEFT] = world.getTileFrom(mode, x - 1, y) ?: Block.NULL
        nearbyTiles[NEARBY_TILE_KEY_RIGHT] = world.getTileFrom(mode, x + 1, y) ?: Block.NULL
        nearbyTiles[NEARBY_TILE_KEY_UP] = world.getTileFrom(mode, x    , y - 1) ?: 4906
        nearbyTiles[NEARBY_TILE_KEY_DOWN] = world.getTileFrom(mode, x    , y + 1) ?: Block.NULL

        // try for
        var ret = 0
        for (i in 0..3) {
            try {
                if (!BlockCodex[nearbyTiles[i]].isSolid) {
                    //&& !BlockCodex[nearbyTiles[i]].isFluid) {
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
        nearbyTiles[NEARBY_TILE_KEY_LEFT] = world.getTileFrom(TERRAIN, x - 1, y) ?: Block.NULL
        nearbyTiles[NEARBY_TILE_KEY_RIGHT] = world.getTileFrom(TERRAIN, x + 1, y) ?: Block.NULL
        nearbyTiles[NEARBY_TILE_KEY_DOWN] = world.getTileFrom(TERRAIN, x    , y + 1) ?: Block.NULL
        nearbyTiles[NEARBY_TILE_KEY_BACK] = world.getTileFrom(WALL,    x    , y) ?: Block.NULL

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

    private fun drawTile(batch: SpriteBatch, mode: Int, tilewisePosX: Int, tilewisePosY: Int, sheetX: Int, sheetY: Int) {
        if (mode == TERRAIN || mode == WALL)
            batch.draw(
                    tilesTerrain.get(sheetX, sheetY),
                    tilewisePosX * TILE_SIZEF,
                    tilewisePosY * TILE_SIZEF
            )
        else if (mode == WIRE)
            batch.draw(
                    tilesWire.get(sheetX, sheetY),
                    tilewisePosX * TILE_SIZEF,
                    tilewisePosY * TILE_SIZEF
            )
        else
            throw IllegalArgumentException()
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
*/