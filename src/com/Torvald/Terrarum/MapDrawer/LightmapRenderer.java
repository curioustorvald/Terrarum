package com.Torvald.Terrarum.MapDrawer;

import com.Torvald.Terrarum.Terrarum;
import com.Torvald.Terrarum.TileProperties.TilePropCodex;
import com.jme3.math.FastMath;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by minjaesong on 16-01-25.
 */
public class LightmapRenderer {

    /**
     * 8-Bit RGB values
     */
    private static volatile int[][] staticLightMap;
    private static boolean lightMapInitialised = false;

    /**
     * For entities that emits light (e.g. Player with shine potion)
     */
    private static ArrayList<LightmapLantern> lanterns = new ArrayList<>();

    private static final int AIR = 0;


    private static final int OFFSET_R = 2;
    private static final int OFFSET_G = 1;
    private static final int OFFSET_B = 0;

    private static final int TSIZE = MapDrawer.TILE_SIZE;

    /**
     * Stores current light map as image.
     * WILL BE PURGED in every single round of light calculation.
     */
    //private static Graphics lightMapGraphicsInstance;


    public LightmapRenderer() {

    }

    public static void addLantern(int x, int y, LightmapLantern lantern) {
        // TODO check for duplicates
        lanterns.add(lantern);
    }

    public static void removeLantern(int x, int y) {
        for (int i = lanterns.size() - 1; i >= 0; i--) {
            LightmapLantern lantern = lanterns.get(i);
            if (lantern.getX() == x && lantern.getY() == y) {
                lanterns.remove(i);
            }
        }
    }

    public static void renderLightMap() {
        if (staticLightMap == null) {
            staticLightMap = new int[Terrarum.game.map.height][Terrarum.game.map.width];

            if (lightMapInitialised) {
                throw new RuntimeException("Attempting to re-initialise 'staticLightMap'");
            }

            lightMapInitialised = true;
        }


        int for_y_start = div16(MapCamera.getCameraY());
        int for_x_start = div16(MapCamera.getCameraX());

        int for_y_end = clampHTile(for_y_start + div16(MapCamera.getRenderHeight()) + 2);
        int for_x_end = clampWTile(for_x_start + div16(MapCamera.getRenderWidth()) + 2);

        /**
		 * Updating order:
		 *       +-----+   +-----+   +-----+   +-----+
		 *       |1    |   |    1|   |3    |   |    3|
		 *       |  2  | > |  2  | > |  2  | > |  2  |
		 *       |    3|   |3    |   |    1|   |1    |
		 *       +-----+   +-----+   +-----+   +-----+
         * round: 1         2         3         4
         * for all staticLightMap[y][x]
		 */

        // Round 1
        purgePartOfLightmap(for_x_start, for_y_start, for_x_end, for_y_end);

        //System.out.println(for_x_start);
        //System.out.println(for_x_end);

        for (int y = for_y_start; y < for_y_end; y++) {
            for (int x = for_x_start; x < for_x_end; x++) {
                staticLightMap[y][x] = calculate(x, y);
            }
        }

        // Round 2
        for (int y = for_y_start; y < for_y_end; y++) {
            for (int x = for_x_end - 1; x >= for_x_start; x--) {
                staticLightMap[y][x] = calculate(x, y);
            }
        }

        // Round 3
        for (int y = for_y_end - 1; y > for_y_start; y--) {
            for (int x = for_x_end - 1; x >= for_x_start; x--) {
                staticLightMap[y][x] = calculate(x, y);
            }
        }

        // Round 4
        for (int y = for_y_end - 1; y > for_y_start; y--) {
            for (int x = for_x_start; x < for_x_end; x++) {
                staticLightMap[y][x] = calculate(x, y);
            }
        }
    }

    public static void draw(Graphics g) {
        int for_x_start = MapCamera.getRenderStartX();
        int for_y_start = MapCamera.getRenderStartY();
        int for_x_end = MapCamera.getRenderEndX();
        int for_y_end = MapCamera.getRenderEndY();

        // draw
        for (int y = for_y_start; y < for_y_end; y++) {
            for (int x = for_x_start; x < for_x_end; x++) {
                // smooth
                if (Terrarum.game.screenZoom >= 1 && ((boolean) Terrarum.game.gameConfig.get("smoothlighting"))) {
                    int thisLightLevel = staticLightMap[y][x];
                    if (y > 0 && x < for_x_end && thisLightLevel == 0 && staticLightMap[y - 1][x] == 0) {
                        // coalesce zero intensity blocks to one
                        int zeroLevelCounter = 1;
                        while (staticLightMap[y][x + zeroLevelCounter] == 0
                                && staticLightMap[y - 1][x + zeroLevelCounter] == 0) {
                            zeroLevelCounter += 1;

                            if (x + zeroLevelCounter >= for_x_end) break;
                        }

                        g.setColor(new Color(0));
                        g.fillRect(
                                Math.round(x * TSIZE * Terrarum.game.screenZoom)
                                , Math.round(y * TSIZE * Terrarum.game.screenZoom)
                                , FastMath.ceil(TSIZE * Terrarum.game.screenZoom) * zeroLevelCounter
                                , FastMath.ceil(TSIZE * Terrarum.game.screenZoom)
                        );

                        x += (zeroLevelCounter - 1);
                    }
                    else {
                        /**    a
                         *   +-+-+
                         *   |i|j|
                         * b +-+-+ c
                         *   |k|l|
                         *   +-+-+
                         *     d
                         */
                        int a = (y == 0) ? thisLightLevel
                                         : (y == Terrarum.game.map.height - 1) ? thisLightLevel
                                                                      : Math.max(staticLightMap[y][x]
                                                                              , staticLightMap[y - 1][x]);
                        int d = (y == 0) ? thisLightLevel
                                         : (y == Terrarum.game.map.height - 1) ? thisLightLevel
                                                                      : Math.max(staticLightMap[y][x]
                                                                              , staticLightMap[y + 1][x]);
                        int b = (x == 0) ? thisLightLevel
                                         : (x == Terrarum.game.map.width - 1) ? thisLightLevel
                                                                     : Math.max(staticLightMap[y][x]
                                                                             , staticLightMap[y][x - 1]);
                        int c = (x == 0) ? thisLightLevel
                                         : (x == Terrarum.game.map.width - 1) ? thisLightLevel
                                                                     : Math.max(staticLightMap[y][x]
                                                                             , staticLightMap[y][x + 1]);
                        int[] colourMapItoL = new int[4];
                        colourMapItoL[0] = colourLinearMix(a, b);
                        colourMapItoL[1] = colourLinearMix(a, c);
                        colourMapItoL[2] = colourLinearMix(b, d);
                        colourMapItoL[3] = colourLinearMix(c, d);

                        for (int iy = 0; iy < 2; iy++) {
                            for (int ix = 0; ix < 2; ix++) {
                                g.setColor(new Color(colourMapItoL[iy * 2 + ix]));

                                g.fillRect(
                                        Math.round(x * TSIZE * Terrarum.game.screenZoom) + (ix * TSIZE / 2 * Terrarum.game.screenZoom)
                                        , Math.round(y * TSIZE * Terrarum.game.screenZoom) + (iy * TSIZE / 2 * Terrarum.game.screenZoom)
                                        , FastMath.ceil(TSIZE * Terrarum.game.screenZoom / 2)
                                        , FastMath.ceil(TSIZE * Terrarum.game.screenZoom / 2)
                                );
                            }
                        }
                    }
                }
                // Retro
                else {
                    int thisLightLevel = staticLightMap[y][x];

                    // coalesce identical intensity blocks to one
                    int sameLevelCounter = 1;
                    while (staticLightMap[y][x + sameLevelCounter] == thisLightLevel) {
                        sameLevelCounter += 1;

                        if (x + sameLevelCounter >= for_x_end) break;
                    }

                    g.setColor(new Color(staticLightMap[y][x]));
                    g.fillRect(
                            Math.round(x * TSIZE * Terrarum.game.screenZoom)
                            , Math.round(y * TSIZE * Terrarum.game.screenZoom)
                            , FastMath.ceil(TSIZE * Terrarum.game.screenZoom) * sameLevelCounter
                            , FastMath.ceil(TSIZE * Terrarum.game.screenZoom)
                    );

                    x += (sameLevelCounter - 1);
                }
            }
        }
    }

    private static int calculate(int x, int y){
        if (!outOfBounds(x, y)){

            float lightColorR = 1f;
            float lightColorG = 1f;
            float lightColorB = 1f;
            int lightColorInt;

            int thisTerrain = Terrarum.game.map.getTileFromTerrain(x, y);
            int thisWall = Terrarum.game.map.getTileFromWall(x, y);

            // open air
            if (thisTerrain == AIR && thisWall == AIR) {
                lightColorInt = Terrarum.game.map.getGlobalLight();
            }
            else {
                // mix light emitter
                if (TilePropCodex.getProp(thisTerrain).getLuminosity() != 0) {
                    int lum = TilePropCodex.getProp(thisTerrain).getLuminosity();
                    lightColorR = getR(lum);
                    lightColorG = getG(lum);
                    lightColorB = getB(lum);
                }

                // mix lantern
                for (LightmapLantern lantern : lanterns) {
                    if (lantern.getX() == x && lantern.getY() == y) {
                        int lum = lantern.getIntensity();
                        lightColorR = getR(lum);
                        lightColorG = getG(lum);
                        lightColorB = getB(lum);
                        break;
                    }
                }

                float[] bgrVal = new float[3]; // {B, G, R}

                // test for each R, G, B channel
                for (int i = 0; i < 3; i++) {
                    int brightest = 0;

                    //get brightest of nearby 4 tiles
                    int nearby = 0;
                    findNearbyBrightest:
                    for (int yoff = -1; yoff <= 1; yoff++) {
                        for (int xoff = -1; xoff <= 1; xoff++) {
                            /**
                             * filter for 'v's as:
                             * +-+-+-+
                             * | |v| |
                             * +-+-+-+
                             * |v| |v|
                             * +-+-+-+
                             * | |v| |
                             * +-+-+-+
                             */
                            if (xoff != yoff && -xoff != yoff) {
                                if (!outOfMapBounds(x + xoff, y + yoff)) {
                                    nearby = getRaw(staticLightMap[y + yoff][x + xoff], i);
                                }

                                if (nearby > brightest) {
                                    brightest = nearby;
                                }

                                if (brightest == 0xFF) break findNearbyBrightest;
                            }
                        }
                    }

                    //return: brightest - opacity
                    bgrVal[i] = darkenFloat(
                            brightest
                            , TilePropCodex.getProp(thisTerrain).getOpacity()
                    );
                }

                // construct lightColor from bgrVal
                lightColorInt = constructRGBFromFloat(
                        bgrVal[OFFSET_R] * lightColorR
                        , bgrVal[OFFSET_G] * lightColorG
                        , bgrVal[OFFSET_B] * lightColorB
                );

            }

            return lightColorInt;
        }
        else {
            throw new IllegalArgumentException("Out of bounds of lightMap");
        }
    }

    /**
     *
     * @param data Raw channel value [0-255]
     * @param darken [0-255]
     * @return darkened data [0-1]
     */
    private static float darkenFloat(int data, int darken) {
        return (darken(data, darken) / 255f);
    }

    /**
     *
     * @param data Raw channel value [0-255]
     * @param darken [0-255]
     * @return darkened data [0-255]
     */
    private static int darken(int data, int darken) {
        if (darken < 0 || darken > 0xFF) { throw new IllegalArgumentException("darken: out of range"); }

        return clampZero(data - darken);
    }

    private static int getRawR(int RGB) {
        return (RGB >> 16) & 0xFF;
    }

    private static int getRawG(int RGB) {
        return (RGB >> 8) & 0xFF;
    }

    private static int getRawB(int RGB) {
        return RGB & 0xFF;
    }

    /**
     *
     * @param RGB
     * @param offset 2 = R, 1 = G, 0 = B
     * @return
     */
    private static int getRaw(int RGB, int offset) {
        if (offset < 0 || offset > 2) throw new IllegalArgumentException("Offset out of range");
        return (RGB >> (8 * offset)) & 0xFF;
    }

    private static float getR(int rgb) {
        return getRawR(rgb) / 255f;
    }

    private static float getG(int rgb) {
        return getRawG(rgb) / 255f;
    }

    private static float getB(int rgb) {
        return getRawB(rgb) / 255f;
    }

    private static int constructRGBFromFloat(int r, int g, int b) {
        if (r < 0 || r > 0xFF) { throw new IllegalArgumentException("Red: out of range"); }
        if (g < 0 || g > 0xFF) { throw new IllegalArgumentException("Green: out of range"); }
        if (b < 0 || b > 0xFF) { throw new IllegalArgumentException("Blue: out of range"); }
        return (r << 16) | (g << 8) | b;
    }

    private static int constructRGBFromFloat(float r, float g, float b) {
        if (r < 0 || r > 1.0f) { throw new IllegalArgumentException("Red: out of range"); }
        if (g < 0 || g > 1.0f) { throw new IllegalArgumentException("Green: out of range"); }
        if (b < 0 || b > 1.0f) { throw new IllegalArgumentException("Blue: out of range"); }

        int intR = Math.round(r * 0xFF);
        int intG = Math.round(g * 0xFF);
        int intB = Math.round(b * 0xFF);

        return constructRGBFromFloat(intR, intG, intB);
    }

    private static int colourLinearMix(int colA, int colB) {
        int r = (getRawR(colA) + getRawR(colB)) >> 1;
        int g = (getRawG(colA) + getRawG(colB)) >> 1;
        int b = (getRawB(colA) + getRawB(colB)) >> 1;
        return constructRGBFromFloat(r, g, b);
    }

    /**
     *
     * @param thisTile
     * @param side1
     * @param side2
     * @param corner
     * @return
     */
    private static int colourQuadraticMix(int thisTile, int side1, int side2, int corner) {
        int rSide = max(getRawR(side1), getRawR(side2), getRawR(corner) / 2);
        int r = arithmeticAverage(rSide, getRawR(thisTile));
        int gSide = max(getRawG(side1), getRawG(side2), getRawG(corner) / 2);
        int g = arithmeticAverage(gSide, getRawG(thisTile));
        int bSide = max(getRawG(side1), getRawG(side2), getRawG(corner) / 2);
        int b = arithmeticAverage(bSide, getRawG(thisTile));

        return constructRGBFromFloat(r, g, b);
    }

    private static int quantise16(int x) {
        if (x < 0) throw new IllegalArgumentException("positive integer only.");
        return (x & 0xFFFF_FFF0);
    }

    private static int div16(int x) {
        if (x < 0) throw new IllegalArgumentException("positive integer only.");
        return (x & 0x7FFF_FFFF) >> 4;
    }

    private static int mul16(int x) {
        if (x < 0) throw new IllegalArgumentException("positive integer only.");
        return (x << 4);
    }

    private static int max(int... i) {
        Arrays.sort(i);
        return i[i.length - 1];
    }

    private static int min(int... i) {
        Arrays.sort(i);
        return i[0];
    }

    private static boolean outOfBounds(int x, int y){
        return ( x < 0 || y < 0 || x >= Terrarum.game.map.width || y >= Terrarum.game.map.height);
    }

    private static boolean outOfMapBounds(int x, int y){
        return ( x < 0 || y < 0 || x >= staticLightMap[0].length || y >= staticLightMap.length);
    }

    private static int clampZero(int i) {
        if (i < 0) return 0;
        else return i;
    }

    private static float clampZero(float i) {
        if (i < 0) return 0;
        else return i;
    }

    public static int[][] getStaticLightMap() {
        return staticLightMap;
    }

    public static int getValueFromMap(int x, int y) {
        return staticLightMap[y][x];
    }

    private static void purgePartOfLightmap(int x1, int y1, int x2, int y2) {
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                if (!outOfMapBounds(x, y)) {
                    staticLightMap[y][x] = 0;
                }
            }
        }
    }

    private static int clampWTile(int x) {
        if (x < 0) {
            return 0;
        }
        else if (x > Terrarum.game.map.width) {
            return Terrarum.game.map.width;
        }
        else {
            return x;
        }
    }

    private static int clampHTile(int x) {
        if (x < 0) {
            return 0;
        }
        else if (x > Terrarum.game.map.height) {
            return Terrarum.game.map.height;
        }
        else {
            return x;
        }
    }

    private static int arithmeticAverage(int... i) {
        int sum = 0;
        for (int k = 0; k < i.length; k++) {
            sum += i[k];
        }
        return Math.round(sum / (float) i.length);
    }
}

class LightmapLantern {
    int x, y;
    int intensity;

    public LightmapLantern(int x, int y, int intensity) {
        this.x = x;
        this.y = y;
        this.intensity = intensity;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getIntensity() {
        return intensity;
    }
}
