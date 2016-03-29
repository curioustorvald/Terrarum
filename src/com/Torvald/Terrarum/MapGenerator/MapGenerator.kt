package com.torvald.terrarum.mapgenerator

import com.torvald.random.HQRNG
import com.torvald.terrarum.gamemap.GameMap
import com.torvald.terrarum.tileproperties.TileNameCode
import com.jme3.math.FastMath
import com.sudoplay.joise.Joise
import com.sudoplay.joise.module.*
import java.util.*

object MapGenerator {

    private lateinit var map: GameMap
    private lateinit var random: Random
    //private static float[] noiseArray;
    private var SEED: Long = 0
    private var WIDTH: Int = 0
    private var HEIGHT: Int = 0

    //private lateinit var heightMap: IntArray
    private lateinit var terrainMap: Array<BitSet>

    private var DIRT_LAYER_DEPTH: Int = 0
    private var TERRAIN_AVERAGE_HEIGHT: Int = 0
    private var minimumFloatingIsleHeight: Int = 0

    private val noiseGradientStart = 0.67f
    private val noiseGradientEnd = 0.56f
    private val noiseGrdCaveEnd = 0.54f

    private val HILL_WIDTH = 256 // power of two!
    //private val MAX_HILL_HEIGHT = 100
    private val TERRAIN_UNDULATION = 250

    private val CAVE_LARGEST_FEATURE = 200

    private var OCEAN_WIDTH = 400
    private var SHORE_WIDTH = 120
    private val MAX_OCEAN_DEPTH = 200

    private var GLACIER_MOUNTAIN_WIDTH = 900
    private val GLACIER_MOUNTAIN_HEIGHT = 300

    private val CAVEGEN_PERTURB_RATE = 0.37f
    private val CAVEGEN_PERTURB2_RATE = 0.25f

    private val CAVEGEN_THRE_START = 0.87f
    private val CAVEGEN_THRE_END = 0.67f

    private val CAVEGEN_LARGEST_FEATURE = 256
    private val CAVEGEN_LARGEST_FEATURE_PERTURB = 128

    private var worldOceanPosition: Int = -1
    private val TYPE_OCEAN_LEFT = 0
    private val TYPE_OCEAN_RIGHT = 1

    private val GRASSCUR_UP = 0
    private val GRASSCUR_RIGHT = 1
    private val GRASSCUR_DOWN = 2
    private val GRASSCUR_LEFT = 3
    
    fun attachMap(map: GameMap) {
        this.map = map
        WIDTH = map.width
        HEIGHT = map.height

        val widthMulFactor = WIDTH / 8192f

        DIRT_LAYER_DEPTH = (100 * HEIGHT / 1024f).toInt()
        minimumFloatingIsleHeight = (25 * (HEIGHT / 1024f)).toInt()
        TERRAIN_AVERAGE_HEIGHT = HEIGHT / 4

        OCEAN_WIDTH = Math.round(OCEAN_WIDTH * widthMulFactor)
        SHORE_WIDTH = Math.round(SHORE_WIDTH * widthMulFactor)
        GLACIER_MOUNTAIN_WIDTH = Math.round(GLACIER_MOUNTAIN_WIDTH * widthMulFactor)
    }

    fun setSeed(seed: Long) {
        this.SEED = seed
    }

    /**
     * Generate terrain and override attached map
     */
    fun generateMap() {
        random = HQRNG(SEED)
        println("[mapgenerator] Seed: " + SEED)

        worldOceanPosition = if (random.nextBoolean()) TYPE_OCEAN_LEFT else TYPE_OCEAN_RIGHT

        //heightMap = raise2(MAX_HILL_HEIGHT / 2)
        //generateOcean(heightMap)
        //placeGlacierMount(heightMap)
        //heightMapToObjectMap(heightMap)


        terrainMap = raise3()


        terrainMapToObjectMap()

        /**
         * Done: more perturbed overworld (harder to supra-navigate)
         * Todo: veined ore distribution (metals) -- use veined simplex noise
         * Todo: clustered gem distribution (Groups: [Ruby, Sapphire], Amethyst, Yellow topaz, emerald, diamond) -- use regular simplex noise
         * Todo: Lakes! Aquifers! Lava chambers!
         * Todo: deserts (variants: SAND_DESERT, SAND_RED)
         * Todo: volcano(es?)
         * Done: variants of beach (SAND, SAND_BEACH, SAND_BLACK, SAND_GREEN)
         */

        carveCave(
                caveGen(1.4f, 1.7f), TileNameCode.AIR, "Carving out cave...")

        fillByMapNoFilterUnderground(
                generate2DSimplexNoiseWorldSize(1f, 1f), 0.9f, TileNameCode.AIR, TileNameCode.STONE, "Collapsing caves...")

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

        /** TODO Cobaltite, Ilmenite, Aurichalcum (and possibly pitchblende?)  */

        floodBottomLava()
        // freeze()
        // fillOcean()
        plantGrass()

        //post-process
        generateFloatingIslands()

        //wire layer
        for (i in 0..HEIGHT - 1) {
            for (j in 0..WIDTH - 1) {
                map.wireArray[i][j] = 0
            }
        }

        // Free some memories
        System.gc()
    }

    /* 1. Raise */

    /**
     * Ridged 2D simplex noise with some perturbing
     * @param xStretch
     * *
     * @param yStretch
     * *
     * @return
     */
    private fun caveGen(xStretch: Float, yStretch: Float): Array<FloatArray> {
        val noiseMap = Array(HEIGHT) { FloatArray(WIDTH) }

        val simplexNoise = SimplexNoise(CAVEGEN_LARGEST_FEATURE, CAVEGEN_PERTURB_RATE, SEED)
        val simplexNoisePerturbMap = SimplexNoise(CAVEGEN_LARGEST_FEATURE_PERTURB, 0.5f, SEED xor random.nextLong())

        val xEnd = WIDTH * yStretch
        val yEnd = HEIGHT * xStretch

        var lowestNoiseVal = 10000f
        var highestNoiseVal = -10000f

        for (y in 0..HEIGHT - 1) {
            for (x in 0..WIDTH - 1) {
                val ny = (y * (xEnd / WIDTH)).toInt()
                val nx = (x * (yEnd / HEIGHT)).toInt()

                val noiseInit = simplexNoise.getNoise(nx, ny) // [-1 , 1]
                val perturbInit = simplexNoisePerturbMap.getNoise(nx, ny) * 0.5f + 0.5f  // [0 , 1]

                /** Ridging part !  */
                val noiseFin = 1f - Math.abs(noiseInit) // [0 , 1]

                val perturb = 1 - perturbInit * CAVEGEN_PERTURB2_RATE // [1 , 1-0.25]
                val noisePerturbed = noiseFin * perturb // [0 , 1]

                if (noisePerturbed < lowestNoiseVal) lowestNoiseVal = noisePerturbed
                if (noisePerturbed > highestNoiseVal) highestNoiseVal = noisePerturbed
                noiseMap[y][x] = noisePerturbed
            }
        }

        // Auto-scaling noise

        for (y in 0..HEIGHT - 1) {
            for (x in 0..WIDTH - 1) {
                val noiseInit = noiseMap[y][x] - lowestNoiseVal

                val noiseFin = noiseInit * (1f / (highestNoiseVal - lowestNoiseVal))

                val noiseThresholded = (if (noiseFin > gradientSqrt(y, CAVEGEN_THRE_START,
                        CAVEGEN_THRE_END))
                    1
                else
                    0).toFloat()

                noiseMap[y][x] = noiseThresholded
            }
        }

        return noiseMap
    }

    private fun generate2DSimplexNoiseWorldSize(xStretch: Float, yStretch: Float): Array<FloatArray> {
        return generate2DSimplexNoise(WIDTH, HEIGHT, xStretch, yStretch)
    }

    /**
     * Generate 2D array of simplex noise.
     * @param sizeX
     * *
     * @param sizeY
     * *
     * @param xStretch
     * *
     * @param yStretch
     * *
     * @return matrix in ![x][y]!
     */
    private fun generate2DSimplexNoise(sizeX: Int, sizeY: Int, xStretch: Float, yStretch: Float): Array<FloatArray> {
        val simplexNoise = SimplexNoise(CAVE_LARGEST_FEATURE, 0.1f, SEED xor random.nextLong())

        val xStart = 0f
        val yStart = 0f

        /** higher = denser.
         * Recommended: (width or height) * 3
         */
        val xEnd = WIDTH * yStretch
        val yEnd = HEIGHT * xStretch

        var lowestNoiseVal = 10000f
        var highestNoiseVal = -10000f

        val result = Array(sizeY) { FloatArray(sizeX) }

        for (i in 0..sizeY - 1) {
            for (j in 0..sizeX - 1) {
                val x = (xStart + i * ((xEnd - xStart) / sizeX)).toInt()
                val y = (yStart + j * ((yEnd - yStart) / sizeY)).toInt()

                val noiseValue = (0.5 * (1 + simplexNoise.getNoise(x, y))).toFloat()

                if (noiseValue < lowestNoiseVal) lowestNoiseVal = noiseValue
                if (noiseValue > highestNoiseVal) highestNoiseVal = noiseValue

                result[i][j] = noiseValue
            }
        }

        // Auto-scaling noise

        for (y in 0..HEIGHT - 1) {
            for (x in 0..WIDTH - 1) {
                val noiseInit = result[y][x] - lowestNoiseVal

                val noiseFin = noiseInit * (1f / (highestNoiseVal - lowestNoiseVal))

                result[y][x] = noiseFin
            }
        }

        return result
    }

    private fun generateOcean(noiseArrayLocal: IntArray): IntArray {
        val oceanLeftP1 = noiseArrayLocal[OCEAN_WIDTH]
        val oceanRightP1 = noiseArrayLocal[noiseArrayLocal.size - OCEAN_WIDTH]

        /**
         * Add ocean so that:

         * +1|       -   -
         * 0|      -  --  ...
         * -1|______  -

         * interpolated to

         * +1|        -   -
         * 0|   _---  --  ...
         * -1|__-      -

         * ↑-- Rough, white noise

         * -1 means -MAX_HILL_HEIGHT
         */
        for (i in 0..OCEAN_WIDTH - 1) {
            if (worldOceanPosition == TYPE_OCEAN_LEFT) {
                noiseArrayLocal[i] = Math.round(
                        interpolateCosine(
                                i.toFloat() / OCEAN_WIDTH, (-MAX_OCEAN_DEPTH).toFloat(), oceanLeftP1.toFloat()))
            } else if (worldOceanPosition == TYPE_OCEAN_RIGHT) {
                noiseArrayLocal[noiseArrayLocal.size - OCEAN_WIDTH + i] = Math.round(
                        interpolateCosine(
                                i.toFloat() / OCEAN_WIDTH, oceanRightP1.toFloat(), (-MAX_OCEAN_DEPTH).toFloat()))
            } else {
                throw RuntimeException("Ocean position were not set correctly.")
            }
        }

        return noiseArrayLocal
    }

    /**
     * http://accidentalnoise.sourceforge.net/minecraftworlds.html
     */
    private fun raise3(): Array<BitSet> {
        val noiseMap = Array(HEIGHT, { BitSet(WIDTH) })

        // Height = Terrain undulation times 2.
        val SCALE_X: Double = (TERRAIN_UNDULATION * 0.5).toDouble()
        val SCALE_Y: Double = (TERRAIN_UNDULATION * 0.25).toDouble()

        val ground_gradient = ModuleGradient()
        ground_gradient.setGradient(0.0, 0.0, 0.0, 1.0)

        /* Lowlands */

        val lowland_shape_fractal = ModuleFractal()
        lowland_shape_fractal.setType(ModuleFractal.FractalType.FBM)
        lowland_shape_fractal.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        lowland_shape_fractal.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        lowland_shape_fractal.setNumOctaves(4)
        lowland_shape_fractal.setFrequency(0.6)
        lowland_shape_fractal.seed = SEED xor random.nextLong()
        //println(lowland_shape_fractal.seed)

        val lowland_autocorrect = ModuleAutoCorrect()
        lowland_autocorrect.setLow(0.0)
        lowland_autocorrect.setHigh(1.0)
        lowland_autocorrect.setSource(lowland_shape_fractal)

        val lowland_scale = ModuleScaleOffset()
        lowland_scale.setSource(lowland_autocorrect)
        lowland_scale.setScale(0.8)
        lowland_scale.setOffset(-2.75)

        val lowland_y_scale = ModuleScaleDomain()
        lowland_y_scale.setSource(lowland_scale)
        lowland_y_scale.setScaleY(0.0)

        val lowland_terrain = ModuleTranslateDomain()
        lowland_terrain.setSource(ground_gradient)
        lowland_terrain.setAxisYSource(lowland_y_scale)

        /* highlands */

        val highland_shape_fractal = ModuleFractal()
        highland_shape_fractal.setType(ModuleFractal.FractalType.RIDGEMULTI)
        highland_shape_fractal.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        highland_shape_fractal.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        highland_shape_fractal.setNumOctaves(4)
        highland_shape_fractal.setFrequency(0.5) // horizontal size. Higher == narrower
        highland_shape_fractal.seed = SEED xor random.nextLong()
        //println(highland_shape_fractal.seed)

        val highland_autocorrect = ModuleAutoCorrect()
        highland_autocorrect.setSource(highland_shape_fractal)
        highland_autocorrect.setLow(0.0)
        highland_autocorrect.setHigh(1.0)

        val highland_scale = ModuleScaleOffset()
        highland_scale.setSource(highland_autocorrect)
        highland_scale.setScale(1.4) // vertical size. Higher == taller
        highland_scale.setOffset(-2.25)

        val highland_y_scale = ModuleScaleDomain()
        highland_y_scale.setSource(highland_scale)
        highland_y_scale.setScaleY(0.0)

        val highland_terrain = ModuleTranslateDomain()
        highland_terrain.setSource(ground_gradient)
        highland_terrain.setAxisYSource(highland_y_scale)

        /* mountains */

        val mountain_shape_fractal = ModuleFractal()
        mountain_shape_fractal.setType(ModuleFractal.FractalType.BILLOW)
        mountain_shape_fractal.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        mountain_shape_fractal.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        mountain_shape_fractal.setNumOctaves(6)
        mountain_shape_fractal.setFrequency(0.55)
        mountain_shape_fractal.seed = SEED xor random.nextLong()
        //println(mountain_shape_fractal.seed)

        val mountain_autocorrect = ModuleAutoCorrect()
        mountain_autocorrect.setSource(mountain_shape_fractal)
        mountain_autocorrect.setLow(0.0)
        mountain_autocorrect.setHigh(1.0)

        val mountain_scale = ModuleScaleOffset()
        mountain_scale.setSource(mountain_autocorrect)
        mountain_scale.setScale(1.66)
        mountain_scale.setOffset(-1.25)

        val mountain_y_scale = ModuleScaleDomain()
        mountain_y_scale.setSource(mountain_scale)
        mountain_y_scale.setScaleY(0.1)

        val mountain_terrain = ModuleTranslateDomain()
        mountain_terrain.setSource(ground_gradient)
        mountain_terrain.setAxisYSource(mountain_y_scale)

        /* selection */

        val terrain_type_fractal = ModuleFractal()
        terrain_type_fractal.setType(ModuleFractal.FractalType.MULTI)
        terrain_type_fractal.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT)
        terrain_type_fractal.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        terrain_type_fractal.setNumOctaves(5)
        terrain_type_fractal.setFrequency(0.4) // <= 0.33
        terrain_type_fractal.seed = SEED xor random.nextLong()
        //println(terrain_type_fractal.seed)

        val terrain_autocorrect = ModuleAutoCorrect()
        terrain_autocorrect.setSource(terrain_type_fractal)
        terrain_autocorrect.setLow(0.0)
        terrain_autocorrect.setHigh(1.0)

        val terrain_type_scale = ModuleScaleDomain()
        terrain_type_scale.setScaleY(0.33)
        terrain_type_scale.setSource(terrain_autocorrect)

        val terrain_type_cache = ModuleCache()
        terrain_type_cache.setSource(terrain_type_scale)

        val highland_mountain_select = ModuleSelect()
        highland_mountain_select.setLowSource(highland_terrain)
        highland_mountain_select.setHighSource(mountain_terrain)
        highland_mountain_select.setControlSource(terrain_type_cache)
        highland_mountain_select.setThreshold(0.55)
        highland_mountain_select.setFalloff(0.15)

        val highland_lowland_select = ModuleSelect()
        highland_lowland_select.setLowSource(lowland_terrain)
        highland_lowland_select.setHighSource(highland_mountain_select)
        highland_lowland_select.setControlSource(terrain_type_cache)
        highland_lowland_select.setThreshold(0.25)
        highland_lowland_select.setFalloff(0.15)


        val ground_select = ModuleSelect()
        ground_select.setLowSource(0.0)
        ground_select.setHighSource(1.0)
        ground_select.setThreshold(0.5)
        ground_select.setControlSource(highland_lowland_select)

        val joise = Joise(ground_select)
        // fill the area as Joise map
        for (y in 0..(TERRAIN_UNDULATION - 1)) {
            for (x in 0..WIDTH) {
                val map: Boolean = (
                        joise.get(
                                x / SCALE_X,
                                y / SCALE_Y
                        ) == 1.0)
                noiseMap[y + TERRAIN_AVERAGE_HEIGHT - (TERRAIN_UNDULATION / 2)].set(x, map)
            }
        }
        // fill the area bottom of the above map as 'filled'
        for (y in TERRAIN_AVERAGE_HEIGHT + (TERRAIN_UNDULATION / 2)..HEIGHT - 1) {
            for (x in 0..WIDTH) {
                noiseMap[y].set(x, true)
            }
        }

        return noiseMap
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

     * @param func_x
     * *
     * @return
     */
    private fun getGlacierMountedAmplitude(func_x: Int): Float {
        if (func_x > GLACIER_MOUNTAIN_WIDTH) {
            return 0f
        } else {
            val func_y = GLACIER_MOUNTAIN_HEIGHT / 2f * Math.cos((10 * func_x / (FastMath.PI * GLACIER_MOUNTAIN_WIDTH)).toDouble()).toFloat() + GLACIER_MOUNTAIN_HEIGHT / 2
            return func_y
        }
    }

    private fun placeGlacierMount(heightMap: IntArray) {
        println("[mapgenerator] Putting glacier...")

        // raise
        for (i in heightMap.indices) {
            if (worldOceanPosition == TYPE_OCEAN_RIGHT) {
                heightMap[i] += Math.round(getGlacierMountedAmplitude(i))
            } else {
                heightMap[i] += Math.round(getGlacierMountedAmplitude(heightMap.size - i - 1))
            }
        }
    }

    /**
     * Cosine interpolation between point a and b.
     * @param x [0.0, 1.0] relative position between a and b
     * *
     * @param a leftmost point
     * *
     * @param b rightmost point
     * *
     * @return
     */
    private fun interpolateCosine(x: Float, a: Float, b: Float): Float {
        val ft = x * FastMath.PI
        val f = (1 - FastMath.cos(ft)) * 0.5f

        return a * (1 - f) + b * f
    }

    private fun heightMapToObjectMap(fs: IntArray) {
        println("[mapgenerator] Shaping world as processed...")

        // iterate for heightmap
        for (x in 0..WIDTH - 1) {
            val medianPosition = TERRAIN_AVERAGE_HEIGHT
            val pillarOffset = medianPosition - fs[x]

            // for pillar length
            for (i in 0..HEIGHT - pillarOffset - 1) {

                if (i < DIRT_LAYER_DEPTH) {
                    map.setTileTerrain(x, i + pillarOffset, TileNameCode.DIRT)
                    map.setTileWall(x, i + pillarOffset, TileNameCode.DIRT)
                } else {
                    map.setTileTerrain(x, i + pillarOffset, TileNameCode.STONE)
                    map.setTileWall(x, i + pillarOffset, TileNameCode.STONE)
                }

            }
        }
    }

    private fun terrainMapToObjectMap() {
        println("[mapgenerator] Shaping world as processed...")
        // generate dirt-stone transition line
        // use catmull spline
        val dirtStoneLine = IntArray(WIDTH)
        val POINTS_GAP = 64 // power of two!
        val splineControlPoints = Array((WIDTH / POINTS_GAP) + 1, { Pair(0, 0) })

        // get spline points
        for (x in 0..(WIDTH / POINTS_GAP)) {
            for (y in 0..TERRAIN_AVERAGE_HEIGHT + TERRAIN_UNDULATION) {
                splineControlPoints[x] = Pair(x * POINTS_GAP, y)
                if (terrainMap[y].get(splineControlPoints[x].first)) break
            }
            println("Spline[$x] x: ${splineControlPoints[x].first}, " +
                    "y: ${splineControlPoints[x].second}")
        }

        // do interpolation
        for (x in 0..dirtStoneLine.size) {
            val x_1 = x / POINTS_GAP

            val splineX0 = splineControlPoints[ clamp(x_1 - 1, 0, dirtStoneLine.size / POINTS_GAP) ].first
            val splineX1 = splineControlPoints[x_1].first
            val splineX2 = splineControlPoints[ clamp(x_1 + 1, 0, dirtStoneLine.size / POINTS_GAP) ].first
            val splineX3 = splineControlPoints[ clamp(x_1 + 2, 0, dirtStoneLine.size / POINTS_GAP) ].first

            val splineP0 = splineControlPoints[ clamp(x_1 - 1, 0, dirtStoneLine.size / POINTS_GAP) ].second.toFloat()
            val splineP1 = splineControlPoints[x_1].second.toFloat()
            val splineP2 = splineControlPoints[ clamp(x_1 + 1, 0, dirtStoneLine.size / POINTS_GAP) ].second.toFloat()
            val splineP3 = splineControlPoints[ clamp(x_1 + 2, 0, dirtStoneLine.size / POINTS_GAP) ].second.toFloat()

            if (x in POINTS_GAP - 1..WIDTH - 2 * POINTS_GAP) {
                dirtStoneLine[x] = Math.round(FastMath.interpolateCatmullRom(
                        (x - splineX1) / POINTS_GAP.toFloat(),
                        0.01f,
                        splineP0,
                        splineP1,
                        splineP2,
                        splineP3
                ))
            }
            else {
                interpolateCosine(
                        (x - splineX1) / POINTS_GAP.toFloat(),
                        splineP1,
                        splineP2
                )
            }
        }

        // scan vertically
        for (x in 0..WIDTH - 1) {
            for (y in 0..HEIGHT - 1) {
                if (terrainMap[clamp(y + DIRT_LAYER_DEPTH, 0, HEIGHT - 1)].get(x)) {
                    // map.setTileTerrain(x, y, TileNameCode.DIRT)
                    // map.setTileWall(x, y, TileNameCode.DIRT)
                    val tile =
                            if (y < dirtStoneLine[x]) TileNameCode.DIRT
                            else                      TileNameCode.STONE
                    map.setTileTerrain(x, y, tile)
                    map.setTileWall(x, y, tile)
                }
            }
        }
    }

    /* 2. Carve */

    private fun carveCave(noisemap: Array<FloatArray>, tile: Int, message: String) {
        println("[mapgenerator] " + message)

        for (i in 0..HEIGHT - 1) {
            for (j in 0..WIDTH - 1) {
                if (noisemap[i][j] > 0.9) {
                    map.setTileTerrain(j, i, tile)
                }
            }
        }
    }

    /**
     * Carve (place air block) by noisemap, inversed gradation filter applied.
     * @param map noisemap
     * *
     * @param scarcity higher = larger blob
     * *
     * @param tile
     * *
     * @param message
     */
    private fun carveByMap(noisemap: Array<FloatArray>, scarcity: Float, tile: Int, message: String) {
        println("[mapgenerator] " + message)

        for (i in 0..HEIGHT - 1) {
            for (j in 0..WIDTH - 1) {
                if (noisemap[i][j] > gradientQuadratic(i, noiseGradientStart, noiseGrdCaveEnd) * scarcity) {
                    map.setTileTerrain(j, i, tile)
                }
            }
        }
    }

    /**
     * Fill by noisemap, gradation filter applied.
     * @param map noisemap
     * *
     * @param scarcity higher = larger blob
     * *
     * @param replaceFrom
     * *
     * @param tile
     * *
     * @param message
     */
    private fun fillByMap(noisemap: Array<FloatArray>, scarcity: Float, replaceFrom: Int, tile: Int, message: String) {
        println("[mapgenerator] " + message)

        for (i in 0..HEIGHT - 1) {
            for (j in 0..WIDTH - 1) {
                if (noisemap[i][j] > getNoiseGradient(i, noiseGradientStart, noiseGradientEnd) * scarcity && map.getTileFromTerrain(j, i) == replaceFrom) {
                    map.setTileTerrain(j, i, tile)
                }
            }
        }
    }

    /**
     * Fill by noisemap, inversed gradation filter applied.
     * @param map noisemap
     * *
     * @param scarcity higher = larger blob
     * *
     * @param replaceFrom
     * *
     * @param tile
     * *
     * @param message
     */
    private fun fillByMapInverseGradFilter(noisemap: Array<FloatArray>, scarcity: Float, replaceFrom: Int, tile: Int, message: String) {
        println("[mapgenerator] " + message)

        for (i in 0..HEIGHT - 1) {
            for (j in 0..WIDTH - 1) {
                if (noisemap[i][j] > getNoiseGradientInversed(i, noiseGradientEnd, noiseGradientStart) * scarcity
                        && map.getTileFromTerrain(j, i) == replaceFrom) {
                    map.setTileTerrain(j, i, tile)
                }
            }
        }
    }

    /**
     * Fill by noisemap, no filter applied. Takes
     *
     * noiseGradientStart / scarcity
     * as carving threshold.
     * @param map noisemap
     * *
     * @param scarcity higher = larger blob
     * *
     * @param replaceFrom
     * *
     * @param tile
     * *
     * @param message
     */
    private fun fillByMapNoFilter(noisemap: Array<FloatArray>, scarcity: Float, replaceFrom: Int, tile: Int, message: String) {
        println("[mapgenerator] " + message)

        for (i in 0..HEIGHT - 1) {
            for (j in 0..WIDTH - 1) {
                if (noisemap[i][j] > noiseGradientStart * scarcity && map.getTileFromTerrain(j, i) == replaceFrom) {
                    map.setTileTerrain(j, i, tile)
                }
            }
        }
    }

    private fun fillByMapNoFilterUnderground(noisemap: Array<FloatArray>, scarcity: Float, replaceFrom: Int, replaceTo: Int, message: String) {
        println("[mapgenerator] " + message)

        for (i in 0..HEIGHT - 1) {
            for (j in 0..WIDTH - 1) {
                if (noisemap[i][j] > noiseGradientStart * scarcity
                        && map.getTileFromTerrain(j, i) ?: 0 == replaceFrom
                        && map.getTileFromWall(j, i) ?: 0 == replaceTo
                ) {
                    map.setTileTerrain(j, i, replaceTo)
                }
            }
        }
    }

    private fun fillByMap(noisemap: Array<FloatArray>, scarcity: Float, replaceFrom: Int, tile: IntArray, message: String) {
        println("[mapgenerator] " + message)

        for (i in 0..HEIGHT - 1) {
            for (j in 0..WIDTH - 1) {
                if (noisemap[i][j] > getNoiseGradient(i, noiseGradientStart, noiseGradientEnd) * scarcity && map.getTileFromTerrain(j, i) == replaceFrom) {
                    map.setTileTerrain(j, i, tile[random.nextInt(tile.size)])
                }
            }
        }
    }

    private fun getNoiseGradient(x: Int, start: Float, end: Float): Float {
        return gradientQuadratic(x, start, end)
    }

    private fun getNoiseGradientInversed(x: Int, start: Float, end: Float): Float {
        return gradientMinusQuadratic(x, start, end)
    }

    private fun gradientSqrt(func_argX: Int, start: Float, end: Float): Float {
        val graph_gradient = (end - start) / FastMath.sqrt((HEIGHT - TERRAIN_AVERAGE_HEIGHT).toFloat()) * FastMath.sqrt((func_argX - TERRAIN_AVERAGE_HEIGHT).toFloat()) + start

        if (func_argX < TERRAIN_AVERAGE_HEIGHT) {
            return start
        } else if (func_argX >= HEIGHT) {
            return end
        } else {
            return graph_gradient
        }
    }

    /**
     * Quadratic polynomial
     * (16/9) * (start-end)/height^2 * (x-height)^2 + end
     * 16/9: terrain is formed from 1/4 of height.
     * 1 - (1/4) = 3/4, reverse it and square it.
     * That makes 16/9.

     * Shape:

     * cavity -
     * small
     * -
     * -
     * --
     * ----
     * cavity          --------
     * large                  ----------------

     * @param func_argX
     * *
     * @param start
     * *
     * @param end
     * *
     * @return
     */
    private fun gradientQuadratic(func_argX: Int, start: Float, end: Float): Float {
        val graph_gradient = FastMath.pow(FastMath.sqr((1 - TERRAIN_AVERAGE_HEIGHT).toFloat()), -1f) * // 1/4 -> 3/4 -> 9/16 -> 16/9
                (start - end) / FastMath.sqr(HEIGHT.toFloat()) *
                             FastMath.sqr((func_argX - HEIGHT).toFloat()) + end

        if (func_argX < TERRAIN_AVERAGE_HEIGHT) {
            return start
        } else if (func_argX >= HEIGHT) {
            return end
        } else {
            return graph_gradient
        }
    }

    /**
     * Double Quadratic polynomial
     * (16/9) * (start-end)/height^2 * (x-height)^2 + end
     * 16/9: terrain is formed from 1/4 of height.
     * 1 - (1/4) = 3/4, reverse it and square it.
     * That makes 16/9.

     * Shape:

     * cavity -
     * small
     * -
     * -
     * --
     * ----
     * cavity          --------
     * large                  ----------------

     * @param func_argX
     * *
     * @param start
     * *
     * @param end
     * *
     * @return
     */
    private fun gradientCubic(func_argX: Int, start: Float, end: Float): Float {
        val graph_gradient = -FastMath.pow(FastMath.pow((1 - TERRAIN_AVERAGE_HEIGHT).toFloat(), 3f), -1f) * // 1/4 -> 3/4 -> 9/16 -> 16/9
                (start - end) / FastMath.pow(HEIGHT.toFloat(), 3f) *
                             FastMath.pow((func_argX - HEIGHT).toFloat(), 3f) + end

        if (func_argX < TERRAIN_AVERAGE_HEIGHT) {
            return start
        } else if (func_argX >= HEIGHT) {
            return end
        } else {
            return graph_gradient
        }
    }

    /**
     * Quadratic polynomial
     * -(16/9) * (start-end)/height^2 * (x - 0.25 * height)^2 + start
     * 16/9: terrain is formed from 1/4 of height.
     * 1 - (1/4) = 3/4, reverse it and square it.
     * That makes 16/9.

     * Shape:

     * cavity                                 _
     * small
     * _
     * _
     * __
     * ____
     * cavity                 ________
     * large ________________

     * @param func_argX
     * *
     * @param start
     * *
     * @param end
     * *
     * @return
     */
    private fun gradientMinusQuadratic(func_argX: Int, start: Float, end: Float): Float {
        val graph_gradient = -FastMath.pow(FastMath.sqr((1 - TERRAIN_AVERAGE_HEIGHT).toFloat()), -1f) * // 1/4 -> 3/4 -> 9/16 -> 16/9
                (start - end) / FastMath.sqr(HEIGHT.toFloat()) *
                             FastMath.sqr((func_argX - TERRAIN_AVERAGE_HEIGHT).toFloat()) + start

        if (func_argX < TERRAIN_AVERAGE_HEIGHT) {
            return start
        } else if (func_argX >= HEIGHT) {
            return end
        } else {
            return graph_gradient
        }
    }

    private fun generateFloatingIslands() {
        println("[mapgenerator] Placing floating islands...")

        val nIslandsMax = Math.round(map.width * 6f / 8192f)
        val nIslandsMin = Math.max(2, Math.round(map.width * 4f / 8192f))
        val nIslands = random.nextInt(nIslandsMax - nIslandsMin) + nIslandsMin
        val prevIndex = -1

        val tiles = intArrayOf(TileNameCode.AIR, TileNameCode.STONE, TileNameCode.DIRT, TileNameCode.GRASS)

        for (i in 0..nIslands - 1) {
            var currentIndex = random.nextInt(FloatingIslandsPreset.PRESETS)
            while (currentIndex == prevIndex) {
                currentIndex = random.nextInt(FloatingIslandsPreset.PRESETS)
            }
            val island = FloatingIslandsPreset.generatePreset(currentIndex, random)

            val startingPosX = random.nextInt(map.width - 2048) + 1024
            val startingPosY = minimumFloatingIsleHeight + random.nextInt(minimumFloatingIsleHeight)

            for (j in island.indices) {
                for (k in 0..island[0].size - 1) {
                    if (island[j][k] > 0) {
                        map.setTileTerrain(k + startingPosX, j + startingPosY, tiles[island[j][k]])
                    }
                }
            }
        }
    }

    /* Flood */

    private fun floodBottomLava() {
        println("[mapgenerator] Flooding bottom lava...")
        for (i in HEIGHT * 14 / 15..HEIGHT - 1) {
            for (j in 0..WIDTH - 1) {
                if (map.terrainArray[i][j].toInt() == 0) {
                    map.setTileTerrain(j, i, TileNameCode.LAVA)
                }
            }
        }
    }

    /* Plant */

    private fun plantGrass() {
        println("[mapgenerator] Planting grass...")

        /* TODO composing dirt and stone
		 * over certain level, use background dirt with stone 'peckles'
		 * beetween levels, use background dirt with larger and denser stone peckles.
		 * under another certain level, use background stone with dirt peckles.
		 */

        for (y in TERRAIN_AVERAGE_HEIGHT - TERRAIN_UNDULATION..TERRAIN_AVERAGE_HEIGHT + TERRAIN_UNDULATION - 1) {
            for (x in 0..map.width - 1) {

                val thisTile = map.getTileFromTerrain(x, y)

                for (i in 0..8) {
                    var nearbyWallTile: Int?
                    nearbyWallTile = map.getTileFromWall(x + i % 3 - 1, y + i / 3 - 1)

                    if (nearbyWallTile == null) break;

                    if (i != 4 && thisTile == TileNameCode.DIRT && nearbyWallTile == TileNameCode.AIR) {
                        map.setTileTerrain(x, y, TileNameCode.GRASS)
                        break
                    }
                }
            }
        }

    }

    private fun isGrassOrDirt(x: Int, y: Int): Boolean {
        return map.getTileFromTerrain(x, y) == TileNameCode.GRASS || map.getTileFromTerrain(x, y) == TileNameCode.DIRT
    }

    private fun replaceIfTerrain(ifTileRaw: Int, x: Int, y: Int, replaceTileRaw: Int) {
        if (map.getTileFromTerrain(x, y) == ifTileRaw) {
            map.setTileTerrain(x, y, replaceTileRaw)
        }
    }

    private fun replaceIfWall(ifTileRaw: Int, x: Int, y: Int, replaceTileRaw: Int) {
        if (map.getTileFromWall(x, y) == ifTileRaw) {
            map.setTileWall(x, y, replaceTileRaw)
        }
    }

    /* Post-process */

    private fun fillOcean() {
        val thisSandList = intArrayOf(
                TileNameCode.SAND, TileNameCode.SAND, TileNameCode.SAND, TileNameCode.SAND,
                TileNameCode.SAND_WHITE, TileNameCode.SAND_WHITE, TileNameCode.SAND_WHITE,
                TileNameCode.SAND_BLACK, TileNameCode.SAND_BLACK, TileNameCode.SAND_GREEN
                )
        val thisRand = HQRNG(SEED xor random.nextLong())
        val thisSand = thisSandList[thisRand.nextInt(thisSandList.size)]
        // val thisSand = TileNameCode.SAND_GREEN

        val thisSandStr = if (thisSand == TileNameCode.SAND_BLACK)
            "black"
        else if (thisSand == TileNameCode.SAND_GREEN)
            "green"
        else if (thisSand == TileNameCode.SAND)
            "yellow"
        else
            "white"
        println("[mapgenerator] Beach sand type: $thisSandStr")

        var ix = 0
        while (ix < OCEAN_WIDTH * 1.5) {
            //flooding
            if (ix < OCEAN_WIDTH) {
                if (worldOceanPosition == TYPE_OCEAN_LEFT) {
                    for (y in getTerrainHeightFromHeightMap(OCEAN_WIDTH)..getTerrainHeightFromHeightMap(ix) - 1) {
                        map.setTileTerrain(ix, y, TileNameCode.WATER)
                    }
                } else if (worldOceanPosition == TYPE_OCEAN_RIGHT) {
                    for (y in getTerrainHeightFromHeightMap(map.width - 1 - OCEAN_WIDTH)..getTerrainHeightFromHeightMap(map.width - 1 - ix) - 1) {
                        map.setTileTerrain(map.width - 1 - ix, y, TileNameCode.WATER)
                    }
                }
            }
            //sand
            // linearly increase thickness of the sand sheet
            for (iy in 0..40 - ix * 40 / (OCEAN_WIDTH + SHORE_WIDTH) - 1) {
                if (worldOceanPosition == TYPE_OCEAN_LEFT) {
                    val terrainPoint = getTerrainHeightFromHeightMap(ix)


                    map.setTileTerrain(ix, terrainPoint + iy, thisSand)
                    // clear grass and make the sheet thicker
                    map.setTileTerrain(ix, terrainPoint + iy - 1, thisSand)
                } else if (worldOceanPosition == TYPE_OCEAN_RIGHT) {
                    val terrainPoint = getTerrainHeightFromHeightMap(map.width - 1 - ix)

                    map.setTileTerrain(map.width - 1 - ix, terrainPoint + iy, thisSand)
                    // clear grass and make the sheet thicker
                    map.setTileTerrain(map.width - 1 - ix, terrainPoint + iy - 1, thisSand)
                }
            }
            ix++
        }
    }

    private fun freeze() {
        for (y in 0..map.height - 1 - 1) {
            for (x in 0..getFrozenAreaWidth(y) - 1) {
                if (worldOceanPosition == TYPE_OCEAN_RIGHT) {
                    replaceIfTerrain(TileNameCode.DIRT, x, y, TileNameCode.SNOW)
                    replaceIfTerrain(TileNameCode.STONE, x, y, TileNameCode.ICE_NATURAL)

                    replaceIfWall(TileNameCode.DIRT, x, y, TileNameCode.SNOW)
                    replaceIfWall(TileNameCode.STONE, x, y, TileNameCode.ICE_NATURAL)
                } else {
                    replaceIfTerrain(TileNameCode.DIRT, map.width - 1 - x, y, TileNameCode.SNOW)
                    replaceIfTerrain(TileNameCode.STONE, map.width - 1 - x, y, TileNameCode.ICE_NATURAL)

                    replaceIfWall(TileNameCode.DIRT, map.width - 1 - x, y, TileNameCode.SNOW)
                    replaceIfWall(TileNameCode.STONE, map.width - 1 - x, y, TileNameCode.ICE_NATURAL)
                }
            }
        }
    }

    /**

     * @return width of the frozen area for mapgenerator.freeze
     */
    private fun getFrozenAreaWidth(y: Int): Int {
        val randDeviation = 7
        // narrower that the actual width
        val width = Math.round(GLACIER_MOUNTAIN_WIDTH * 0.625f)
        val height: Int
        if (worldOceanPosition == TYPE_OCEAN_RIGHT) {
            height = getTerrainHeightFromHeightMap(width)
        } else {
            height = getTerrainHeightFromHeightMap(map.width - 1 - width)
        }
        val k = width / FastMath.sqrt(height.toFloat())

        if (y < height) {
            // ground
            return width
        } else {
            // underground
            return Math.round(
                    k * FastMath.sqrt(y.toFloat()) + (random.nextInt(3) - 1))
        }
    }

    /**

     * @param x position of heightmap
     * *
     * @return
     */
    private fun getTerrainHeightFromHeightMap(x: Int): Int {
        TODO()
    }


    fun getGeneratorSeed(): Long {
        return SEED
    }

    /* Utility */

    private fun clampN(clampNumber: Int, num: Int): Int {
        return FastMath.floor((num / clampNumber).toFloat()) * clampNumber
    }

    private fun outOfBound(w: Int, h: Int, x: Int, y: Int): Boolean {
        return !(x > 0 && y > 0 && x < w && y < h)
    }

    private fun getDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return FastMath.sqrt(FastMath.pow(x1 - x2, 2f) + FastMath.pow(y2 - y1, 2f))
    }

    private fun circularDig(i: Int, j: Int, brushSize: Int, fillFrom: Int, fill: Int) {
        val halfBrushSize = brushSize * 0.5f

        for (pointerY in 0..brushSize - 1) {
            for (pointerX in 0..brushSize - 1) {
                if (getDistance(j.toFloat(), i.toFloat(), j + pointerX - halfBrushSize, i + pointerY - halfBrushSize) <= FastMath.floor((brushSize / 2).toFloat()) - 1) {
                    if (Math.round(j + pointerX - halfBrushSize) > brushSize
                        && Math.round(j + pointerX - halfBrushSize) < WIDTH - brushSize
                        && Math.round(i + pointerY - halfBrushSize) > brushSize
                        && Math.round(i + pointerY - halfBrushSize) < HEIGHT - brushSize) {
                        if (map.terrainArray[Math.round(i + pointerY - halfBrushSize)][Math.round(j + pointerX - halfBrushSize)] == fillFrom.toByte()) {
                            map.terrainArray[Math.round(i + pointerY - halfBrushSize)][Math.round(j + pointerX - halfBrushSize)] = fill.toByte()
                        }
                    }
                }
            }
        }
    }

    private fun clamp(x: Int, min: Int, max: Int): Int = if (x < min) min else if (x > max) max else x

}