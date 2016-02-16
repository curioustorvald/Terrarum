package com.Torvald.Terrarum.MapGenerator;

import com.Torvald.Rand.HQRNG;
import com.Torvald.Terrarum.GameMap.GameMap;
import com.jme3.math.FastMath;
import com.sun.istack.internal.NotNull;

public class MapGenerator {

    @NotNull private static GameMap map;
    private static HQRNG random;
    //private static float[] noiseArray;
    @NotNull private static long seed;
    @NotNull private static int width;
    @NotNull private static int height;

    private static int[] heightMap;

    private static int dirtThickness;
    private static int TERRAIN_AVERAGE_HEIGHT;
    private static int minimumFloatingIsleHeight;

    private static final float noiseGradientStart = 0.67f;
    private static final float noiseGradientEnd = 0.56f;
    private static final float noiseGrdCaveEnd = 0.54f;

    private static final int HILL_WIDTH = 256; // power of two!
    private static final int MAX_HILL_HEIGHT = 100;

    private static int OCEAN_WIDTH = 400;
    private static int SHORE_WIDTH = 120;
    private static int MAX_OCEAN_DEPTH = 200;

    private static int GLACIER_MOUNTAIN_WIDTH = 900;
    private static final int GLACIER_MOUNTAIN_HEIGHT = 300;

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

    private static final byte DIAMOND = 25;
    private static final byte RUBY = 21;
    private static final byte EMERALD = 22;
    private static final byte SAPPHIRE = 23;
    private static final byte TOPAZ = 24;
    private static final byte AMETHYST = 26;

    private static final byte SNOW = 27;
    private static final byte ICE_FRAGILE = 28;
    private static final byte ICE_NATURAL = 29;
    private static final byte ICE_MAGICAL = 30;

    private static final byte WATER = (byte) 239;
    private static final byte LAVA = (byte) 255;

    @NotNull private static int worldOceanPosition;
    private static final int TYPE_OCEAN_LEFT = 0;
    private static final int TYPE_OCEAN_RIGHT = 1;

    private static final int GRASSCUR_UP = 0;
    private static final int GRASSCUR_RIGHT = 1;
    private static final int GRASSCUR_DOWN = 2;
    private static final int GRASSCUR_LEFT = 3;

    public static void attachMap(GameMap map) {
        MapGenerator.map = map;
        width = map.width;
        height = map.height;

        float widthMulFactor = (width / 8192f);

        dirtThickness = (int) (100 * height / 1024f);
        minimumFloatingIsleHeight = (int) (25 * (height / 1024f));
        TERRAIN_AVERAGE_HEIGHT = height / 4;

        OCEAN_WIDTH = Math.round(OCEAN_WIDTH * widthMulFactor);
        SHORE_WIDTH = Math.round(SHORE_WIDTH * widthMulFactor);
        GLACIER_MOUNTAIN_WIDTH = Math.round(GLACIER_MOUNTAIN_WIDTH * widthMulFactor);
    }

    public static void setSeed(long seed) {
        MapGenerator.seed = seed;
    }

    /**
     * Generate terrain and override attached map
     */
    public static void generateMap() {
        random = new HQRNG(seed);
        System.out.println("[MapGenerator] Seed: " + seed);

        worldOceanPosition = random.nextBoolean() ? TYPE_OCEAN_LEFT : TYPE_OCEAN_RIGHT;

        heightMap = raise2(MAX_HILL_HEIGHT / 2);
        generateOcean(heightMap);
        placeGlacierMount(heightMap);
        heightMapToObjectMap(heightMap);

        carveByMap(
                generate2DSimplexNoiseWorldSize(2.5f, 1.666f)
                , 1
                , AIR
                , "Carving out cave..."
        );

        /*fillByMapInverseGradFilter(
                generate2DSimplexNoiseWorldSize(2.5f, 2.5f)
                , 1.02f
                , DIRT
                , STONE
                , "Planting stones on dirt layers..."
        );
        fillByMapInverseGradFilter(
                generate2DSimplexNoiseWorldSize(2.5f, 2.5f)
                , 0.98f
                , STONE
                , DIRT
                , "Planting dirts..."
        );
        fillByMapInverseGradFilter(
                generate2DSimplexNoiseWorldSize(2.5f, 2.5f)
                , 0.92f
                , STONE
                , GRAVEL
                , "Planting gravels..."
        );*/

        /**
         * Plant ores
         */
        /*fillByMap(
                generate2DSimplexNoiseWorldSize(5, 5)
                , 0.78f
                , STONE
                , DIAMOND
                , "Planting diamonds..."
        );

        byte[] berylsArray = {RUBY, EMERALD, SAPPHIRE, TOPAZ, AMETHYST};
        fillByMap(
                generate2DSimplexNoiseWorldSize(5, 5)
                , 0.8f
                , STONE
                , berylsArray
                , "Planting beryls..."
        );

        fillByMap(
                generate2DSimplexNoiseWorldSize(5, 5)
                , 0.80f
                , STONE
                , GOLD
                , "Planting golds..."
        );
        fillByMap(
                generate2DSimplexNoiseWorldSize(5, 5)
                , 0.866f
                , STONE
                , IRON
                , "Planting irons..."
        );
        fillByMap(
                generate2DSimplexNoiseWorldSize(5, 5)
                , 0.88f
                , STONE
                , COPPER
                , "Planting coppers..."
        );*/

        /** TODO Cobaltite, Ilmenite, Aurichalcum (and possibly pitchblende?) **/

        /*fillByMap(
                generate2DSimplexNoiseWorldSize(5, 5)
                , 1.21f
                , STONE
                , COAL
                , "Planting coals..."
        );*/

        floodBottomLava();
        freeze();
        fillOcean();
        plantGrass();

        //post-process
        generateFloatingIslands();

        //wire layer
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                map.getWireArray()[i][j] = 0;
            }
        }

        // Free some memories
        System.gc();
    }

	/* 1. Raise */

    private static float[][] generate2DSimplexNoiseWorldSize(float xDensity, float yDensity) {
        return generate2DSimplexNoise(width, height, xDensity, yDensity);
    }

    /**
     * Generate 2D array of simplex noise.
     * @param sizeX
     * @param sizeY
     * @param xDensity higher == dense (smaller blob), lower == sparse (larger blob)
     * @param yDensity higher == dense (smaller blob), lower == sparse (larger blob)
     * @return matrix in ![x][y]!
     */
    private static float[][] generate2DSimplexNoise(int sizeX, int sizeY, float xDensity, float yDensity){
        SimplexNoise simplexNoise = new SimplexNoise(HILL_WIDTH, 0.1f, seed ^ random.nextLong());

        float xStart=0;
        float yStart=0;

        /** higher = denser.
         * Recommended: (width or height) * 3
         */
        float xEnd=height * xDensity;
        float yEnd=width * yDensity;

        float[][] result=new float[sizeY][sizeX];

        for(int i=0;i<sizeY;i++){
            for(int j=0;j<sizeX;j++){
                int x=(int)(xStart+i*((xEnd-xStart)/sizeX));
                int y=(int)(yStart+j*((yEnd-yStart)/sizeY));
                result[i][j]=(float) (0.5*(1+simplexNoise.getNoise(x,y)));
            }
        }

        return result;
    }

    private static float[] generateWhiteNoiseArray(long seed) {
        float[] noiseArray = new float[MapGenerator.width + 1];
        for (int i = 0; i < noiseArray.length; i++) {
            noiseArray[i] = random.nextFloat();
        }

        return noiseArray;
    }

    private static int[] generateOcean(int[] noiseArrayLocal) {
        int oceanLeftP1 = noiseArrayLocal[OCEAN_WIDTH];
        int oceanRightP1 = noiseArrayLocal[noiseArrayLocal.length - OCEAN_WIDTH];

        /**
         * Add ocean so that:
         *
         *     +1|       -   -
         *      0|      -  --  ...
         *     -1|______  -
         *
         *     interpolated to
         *
         *     +1|        -   -
         *      0|   _---  --  ...
         *     -1|__-      -
         *
         *               ↑-- Rough, white noise
         *
         *  -1 means -MAX_HILL_HEIGHT
         */
        for (int i = 0; i < OCEAN_WIDTH; i++) {
            if (worldOceanPosition == TYPE_OCEAN_LEFT) {
                noiseArrayLocal[i] = Math.round(
                        interpolateCosine(
                                (float) (i) / OCEAN_WIDTH
                                , -MAX_OCEAN_DEPTH, oceanLeftP1
                        )
                );
            }
            else if (worldOceanPosition == TYPE_OCEAN_RIGHT) {
                noiseArrayLocal[noiseArrayLocal.length - OCEAN_WIDTH + i] = Math.round(
                        interpolateCosine(
                                (float) (i) / OCEAN_WIDTH
                                , oceanRightP1, -MAX_OCEAN_DEPTH
                        )
                );
            }
            else {
                throw new RuntimeException("Ocean position were not set correctly.");
            }
        }

        return noiseArrayLocal;
    }

    /**
     * <a href>http://freespace.virgin.net/hugo.elias/models/m_perlin.htm</a>
     * @param maxval_div_2 max height (deviation from zero) divided by two.
     * @return noise array with range of [-maxval, maxval]
     */
    private static int[] raise2(int maxval_div_2) {

        int finalPerlinAmp = maxval_div_2; // 1 + 1/2 + 1/4 + 1/8 + ... == 2
        int perlinOctaves = FastMath.intLog2(maxval_div_2) +1 -1; // max: for every 2nd node

        int[] perlinMap = new int[width]; // [-2 * finalPerlinAmp, finalPerlinAmp]

        // assert
        if ((HILL_WIDTH) >>> (perlinOctaves - 1) == 0) {
            throw new RuntimeException("sample width of zero detected.");
        }

        // sample noise and add
        for (int oct = 1; oct <= perlinOctaves; oct++) {
            // perlinAmp: 16364 -> 8192 -> 4096 -> 2048 -> ...
            // This applies persistence of 1/2
            int perlinAmp = finalPerlinAmp >>> (oct - 1);

            int perlinSampleDist = (HILL_WIDTH) >>> (oct - 1);

            // sample first
            int[] perlinSamples = new int[width / perlinSampleDist + 1];
            for (int sample = 0; sample < perlinSamples.length; sample++) {
                perlinSamples[sample] = random.nextInt(perlinAmp * 2) - perlinAmp;
            }

            // add interpolated value to map
            for (int i = 0; i < perlinMap.length; i++) {
                int perlinPointLeft = perlinSamples[i / perlinSampleDist];
                int perlinPointRight = perlinSamples[i / perlinSampleDist + 1];

                perlinMap[i] += Math.round(
                        interpolateCosine(
                                ((float) (i % perlinSampleDist)) / perlinSampleDist
                                , perlinPointLeft, perlinPointRight
                        )
                        // using cosine; making tops rounded
                );
            }
        }

        for (int k = 0; k < 1; k++) {
            for (int i = 0; i < perlinMap.length; i++) {
                // averaging smoothing
                if (i > 1 && i < perlinMap.length - 2) {
                    perlinMap[i] = Math.round(
                            (perlinMap[i - 1] + perlinMap[i + 1]) / 2
                    );
                }
            }
        }

        // single bump removal
        for (int i = 0; i < perlinMap.length; i++) {
            if (i > 1 && i < perlinMap.length - 2) {
                int p1 = perlinMap[i - 1];
                int p2 = perlinMap[i];
                int p3 = perlinMap[i + 1];
                //  _-_ / -_- -> ___ / ---
                if (p1 == p3 && p1 != p2) {
                    perlinMap[i] = p1;
                }
                // -^_ -> --_
                else if (p1 > p3 && p2 > p1) {
                    perlinMap[i] = p1;
                }
                // _^- -> _--
                else if (p3 > p1 && p2 > p3) {
                    perlinMap[i] = p3;
                }
                // -_^ -> --^
                else if (p3 > p1 && p2 < p1) {
                    perlinMap[i] = p1;
                }
                // ^_- -> ^--
                else if (p1 > p3 && p2 < p3) {
                    perlinMap[i] = p1;
                }
            }
        }

        return perlinMap;
    }

    /**
     * | ----
     * |     ---
     * |        ---
     * |           --
     * |             -
     * |              --
     * |                ---
     * |                   ---
     * -                      ----------------------------
     *
     * @param func_x
     * @return
     */
    private static float getGlacierMountedAmplitude(int func_x) {
        if (func_x > GLACIER_MOUNTAIN_WIDTH) {
            return 0;
        }
        else {
            float func_y = (GLACIER_MOUNTAIN_HEIGHT / 2f)
                    * (float) Math.cos(10 * func_x / (FastMath.PI * GLACIER_MOUNTAIN_WIDTH))
                    + (GLACIER_MOUNTAIN_HEIGHT / 2);
            return func_y;
        }
    }

    private static void placeGlacierMount(int[] heightMap) {
        System.out.println("[MapGenerator] Putting glacier...");

        // raise
        for (int i = 0; i < heightMap.length; i++) {
            if (worldOceanPosition == TYPE_OCEAN_RIGHT) {
                heightMap[i] += Math.round(getGlacierMountedAmplitude(i));
            }
            else {
                heightMap[i] += Math.round(getGlacierMountedAmplitude(heightMap.length - i - 1));
            }
        }
    }

    /**
     * Cosine interpolation between point a and b.
     * @param x [0.0, 1.0] relative position between a and b
     * @param a leftmost point
     * @param b rightmost point
     * @return
     */
    private static float interpolateCosine(float x, float a, float b) {
        float ft = x * FastMath.PI;
        float f = (1 - FastMath.cos(ft)) * 0.5f;

        return (a * (1 - f) + b * f);
    }

    private static void heightMapToObjectMap(int[] fs) {
        System.out.println("[MapGenerator] Shaping world as processed...");

        // iterate for heightmap
        for (int x = 0; x < width; x++) {
            int medianPosition = TERRAIN_AVERAGE_HEIGHT;
            int pillarOffset = medianPosition - fs[x];

            // for pillar length
            for (int i = 0; i < height - pillarOffset; i++) {

                if (i < dirtThickness) {
                    map.getTerrainArray()[i + pillarOffset][x] = DIRT;
                    map.getWallArray()[i + pillarOffset][x] = DIRT;
                } else {
                    map.getTerrainArray()[i + pillarOffset][x] = STONE;
                    map.getWallArray()[i + pillarOffset][x] = STONE;
                }

            }
        }

    }

	/* 2. Carve */

    /**
     * Carve (place air block) by noisemap, inversed gradation filter applied.
     * @param map noisemap
     * @param scarcity higher = larger blob
     * @param tile
     * @param message
     */
    private static void carveByMap(float[][] map, float scarcity, byte tile, String message) {
        System.out.println("[MapGenerator] " + message);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (map[i][j] > noiseMapGetGradientQuadPoly(i, noiseGradientStart, noiseGrdCaveEnd) / scarcity) {
                    MapGenerator.map.getTerrainArray()[i][j] = tile;
                }
            }
        }
    }

    /**
     * Fill by noisemap, gradation filter applied.
     * @param map noisemap
     * @param scarcity higher = larger blob
     * @param replaceFrom
     * @param tile
     * @param message
     */
    private static void fillByMap(float[][] map, float scarcity, byte replaceFrom, byte tile, String message) {
        System.out.println("[MapGenerator] " + message);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (map[i][j] > getNoiseGradient(i, noiseGradientStart, noiseGradientEnd) / scarcity
                        && MapGenerator.map.getTileFromTerrain(j, i) == replaceFrom) {
                    MapGenerator.map.getTerrainArray()[i][j] = tile;
                }
            }
        }
    }

    /**
     * Fill by noisemap, inversed gradation filter applied.
     * @param map noisemap
     * @param scarcity higher = larger blob
     * @param replaceFrom
     * @param tile
     * @param message
     */
    private static void fillByMapInverseGradFilter(float[][] map, float scarcity, byte replaceFrom, byte tile, String message) {
        System.out.println("[MapGenerator] " + message);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (map[i][j] > getNoiseGradientInversed(i, noiseGradientEnd, noiseGradientStart) / scarcity
                        && MapGenerator.map.getTileFromTerrain(j, i) == replaceFrom) {
                    MapGenerator.map.getTerrainArray()[i][j] = tile;
                }
            }
        }
    }

    /**
     * Fill by noisemap, no filter applied. Takes
     * <p>noiseGradientStart / scarcity</p>
     * as carving threshold.
     * @param map noisemap
     * @param scarcity higher = larger blob
     * @param replaceFrom
     * @param tile
     * @param message
     */
    private static void fillByMapNoFilter(float[][] map, float scarcity, byte replaceFrom, byte tile, String message) {
        System.out.println("[MapGenerator] " + message);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (map[i][j] > noiseGradientStart / scarcity
                        && MapGenerator.map.getTileFromTerrain(j, i) == replaceFrom) {
                    MapGenerator.map.getTerrainArray()[i][j] = tile;
                }
            }
        }
    }

    private static void fillByMap(float[][] map, float scarcity, byte replaceFrom, byte[] tile, String message) {
        System.out.println("[MapGenerator] " + message);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (map[i][j] > getNoiseGradient(i, noiseGradientStart, noiseGradientEnd) / scarcity
                        && MapGenerator.map.getTileFromTerrain(j, i) == replaceFrom) {
                    MapGenerator.map.getTerrainArray()[i][j]
                            = tile[random.nextInt(tile.length)];
                }
            }
        }
    }

    private static float getNoiseGradient(int x, float start, float end) {
        return noiseMapGetGradientQuadPoly(x, start, end);
    }

    private static float getNoiseGradientInversed(int x, float start, float end) {
        return noiseMapGetGradientMinusQuadPoly(x, start, end);
    }

    /**
     * Quadratic polynomial
     * (16/9) * (start-end)/height^2 * (x-height)^2 + end
     * 16/9: terrain is formed from 1/4 of height.
     * 1 - (1/4) = 3/4, reverse it and square it.
     * That makes 16/9.
     *
     * Shape:
     *
     * cavity -
     *  small
     *         -
     *          -
     *           --
     *             ----
     * cavity          --------
     *  large                  ----------------
     *
     * @param func_argX
     * @param start
     * @param end
     * @return
     */
    private static float noiseMapGetGradientQuadPoly(int func_argX, float start, float end) {
        float graph_gradient =
                FastMath.pow(FastMath.sqr(1 - TERRAIN_AVERAGE_HEIGHT), -1) // 1/4 -> 3/4 -> 9/16 -> 16/9
                * (start - end) / FastMath.sqr(height)
                * FastMath.sqr(func_argX - height)
                + end
                ;

        if (func_argX < TERRAIN_AVERAGE_HEIGHT) {
            return start;
        }
        else if (func_argX >= height) {
            return end;
        }
        else {
            return graph_gradient;
        }
    }

    /**
     * Double Quadratic polynomial
     * (16/9) * (start-end)/height^2 * (x-height)^2 + end
     * 16/9: terrain is formed from 1/4 of height.
     * 1 - (1/4) = 3/4, reverse it and square it.
     * That makes 16/9.
     *
     * Shape:
     *
     * cavity -
     *  small
     *         -
     *          -
     *           --
     *             ----
     * cavity          --------
     *  large                  ----------------
     *
     * @param func_argX
     * @param start
     * @param end
     * @return
     */
    private static float noiseMapGetGradientCubicPoly(int func_argX, float start, float end) {
        float graph_gradient =
                -FastMath.pow(FastMath.pow(1 - TERRAIN_AVERAGE_HEIGHT, 3), -1) // 1/4 -> 3/4 -> 9/16 -> 16/9
                        * (start - end) / FastMath.pow(height, 3)
                        * FastMath.pow(func_argX - height, 3)
                        + end
                ;

        if (func_argX < TERRAIN_AVERAGE_HEIGHT) {
            return start;
        }
        else if (func_argX >= height) {
            return end;
        }
        else {
            return graph_gradient;
        }
    }

    /**
     * Quadratic polynomial
     * -(16/9) * (start-end)/height^2 * (x - 0.25 * height)^2 + start
     * 16/9: terrain is formed from 1/4 of height.
     * 1 - (1/4) = 3/4, reverse it and square it.
     * That makes 16/9.
     *
     * Shape:
     *
     * cavity                                 _
     *  small
     *                                       _
     *                                      _
     *                                    __
     *                                ____
     * cavity                 ________
     *  large ________________
     *
     * @param func_argX
     * @param start
     * @param end
     * @return
     */
    private static float noiseMapGetGradientMinusQuadPoly(int func_argX, float start, float end) {
        float graph_gradient =
                -FastMath.pow(FastMath.sqr(1 - TERRAIN_AVERAGE_HEIGHT), -1) // 1/4 -> 3/4 -> 9/16 -> 16/9
                        * (start - end) / FastMath.sqr(height)
                        * FastMath.sqr(func_argX - TERRAIN_AVERAGE_HEIGHT)
                        + start
                ;

        if (func_argX < TERRAIN_AVERAGE_HEIGHT) {
            return start;
        }
        else if (func_argX >= height) {
            return end;
        }
        else {
            return graph_gradient;
        }
    }

    private static void generateFloatingIslands() {
        System.out.println("[MapGenerator] Placing floating islands...");

        int nIslandsMax = Math.round(map.width * 6f / 8192f);
        int nIslandsMin = Math.max(2, Math.round(map.width * 4f / 8192f));
        int nIslands = random.nextInt(nIslandsMax - nIslandsMin) + nIslandsMin;
        int prevIndex = -1;

        for (int i = 0; i < nIslands; i++) {
            int currentIndex = random.nextInt(FloatingIslandsPreset.presets);
            while (currentIndex == prevIndex) {
                currentIndex = random.nextInt(FloatingIslandsPreset.presets);
            }
            int[][] island = FloatingIslandsPreset.generatePreset(currentIndex, random);

            int startingPosX = random.nextInt(map.width - 2048) + 1024;
            int startingPosY = minimumFloatingIsleHeight + random.nextInt(minimumFloatingIsleHeight);

            for (int j = 0; j < island.length; j++) {
                for (int k = 0; k < island[0].length; k++) {
                    if (island[j][k] > 0) {
                        map.getTerrainArray()[j + startingPosY][k + startingPosX]
                                = (byte) island[j][k];
                    }
                }
            }
        }
    }
	
	/* Flood */

    private static void floodBottomLava() {
        System.out.println("[MapGenerator] Flooding bottom lava...");
        for (int i = height * 14 / 15; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (map.getTerrainArray()[i][j] == 0) {
                    map.getTerrainArray()[i][j] = LAVA;
                }
            }
        }
    }
	
	/* Plant */

    private static void plantGrass() {
        System.out.println("[MapGenerator] Planting grass...");

		/* TODO composing dirt and stone
		 * over certain level, use background dirt with stone 'peckles'
		 * beetween levels, use background dirt with larger and denser stone peckles.
		 * under another certain level, use background stone with dirt peckles.
		 */

        for (int y = TERRAIN_AVERAGE_HEIGHT - MAX_HILL_HEIGHT
                ; y < TERRAIN_AVERAGE_HEIGHT + MAX_HILL_HEIGHT
                ; y++) {
            for (int x = 0; x < map.width; x++) {

                int thisTile = map.getTileFromTerrain(x, y);

                for (int i = 0; i < 9; i++) {
                    int nearbyTile = -1;
                    try { nearbyTile = map.getTileFromTerrain(x + (i / 3) - 1, y + (i % 3) - 1); }
                    catch (ArrayIndexOutOfBoundsException e) {}

                    if (i != 4 && thisTile == DIRT && nearbyTile == AIR) {
                        map.getTerrainArray()[y][x] = GRASS;
                        break;
                    }
                }
            }
        }

    }

    private static boolean isGrassOrDirt(int x, int y) {
        return map.getTileFromTerrain(x, y) == GRASS || map.getTileFromTerrain(x, y) == DIRT;
    }

    private static void replaceIfTerrain(byte ifTile, int x, int y, byte replaceTile) {
        if (map.getTileFromTerrain(x, y) == ifTile) {
            map.getTerrainArray()[y][x] = replaceTile;
        }
    }

    private static void replaceIfWall(byte ifTile, int x, int y, byte replaceTile) {
        if (map.getTileFromWall(x, y) == ifTile) {
            map.getWallArray()[y][x] = replaceTile;
        }
    }

	/* Post-process */

    private static void fillOcean() {
        for (int ix = 0; ix < OCEAN_WIDTH * 1.5; ix++) {
            //flooding
            if (ix < OCEAN_WIDTH) {
                if (worldOceanPosition == TYPE_OCEAN_LEFT) {
                    for (int y = getTerrainHeightFromHeightMap(OCEAN_WIDTH)
                            ; y < getTerrainHeightFromHeightMap(ix)
                            ; y++) {
                        map.getTerrainArray()
                                [y][ix] = WATER;
                    }
                }
                else if (worldOceanPosition == TYPE_OCEAN_RIGHT) {
                    for (int y = getTerrainHeightFromHeightMap(map.width - 1 - OCEAN_WIDTH)
                            ; y < getTerrainHeightFromHeightMap(map.width - 1 - ix)
                            ; y++) {
                        map.getTerrainArray()
                                [y][map.width - 1 - ix] = WATER;
                    }
                }
            }
            //sand
            // linearly increase thickness of the sand sheet
            for (int iy = 0; iy < 40 - (ix * 40 / (OCEAN_WIDTH + SHORE_WIDTH)); iy++) {
                if (worldOceanPosition == TYPE_OCEAN_LEFT) {
                    int terrainPoint = getTerrainHeightFromHeightMap(ix);

                    map.getTerrainArray()
                            [terrainPoint + iy]
                            [ix] = SAND;
                    map.getTerrainArray()
                            [terrainPoint + iy - 1] // clear grass and make the sheet thicker
                            [ix] = SAND;
                }
                else if (worldOceanPosition == TYPE_OCEAN_RIGHT) {
                    int terrainPoint = getTerrainHeightFromHeightMap(map.width - 1 - ix);

                    map.getTerrainArray()
                            [terrainPoint + iy]
                            [map.width - 1 - ix] = SAND;
                    map.getTerrainArray()
                            [terrainPoint + iy - 1] // clear grass and make the sheet thicker
                            [map.width - 1 - ix] = SAND;
                }
            }
        }
    }

    private static void freeze() {
        for (int y = 0; y < map.height - 1; y++) {
            for (int x = 0; x < getFrozenAreaWidth(y); x++) {
                if (worldOceanPosition == TYPE_OCEAN_RIGHT) {
                    replaceIfTerrain(DIRT, x, y, SNOW);
                    replaceIfTerrain(STONE, x, y, ICE_NATURAL);

                    replaceIfWall(DIRT, x, y, SNOW);
                    replaceIfWall(STONE, x, y, ICE_NATURAL);
                }
                else {
                    replaceIfTerrain(DIRT, map.width - 1 - x, y, SNOW);
                    replaceIfTerrain(STONE, map.width - 1 - x, y, ICE_NATURAL);

                    replaceIfWall(DIRT, map.width - 1 - x, y, SNOW);
                    replaceIfWall(STONE, map.width - 1 - x, y, ICE_NATURAL);
                }
            }
        }
    }

    /**
     *
     * @return width of the frozen area for MapGenerator.freeze
     */
    private static int getFrozenAreaWidth(int y) {
        int randDeviation = 7;
        // narrower that the actual width
        int width = Math.round(GLACIER_MOUNTAIN_WIDTH * 0.625f);
        int height;
        if (worldOceanPosition == TYPE_OCEAN_RIGHT) {
            height = getTerrainHeightFromHeightMap(width);
        }
        else {
            height = getTerrainHeightFromHeightMap(map.width - 1 - width);
        }
        float k = (width) / FastMath.sqrt(height);

        if (y < height) {
            // ground
            return width;
        }
        else {
            // underground
            return Math.round(
                    k * FastMath.sqrt(y) + (random.nextInt(3) - 1)
            );
        }
    }

    /**
     *
     * @param x position of heightmap
     * @return
     */
    private static int getTerrainHeightFromHeightMap(int x) {
        return TERRAIN_AVERAGE_HEIGHT - heightMap[x];
    }

	/* Utility */

    private static int clampN(int clampNumber, int num) {
        return FastMath.floor(num / clampNumber) * clampNumber;
    }

    private static boolean outOfBound(int w, int h, int x, int y) {
        return !(x > 0 && y > 0 && x < w && y < h);
    }

    private static float getDistance(float x1, float y1, float x2, float y2) {
        return FastMath.sqrt(FastMath.pow(x1 - x2, 2) + FastMath.pow(y2 - y1, 2));
    }

    private static void circularDig(int i, int j, int brushSize, int fillFrom, int fill) {
        float halfBrushSize = (brushSize * 0.5f);

        for (int pointerY = 0; pointerY < brushSize; pointerY++) {
            for (int pointerX = 0; pointerX < brushSize; pointerX++) {
                if (getDistance(j
                        , i
                        , j + pointerX - halfBrushSize
                        , i + pointerY - halfBrushSize)
                        <= FastMath.floor(brushSize / 2) - 1
                        ) {
                    if (
                            Math.round(j + pointerX - halfBrushSize) > brushSize
                                    && Math.round(j + pointerX - halfBrushSize) < width - brushSize
                                    && Math.round(i + pointerY - halfBrushSize) > brushSize
                                    && Math.round(i + pointerY - halfBrushSize) < height - brushSize
                            ) {
                        if (
                                map.getTerrainArray()
                                        [Math.round(i + pointerY - halfBrushSize)]
                                        [Math.round(j + pointerX - halfBrushSize)]
                                        == (byte) fillFrom
                                ) {
                            map.getTerrainArray()
                                    [Math.round(i + pointerY - halfBrushSize)]
                                    [Math.round(j + pointerX - halfBrushSize)]
                                    = (byte) fill;
                        }
                    }
                }
            }
        }
    }
}
