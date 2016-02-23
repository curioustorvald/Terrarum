package com.Torvald.Terrarum.MapGenerator;

import com.Torvald.Rand.HQRNG;
import com.Torvald.Terrarum.GameMap.GameMap;
import com.Torvald.Terrarum.GameMap.MapLayer;
import com.Torvald.Terrarum.TileProperties.TileNameCode;
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

    private static final int CAVE_LARGEST_FEATURE = 200;

    private static int OCEAN_WIDTH = 400;
    private static int SHORE_WIDTH = 120;
    private static int MAX_OCEAN_DEPTH = 200;

    private static final int TERRAIN_PERTURB_OFFSETMAX = 32; // [-val , val]
    private static final int TERRAIN_PERTURB_LARGESTFEATURE = 256;
    private static final float TERRAIN_PERTURB_RATE = 0.5f;

    private static int GLACIER_MOUNTAIN_WIDTH = 900;
    private static final int GLACIER_MOUNTAIN_HEIGHT = 300;

    private static final float CAVEGEN_PERTURB_RATE = 0.37f;
    private static final float CAVEGEN_PERTURB2_RATE = 0.25f;

    private static final float CAVEGEN_THRE_START = 0.87f;
    private static final float CAVEGEN_THRE_END = 0.67f;

    private static final int CAVEGEN_LARGEST_FEATURE = 256;
    private static final int CAVEGEN_LARGEST_FEATURE_PERTURB = 128;

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

        perturbTerrain();

        carveCave(
                caveGen(1.4f, 1.7f)
                , TileNameCode.AIR
                , "Carving out cave..."
        );

        fillByMapNoFilterUnderground(
                generate2DSimplexNoiseWorldSize(1f, 1f)
                , 0.9f
                , TileNameCode.AIR
                , TileNameCode.STONE
                , "Collapsing caves..."
        );

        /*fillByMapInverseGradFilter(
                generate2DSimplexNoiseWorldSize(2.5f, 2.5f)
                , 1.02f
                , TileNameCode.DIRT
                , TileNameCode.STONE
                , "Planting stones on dirt layers..."
        );
        fillByMapInverseGradFilter(
                generate2DSimplexNoiseWorldSize(2.5f, 2.5f)
                , 0.98f
                , TileNameCode.STONE
                , TileNameCode.DIRT
                , "Planting dirts..."
        );
        fillByMapInverseGradFilter(
                generate2DSimplexNoiseWorldSize(2.5f, 2.5f)
                , 0.92f
                , TileNameCode.STONE
                , GRAVEL
                , "Planting gravels..."
        );*/

        /**
         * Plant ores
         */
        /*fillByMap(
                generate2DSimplexNoiseWorldSize(5, 5)
                , 0.78f
                , TileNameCode.STONE
                , DIAMOND
                , "Planting diamonds..."
        );

        byte[] berylsArray = {RUBY, EMERALD, SAPPHIRE, TOPAZ, AMETHYST};
        fillByMap(
                generate2DSimplexNoiseWorldSize(5, 5)
                , 0.8f
                , TileNameCode.STONE
                , berylsArray
                , "Planting beryls..."
        );

        fillByMap(
                generate2DSimplexNoiseWorldSize(5, 5)
                , 0.80f
                , TileNameCode.STONE
                , GOLD
                , "Planting golds..."
        );
        fillByMap(
                generate2DSimplexNoiseWorldSize(5, 5)
                , 0.866f
                , TileNameCode.STONE
                , IRON
                , "Planting irons..."
        );
        fillByMap(
                generate2DSimplexNoiseWorldSize(5, 5)
                , 0.88f
                , TileNameCode.STONE
                , COPPER
                , "Planting coppers..."
        );*/

        /** TODO Cobaltite, Ilmenite, Aurichalcum (and possibly pitchblende?) **/

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

    /**
     * Ridged 2D simplex noise with some perturbing
     * @param xStretch
     * @param yStretch
     * @return
     */
    private static float[][] caveGen(float xStretch, float
            yStretch) {
        float[][] noiseMap = new float[height][width];

        SimplexNoise simplexNoise = new SimplexNoise(CAVEGEN_LARGEST_FEATURE, CAVEGEN_PERTURB_RATE
                , seed);
        SimplexNoise simplexNoisePerturbMap = new SimplexNoise(CAVEGEN_LARGEST_FEATURE_PERTURB, 0.5f
                , seed ^ random.nextLong());

        float xEnd=width * yStretch;
        float yEnd=height * xStretch;

        float lowestNoiseVal = 10000f;
        float highestNoiseVal = -10000f;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int ny=(int)(y * ((xEnd)/width));
                int nx=(int)(x * ((yEnd)/height));

                float noiseInit = simplexNoise.getNoise(nx,ny); // [-1 , 1]
                float perturbInit = (simplexNoisePerturbMap.getNoise(nx, ny) * 0.5f) + 0.5f;  // [0 , 1]

                /** Ridging part ! */
                float noiseFin = 1f - Math.abs(noiseInit); // [0 , 1]

                float perturb = 1 - (perturbInit * CAVEGEN_PERTURB2_RATE); // [1 , 1-0.25]
                float noisePerturbed = (noiseFin * perturb); // [0 , 1]

                if (noisePerturbed < lowestNoiseVal) lowestNoiseVal = noisePerturbed;
                if (noisePerturbed > highestNoiseVal) highestNoiseVal = noisePerturbed;
                noiseMap[y][x] = noisePerturbed;
            }
        }

        // Auto-scaling noise

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float noiseInit = noiseMap[y][x] - lowestNoiseVal;

                float noiseFin = noiseInit * (1f / (highestNoiseVal - lowestNoiseVal));

                float noiseThresholded = noiseFin > gradientSqrt(y, CAVEGEN_THRE_START,
                        CAVEGEN_THRE_END
                ) ? 1 : 0;

                noiseMap[y][x] = noiseThresholded;
            }
        }

        return noiseMap;
    }

    private static float[][] generate2DSimplexNoiseWorldSize(float xStretch, float yStretch) {
        return generate2DSimplexNoise(width, height, xStretch, yStretch);
    }

    /**
     * Generate 2D array of simplex noise.
     * @param sizeX
     * @param sizeY
     * @param xStretch
     * @param yStretch
     * @return matrix in ![x][y]!
     */
    private static float[][] generate2DSimplexNoise(int sizeX, int sizeY, float xStretch, float yStretch){
        SimplexNoise simplexNoise = new SimplexNoise(CAVE_LARGEST_FEATURE, 0.1f, seed ^ random.nextLong());

        float xStart=0;
        float yStart=0;

        /** higher = denser.
         * Recommended: (width or height) * 3
         */
        float xEnd=width * yStretch;
        float yEnd=height * xStretch;

        float lowestNoiseVal = 10000f;
        float highestNoiseVal = -10000f;

        float[][] result=new float[sizeY][sizeX];

        for(int i=0;i<sizeY;i++){
            for(int j=0;j<sizeX;j++){
                int x=(int)(xStart+i*((xEnd-xStart)/sizeX));
                int y=(int)(yStart+j*((yEnd-yStart)/sizeY));

                float noiseValue = (float) (0.5*(1+simplexNoise.getNoise(x,y)));

                if (noiseValue < lowestNoiseVal) lowestNoiseVal = noiseValue;
                if (noiseValue > highestNoiseVal) highestNoiseVal = noiseValue;

                result[i][j] = noiseValue;
            }
        }

        // Auto-scaling noise

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float noiseInit = result[y][x] - lowestNoiseVal;

                float noiseFin = noiseInit * (1f / (highestNoiseVal - lowestNoiseVal));

                result[y][x] = noiseFin;
            }
        }

        return result;
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
                    map.getTerrainArray()[i + pillarOffset][x] = TileNameCode.DIRT;
                    map.getWallArray()[i + pillarOffset][x] = TileNameCode.DIRT;
                } else {
                    map.getTerrainArray()[i + pillarOffset][x] = TileNameCode.STONE;
                    map.getWallArray()[i + pillarOffset][x] = TileNameCode.STONE;
                }

            }
        }
    }

    private static void perturbTerrain() {
        SimplexNoise perturbGen = new SimplexNoise(TERRAIN_PERTURB_LARGESTFEATURE
                , TERRAIN_PERTURB_RATE, seed ^ random.nextLong());

        float[][] perturbMap = new float[height][width];

        byte[][] layerWall = map.getWallArray();
        byte[][] layerTerrain = map.getTerrainArray();
        MapLayer newLayerWall = new MapLayer(width, height);
        MapLayer newLayerTerrain = new MapLayer(width, height);

        float lowestNoiseVal = 10000f;
        float highestNoiseVal = -10000f;

        for (int y = 0; y < map.height; y++) {
            for (int x = 0; x < map.width; x++) {
                float noise = perturbGen.getNoise(x, y); // [-1, 1]
                perturbMap[y][x] = noise;
                if (noise < lowestNoiseVal) lowestNoiseVal = noise;
                if (noise > highestNoiseVal) highestNoiseVal = noise;
            }
        }

        // Auto-scale noise [-1, 1]
        /**
         * See ./work_files/Linear_autoscale.gcx
         */
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float noiseInit = perturbMap[y][x];
                float noiseFin = (noiseInit - ((highestNoiseVal + lowestNoiseVal) / 2f))
                        * (2f / (highestNoiseVal - lowestNoiseVal));

                perturbMap[y][x] = noiseFin;
            }
        }

        // Perturb to x-axis, apply to newLayer
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float offsetOrigin = perturbMap[y][x] * 0.5f + 0.5f; // [0 , 1]
                int offset = Math.round(offsetOrigin * TERRAIN_PERTURB_OFFSETMAX);

                byte tileWall = layerWall[y][x];
                byte tileTerrain = layerTerrain[y][x];

                try {
                    //newLayerWall.setTile(x + offset, y, tileWall);
                    //newLayerTerrain.setTile(x + offset, y, tileTerrain);
                    //layerWall[y][x] = 0;
                    //layerTerrain[y][x] = 0;
                    layerWall[y - offset][x] = tileWall;
                    layerTerrain[y - offset][x] = tileTerrain;
                }
                catch (ArrayIndexOutOfBoundsException e) {
                }
            }
        }

        // set reference (pointer) of original map layer to new layers
        //map.overwriteLayerWall(newLayerWall);
        //map.overwriteLayerTerrain(newLayerTerrain);

    }

	/* 2. Carve */

    private static void carveCave(float[][] map, byte tile, String message) {
        System.out.println("[MapGenerator] " + message);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (map[i][j] > 0.9) {
                    MapGenerator.map.getTerrainArray()[i][j] = tile;
                }
            }
        }
    }

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
                if (map[i][j] > gradientQuadratic(i, noiseGradientStart, noiseGrdCaveEnd) *
                        scarcity) {
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
                if (map[i][j] > getNoiseGradient(i, noiseGradientStart, noiseGradientEnd) * scarcity
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
                if (map[i][j] > getNoiseGradientInversed(i, noiseGradientEnd, noiseGradientStart)
                        * scarcity
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
                if (map[i][j] > noiseGradientStart * scarcity
                        && MapGenerator.map.getTileFromTerrain(j, i) == replaceFrom) {
                    MapGenerator.map.getTerrainArray()[i][j] = tile;
                }
            }
        }
    }

    private static void fillByMapNoFilterUnderground(float[][] map, float scarcity, byte replaceFrom, byte
            tile, String message) {
        System.out.println("[MapGenerator] " + message);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (map[i][j] > noiseGradientStart * scarcity
                        && MapGenerator.map.getTileFromTerrain(j, i) == replaceFrom
                        && MapGenerator.map.getTileFromWall(j, i) == TileNameCode.STONE) {
                    MapGenerator.map.getTerrainArray()[i][j] = tile;
                }
            }
        }
    }

    private static void fillByMap(float[][] map, float scarcity, byte replaceFrom, byte[] tile, String message) {
        System.out.println("[MapGenerator] " + message);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (map[i][j] > getNoiseGradient(i, noiseGradientStart, noiseGradientEnd) * scarcity
                        && MapGenerator.map.getTileFromTerrain(j, i) == replaceFrom) {
                    MapGenerator.map.getTerrainArray()[i][j]
                            = tile[random.nextInt(tile.length)];
                }
            }
        }
    }

    private static float getNoiseGradient(int x, float start, float end) {
        return gradientQuadratic(x, start, end);
    }

    private static float getNoiseGradientInversed(int x, float start, float end) {
        return gradientMinusQuadratic(x, start, end);
    }

    private static float gradientSqrt(int func_argX, float start, float end) {
        float graph_gradient =
                ((end - start) / FastMath.sqrt(height - TERRAIN_AVERAGE_HEIGHT))
                        * FastMath.sqrt(func_argX - TERRAIN_AVERAGE_HEIGHT)
                        + start;

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
    private static float gradientQuadratic(int func_argX, float start, float end) {
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
    private static float gradientCubic(int func_argX, float start, float end) {
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
    private static float gradientMinusQuadratic(int func_argX, float start, float end) {
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
                    map.getTerrainArray()[i][j] = TileNameCode.LAVA;
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
                    int nearbyWallTile = -1;
                    try { nearbyWallTile = map.getTileFromWall(x + (i % 3) - 1, y + (i / 3) - 1); }
                    catch (ArrayIndexOutOfBoundsException e) {}

                    if (i != 4 && thisTile == TileNameCode.DIRT && nearbyWallTile == TileNameCode.AIR) {
                        map.getTerrainArray()[y][x] = TileNameCode.GRASS;
                        break;
                    }
                }
            }
        }

    }

    private static boolean isGrassOrDirt(int x, int y) {
        return map.getTileFromTerrain(x, y) == TileNameCode.GRASS || map.getTileFromTerrain(x, y) == TileNameCode.DIRT;
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
                                [y][ix] = TileNameCode.WATER;
                    }
                }
                else if (worldOceanPosition == TYPE_OCEAN_RIGHT) {
                    for (int y = getTerrainHeightFromHeightMap(map.width - 1 - OCEAN_WIDTH)
                            ; y < getTerrainHeightFromHeightMap(map.width - 1 - ix)
                            ; y++) {
                        map.getTerrainArray()
                                [y][map.width - 1 - ix] = TileNameCode.WATER;
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
                            [ix] = TileNameCode.SAND;
                    map.getTerrainArray()
                            [terrainPoint + iy - 1] // clear grass and make the sheet thicker
                            [ix] = TileNameCode.SAND;
                }
                else if (worldOceanPosition == TYPE_OCEAN_RIGHT) {
                    int terrainPoint = getTerrainHeightFromHeightMap(map.width - 1 - ix);

                    map.getTerrainArray()
                            [terrainPoint + iy]
                            [map.width - 1 - ix] = TileNameCode.SAND;
                    map.getTerrainArray()
                            [terrainPoint + iy - 1] // clear grass and make the sheet thicker
                            [map.width - 1 - ix] = TileNameCode.SAND;
                }
            }
        }
    }

    private static void freeze() {
        for (int y = 0; y < map.height - 1; y++) {
            for (int x = 0; x < getFrozenAreaWidth(y); x++) {
                if (worldOceanPosition == TYPE_OCEAN_RIGHT) {
                    replaceIfTerrain(TileNameCode.DIRT, x, y, TileNameCode.SNOW);
                    replaceIfTerrain(TileNameCode.STONE, x, y, TileNameCode.ICE_NATURAL);

                    replaceIfWall(TileNameCode.DIRT, x, y, TileNameCode.SNOW);
                    replaceIfWall(TileNameCode.STONE, x, y, TileNameCode.ICE_NATURAL);
                }
                else {
                    replaceIfTerrain(TileNameCode.DIRT, map.width - 1 - x, y, TileNameCode.SNOW);
                    replaceIfTerrain(TileNameCode.STONE, map.width - 1 - x, y, TileNameCode.ICE_NATURAL);

                    replaceIfWall(TileNameCode.DIRT, map.width - 1 - x, y, TileNameCode.SNOW);
                    replaceIfWall(TileNameCode.STONE, map.width - 1 - x, y, TileNameCode.ICE_NATURAL);
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
