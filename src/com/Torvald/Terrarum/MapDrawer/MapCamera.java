package com.Torvald.Terrarum.MapDrawer;

import com.Torvald.Terrarum.*;
import com.Torvald.Terrarum.Actors.Player;
import com.Torvald.Terrarum.GameMap.GameMap;
import com.Torvald.Terrarum.GameMap.MapLayer;
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

    private static final int WALL = 0;
    private static final int TERRAIN = 1;
    private static final int WIRE = 2;

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

    private static final byte AIR = 0;

    private static final byte STONE = 1;
    private static final byte DIRT = 2;
    private static final byte GRASS = 3;

    private static final byte SAND = 13;
    private static final byte GRAVEL = 14;

    private static final byte COPPER = 15;
    private static final byte IRON = 16;
    private static final byte GOLD = 17;
    private static final byte SILVER = 18;
    private static final byte ILMENITE = 19;
    private static final byte AURICHALCUM = 20;

    private static final byte SNOW = 27;
    private static final byte ICE_FRAGILE = 28;
    private static final byte ICE_NATURAL = 29;
    private static final byte ICE_MAGICAL = 30;

    private static Byte[] TILES_CONNECT_SELF = {
              COPPER
            , IRON
            , GOLD
            , SILVER
            , ILMENITE
            , AURICHALCUM
            , ICE_MAGICAL
    };

    private static Byte[] TILES_DARKEN_AIR = {
              STONE
            , DIRT
            , GRASS
            , SAND
            , GRAVEL
            , SNOW
            , ICE_NATURAL
            , (byte)224, (byte)225, (byte)226, (byte)227, (byte)228, (byte)229, (byte)230, (byte)231
            , (byte)232, (byte)233, (byte)234, (byte)235, (byte)236, (byte)237, (byte)238, (byte)239
            , (byte)240, (byte)241, (byte)242, (byte)243, (byte)244, (byte)245, (byte)246, (byte)247
            , (byte)248, (byte)249, (byte)250, (byte)251, (byte)252, (byte)253, (byte)254, (byte)255
    };

    /**
     * Tiles that half-transparent and has hue
     * will blend colour using colour multiplication
     * i.e. red hues get lost if you dive into the water
     */
    private static Byte[] TILES_BLEND_MUL = {
              (byte)224, (byte)225, (byte)226, (byte)227, (byte)228, (byte)229, (byte)230, (byte)231
            , (byte)232, (byte)233, (byte)234, (byte)235, (byte)236, (byte)237, (byte)238, (byte)239
            , (byte)240, (byte)241, (byte)242, (byte)243, (byte)244, (byte)245, (byte)246, (byte)247
            , (byte)248, (byte)249, (byte)250, (byte)251, (byte)252, (byte)253, (byte)254, (byte)255
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

        tilesTerrain = new SpriteSheet("./res/graphics/terrain/terrainplusplus.png"
                , TSIZE
                , TSIZE
        );

        tilesWire = new SpriteSheet("./res/graphics/terrain/wire.png"
                , TSIZE
                , TSIZE
        );

        tilesetBook = new SpriteSheet[9];
        tilesetBook[WALL] = tilesWall;
        tilesetBook[TERRAIN] = tilesTerrain;
        tilesetBook[WIRE] = tilesWire;
    }

    public static void update(GameContainer gc, int delta_t) {
        Player player = Terrarum.game.getPlayer();

        renderWidth = FastMath.ceil(Terrarum.WIDTH / Terrarum.game.screenZoom);
        renderHeight = FastMath.ceil(Terrarum.HEIGHT / Terrarum.game.screenZoom);

        // position - (WH / 2)
        cameraX = clamp(
                Math.round(player.getNextHitbox().getPointedX() - (renderWidth / 2))
                , map.width * TSIZE - renderWidth
        );
        cameraY = clamp(
                Math.round(player.getNextHitbox().getPointedY() - (renderHeight / 2))
                , map.height * TSIZE - renderHeight
        );
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

        MapLayer currentLayer = (mode % 3 == WALL) ? map.getLayerWall()
                                               : (mode % 3 == TERRAIN) ? map.getLayerTerrain()
                                                                   : map.getLayerWire();

        // initialise
        tilesetBook[mode].startUse();

        // loop
        for (int y = for_y_start; y < for_y_end; y++) {
            for (int x = for_x_start; x < for_x_end; x++) {

                int thisTile = currentLayer.getTile(x, y);
                int thisTerrainTile = map.getTileFromTerrain(x, y);

                // draw
                if (

                        (
                                (       // wall and not blocked
                                        (mode == WALL) && (!isOpaque(thisTerrainTile))
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
                                        ))
                        ) {

                    if (mode == TERRAIN) {
                        int nearbyTilesInfo;
                        //if (thisTile == DIRT) {
                        //    nearbyTilesInfo = getGrassInfo(x, y, GRASS);
                        //}
                        //else {
                        //    nearbyTilesInfo = getNearbyTilesInfo(x, y, AIR);
                        //}

                        if (isDarkenAir((byte) thisTile)) {
                            nearbyTilesInfo = getNearbyTilesInfo(x, y, AIR);
                        }
                        else if (isConnectSelf((byte) thisTile)) {
                            nearbyTilesInfo = getNearbyTilesInfo(x, y, thisTile);
                        }
                        else {
                            nearbyTilesInfo = 0;
                        }


                        int thisTileX = nearbyTilesInfo;
                        int thisTileY = thisTile;

                        if (drawModeTilesBlendMul) {
                            if (isBlendMul((byte) thisTile)) drawTile(TERRAIN, x, y, thisTileX, thisTileY);
                        }
                        else {
                            // currently it draws all the transparent tile and colour mixes
                            // on top of the previously drawn tile
                            // TODO check wether it works as intended when skybox is dark
                            // add instruction "if (!isBlendMul((byte) thisTile))"
                            drawTile(TERRAIN, x, y, thisTileX, thisTileY);
                        }
                    }
                    else {
                        drawTile(mode, x, y, mod16(thisTile), div16(thisTile));
                    }
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
    private static int getNearbyTilesInfo(int x, int y, int mark) {
        int[] nearbyTiles = new int[4];
        if (x == 0) { nearbyTiles[NEARBY_TILE_KEY_LEFT] = 0xFF; }
        else { nearbyTiles[NEARBY_TILE_KEY_LEFT] = map.getTileFromTerrain(x - 1, y); }

        if (x == map.width - 1) { nearbyTiles[NEARBY_TILE_KEY_RIGHT] = 0xFF; }
        else { nearbyTiles[NEARBY_TILE_KEY_RIGHT] = map.getTileFromTerrain(x + 1, y); }

        if (y == 0) { nearbyTiles[NEARBY_TILE_KEY_UP] = 0; }
        else { nearbyTiles[NEARBY_TILE_KEY_UP] = map.getTileFromTerrain(x, y - 1); }

        if (y == map.height - 1) { nearbyTiles[NEARBY_TILE_KEY_DOWN] = 0xFF; }
        else { nearbyTiles[NEARBY_TILE_KEY_DOWN] = map.getTileFromTerrain(x, y + 1); }

        // try for
        int ret = 0;
        for (int i = 0; i < 4; i++) {
            if (nearbyTiles[i] == mark) {
                ret += (1 << i); // add 1, 2, 4, 8 for i = 0, 1, 2, 3
            }
        }

        return ret;

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

    private static int div16(int x) {
        return (x & 0x7FFF_FFFF) >> 4;
    }

    private static int mod16(int x) {
        return x & 0b1111;
    }

    private static int quantise16(int x) {
        return (x & 0xFFFF_FFF0);
    }

    private static int clampW(int x) {
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

    private static int clampH(int x) {
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

    private static int clampWTile(int x) {
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

    private static int clampHTile(int x) {
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

    private static int clamp(int x, int lim) {
        if (x < 0) {
            return 0;
        }
        else if (x > lim) {
            return lim;
        }
        else {
            return x;
        }
    }

    private static Image getTileByIndex(SpriteSheet s, int i) {
        return s.getSprite(i % 16, i / 16);
    }

    private static boolean isOpaque(int x) {
        return (x >= 1 && x <= 38)
                || (x >= 41 && x <= 44)
                || (x >= 46 && x <= 47)
                || (x >= 64 && x <= 86)
                || (x >= 88 && x <= 116);
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

    private static boolean isConnectSelf(byte b) {
        return (Arrays.asList(TILES_CONNECT_SELF).contains(b));
    }

    private static boolean isDarkenAir(byte b) {
        return (Arrays.asList(TILES_DARKEN_AIR).contains(b));
    }

    private static boolean isBlendMul(byte b) {
        return (Arrays.asList(TILES_BLEND_MUL).contains(b));
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
