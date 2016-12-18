package net.torvald.terrarum.mapdrawer

import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.PairedMapLayer
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.tileproperties.Tile
import net.torvald.terrarum.tileproperties.TileCodex
import com.jme3.math.FastMath
import net.torvald.terrarum.concurrent.ThreadPool
import net.torvald.terrarum.blendMul
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.mapdrawer.MapDrawer.TILE_SIZE
import org.lwjgl.opengl.GL11
import org.newdawn.slick.*
import java.util.*

/**
 * Created by minjaesong on 16-01-19.
 */
object MapCamera {
    val world: GameWorld = Terrarum.ingame.world

    var cameraX = 0
        private set
    var cameraY = 0
        private set

    private val TILE_SIZE = MapDrawer.TILE_SIZE

    var tilesWall: SpriteSheet = SpriteSheet("./assets/graphics/terrain/wall.png", TILE_SIZE, TILE_SIZE)
        private set
    var tilesTerrain: SpriteSheet = SpriteSheet("./assets/graphics/terrain/terrain.tga", TILE_SIZE, TILE_SIZE)
        private set // Slick has some weird quirks with PNG's transparency. I'm using 32-bit targa here.
    var tilesWire: SpriteSheet = SpriteSheet("./assets/graphics/terrain/wire.png", TILE_SIZE, TILE_SIZE)
        private set
    var tilesetBook: Array<SpriteSheet> = arrayOf(tilesWall, tilesTerrain, tilesWire)
        private set

    val WALL = GameWorld.WALL
    val TERRAIN = GameWorld.TERRAIN
    val WIRE = GameWorld.WIRE

    var renderWidth: Int = 0
        private set
    var renderHeight: Int = 0
        private set

    private val NEARBY_TILE_KEY_UP = 0
    private val NEARBY_TILE_KEY_RIGHT = 1
    private val NEARBY_TILE_KEY_DOWN = 2
    private val NEARBY_TILE_KEY_LEFT = 3

    private val NEARBY_TILE_CODE_UP = 1
    private val NEARBY_TILE_CODE_RIGHT = 2
    private val NEARBY_TILE_CODE_DOWN = 4
    private val NEARBY_TILE_CODE_LEFT = 8

    /**
     * Connectivity group 01 : artificial tiles
     * It holds different shading rule to discriminate with group 02, index 0 is single tile.
     * These are the tiles that only connects to itself, will not connect to colour variants
     */
    val TILES_CONNECT_SELF = arrayOf(
            Tile.ICE_MAGICAL,
            Tile.GLASS_CRUDE,
            Tile.GLASS_CLEAN,
            Tile.ILLUMINATOR_BLACK,
            Tile.ILLUMINATOR_BLUE,
            Tile.ILLUMINATOR_BROWN,
            Tile.ILLUMINATOR_CYAN,
            Tile.ILLUMINATOR_FUCHSIA,
            Tile.ILLUMINATOR_GREEN,
            Tile.ILLUMINATOR_GREEN_DARK,
            Tile.ILLUMINATOR_GREY_DARK,
            Tile.ILLUMINATOR_GREY_LIGHT,
            Tile.ILLUMINATOR_GREY_MED,
            Tile.ILLUMINATOR_ORANGE,
            Tile.ILLUMINATOR_PURPLE,
            Tile.ILLUMINATOR_RED,
            Tile.ILLUMINATOR_TAN,
            Tile.ILLUMINATOR_WHITE,
            Tile.ILLUMINATOR_YELLOW,
            Tile.ILLUMINATOR_BLACK_OFF,
            Tile.ILLUMINATOR_BLUE_OFF,
            Tile.ILLUMINATOR_BROWN_OFF,
            Tile.ILLUMINATOR_CYAN_OFF,
            Tile.ILLUMINATOR_FUCHSIA_OFF,
            Tile.ILLUMINATOR_GREEN_OFF,
            Tile.ILLUMINATOR_GREEN_DARK_OFF,
            Tile.ILLUMINATOR_GREY_DARK_OFF,
            Tile.ILLUMINATOR_GREY_LIGHT_OFF,
            Tile.ILLUMINATOR_GREY_MED_OFF,
            Tile.ILLUMINATOR_ORANGE_OFF,
            Tile.ILLUMINATOR_PURPLE_OFF,
            Tile.ILLUMINATOR_RED_OFF,
            Tile.ILLUMINATOR_TAN_OFF,
            Tile.ILLUMINATOR_WHITE_OFF,
            Tile.ILLUMINATOR_YELLOW,
            Tile.SANDSTONE,
            Tile.SANDSTONE_BLACK,
            Tile.SANDSTONE_DESERT,
            Tile.SANDSTONE_RED,
            Tile.SANDSTONE_WHITE,
            Tile.SANDSTONE_GREEN,
            Tile.DAYLIGHT_CAPACITOR
    )

    /**
     * Connectivity group 02 : natural tiles
     * It holds different shading rule to discriminate with group 01, index 0 is middle tile.
     */
    val TILES_CONNECT_MUTUAL = arrayOf(
            Tile.STONE,
            Tile.STONE_QUARRIED,
            Tile.STONE_TILE_WHITE,
            Tile.STONE_BRICKS,
            Tile.DIRT,
            Tile.GRASS,
            Tile.PLANK_BIRCH,
            Tile.PLANK_BLOODROSE,
            Tile.PLANK_EBONY,
            Tile.PLANK_NORMAL,
            Tile.SAND,
            Tile.SAND_WHITE,
            Tile.SAND_RED,
            Tile.SAND_DESERT,
            Tile.SAND_BLACK,
            Tile.SAND_GREEN,
            Tile.GRAVEL,
            Tile.GRAVEL_GREY,
            Tile.SNOW,
            Tile.ICE_NATURAL,
            Tile.ORE_COPPER,
            Tile.ORE_IRON,
            Tile.ORE_GOLD,
            Tile.ORE_SILVER,
            Tile.ORE_ILMENITE,
            Tile.ORE_AURICHALCUM,

            Tile.WATER,
            Tile.WATER_1,
            Tile.WATER_2,
            Tile.WATER_3,
            Tile.WATER_4,
            Tile.WATER_5,
            Tile.WATER_6,
            Tile.WATER_7,
            Tile.WATER_8,
            Tile.WATER_9,
            Tile.WATER_10,
            Tile.WATER_11,
            Tile.WATER_12,
            Tile.WATER_13,
            Tile.WATER_14,
            Tile.WATER_15,
            Tile.LAVA,
            Tile.LAVA_1,
            Tile.LAVA_2,
            Tile.LAVA_3,
            Tile.LAVA_4,
            Tile.LAVA_5,
            Tile.LAVA_6,
            Tile.LAVA_7,
            Tile.LAVA_8,
            Tile.LAVA_9,
            Tile.LAVA_10,
            Tile.LAVA_11,
            Tile.LAVA_12,
            Tile.LAVA_13,
            Tile.LAVA_14,
            Tile.LAVA_15
    )

    /**
     * Torches, levers, switches, ...
     */
    val TILES_WALL_STICKER = arrayOf(
            Tile.TORCH,
            Tile.TORCH_FROST,
            Tile.TORCH_OFF,
            Tile.TORCH_FROST_OFF
    )

    /**
     * platforms, ...
     */
    val TILES_WALL_STICKER_CONNECT_SELF = arrayOf(
            Tile.PLATFORM_BIRCH,
            Tile.PLATFORM_BLOODROSE,
            Tile.PLATFORM_EBONY,
            Tile.PLATFORM_STONE,
            Tile.PLATFORM_WOODEN
    )

    /**
     * Tiles that half-transparent and has hue
     * will blend colour using colour multiplication
     * i.e. red hues get lost if you dive into the water
     */
    val TILES_BLEND_MUL = arrayOf(
            Tile.WATER,
            Tile.WATER_1,
            Tile.WATER_2,
            Tile.WATER_3,
            Tile.WATER_4,
            Tile.WATER_5,
            Tile.WATER_6,
            Tile.WATER_7,
            Tile.WATER_8,
            Tile.WATER_9,
            Tile.WATER_10,
            Tile.WATER_11,
            Tile.WATER_12,
            Tile.WATER_13,
            Tile.WATER_14,
            Tile.WATER_15,
            Tile.LAVA,
            Tile.LAVA_1,
            Tile.LAVA_2,
            Tile.LAVA_3,
            Tile.LAVA_4,
            Tile.LAVA_5,
            Tile.LAVA_6,
            Tile.LAVA_7,
            Tile.LAVA_8,
            Tile.LAVA_9,
            Tile.LAVA_10,
            Tile.LAVA_11,
            Tile.LAVA_12,
            Tile.LAVA_13,
            Tile.LAVA_14,
            Tile.LAVA_15
    )

    fun update(gc: GameContainer, delta_t: Int) {
        val player = Terrarum.ingame.player

        renderWidth = FastMath.ceil(Terrarum.WIDTH / Terrarum.ingame.screenZoom) // div, not mul
        renderHeight = FastMath.ceil(Terrarum.HEIGHT / Terrarum.ingame.screenZoom)

        // position - (WH / 2)
        cameraX = Math.round( // X only: ROUNDWORLD implementation
                player.hitbox.centeredX.toFloat() - renderWidth / 2)
        cameraY = Math.round(FastMath.clamp(
                player.hitbox.centeredY.toFloat() - renderHeight / 2,
                TILE_SIZE.toFloat(), world.height * TILE_SIZE - renderHeight - TILE_SIZE.toFloat()))

    }

    fun renderBehind(gc: GameContainer, g: Graphics) {
        /**
         * render to camera
         */
        blendNormal()
        drawTiles(g, WALL, false)
        drawTiles(g, TERRAIN, false)
    }

    fun renderFront(gc: GameContainer, g: Graphics) {
        blendMul()
        drawTiles(g, TERRAIN, true)
        blendNormal()
    }

    private fun drawTiles(g: Graphics, mode: Int, drawModeTilesBlendMul: Boolean) {
        val for_y_start = MapCamera.cameraY / TILE_SIZE
        val for_y_end = MapCamera.clampHTile(for_y_start + (MapCamera.renderHeight / TILE_SIZE) + 2)

        val for_x_start = MapCamera.cameraX / TILE_SIZE - 1
        val for_x_end = for_x_start + (MapCamera.renderWidth / TILE_SIZE) + 3

        // initialise
        MapCamera.tilesetBook[mode].startUse()

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

                // draw
                try {
                    if (
                    (mode == WALL || mode == TERRAIN) &&  // not an air tile
                    (thisTile ?: 0) > 0) //&& // commented out: meh
                    // check if light level of nearby or this tile is illuminated
                    /*(    LightmapRenderer.getValueFromMap(x, y) ?: 0 > 0 ||
                         LightmapRenderer.getValueFromMap(x - 1, y) ?: 0 > 0 ||
                         LightmapRenderer.getValueFromMap(x + 1, y) ?: 0 > 0 ||
                         LightmapRenderer.getValueFromMap(x, y - 1) ?: 0 > 0 ||
                         LightmapRenderer.getValueFromMap(x, y + 1) ?: 0 > 0 ||
                         LightmapRenderer.getValueFromMap(x - 1, y - 1) ?: 0 > 0 ||
                         LightmapRenderer.getValueFromMap(x + 1, y + 1) ?: 0 > 0 ||
                         LightmapRenderer.getValueFromMap(x + 1, y - 1) ?: 0 > 0 ||
                         LightmapRenderer.getValueFromMap(x - 1, y + 1) ?: 0 > 0)
                    )*/ {
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


                        val thisTileX: Int
                        if (!noDamageLayer)
                            thisTileX = PairedMapLayer.RANGE * ((thisTile ?: 0) % PairedMapLayer.RANGE) + nearbyTilesInfo
                        else
                            thisTileX = nearbyTilesInfo

                        val thisTileY = (thisTile ?: 0) / PairedMapLayer.RANGE

                        if (drawModeTilesBlendMul) {
                            if (MapCamera.isBlendMul(thisTile)) {
                                drawTile(mode, x, y, thisTileX, thisTileY)
                            }
                        } else {
                            // do NOT add "if (!isBlendMul(thisTile))"!
                            // or else they will not look like they should be when backed with wall
                            drawTile(mode, x, y, thisTileX, thisTileY)
                        }
                    } // end if (not an air and is illuminated)
                    else {
                        zeroTileCounter++
                    }
                } catch (e: NullPointerException) {
                    // do nothing. WARNING: This exception handling may hide erratic behaviour completely.
                }

            }
        }

        MapCamera.tilesetBook[mode].endUse()
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
        nearbyTiles[NEARBY_TILE_KEY_LEFT] = world.getTileFrom(mode, x - 1, y) ?: 4096
        nearbyTiles[NEARBY_TILE_KEY_RIGHT] = world.getTileFrom(mode, x + 1, y) ?: 4096
        nearbyTiles[NEARBY_TILE_KEY_UP] = world.getTileFrom(mode, x    , y - 1) ?: 4906
        nearbyTiles[NEARBY_TILE_KEY_DOWN] = world.getTileFrom(mode, x    , y + 1) ?: 4096

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
        nearbyTiles[NEARBY_TILE_KEY_LEFT] = world.getTileFrom(mode, x - 1, y) ?: 4096
        nearbyTiles[NEARBY_TILE_KEY_RIGHT] = world.getTileFrom(mode, x + 1, y) ?: 4096
        nearbyTiles[NEARBY_TILE_KEY_UP] = world.getTileFrom(mode, x    , y - 1) ?: 4906
        nearbyTiles[NEARBY_TILE_KEY_DOWN] = world.getTileFrom(mode, x    , y + 1) ?: 4096

        // try for
        var ret = 0
        for (i in 0..3) {
            try {
                if (!TileCodex[nearbyTiles[i]].isSolid &&
                    !TileCodex[nearbyTiles[i]].isFluid) {
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
        nearbyTiles[NEARBY_TILE_KEY_LEFT] = world.getTileFrom(TERRAIN, x - 1, y) ?: 4096
        nearbyTiles[NEARBY_TILE_KEY_RIGHT] = world.getTileFrom(TERRAIN, x + 1, y) ?: 4096
        nearbyTiles[NEARBY_TILE_KEY_DOWN] = world.getTileFrom(TERRAIN, x    , y + 1) ?: 4096
        nearbyTiles[NEARBY_TILE_KEY_BACK] = world.getTileFrom(WALL,    x    , y) ?: 4096

        try {
            if (TileCodex[nearbyTiles[NEARBY_TILE_KEY_DOWN]].isSolid)
                // has tile on the bottom
                return 3
            else if (TileCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolid
                     && TileCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolid)
                // has tile on both sides
                return 0
            else if (TileCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolid)
                // has tile on the right
                return 2
            else if (TileCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolid)
                // has tile on the left
                return 1
            else if (TileCodex[nearbyTiles[NEARBY_TILE_KEY_BACK]].isSolid)
                // has tile on the back
                return 0
            else
                return 3
        } catch (e: ArrayIndexOutOfBoundsException) {
            return if (TileCodex[nearbyTiles[NEARBY_TILE_KEY_DOWN]].isSolid)
                // has tile on the bottom
                3 else 0
        }
    }

    fun getNearbyTilesInfoPlatform(x: Int, y: Int): Int {
        val nearbyTiles = IntArray(4)
        nearbyTiles[NEARBY_TILE_KEY_LEFT] =  world.getTileFrom(TERRAIN, x - 1, y) ?: 4096
        nearbyTiles[NEARBY_TILE_KEY_RIGHT] = world.getTileFrom(TERRAIN, x + 1, y) ?: 4096

        if ((TileCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolid &&
            TileCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolid) ||
            isPlatform(nearbyTiles[NEARBY_TILE_KEY_LEFT]) &&
            isPlatform(nearbyTiles[NEARBY_TILE_KEY_RIGHT])) // LR solid || LR platform
            return 0
        else if (TileCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolid &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_LEFT]) &&
                 !TileCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolid &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_RIGHT])) // L solid and not platform && R not solid and not platform
            return 4
        else if (TileCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolid &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_RIGHT]) &&
                 !TileCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolid &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_LEFT])) // R solid and not platform && L not solid and nto platform
            return 6
        else if (TileCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolid &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_LEFT])) // L solid && L not platform
            return 3
        else if (TileCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolid &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_RIGHT])) // R solid && R not platform
            return 5
        else if ((TileCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolid ||
                  isPlatform(nearbyTiles[NEARBY_TILE_KEY_LEFT])) &&
                 !TileCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolid &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_RIGHT])) // L solid or platform && R not solid and not platform
            return 1
        else if ((TileCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolid ||
                  isPlatform(nearbyTiles[NEARBY_TILE_KEY_RIGHT])) &&
                 !TileCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolid &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_LEFT])) // R solid or platform && L not solid and not platform
            return 2
        else
            return 7
    }

    private fun drawTile(mode: Int, tilewisePosX: Int, tilewisePosY: Int, sheetX: Int, sheetY: Int) {
        tilesetBook[mode].renderInUse(
                FastMath.floor((tilewisePosX * TILE_SIZE).toFloat()),
                FastMath.floor((tilewisePosY * TILE_SIZE).toFloat()),
                sheetX, sheetY
        )
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

    fun getRenderStartX(): Int = cameraX / TILE_SIZE
    fun getRenderStartY(): Int = cameraY / TILE_SIZE

    fun getRenderEndX(): Int = clampWTile(getRenderStartX() + (renderWidth / TILE_SIZE) + 2)
    fun getRenderEndY(): Int = clampHTile(getRenderStartY() + (renderHeight / TILE_SIZE) + 2)

    fun isConnectSelf(b: Int?): Boolean = TILES_CONNECT_SELF.contains(b)
    fun isConnectMutual(b: Int?): Boolean = TILES_CONNECT_MUTUAL.contains(b)
    fun isWallSticker(b: Int?): Boolean = TILES_WALL_STICKER.contains(b)
    fun isPlatform(b: Int?): Boolean = TILES_WALL_STICKER_CONNECT_SELF.contains(b)
    fun isBlendMul(b: Int?): Boolean = TILES_BLEND_MUL.contains(b)

    fun tileInCamera(x: Int, y: Int) =
            x >= cameraX.div(TILE_SIZE) && y >= cameraY.div(TILE_SIZE) &&
            x <= cameraX.plus(renderWidth).div(TILE_SIZE) && y <= cameraY.plus(renderWidth).div(TILE_SIZE)
}
