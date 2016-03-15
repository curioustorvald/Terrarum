package com.Torvald.Terrarum.MapGenerator

import com.Torvald.Rand.HQRNG
import com.Torvald.Terrarum.GameMap.GameMap
import com.Torvald.Terrarum.GameMap.MapLayer
import com.Torvald.Terrarum.TileProperties.TileNameCode
import com.jme3.math.FastMath
import java.util.*

/**
 * Created by minjaesong on 16-03-15.
 */
object MapGenerator {

    private lateinit var map: GameMap
    private lateinit var random: Random
    //private static float[] noiseArray;
    private var seed: Long = 0
    private var width: Int = 0
    private var height: Int = 0

    private lateinit var heightMap: IntArray

    private var dirtThickness: Int = 0
    private var TERRAIN_AVERAGE_HEIGHT: Int = 0
    private var minimumFloatingIsleHeight: Int = 0

    private val noiseGradientStart = 0.67f
    private val noiseGradientEnd = 0.56f
    private val noiseGrdCaveEnd = 0.54f

    private val HILL_WIDTH = 256 // power of two!
    private val MAX_HILL_HEIGHT = 100

    private val CAVE_LARGEST_FEATURE = 200

    private var OCEAN_WIDTH = 400
    private var SHORE_WIDTH = 120
    private val MAX_OCEAN_DEPTH = 200

    private val TERRAIN_PERTURB_OFFSETMAX = 0 // [-val , val]
    private val TERRAIN_PERTURB_LARGESTFEATURE = 256
    private val TERRAIN_PERTURB_RATE = 0.5f

    private var GLACIER_MOUNTAIN_WIDTH = 900
    private val GLACIER_MOUNTAIN_HEIGHT = 300

    private val CAVEGEN_PERTURB_RATE = 0.37f
    private val CAVEGEN_PERTURB2_RATE = 0.25f

    private val CAVEGEN_THRE_START = 0.87f
    private val CAVEGEN_THRE_END = 0.67f

    private val CAVEGEN_LARGEST_FEATURE = 256
    private val CAVEGEN_LARGEST_FEATURE_PERTURB = 128

    private var worldOceanPosition: Int? = null
    private val TYPE_OCEAN_LEFT = 0
    private val TYPE_OCEAN_RIGHT = 1

    private val GRASSCUR_UP = 0
    private val GRASSCUR_RIGHT = 1
    private val GRASSCUR_DOWN = 2
    private val GRASSCUR_LEFT = 3

    @JvmStatic
    fun attachMap(map: GameMap) {
        this.map = map
        width = map.width
        height = map.height

        val widthMulFactor = width / 8192f

        dirtThickness = (100 * height / 1024f).toInt()
        minimumFloatingIsleHeight = (25 * (height / 1024f)).toInt()
        TERRAIN_AVERAGE_HEIGHT = height / 4

        OCEAN_WIDTH = Math.round(OCEAN_WIDTH * widthMulFactor)
        SHORE_WIDTH = Math.round(SHORE_WIDTH * widthMulFactor)
        GLACIER_MOUNTAIN_WIDTH = Math.round(GLACIER_MOUNTAIN_WIDTH * widthMulFactor)
    }

    @JvmStatic
    fun setSeed(seed: Long) {
        this.seed = seed
    }

    /**
     * Generate terrain and override attached map
     */
    @JvmStatic
    fun generateMap() {
        random = HQRNG(seed)
        println("[MapGenerator] Seed: " + seed)

        worldOceanPosition = if (random.nextBoolean()) TYPE_OCEAN_LEFT else TYPE_OCEAN_RIGHT

        heightMap = raise2(MAX_HILL_HEIGHT / 2)
        generateOcean(heightMap)
        placeGlacierMount(heightMap)
        heightMapToObjectMap(heightMap)

        perturbTerrain()

        /**
         * Todo: more perturbed overworld (harder to supra-navigate)
         * Todo: veined ore distribution (metals) -- use veined simplex noise
         * Todo: clustered gem distribution (Groups: [Ruby, Sapphire], Amethyst, Yellow topaz, emerald, diamond) -- use regular simplex noise
         * Todo: Lakes! Aquifers! Lava chambers!
         * Todo: deserts (variants: SAND_DESERT, SAND_RED)
         * Todo: volcano(es?)
         * Done: variants of beach (SAND_BEACH, SAND_BLACK, SAND_GREEN)
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
        freeze()
        fillOcean()
        plantGrass()

        //post-process
        generateFloatingIslands()

        //wire layer
        for (i in 0..height - 1) {
            for (j in 0..width - 1) {
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
        val noiseMap = Array(height) { FloatArray(width) }

        val simplexNoise = SimplexNoise(CAVEGEN_LARGEST_FEATURE, CAVEGEN_PERTURB_RATE, seed)
        val simplexNoisePerturbMap = SimplexNoise(CAVEGEN_LARGEST_FEATURE_PERTURB, 0.5f, seed xor random.nextLong())

        val xEnd = width * yStretch
        val yEnd = height * xStretch

        var lowestNoiseVal = 10000f
        var highestNoiseVal = -10000f

        for (y in 0..height - 1) {
            for (x in 0..width - 1) {
                val ny = (y * (xEnd / width)).toInt()
                val nx = (x * (yEnd / height)).toInt()

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

        for (y in 0..height - 1) {
            for (x in 0..width - 1) {
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
        return generate2DSimplexNoise(width, height, xStretch, yStretch)
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
        val simplexNoise = SimplexNoise(CAVE_LARGEST_FEATURE, 0.1f, seed xor random.nextLong())

        val xStart = 0f
        val yStart = 0f

        /** higher = denser.
         * Recommended: (width or height) * 3
         */
        val xEnd = width * yStretch
        val yEnd = height * xStretch

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

        for (y in 0..height - 1) {
            for (x in 0..width - 1) {
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
     * [http://freespace.virgin.net/hugo.elias/models/m_perlin.htm](null)
     * @param maxval_div_2 max height (deviation from zero) divided by two.
     * *
     * @return noise array with range of [-maxval, maxval]
     */
    private fun raise2(maxval_div_2: Int): IntArray {

        val finalPerlinAmp = maxval_div_2 // 1 + 1/2 + 1/4 + 1/8 + ... == 2
        val perlinOctaves = FastMath.intLog2(maxval_div_2) + 1 - 1 // max: for every 2nd node

        val perlinMap = IntArray(width) // [-2 * finalPerlinAmp, finalPerlinAmp]

        // assert
        if (HILL_WIDTH.ushr(perlinOctaves - 1) == 0) {
            throw RuntimeException("sample width of zero detected.")
        }

        // sample noise and add
        for (oct in 1..perlinOctaves) {
            // perlinAmp: 16364 -> 8192 -> 4096 -> 2048 -> ...
            // This applies persistence of 1/2
            val perlinAmp = finalPerlinAmp.ushr(oct - 1)

            val perlinSampleDist = HILL_WIDTH.ushr(oct - 1)

            // sample first
            val perlinSamples = IntArray(width / perlinSampleDist + 1)
            for (sample in perlinSamples.indices) {
                perlinSamples[sample] = random.nextInt(perlinAmp * 2) - perlinAmp
            }

            // add interpolated value to map
            for (i in perlinMap.indices) {
                val perlinPointLeft = perlinSamples[i / perlinSampleDist]
                val perlinPointRight = perlinSamples[i / perlinSampleDist + 1]

                perlinMap[i] += Math.round(
                        interpolateCosine(
                                (i % perlinSampleDist).toFloat() / perlinSampleDist, perlinPointLeft.toFloat(), perlinPointRight.toFloat()))// using cosine; making tops rounded
            }
        }

        for (k in 0..0) {
            for (i in perlinMap.indices) {
                // averaging smoothing
                if (i > 1 && i < perlinMap.size - 2) {
                    perlinMap[i] = Math.round(
                            ((perlinMap[i - 1] + perlinMap[i + 1]) / 2).toFloat())
                }
            }
        }

        // single bump removal
        for (i in perlinMap.indices) {
            if (i > 1 && i < perlinMap.size - 2) {
                val p1 = perlinMap[i - 1]
                val p2 = perlinMap[i]
                val p3 = perlinMap[i + 1]
                //  _-_ / -_- -> ___ / ---
                if (p1 == p3 && p1 != p2) {
                    perlinMap[i] = p1
                } else if (p1 > p3 && p2 > p1) {
                    perlinMap[i] = p1
                } else if (p3 > p1 && p2 > p3) {
                    perlinMap[i] = p3
                } else if (p3 > p1 && p2 < p1) {
                    perlinMap[i] = p1
                } else if (p1 > p3 && p2 < p3) {
                    perlinMap[i] = p1
                }// ^_- -> ^--
                // -_^ -> --^
                // _^- -> _--
                // -^_ -> --_
            }
        }

        return perlinMap
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
        println("[MapGenerator] Putting glacier...")

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
        println("[MapGenerator] Shaping world as processed...")

        // iterate for heightmap
        for (x in 0..width - 1) {
            val medianPosition = TERRAIN_AVERAGE_HEIGHT
            val pillarOffset = medianPosition - fs[x]

            // for pillar length
            for (i in 0..height - pillarOffset - 1) {

                if (i < dirtThickness) {
                    map.setTileTerrain(x, i + pillarOffset, TileNameCode.DIRT)
                    map.setTileWall(x, i + pillarOffset, TileNameCode.DIRT)
                } else {
                    map.setTileTerrain(x, i + pillarOffset, TileNameCode.STONE)
                    map.setTileWall(x, i + pillarOffset, TileNameCode.STONE)
                }

            }
        }
    }

    private fun perturbTerrain() {
        val perturbGen = SimplexNoise(TERRAIN_PERTURB_LARGESTFEATURE, TERRAIN_PERTURB_RATE, seed xor random.nextLong())

        val perturbMap = Array(height) { FloatArray(width) }

        val layerWall = map.wallArray
        val layerTerrain = map.terrainArray
        val newLayerWall = MapLayer(width, height)
        val newLayerTerrain = MapLayer(width, height)

        var lowestNoiseVal = 10000f
        var highestNoiseVal = -10000f

        for (y in 0..map.height - 1) {
            for (x in 0..map.width - 1) {
                val noise = perturbGen.getNoise(x, y) // [-1, 1]
                perturbMap[y][x] = noise
                if (noise < lowestNoiseVal) lowestNoiseVal = noise
                if (noise > highestNoiseVal) highestNoiseVal = noise
            }
        }

        // Auto-scale noise [-1, 1]
        /**
         * See ./work_files/Linear_autoscale.gcx
         */
        for (y in 0..height - 1) {
            for (x in 0..width - 1) {
                val noiseInit = perturbMap[y][x]
                val noiseFin = (noiseInit - (highestNoiseVal + lowestNoiseVal) / 2f) * (2f / (highestNoiseVal - lowestNoiseVal))

                perturbMap[y][x] = noiseFin
            }
        }

        // Perturb to x-axis, apply to newLayer
        for (y in 0..height - 1) {
            for (x in 0..width - 1) {
                val offsetOrigin = perturbMap[y][x] * 0.5f + 0.5f // [0 , 1]
                val offset = Math.round(offsetOrigin * TERRAIN_PERTURB_OFFSETMAX)

                val tileWall = layerWall[y][x]
                val tileTerrain = layerTerrain[y][x]

                try {
                    //newLayerWall.setTile(x + offset, y, tileWall);
                    //newLayerTerrain.setTile(x + offset, y, tileTerrain);
                    //layerWall[y][x] = 0;
                    //layerTerrain[y][x] = 0;
                    layerWall[y - offset][x] = tileWall
                    layerTerrain[y - offset][x] = tileTerrain
                } catch (e: ArrayIndexOutOfBoundsException) {
                }

            }
        }

        // set reference (pointer) of original map layer to new layers
        //map.overwriteLayerWall(newLayerWall);
        //map.overwriteLayerTerrain(newLayerTerrain);

    }

    /* 2. Carve */

    private fun carveCave(noisemap: Array<FloatArray>, tile: Int, message: String) {
        println("[MapGenerator] " + message)

        for (i in 0..height - 1) {
            for (j in 0..width - 1) {
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
        println("[MapGenerator] " + message)

        for (i in 0..height - 1) {
            for (j in 0..width - 1) {
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
        println("[MapGenerator] " + message)

        for (i in 0..height - 1) {
            for (j in 0..width - 1) {
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
        println("[MapGenerator] " + message)

        for (i in 0..height - 1) {
            for (j in 0..width - 1) {
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
        println("[MapGenerator] " + message)

        for (i in 0..height - 1) {
            for (j in 0..width - 1) {
                if (noisemap[i][j] > noiseGradientStart * scarcity && map.getTileFromTerrain(j, i) == replaceFrom) {
                    map.setTileTerrain(j, i, tile)
                }
            }
        }
    }

    private fun fillByMapNoFilterUnderground(noisemap: Array<FloatArray>, scarcity: Float, replaceFrom: Int, tile: Int, message: String) {
        println("[MapGenerator] " + message)

        for (i in 0..height - 1) {
            for (j in 0..width - 1) {
                if (noisemap[i][j] > noiseGradientStart * scarcity
                        && map.getTileFromTerrain(j, i) == replaceFrom
                        && map.getTileFromWall(j, i) == TileNameCode.STONE) {
                    map.setTileTerrain(j, i, tile)
                }
            }
        }
    }

    private fun fillByMap(noisemap: Array<FloatArray>, scarcity: Float, replaceFrom: Int, tile: IntArray, message: String) {
        println("[MapGenerator] " + message)

        for (i in 0..height - 1) {
            for (j in 0..width - 1) {
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
        val graph_gradient = (end - start) / FastMath.sqrt((height - TERRAIN_AVERAGE_HEIGHT).toFloat()) * FastMath.sqrt((func_argX - TERRAIN_AVERAGE_HEIGHT).toFloat()) + start

        if (func_argX < TERRAIN_AVERAGE_HEIGHT) {
            return start
        } else if (func_argX >= height) {
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
                (start - end) / FastMath.sqr(height.toFloat()) *
                FastMath.sqr((func_argX - height).toFloat()) + end

        if (func_argX < TERRAIN_AVERAGE_HEIGHT) {
            return start
        } else if (func_argX >= height) {
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
                (start - end) / FastMath.pow(height.toFloat(), 3f) *
                FastMath.pow((func_argX - height).toFloat(), 3f) + end

        if (func_argX < TERRAIN_AVERAGE_HEIGHT) {
            return start
        } else if (func_argX >= height) {
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
        val graph_gradient = -FastMath.pow(FastMath.sqr((1 - TERRAIN_AVERAGE_HEIGHT).toFloat()), -1f) *// 1/4 -> 3/4 -> 9/16 -> 16/9
                (start - end) / FastMath.sqr(height.toFloat()) *
                FastMath.sqr((func_argX - TERRAIN_AVERAGE_HEIGHT).toFloat()) + start

        if (func_argX < TERRAIN_AVERAGE_HEIGHT) {
            return start
        } else if (func_argX >= height) {
            return end
        } else {
            return graph_gradient
        }
    }

    private fun generateFloatingIslands() {
        println("[MapGenerator] Placing floating islands...")

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
        println("[MapGenerator] Flooding bottom lava...")
        for (i in height * 14 / 15..height - 1) {
            for (j in 0..width - 1) {
                if (map.terrainArray[i][j].toInt() == 0) {
                    map.setTileTerrain(j, i, TileNameCode.LAVA)
                }
            }
        }
    }

    /* Plant */

    private fun plantGrass() {
        println("[MapGenerator] Planting grass...")

        /* TODO composing dirt and stone
		 * over certain level, use background dirt with stone 'peckles'
		 * beetween levels, use background dirt with larger and denser stone peckles.
		 * under another certain level, use background stone with dirt peckles.
		 */

        for (y in TERRAIN_AVERAGE_HEIGHT - MAX_HILL_HEIGHT..TERRAIN_AVERAGE_HEIGHT + MAX_HILL_HEIGHT - 1) {
            for (x in 0..map.width - 1) {

                val thisTile = map.getTileFromTerrain(x, y)

                for (i in 0..8) {
                    var nearbyWallTile = -1
                    try {
                        nearbyWallTile = map.getTileFromWall(x + i % 3 - 1, y + i / 3 - 1)
                    } catch (e: ArrayIndexOutOfBoundsException) {
                    }

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
        val thisSandList = intArrayOf(TileNameCode.SAND_BEACH, TileNameCode.SAND_BLACK, TileNameCode.SAND_GREEN, TileNameCode.SAND_BEACH, TileNameCode.SAND_BEACH, TileNameCode.SAND_BLACK)
        val thisRand = HQRNG(seed xor random.nextLong())
        val thisSand = thisSandList[thisRand.nextInt(thisSandList.size)]

        val thisSandStr = if (thisSand == TileNameCode.SAND_BLACK)
            "black"
        else if (thisSand == TileNameCode.SAND_GREEN)
            "green"
        else
            "white"
        println("[MapGenerator] Beach sand type: " + thisSandStr)

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

     * @return width of the frozen area for MapGenerator.freeze
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
        return TERRAIN_AVERAGE_HEIGHT - heightMap!![x]
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
                            && Math.round(j + pointerX - halfBrushSize) < width - brushSize
                            && Math.round(i + pointerY - halfBrushSize) > brushSize
                            && Math.round(i + pointerY - halfBrushSize) < height - brushSize) {
                        if (map.terrainArray[Math.round(i + pointerY - halfBrushSize)][Math.round(j + pointerX - halfBrushSize)] == fillFrom.toByte()) {
                            map.terrainArray[Math.round(i + pointerY - halfBrushSize)][Math.round(j + pointerX - halfBrushSize)] = fill.toByte()
                        }
                    }
                }
            }
        }
    }

}