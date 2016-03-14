package com.Torvald.Terrarum.MapDrawer;

import com.Torvald.ColourUtil.Col40;
import com.Torvald.Terrarum.Actors.Actor;
import com.Torvald.Terrarum.Actors.ActorWithBody;
import com.Torvald.Terrarum.Actors.Glowing;
import com.Torvald.Terrarum.Actors.Luminous;
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
    private static volatile char[][] staticLightMap;
    private static boolean lightMapInitialised = false;

    /**
     * For entities that emits light (e.g. Player with shine potion)
     */
    private static ArrayList<LightmapLantern> lanterns = new ArrayList<>();

    private static final int AIR = 0;
    private static final int SUNSTONE = 41; // TODO add sunstone: emits same light as Map.GL. Goes dark at night


    private static final int OFFSET_R = 2;
    private static final int OFFSET_G = 1;
    private static final int OFFSET_B = 0;

    private static final int TSIZE = MapDrawer.TILE_SIZE;

    // color model related vars
    public static final int MUL = Col40.MUL;
    public static final int MUL_2 = Col40.MUL_2;
    public static final int CHANNEL_MAX = Col40.MAX_STEP;
    public static final float CHANNEL_MAX_FLOAT = (float) CHANNEL_MAX;
    public static final int COLOUR_DOMAIN_SIZE = Col40.COLOUR_DOMAIN_SIZE;

    public LightmapRenderer() {

    }

    @Deprecated
    public static void addLantern(int x, int y, char intensity) {
        LightmapLantern thisLantern = new LightmapLantern(x, y, intensity);

        for (int i = lanterns.size() - 1; i >= 0; i--) {
            LightmapLantern lanternInList = lanterns.get(i);
            // found duplicates
            if (lanternInList.getX() == x && lanternInList.getY() == y) {
                // add colour
                char addedL = addRaw(intensity, lanternInList.getIntensity());
                lanternInList.intensity = addedL;
                return;
            }
        }
        //else
        lanterns.add(thisLantern);
    }

    @Deprecated
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
            staticLightMap = new char[Terrarum.game.map.height][Terrarum.game.map.width];

            if (lightMapInitialised) {
                throw new RuntimeException("Attempting to re-initialise 'staticLightMap'");
            }

            lightMapInitialised = true;
        }


        int for_y_start = div16(MapCamera.getCameraY()) - 1; // fix for premature lightmap rendering
        int for_x_start = div16(MapCamera.getCameraX()) - 1; // on topmost/leftmost side

        int for_y_end = clampHTile(for_y_start + div16(MapCamera.getRenderHeight()) + 2) + 1; // same fix as above
        int for_x_end = clampWTile(for_x_start + div16(MapCamera.getRenderWidth()) + 2)  + 1;

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

        purgePartOfLightmap(for_x_start, for_y_start, for_x_end, for_y_end);
        // if wider purge were not applied, GL changing (sunset, sunrise) will behave incorrectly
        // ("leakage" of non-updated sunlight)

        try {
            // Round 1
            for (int y = for_y_start; y < for_y_end; y++) {
                for (int x = for_x_start; x < for_x_end; x++) {
                    staticLightMap[y][x] = calculate(x, y);
                }
            }

            // Round 4
            for (int y = for_y_end - 1; y > for_y_start; y--) {
                for (int x = for_x_start; x < for_x_end; x++) {
                    staticLightMap[y][x] = calculate(x, y);
                }
            }

            // Round 3
            for (int y = for_y_end - 1; y > for_y_start; y--) {
                for (int x = for_x_end - 1; x >= for_x_start; x--) {
                    staticLightMap[y][x] = calculate(x, y);
                }
            }

            // Round 2
            for (int y = for_y_start; y < for_y_end; y++) {
                for (int x = for_x_end - 1; x >= for_x_start; x--) {
                    staticLightMap[y][x] = calculate(x, y);
                }
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {}
    }

    public static void draw(Graphics g) {
        int for_x_start = MapCamera.getRenderStartX() - 1;
        int for_y_start = MapCamera.getRenderStartY() - 1;
        int for_x_end = MapCamera.getRenderEndX();
        int for_y_end = MapCamera.getRenderEndY();

        // draw
        try {
            for (int y = for_y_start; y < for_y_end; y++) {
                for (int x = for_x_start; x < for_x_end; x++) {
                    // smooth
                    if (Terrarum.game.screenZoom >= 1
                            && Terrarum.gameConfig.getAsBoolean("smoothlighting")) {
                        char thisLightLevel = staticLightMap[y][x];
                        if (y > 0 && x < for_x_end && thisLightLevel == 0 && staticLightMap[y - 1][x] == 0) {
                            try {
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
                                        , FastMath.ceil(
                                                TSIZE * Terrarum.game.screenZoom) * zeroLevelCounter
                                        , FastMath.ceil(TSIZE * Terrarum.game.screenZoom)
                                );

                                x += (zeroLevelCounter - 1);
                            }
                            catch (ArrayIndexOutOfBoundsException e) {
                                // do nothing
                            }
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
                            char a = (y == 0) ? thisLightLevel
                                              : (y == Terrarum.game.map.height - 1) ? thisLightLevel
                                                                                    : maximiseRGB(
                                                                                            staticLightMap[y][x]
                                                                                            ,
                                                                                            staticLightMap[y - 1][x]);
                            char d = (y == 0) ? thisLightLevel
                                              : (y == Terrarum.game.map.height - 1) ? thisLightLevel
                                                                                    : maximiseRGB(
                                                                                            staticLightMap[y][x]
                                                                                            ,
                                                                                            staticLightMap[y + 1][x]);
                            char b = (x == 0) ? thisLightLevel
                                              : (x == Terrarum.game.map.width - 1) ? thisLightLevel
                                                                                   : maximiseRGB(
                                                                                           staticLightMap[y][x]
                                                                                           ,
                                                                                           staticLightMap[y][x - 1]);
                            char c = (x == 0) ? thisLightLevel
                                              : (x == Terrarum.game.map.width - 1) ? thisLightLevel
                                                                                   : maximiseRGB(
                                                                                           staticLightMap[y][x]
                                                                                           ,
                                                                                           staticLightMap[y][x + 1]);
                            char[] colourMapItoL = new char[4];
                            colourMapItoL[0] = colourLinearMix(a, b);
                            colourMapItoL[1] = colourLinearMix(a, c);
                            colourMapItoL[2] = colourLinearMix(b, d);
                            colourMapItoL[3] = colourLinearMix(c, d);

                            for (int iy = 0; iy < 2; iy++) {
                                for (int ix = 0; ix < 2; ix++) {
                                    g.setColor(toTargetColour(colourMapItoL[iy * 2 + ix]));

                                    g.fillRect(
                                            Math.round(
                                                    x * TSIZE * Terrarum.game.screenZoom) + (ix * TSIZE / 2 * Terrarum.game.screenZoom)
                                            , Math.round(
                                                    y * TSIZE * Terrarum.game.screenZoom) + (iy * TSIZE / 2 * Terrarum.game.screenZoom)
                                            , FastMath.ceil(TSIZE * Terrarum.game.screenZoom / 2)
                                            , FastMath.ceil(TSIZE * Terrarum.game.screenZoom / 2)
                                    );
                                }
                            }
                        }
                    }
                    // Retro
                    else {
                        try {
                            int thisLightLevel = staticLightMap[y][x];

                            // coalesce identical intensity blocks to one
                            int sameLevelCounter = 1;
                            while (staticLightMap[y][x + sameLevelCounter] == thisLightLevel) {
                                sameLevelCounter += 1;

                                if (x + sameLevelCounter >= for_x_end) break;
                            }

                            g.setColor(toTargetColour(staticLightMap[y][x]));
                            g.fillRect(
                                    Math.round(x * TSIZE * Terrarum.game.screenZoom)
                                    , Math.round(y * TSIZE * Terrarum.game.screenZoom)
                                    , FastMath.ceil(
                                            TSIZE * Terrarum.game.screenZoom) * sameLevelCounter
                                    , FastMath.ceil(TSIZE * Terrarum.game.screenZoom)
                            );

                            x += (sameLevelCounter - 1);
                        }
                        catch (ArrayIndexOutOfBoundsException e) {
                            // do nothing
                        }
                    }
                }
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {}
    }

    private static char calculate(int x, int y) {
        return calculate(x, y, false);
    }

    private static char calculate(int x, int y, boolean doNotCalculateAmbient){
        char lightLevelThis = 0;
        int thisTerrain = Terrarum.game.map.getTileFromTerrain(x, y);
        int thisWall = Terrarum.game.map.getTileFromWall(x, y);
        char thisTileLuminosity = TilePropCodex.getProp(thisTerrain).getLuminosity();
        char thisTileOpacity = TilePropCodex.getProp(thisTerrain).getOpacity();
        char sunLight = Terrarum.game.map.getGlobalLight();

        // MIX TILE
        // open air
        if (thisTerrain == AIR && thisWall == AIR) {
            lightLevelThis = sunLight;
        }
        // luminous tile transparent (allows sunlight to pass)
        else if (thisWall == AIR && thisTileLuminosity > 0) {
            char darkenSunlight = darkenColoured(sunLight, thisTileOpacity);
            lightLevelThis = screenBlend(darkenSunlight, thisTileLuminosity);
        }
        // luminous tile (opaque)
        else if (thisWall != AIR && thisTileLuminosity > 0) {
            lightLevelThis = thisTileLuminosity;
        }
        // END MIX TILE

        // mix lantern
        for (LightmapLantern lantern : lanterns) {
            if (lantern.getX() == x && lantern.getY() == y) {
                lightLevelThis = screenBlend(lightLevelThis, lantern.getIntensity());
                break;
            }
        }

        // mix luminous actor
        for (Actor actor : Terrarum.game.actorContainer) {
            if (actor instanceof Luminous && actor instanceof ActorWithBody) {
                Luminous actorLum = (Luminous) actor;
                ActorWithBody actorBody = (ActorWithBody) actor;
                int tileX = Math.round(actorBody.getHitbox().getPointedX() / TSIZE);
                int tileY = Math.round(actorBody.getHitbox().getPointedY() / TSIZE)
                        - 1;
                char actorLuminosity = actorLum.getLuminosity();
                if (x == tileX && y == tileY) {
                    lightLevelThis = screenBlend(lightLevelThis, actorLuminosity);
                }
            }
        }


        if (!doNotCalculateAmbient) {
            // calculate ambient
            char ambient = 0;
            char nearby = 0;
            findNearbyBrightest:
            for (int yoff = -1; yoff <= 1; yoff++) {
                for (int xoff = -1; xoff <= 1; xoff++) {
                    /**
                     * filter for 'v's as:
                     * +-+-+-+
                     * |a|v|a|
                     * +-+-+-+
                     * |v| |v|
                     * +-+-+-+
                     * |a|v|a|
                     * +-+-+-+
                     */
                    if (xoff != yoff && -xoff != yoff) { // 'v' tiles
                        if (!outOfMapBounds(x + xoff, y + yoff)) {
                            nearby = staticLightMap[y + yoff][x + xoff];
                        }
                    }
                    else if (xoff != 0 && yoff != 0) { // 'a' tiles
                        if (!outOfMapBounds(x + xoff, y + yoff)) {
                            nearby = darkenUniformInt(staticLightMap[y + yoff][x + xoff]
                                    , 2); //2
                            // mix some to have more 'spreading'
                            // so that light spreads in a shape of an octagon instead of a diamond
                        }
                    }
                    else {
                        nearby = 0; // exclude 'me' tile
                    }

                    ambient = maximiseRGB(ambient, nearby); // keep base value as brightest nearby
                }
            }

            ambient = darkenColoured(ambient,
                    thisTileOpacity
            ); // get real ambient by appling opacity value

            // mix and return lightlevel and ambient
            return maximiseRGB(lightLevelThis, ambient);
        }
        else {
            return lightLevelThis;
        }
    }

    /**
     * Subtract each channel's RGB value.
     * It works like:
     *     f(data, darken) = RGB(data.r - darken.r, data.g - darken.g, data.b - darken.b)
     *
     * @param data Raw channel value [0-39] per channel
     * @param darken [0-39] per channel
     * @return darkened data [0-39] per channel
     */
    private static char darkenColoured(char data, char darken) {
        if (darken < 0 || darken >= COLOUR_DOMAIN_SIZE) { throw new IllegalArgumentException("darken: out of " +
                "range"); }

        float r = clampZero(getR(data) - getR(darken));
        float g = clampZero(getG(data) - getG(darken));
        float b = clampZero(getB(data) - getB(darken));

        return constructRGBFromFloat(r, g, b);
    }

    /**
     * Darken each channel by 'darken' argument
     * It works like:
     *     f(data, darken) = RGB(data.r - darken, data.g - darken, data.b - darken)
     * @param data [0-39] per channel
     * @param darken [0-1]
     * @return
     */
    private static char darkenUniformFloat(char data, float darken) {
        if (darken < 0 || darken > 1f) { throw new IllegalArgumentException("darken: out of " +
                "range"); }

        float r = clampZero(getR(data) - darken);
        float g = clampZero(getG(data) - darken);
        float b = clampZero(getB(data) - darken);

        return constructRGBFromFloat(r, g, b);
    }

    /**
     * Darken each channel by 'darken' argument
     * It works like:
     *     f(data, darken) = RGB(data.r - darken, data.g - darken, data.b - darken)
     * @param data [0-39] per channel
     * @param darken [0-39]
     * @return
     */
    private static char darkenUniformInt(char data, int darken) {
        if (darken < 0 || darken > CHANNEL_MAX) { throw new IllegalArgumentException("darken: out of " +
                "range"); }

        int r = clampZero(getRawR(data) - darken);
        int g = clampZero(getRawG(data) - darken);
        int b = clampZero(getRawB(data) - darken);

        return constructRGBFromInt(r, g, b);
    }


    /**
     * Add each channel's RGB value.
     * It works like:
     *     f(data, brighten) = RGB(data.r + darken.r, data.g + darken.g, data.b + darken.b)
     * @param data Raw channel value [0-39] per channel
     * @param brighten [0-39] per channel
     * @return brightened data [0-39] per channel
     */
    private static char brightenColoured(char data, char brighten) {
        if (brighten < 0 || brighten >= COLOUR_DOMAIN_SIZE) { throw new IllegalArgumentException("brighten: out of " +
                "range"); }

       float r = clampFloat(getR(data) + getR(brighten));
       float g = clampFloat(getG(data) + getG(brighten));
       float b = clampFloat(getB(data) + getB(brighten));

        return constructRGBFromFloat(r, g, b);
    }

    /** Get each channel from two RGB values, return new RGB that has max value of each channel
     * @param rgb
     * @param rgb2
     * @return
     */
    private static char maximiseRGB(char rgb, char rgb2) {
        int r1 = getRawR(rgb); int r2 = getRawR(rgb2); int newR = (r1 > r2) ? r1 : r2;
        int g1 = getRawG(rgb); int g2 = getRawG(rgb2); int newG = (g1 > g2) ? g1 : g2;
        int b1 = getRawB(rgb); int b2 = getRawB(rgb2); int newB = (b1 > b2) ? b1 : b2;

        return constructRGBFromInt(newR, newG, newB);
    }

    private static char screenBlend(char rgb, char rgb2) {
        float r1 = getR(rgb); float r2 = getR(rgb2); float newR = 1 - (1 - r1) * (1 - r2);
        float g1 = getG(rgb); float g2 = getG(rgb2); float newG = 1 - (1 - g1) * (1 - g2);
        float b1 = getB(rgb); float b2 = getB(rgb2); float newB = 1 - (1 - b1) * (1 - b2);

        return constructRGBFromFloat(newR, newG, newB);
    }

    public static int getRawR(char RGB) {
        return RGB / MUL_2;
    }

    public static int getRawG(char RGB) {
        return (RGB % MUL_2) / MUL;
    }

    public static int getRawB(char RGB) {
        return RGB % MUL;
    }

    /**
     *
     * @param RGB
     * @param offset 2 = R, 1 = G, 0 = B
     * @return
     */
    public static int getRaw(char RGB, int offset) {
        if (offset == OFFSET_R) return getRawR(RGB);
        if (offset == OFFSET_G) return getRawG(RGB);
        if (offset == OFFSET_B) return getRawB(RGB);
        else throw new IllegalArgumentException("Channel offset out of range");
    }

    private static float getR(char rgb) {
        return getRawR(rgb) / CHANNEL_MAX_FLOAT;
    }

    private static float getG(char rgb) {
        return getRawG(rgb) / CHANNEL_MAX_FLOAT;
    }

    private static float getB(char rgb) {
        return getRawB(rgb) / CHANNEL_MAX_FLOAT;
    }

    private static char addRaw(char rgb1, char rgb2) {
        int newR = clampByte(getRawR(rgb1) + getRawB(rgb2));
        int newG = clampByte(getRawG(rgb1) + getRawG(rgb2));
        int newB = clampByte(getRawB(rgb1) + getRawB(rgb2));

        return constructRGBFromInt(newR, newG, newB);
    }

    public static char constructRGBFromInt(int r, int g, int b) {
        if (r < 0 || r > CHANNEL_MAX) { throw new IllegalArgumentException("Red: out of range"); }
        if (g < 0 || g > CHANNEL_MAX) { throw new IllegalArgumentException("Green: out of range"); }
        if (b < 0 || b > CHANNEL_MAX) { throw new IllegalArgumentException("Blue: out of range"); }
        return (char) (r * MUL_2 + g * MUL + b);
    }

    private static char constructRGBFromFloat(float r, float g, float b) {
        if (r < 0 || r > 1.0f) { throw new IllegalArgumentException("Red: out of range"); }
        if (g < 0 || g > 1.0f) { throw new IllegalArgumentException("Green: out of range"); }
        if (b < 0 || b > 1.0f) { throw new IllegalArgumentException("Blue: out of range"); }

        int intR = Math.round(r * CHANNEL_MAX);
        int intG = Math.round(g * CHANNEL_MAX);
        int intB = Math.round(b * CHANNEL_MAX);

        return constructRGBFromInt(intR, intG, intB);
    }

    private static char colourLinearMix(char colA, char colB) {
        int r = (getRawR(colA) + getRawR(colB)) >> 1;
        int g = (getRawG(colA) + getRawG(colB)) >> 1;
        int b = (getRawB(colA) + getRawB(colB)) >> 1;
        return constructRGBFromInt(r, g, b);
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
        return (i < 0) ? 0 : i;
    }

    private static float clampZero(float i) {
        return (i < 0) ? 0 : i;
    }

    private static int clampByte(int i) {
        return (i < 0) ? 0 : (i > CHANNEL_MAX) ? CHANNEL_MAX : i;
    }

    private static float clampFloat(float i) {
        return (i < 0) ? 0 : (i > 1) ? 1 : i;
    }

    public static char getValueFromMap(int x, int y) {
        return staticLightMap[y][x];
    }

    private static void purgePartOfLightmap(int x1, int y1, int x2, int y2) {
        try {
            for (int y = y1 - 1; y < y2 + 1; y++) {
                for (int x = x1 - 1; x < x2 + 1; x++) {
                    if (y == y1 - 1 || y == y2 || x == x1 - 1 || x == x2) {
                        // fill the rim with (pre) calculation
                        staticLightMap[y][x] = preCalculateUpdateGLOnly(x, y);
                    }
                    else {
                        staticLightMap[y][x] = 0;
                    }
                }
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {}
    }

    private static char preCalculateUpdateGLOnly(int x, int y) {
        int thisWall = Terrarum.game.map.getTileFromWall(x, y);
        int thisTerrain = Terrarum.game.map.getTileFromTerrain(x, y);
        char thisTileLuminosity = TilePropCodex.getProp(thisTerrain).getLuminosity();
        char thisTileOpacity = TilePropCodex.getProp(thisTerrain).getOpacity();
        char sunLight = Terrarum.game.map.getGlobalLight();

        char lightLevelThis;

        // MIX TILE
        // open air
        if (thisTerrain == AIR && thisWall == AIR) {
            lightLevelThis = sunLight;
        }
        // luminous tile transparent (allows sunlight to pass)
        else if (thisWall == AIR && thisTileLuminosity > 0) {
            char darkenSunlight = darkenColoured(sunLight, thisTileOpacity);
            lightLevelThis = screenBlend(darkenSunlight, thisTileLuminosity);
        }
        else {
            lightLevelThis = getValueFromMap(x, y);
        }
        return lightLevelThis;
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

    private static Color toTargetColour(char raw) {
        return new Col40().createSlickColor(raw);
    }
}

class LightmapLantern {
    int x, y;
    char intensity;

    public LightmapLantern(int x, int y, char intensity) {
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

    public char getIntensity() {
        return intensity;
    }
}
