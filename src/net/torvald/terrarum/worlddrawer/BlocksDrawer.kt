package net.torvald.terrarum.worlddrawer

import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.PairedMapLayer
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import com.jme3.math.FastMath
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.itemproperties.ItemCodex.ITEM_TILES
import net.torvald.terrarum.worlddrawer.WorldCamera.x
import net.torvald.terrarum.worlddrawer.WorldCamera.y
import net.torvald.terrarum.worlddrawer.WorldCamera.height
import net.torvald.terrarum.worlddrawer.WorldCamera.width
import org.lwjgl.opengl.GL11
import org.newdawn.slick.*
import org.newdawn.slick.imageout.ImageOut


/**
 * Created by minjaesong on 16-01-19.
 */
object BlocksDrawer {
    private val world: GameWorld = Terrarum.ingame!!.world
    private val TILE_SIZE = FeaturesDrawer.TILE_SIZE
    private val TILE_SIZEF = FeaturesDrawer.TILE_SIZE.toFloat()

    // TODO modular
    val tilesTerrain = SpriteSheet(ModMgr.getPath("basegame", "blocks/terrain.tga.gz"), TILE_SIZE, TILE_SIZE) // 64 MB
    val tilesWire = SpriteSheet(ModMgr.getPath("basegame", "blocks/wire.tga.gz"), TILE_SIZE, TILE_SIZE) // 4 MB


    val tileItemWall = Image(TILE_SIZE * 16, TILE_SIZE * GameWorld.TILES_SUPPORTED / 16) // 4 MB


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


    init {
        val tg = tileItemWall.graphics

        // initialise item_wall images
        (ITEM_TILES).forEach {
            tg.drawImage(
                    tilesTerrain.getSubImage(
                            (it % 16) * 16,
                            (it / 16)
                    ),
                    (it % 16) * TILE_SIZE.toFloat(),
                    (it / 16) * TILE_SIZE.toFloat(),
                    wallOverlayColour
            )
        }

        //tg.flush()
        //ImageOut.write(tileItemWall, "./tileitemwalltest.png")

        tg.destroy()
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




    fun renderWall(g: Graphics) {
        /**
         * render to camera
         */
        blendNormal()

        tilesTerrain.startUse()
        drawTiles(g, WALL, false)
        tilesTerrain.endUse()

        blendMul()

        g.color = wallOverlayColour
        g.fillRect(WorldCamera.x.toFloat(), WorldCamera.y.toFloat(),
                WorldCamera.width.toFloat() + 1, WorldCamera.height.toFloat() + 1
        )

        blendNormal()
    }

    fun renderTerrain(g: Graphics) {
        /**
         * render to camera
         */
        blendNormal()

        tilesTerrain.startUse()
        drawTiles(g, TERRAIN, false) // regular tiles
        tilesTerrain.endUse()
    }

    fun renderFront(g: Graphics, drawWires: Boolean) {
        /**
         * render to camera
         */
        blendMul()

        tilesTerrain.startUse()
        drawTiles(g, TERRAIN, true) // blendmul tiles
        tilesTerrain.endUse()

        if (drawWires) {
            tilesWire.startUse()
            drawTiles(g, WIRE, false)
            tilesWire.endUse()
        }

        blendNormal()
    }

    private val tileDrawLightThreshold = 2

    private fun drawTiles(g: Graphics, mode: Int, drawModeTilesBlendMul: Boolean) {
        val for_y_start = y / TILE_SIZE
        val for_y_end = BlocksDrawer.clampHTile(for_y_start + (height / TILE_SIZE) + 2)

        val for_x_start = x / TILE_SIZE - 1
        val for_x_end = for_x_start + (width / TILE_SIZE) + 3

        var zeroTileCounter = 0

        // loop
        for (y in for_y_start..for_y_end) {
            for (x in for_x_start..for_x_end - 1) {

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
                    if ((mode == WALL || mode == TERRAIN) &&  // not an air tile
                        (thisTile ?: 0) != Block.AIR) {
                    // check if light level of nearby or this tile is illuminated
                        if ( LightmapRenderer.getHighestRGB(x, y) ?: 0 >= tileDrawLightThreshold ||
                             LightmapRenderer.getHighestRGB(x - 1, y) ?: 0 >= tileDrawLightThreshold ||
                             LightmapRenderer.getHighestRGB(x + 1, y) ?: 0 >= tileDrawLightThreshold ||
                             LightmapRenderer.getHighestRGB(x, y - 1) ?: 0 >= tileDrawLightThreshold ||
                             LightmapRenderer.getHighestRGB(x, y + 1) ?: 0 >= tileDrawLightThreshold ||
                             LightmapRenderer.getHighestRGB(x - 1, y - 1) ?: 0 >= tileDrawLightThreshold ||
                             LightmapRenderer.getHighestRGB(x + 1, y + 1) ?: 0 >= tileDrawLightThreshold ||
                             LightmapRenderer.getHighestRGB(x + 1, y - 1) ?: 0 >= tileDrawLightThreshold ||
                             LightmapRenderer.getHighestRGB(x - 1, y + 1) ?: 0 >= tileDrawLightThreshold) {
                            // blackness
                            if (zeroTileCounter > 0) {
                                /* unable to do anything */

                                zeroTileCounter = 0
                            }


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
                                if (BlocksDrawer.isBlendMul(thisTile)) {
                                    drawTile(mode, x, y, thisTileX, thisTileY)
                                }
                            }
                            else {
                                // do NOT add "if (!isBlendMul(thisTile))"!
                                // or else they will not look like they should be when backed with wall
                                drawTile(mode, x, y, thisTileX, thisTileY)
                            }

                            // draw a breakage
                            if (mode == TERRAIN || mode == WALL) {
                                val breakage = if (mode == TERRAIN) world.getTerrainDamage(x, y) else world.getWallDamage(x, y)
                                val maxHealth = BlockCodex[world.getTileFromTerrain(x, y)].strength
                                val stage = (breakage / maxHealth).times(breakAnimSteps).roundInt()
                                // actual drawing
                                if (stage > 0) {
                                    // alpha blending works, but no GL blend func...
                                    drawTile(mode, x, y, 5 + stage, 0)
                                }
                            }


                        } // end if (is illuminated)
                        // draw black patch
                        else {
                            zeroTileCounter++ // unused for now

                            GL11.glColor4f(0f, 0f, 0f, 1f)

                            GL11.glTexCoord2f(0f, 0f)
                            GL11.glVertex3f(x * TILE_SIZE.toFloat(), y * TILE_SIZE.toFloat(), 0f)
                            GL11.glTexCoord2f(0f, 0f + TILE_SIZE)
                            GL11.glVertex3f(x * TILE_SIZE.toFloat(), (y + 1) * TILE_SIZE.toFloat(), 0f)
                            GL11.glTexCoord2f(0f + TILE_SIZE, 0f + TILE_SIZE)
                            GL11.glVertex3f((x + 1) * TILE_SIZE.toFloat(), (y + 1) * TILE_SIZE.toFloat(), 0f)
                            GL11.glTexCoord2f(0f + TILE_SIZE, 0f)
                            GL11.glVertex3f((x + 1) * TILE_SIZE.toFloat(), y * TILE_SIZE.toFloat(), 0f)

                            GL11.glColor4f(1f, 1f, 1f, 1f)
                        }
                    } // end if (not an air)
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

    private fun drawTile(mode: Int, tilewisePosX: Int, tilewisePosY: Int, sheetX: Int, sheetY: Int) {
        if (mode == TERRAIN || mode == WALL)
            tilesTerrain.renderInUse(
                    FastMath.floor((tilewisePosX * TILE_SIZE).toFloat()),
                    FastMath.floor((tilewisePosY * TILE_SIZE).toFloat()),
                    sheetX, sheetY
            )
        else if (mode == WIRE)
            tilesWire.renderInUse(
                    FastMath.floor((tilewisePosX * TILE_SIZE).toFloat()),
                    FastMath.floor((tilewisePosY * TILE_SIZE).toFloat()),
                    sheetX, sheetY
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

    fun getRenderStartX(): Int = x / TILE_SIZE
    fun getRenderStartY(): Int = y / TILE_SIZE

    fun getRenderEndX(): Int = clampWTile(getRenderStartX() + (width / TILE_SIZE) + 2)
    fun getRenderEndY(): Int = clampHTile(getRenderStartY() + (height / TILE_SIZE) + 2)

    fun isConnectSelf(b: Int?): Boolean = TILES_CONNECT_SELF.contains(b)
    fun isConnectMutual(b: Int?): Boolean = TILES_CONNECT_MUTUAL.contains(b)
    fun isWallSticker(b: Int?): Boolean = TILES_WALL_STICKER.contains(b)
    fun isPlatform(b: Int?): Boolean = TILES_WALL_STICKER_CONNECT_SELF.contains(b)
    fun isBlendMul(b: Int?): Boolean = TILES_BLEND_MUL.contains(b)

    fun tileInCamera(x: Int, y: Int) =
            x >= WorldCamera.x.div(TILE_SIZE) && y >= WorldCamera.y.div(TILE_SIZE) &&
            x <= WorldCamera.x.plus(width).div(TILE_SIZE) && y <= WorldCamera.y.plus(width).div(TILE_SIZE)
}
