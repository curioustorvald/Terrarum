package com.Torvald.Terrarum.MapDrawer;

import com.Torvald.Terrarum.*;
import com.Torvald.Terrarum.Actors.Player;
import com.Torvald.Terrarum.GameMap.GameMap;
import com.Torvald.Terrarum.GameMap.PairedMapLayer;
import com.Torvald.Terrarum.TileProperties.TileNameCode;
import com.Torvald.Terrarum.TileProperties.TilePropCodex;
import com.jme3.math.FastMath;
import org.lwjgl.opengl.GL11;
import org.newdawn.slick.*;

import java.util.Arrays;

/**
 * Created by minjaesong on 16-01-19.
 */
public class MapCamera {

    private static GameMap map;

    private static int cameraX = 0;
    private static int cameraY = 0;

    private static SpriteSheet tilesWall;
    private static SpriteSheet tilesTerrain;
    private static SpriteSheet tilesWire;

    private static int TSIZE = MapDrawer.TILE_SIZE;


    private static SpriteSheet[] tilesetBook;

    private static final int WALL = GameMap.WALL;
    private static final int TERRAIN = GameMap.TERRAIN;
    private static final int WIRE = GameMap.WIRE;

    private static int renderWidth;
    private static int renderHeight;

    private static final int NEARBY_TILE_KEY_UP = 0;
    private static final int NEARBY_TILE_KEY_RIGHT = 1;
    private static final int NEARBY_TILE_KEY_DOWN = 2;
    private static final int NEARBY_TILE_KEY_LEFT = 3;

    private static final int NEARBY_TILE_CODE_UP = 0b0001;
    private static final int NEARBY_TILE_CODE_RIGHT = 0b0010;
    private static final int NEARBY_TILE_CODE_DOWN = 0b0100;
    private static final int NEARBY_TILE_CODE_LEFT = 0b1000;

    /**
     * Connectivity group 01 : artificial tiles
     * It holds different shading rule to discriminate with group 02, index 0 is single tile.
     * These are the tiles that only connects to itself, will not connect to colour variants
     */
    private static Integer[] TILES_CONNECT_SELF = {
              TileNameCode.ICE_MAGICAL
            , TileNameCode.ILLUMINATOR_BLACK
            , TileNameCode.ILLUMINATOR_BLUE
            , TileNameCode.ILLUMINATOR_BROWN
            , TileNameCode.ILLUMINATOR_CYAN
            , TileNameCode.ILLUMINATOR_FUCHSIA
            , TileNameCode.ILLUMINATOR_GREEN
            , TileNameCode.ILLUMINATOR_GREEN_DARK
            , TileNameCode.ILLUMINATOR_GREY_DARK
            , TileNameCode.ILLUMINATOR_GREY_LIGHT
            , TileNameCode.ILLUMINATOR_GREY_MED
            , TileNameCode.ILLUMINATOR_ORANGE
            , TileNameCode.ILLUMINATOR_PURPLE
            , TileNameCode.ILLUMINATOR_RED
            , TileNameCode.ILLUMINATOR_TAN
            , TileNameCode.ILLUMINATOR_WHITE
            , TileNameCode.ILLUMINATOR_YELLOW

    };

    /**
     * Connectivity group 02 : natural tiles
     * It holds different shading rule to discriminate with group 01, index 0 is middle tile.
     */
    private static Integer[] TILES_CONNECT_MUTUAL = {
              TileNameCode.STONE
            , TileNameCode.DIRT
            , TileNameCode.GRASS
            , TileNameCode.PLANK_BIRCH
            , TileNameCode.PLANK_BLOODROSE
            , TileNameCode.PLANK_EBONY
            , TileNameCode.PLANK_NORMAL
            , TileNameCode.SAND
            , TileNameCode.SAND_BEACH
            , TileNameCode.SAND_RED
            , TileNameCode.SAND_DESERT
            , TileNameCode.SAND_BLACK
            , TileNameCode.GRAVEL
            , TileNameCode.GRAVEL_GREY
            , TileNameCode.SNOW
            , TileNameCode.ICE_NATURAL
            , TileNameCode.ORE_COPPER
            , TileNameCode.ORE_IRON
            , TileNameCode.ORE_GOLD
            , TileNameCode.ORE_SILVER
            , TileNameCode.ORE_ILMENITE
            , TileNameCode.ORE_AURICHALCUM

            , TileNameCode.WATER_1
            , TileNameCode.WATER_2
            , TileNameCode.WATER_3
            , TileNameCode.WATER_4
            , TileNameCode.WATER_5
            , TileNameCode.WATER_6
            , TileNameCode.WATER_7
            , TileNameCode.WATER_8
            , TileNameCode.WATER_9
            , TileNameCode.WATER_10
            , TileNameCode.WATER_11
            , TileNameCode.WATER_12
            , TileNameCode.WATER_13
            , TileNameCode.WATER_14
            , TileNameCode.WATER_15
            , TileNameCode.LAVA
            , TileNameCode.LAVA_1
            , TileNameCode.LAVA_2
            , TileNameCode.LAVA_3
            , TileNameCode.LAVA_4
            , TileNameCode.LAVA_5
            , TileNameCode.LAVA_6
            , TileNameCode.LAVA_7
            , TileNameCode.LAVA_8
            , TileNameCode.LAVA_9
            , TileNameCode.LAVA_10
            , TileNameCode.LAVA_11
            , TileNameCode.LAVA_12
            , TileNameCode.LAVA_13
            , TileNameCode.LAVA_14
            , TileNameCode.LAVA_15
            , TileNameCode.LAVA
    };

    /**
     * Torches, levers, switches, ...
     */
    private static Integer[] TILES_WALL_STICKER = {
              TileNameCode.TORCH
            , TileNameCode.TORCH_FROST
            , TileNameCode.TORCH_OFF
            , TileNameCode.TORCH_FROST_OFF
    };

    /**
     * platforms, ...
     */
    private static Integer[] TILES_WALL_STICKER_CONNECT_SELF = {

    };

    /**
     * Tiles that half-transparent and has hue
     * will blend colour using colour multiplication
     * i.e. red hues get lost if you dive into the water
     */
    private static Integer[] TILES_BLEND_MUL = {
              TileNameCode.WATER
            , TileNameCode.LAVA
    };

    /**
     * @param map
     */
    public MapCamera(GameMap map) throws SlickException {
        this.map = map;

        tilesWall = new SpriteSheet("./res/graphics/terrain/wall.png"
                , TSIZE
                , TSIZE
        );

        tilesTerrain = new SpriteSheet("./res/graphics/terrain/terrain.png"
                , TSIZE
                , TSIZE
        );

        tilesWire = new SpriteSheet("./res/graphics/terrain/wire.png"
                , TSIZE
                , TSIZE
        );

        tilesetBook = new SpriteSheet[3];
        tilesetBook[WALL] = tilesWall;
        tilesetBook[TERRAIN] = tilesTerrain;
        tilesetBook[WIRE] = tilesWire;
    }

    public static void update(GameContainer gc, int delta_t) {
        Player player = Terrarum.game.getPlayer();

        renderWidth = FastMath.ceil(Terrarum.WIDTH / Terrarum.game.screenZoom); // div, not mul
        renderHeight = FastMath.ceil(Terrarum.HEIGHT / Terrarum.game.screenZoom);

        // position - (WH / 2)
        cameraX = Math.round(FastMath.clamp(
                player.getHitbox().getCenteredX() - (renderWidth / 2)
                , TSIZE, map.width * TSIZE - renderWidth - TSIZE
        ));
        cameraY = Math.round(FastMath.clamp(
                player.getHitbox().getCenteredY() - (renderHeight / 2)
                , TSIZE, map.height * TSIZE - renderHeight - TSIZE
        ));
    }

    public static void renderBehind(GameContainer gc, Graphics g) {
        /**
         * render to camera
         */
        setBlendModeNormal();
        drawTiles(WALL, false);
        drawTiles(TERRAIN, false);
    }

    public static void renderFront(GameContainer gc, Graphics g) {
        setBlendModeMul();
        drawTiles(TERRAIN, true);
        setBlendModeNormal();
    }

    private static void drawTiles(int mode, boolean drawModeTilesBlendMul) {
        int for_y_start = div16(cameraY);
        int for_x_start = div16(cameraX);

        int for_y_end = clampHTile(for_y_start + div16(renderHeight) + 2);
        int for_x_end = clampWTile(for_x_start + div16(renderWidth) + 2);

        // initialise
        tilesetBook[mode].startUse();

        // loop
        for (int y = for_y_start; y < for_y_end; y++) {
            for (int x = for_x_start; x < for_x_end; x++) {

                int thisTile;
                if (mode % 3 == WALL) thisTile = map.getTileFromWall(x, y);
                else if (mode % 3 == TERRAIN) thisTile = map.getTileFromTerrain(x, y);
                else if (mode % 3 == WIRE) thisTile = map.getTileFromWire(x, y);
                else throw new IllegalArgumentException();

                boolean noDamageLayer = (mode % 3 == WIRE);

                // draw
                try {
                    if (

                            (
                                    (       // wall and not blocked
                                            (mode == WALL) && isWallThatBeDrawn(x, y)
                                    )
                                            ||
                                            (mode == TERRAIN)
                            )       // not an air tile
                                    && (thisTile > 0)
                                    &&
                                    // check if light level of upper tile is zero and
                                    // that of this tile is also zero
                                    (((y > 0)
                                            && !((LightmapRenderer.getValueFromMap(x, y) == 0)
                                            && (LightmapRenderer.getValueFromMap(x, y - 1) == 0))
                                    )
                                            ||
                                            // check if light level of this tile is zero, for y = 0
                                            ((y == 0)
                                                    && (LightmapRenderer.getValueFromMap(x, y) > 0)
                                            ))) {

                        int nearbyTilesInfo;
                        if (isWallSticker(thisTile)) {
                            nearbyTilesInfo = getNearbyTilesInfoWallSticker(x, y);
                        }
                        else if (isConnectMutual(thisTile)) {
                            nearbyTilesInfo = getNearbyTilesInfoNonSolid(x, y, mode);
                        }
                        else if (isConnectSelf(thisTile)) {
                            nearbyTilesInfo = getNearbyTilesInfo(x, y, mode, thisTile);
                        }
                        else {
                            nearbyTilesInfo = 0;
                        }


                        int thisTileX;
                        if (!noDamageLayer)
                            thisTileX = PairedMapLayer.RANGE * (thisTile % PairedMapLayer.RANGE)
                                    + nearbyTilesInfo;
                        else
                            thisTileX = nearbyTilesInfo;

                        int thisTileY = thisTile / PairedMapLayer.RANGE;

                        if (drawModeTilesBlendMul) {
                            if (isBlendMul(thisTile)) {
                                drawTile(mode, x, y, thisTileX, thisTileY);
                            }
                        }
                        else {
                            // currently it draws all the transparent tile and colour mixes
                            // on top of the previously drawn tile
                            // TODO check wether it works as intended when skybox is dark
                            // add instruction "if (!isBlendMul((byte) thisTile))"
                            if (!isBlendMul(thisTile)) {
                                drawTile(mode, x, y, thisTileX, thisTileY);
                            }
                        }
                    }
                }
                catch (NullPointerException e) {
                    // do nothing. This exception handling may hide erratic behaviour completely.
                }
            }
        }

        tilesetBook[mode].endUse();
    }

    private static int getGrassInfo(int x, int y, int from, int to) {
        return 0;
    }

    /**
     *
     * @param x
     * @param y
     * @return [0-15] 1: up, 2: right, 4: down, 8: left
     */
    private static int getNearbyTilesInfo(int x, int y, int mode, int mark) {
        int[] nearbyTiles = new int[4];
        if (x == 0) { nearbyTiles[NEARBY_TILE_KEY_LEFT] = 4096; }
        else { nearbyTiles[NEARBY_TILE_KEY_LEFT] = map.getTileFrom(mode, x - 1, y); }

        if (x == map.width - 1) { nearbyTiles[NEARBY_TILE_KEY_RIGHT] = 4096; }
        else { nearbyTiles[NEARBY_TILE_KEY_RIGHT] = map.getTileFrom(mode, x + 1, y); }

        if (y == 0) { nearbyTiles[NEARBY_TILE_KEY_UP] = 0; }
        else { nearbyTiles[NEARBY_TILE_KEY_UP] = map.getTileFrom(mode, x, y - 1); }

        if (y == map.height - 1) { nearbyTiles[NEARBY_TILE_KEY_DOWN] = 4096; }
        else { nearbyTiles[NEARBY_TILE_KEY_DOWN] = map.getTileFrom(mode, x, y + 1); }

        // try for
        int ret = 0;
        for (int i = 0; i < 4; i++) {
            if (nearbyTiles[i] == mark) {
                ret += (1 << i); // add 1, 2, 4, 8 for i = 0, 1, 2, 3
            }
        }

        return ret;
    }

    private static int getNearbyTilesInfoNonSolid(int x, int y, int mode) {
        int[] nearbyTiles = new int[4];
        if (x == 0) { nearbyTiles[NEARBY_TILE_KEY_LEFT] = 4096; }
        else { nearbyTiles[NEARBY_TILE_KEY_LEFT] = map.getTileFrom(mode, x - 1, y); }

        if (x == map.width - 1) { nearbyTiles[NEARBY_TILE_KEY_RIGHT] = 4096; }
        else { nearbyTiles[NEARBY_TILE_KEY_RIGHT] = map.getTileFrom(mode, x + 1, y); }

        if (y == 0) { nearbyTiles[NEARBY_TILE_KEY_UP] = 0; }
        else { nearbyTiles[NEARBY_TILE_KEY_UP] = map.getTileFrom(mode, x, y - 1); }

        if (y == map.height - 1) { nearbyTiles[NEARBY_TILE_KEY_DOWN] = 4096; }
        else { nearbyTiles[NEARBY_TILE_KEY_DOWN] = map.getTileFrom(mode, x, y + 1); }

        // try for
        int ret = 0;
        for (int i = 0; i < 4; i++) {
            if (!TilePropCodex.getProp(nearbyTiles[i]).isSolid()) {
                ret += (1 << i); // add 1, 2, 4, 8 for i = 0, 1, 2, 3
            }
        }

        return ret;
    }

    private static int getNearbyTilesInfoWallSticker(int x, int y) {
        int[] nearbyTiles = new int[4];
        int NEARBY_TILE_KEY_BACK = NEARBY_TILE_KEY_UP;
        if (x == 0) { nearbyTiles[NEARBY_TILE_KEY_LEFT] = 4096; }
        else { nearbyTiles[NEARBY_TILE_KEY_LEFT] = map.getTileFrom(TERRAIN, x - 1, y); }

        if (x == map.width - 1) { nearbyTiles[NEARBY_TILE_KEY_RIGHT] = 4096; }
        else { nearbyTiles[NEARBY_TILE_KEY_RIGHT] = map.getTileFrom(TERRAIN, x + 1, y); }

        if (y == map.height - 1) { nearbyTiles[NEARBY_TILE_KEY_DOWN] = 4096; }
        else { nearbyTiles[NEARBY_TILE_KEY_DOWN] = map.getTileFrom(TERRAIN, x, y + 1); }

        nearbyTiles[NEARBY_TILE_KEY_BACK] = map.getTileFrom(WALL, x, y);

        try {
            if (TilePropCodex.getProp(nearbyTiles[NEARBY_TILE_KEY_RIGHT]).isSolid()
                    && TilePropCodex.getProp(nearbyTiles[NEARBY_TILE_KEY_LEFT]).isSolid()) {
                if (TilePropCodex.getProp(nearbyTiles[NEARBY_TILE_KEY_BACK]).isSolid())
                     return 0;
                else return 3;
            }
            else if (TilePropCodex.getProp(nearbyTiles[NEARBY_TILE_KEY_RIGHT]).isSolid()) {
                return 2;
            }
            else if (TilePropCodex.getProp(nearbyTiles[NEARBY_TILE_KEY_LEFT]).isSolid()) {
                return 1;
            }
            else if (TilePropCodex.getProp(nearbyTiles[NEARBY_TILE_KEY_BACK]).isSolid()) {
                return 0;
            }
            else return 3;
        }
        catch (ArrayIndexOutOfBoundsException e) {
            return TilePropCodex.getProp(nearbyTiles[NEARBY_TILE_KEY_BACK]).isSolid()
                   ? 0 : 3;
        }
    }

    private static void drawTile(int mode, int tilewisePosX, int tilewisePosY, int sheetX, int sheetY) {
        if (Terrarum.game.screenZoom == 1) {
            tilesetBook[mode].renderInUse(
                    FastMath.floor(tilewisePosX * TSIZE)
                    , FastMath.floor(tilewisePosY * TSIZE)
                    , sheetX
                    , sheetY
            );
        }
        else {
            tilesetBook[mode].getSprite(
                    sheetX
                    , sheetY
            ).drawEmbedded(
                    Math.round(tilewisePosX * TSIZE * Terrarum.game.screenZoom)
                    , Math.round(tilewisePosY * TSIZE * Terrarum.game.screenZoom)
                    , FastMath.ceil(TSIZE * Terrarum.game.screenZoom)
                    , FastMath.ceil(TSIZE * Terrarum.game.screenZoom)
            );
        }
    }

    public static int div16(int x) {
        return (x & 0x7FFF_FFFF) >> 4;
    }

    public static int mod16(int x) {
        return x & 0b1111;
    }

    public static int quantise16(int x) {
        return (x & 0xFFFF_FFF0);
    }

    public static int clampW(int x) {
        if (x < 0) {
            return 0;
        }
        else if (x > map.width * TSIZE) {
            return map.width * TSIZE;
        }
        else {
            return x;
        }
    }

    public static int clampH(int x) {
        if (x < 0) {
            return 0;
        }
        else if (x > map.height * TSIZE) {
            return map.height * TSIZE;
        }
        else {
            return x;
        }
    }

    public static int clampWTile(int x) {
        if (x < 0) {
            return 0;
        }
        else if (x > map.width) {
            return map.width;
        }
        else {
            return x;
        }
    }

    public static int clampHTile(int x) {
        if (x < 0) {
            return 0;
        }
        else if (x > map.height) {
            return map.height;
        }
        else {
            return x;
        }
    }

    private static Image getTileByIndex(SpriteSheet s, int i) {
        return s.getSprite(i % 16, i / 16);
    }

    private static boolean isWallThatBeDrawn(int x, int y) {
        for (int i = 0; i < 9; i++) {
            int tx = x + (i % 3 - 1);
            int ty = y + (i / 3 - 1);

            if (tx < 0) tx = 0;
            else if (tx >= map.width) tx = map.width;
            if (ty < 0) ty = 0;
            else if (ty >= map.width) ty = map.width;

            try {
                if (!isOpaque(map.getTileFromTerrain(tx, ty))) {
                    return true;
                }
            }
            catch (ArrayIndexOutOfBoundsException e) {
                return false;
            }
        }
        return false;
    }

    private static boolean isOpaque(int x) {
        return TilePropCodex.getProp(x).isOpaque();
    }

    public static int getCameraX() {
        return cameraX;
    }

    public static int getCameraY() {
        return cameraY;
    }

    public static int getRenderWidth() {
        return renderWidth;
    }

    public static int getRenderHeight() {
        return renderHeight;
    }

    public static int getRenderStartX() {
        return div16(cameraX);
    }

    public static int getRenderStartY() {
        return div16(cameraY);
    }

    public static int getRenderEndX() {
        return clampWTile(getRenderStartX() + div16(renderWidth) + 2);
    }

    public static int getRenderEndY() {
        return clampHTile(getRenderStartY() + div16(renderHeight) + 2);
    }

    private static boolean isConnectSelf(int b) {
        return Arrays.asList(TILES_CONNECT_SELF).contains(b);
    }

    private static boolean isConnectMutual(int b) {
        return Arrays.asList(TILES_CONNECT_MUTUAL).contains(b);
    }

    private static boolean isWallSticker(int b) {
        return Arrays.asList(TILES_WALL_STICKER).contains(b);
    }

    private static boolean isPlatform(int b) {
        return Arrays.asList(TILES_WALL_STICKER_CONNECT_SELF).contains(b);
    }

    private static boolean isBlendMul(int b) {
        return Arrays.asList(TILES_BLEND_MUL).contains(b);
    }

    private static void setBlendModeMul() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private static void setBlendModeNormal() {
        GL11.glDisable(GL11.GL_BLEND);
        Terrarum.appgc.getGraphics().setDrawMode(Graphics.MODE_NORMAL);
    }
}
