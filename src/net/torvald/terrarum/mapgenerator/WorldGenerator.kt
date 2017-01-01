package net.torvald.terrarum.mapgenerator

import net.torvald.random.HQRNG
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.tileproperties.Tile
import com.jme3.math.FastMath
import com.sudoplay.joise.Joise
import com.sudoplay.joise.module.*
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.concurrent.ThreadParallel
import net.torvald.terrarum.gameactors.ThreadActorUpdate
import net.torvald.terrarum.gameactors.roundInt
import java.util.*

object WorldGenerator {

    internal lateinit var world: GameWorld
    internal lateinit var random: Random
    //private static float[] noiseArray;
    var SEED: Long = 0
        set(value) {
            field = value
            world.generatorSeed = value
        }
    var WIDTH: Int = 0
    var HEIGHT: Int = 0

    //private lateinit var heightMap: IntArray
    private lateinit var terrainMap: Array<BitSet>

    var DIRT_LAYER_DEPTH: Int = 0
    var TERRAIN_AVERAGE_HEIGHT: Int = 0
    private var minimumFloatingIsleHeight: Int = 0

    private val NOISE_GRAD_START = 0.67f
    private val NOISE_GRAD_END = 0.56f

    private val NOISE_SIMPLEX_ORE_START = 1.42f
    private val NOISE_SIMPLEX_ORE_END = 1.28f

    private val HILL_WIDTH = 256 // power of two!
    //private val MAX_HILL_HEIGHT = 100
    private val TERRAIN_UNDULATION = 250

    private val SIMPLEXGEN_LARGEST_FEATURE = 200

    private var OCEAN_WIDTH = 400
    private var SHORE_WIDTH = 120
    private val MAX_OCEAN_DEPTH = 200

    private var GLACIER_MOUNTAIN_WIDTH = 900
    private val GLACIER_MOUNTAIN_HEIGHT = 300

    private val CAVEGEN_THRE_START = 0.95f
    private val CAVEGEN_THRE_END = 0.67f


    private var worldOceanPosition: Int = -1
    private val TYPE_OCEAN_LEFT = 0
    private val TYPE_OCEAN_RIGHT = 1

    private val GRASSCUR_UP = 0
    private val GRASSCUR_RIGHT = 1
    private val GRASSCUR_DOWN = 2
    private val GRASSCUR_LEFT = 3

    internal val TILE_MACRO_ALL = -1

    fun attachMap(world: GameWorld) {
        this.world = world
        WIDTH = world.width
        HEIGHT = world.height

        val widthMulFactor = WIDTH / 8192f

        DIRT_LAYER_DEPTH = (100 * HEIGHT / 1024f).toInt()
        minimumFloatingIsleHeight = (25 * (HEIGHT / 1024f)).toInt()
        TERRAIN_AVERAGE_HEIGHT = HEIGHT / 3

        OCEAN_WIDTH = Math.round(OCEAN_WIDTH * widthMulFactor)
        SHORE_WIDTH = Math.round(SHORE_WIDTH * widthMulFactor)
        GLACIER_MOUNTAIN_WIDTH = Math.round(GLACIER_MOUNTAIN_WIDTH * widthMulFactor)
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


        fillMapByNoiseMap()

        /**
         * Done: more perturbed overworld (harder to supra-navigate)
         * Done: veined ore distribution (metals) -- use veined simplex noise
         * Done: clustered gem distribution (clusters: [Ruby, Sapphire], Amethyst, Yellow topaz, emerald, diamond) -- use regular simplex noise
         * Todo: Lakes! Aquifers! Lava chambers!
         * Todo: deserts (variants: SAND_DESERT, SAND_RED)
         * Todo: volcano(es?)
         * Done: variants of beach (SAND, SAND_BEACH, SAND_BLACK, SAND_GREEN)
         */

        val noiseArray = arrayOf(
                  TaggedJoise("Carving caves", noiseRidged(1.7f, 1.4f), 1f, TILE_MACRO_ALL, TILE_MACRO_ALL, Tile.AIR, NoiseFilterSqrt, CAVEGEN_THRE_START, CAVEGEN_THRE_END)
                , TaggedJoise("Collapsing caves", noiseBlobs(0.5f, 0.5f), 0.3f, Tile.AIR, Tile.STONE, Tile.STONE, NoiseFilterUniform)
//
                //, TaggedJoise("Putting stone patches on the ground", noiseBlobs(0.8f, 0.8f), 1.02f, intArrayOf(Tile.DIRT, Tile.GRASS), Tile.DIRT, Tile.STONE, NoiseFilterQuadratic, NOISE_GRAD_END, NOISE_GRAD_START)
                //, TaggedJoise("Placing dirt spots in the cave", noiseBlobs(0.5f, 0.5f), 0.98f, Tile.STONE, Tile.STONE, Tile.DIRT, NoiseFilterQuadratic, NOISE_GRAD_END, NOISE_GRAD_START)
                //, TaggedJoise("Quarrying some stone into gravels", noiseBlobs(0.5f, 0.5f), 0.98f, Tile.STONE, Tile.STONE, Tile.GRAVEL, NoiseFilterQuadratic, NOISE_GRAD_END, NOISE_GRAD_START)
//
                //, TaggedJoise("Growing copper veins", noiseRidged(1.7f, 1.7f), 1.68f, Tile.STONE, Tile.STONE, Tile.ORE_COPPER)
                //, TaggedJoise("Cutting copper veins", noiseBlobs(0.4f, 0.4f), 0.26f, Tile.ORE_COPPER, Tile.STONE, Tile.STONE)
//
                //, TaggedJoise("Growing iron veins", noiseRidged(1.7f, 1.7f), 1.68f, Tile.STONE, Tile.STONE, Tile.ORE_IRON)
                //, TaggedJoise("Cutting iron veins", noiseBlobs(0.7f, 0.7f), 0.26f, Tile.ORE_IRON, Tile.STONE, Tile.STONE)
//
                //, TaggedJoise("Growing silver veins", noiseRidged(1.7f, 1.7f), 1.71f, Tile.STONE, Tile.STONE, Tile.ORE_SILVER)
                //, TaggedJoise("Cutting silver veins", noiseBlobs(0.7f, 0.7f), 0.26f, Tile.ORE_SILVER, Tile.STONE, Tile.STONE)
//
                //, TaggedJoise("Growing gold veins", noiseRidged(1.7f, 1.7f), 1.73f, Tile.STONE, Tile.STONE, Tile.ORE_GOLD)
                //, TaggedJoise("Cutting gold veins", noiseBlobs(0.7f, 0.7f), 0.26f, Tile.ORE_GOLD, Tile.STONE, Tile.STONE)
//              //  FIXME gem clusters are too large
                //, TaggedJoise("Growing topaz clusters", noiseBlobs(0.9f, 0.9f), 2f, Tile.STONE, Tile.STONE, Tile.RAW_TOPAZ)
                //, TaggedJoise("Growing aluminium oxide clusters", noiseBlobs(0.9f, 0.9f), 1.7f, Tile.STONE, Tile.STONE, intArrayOf(Tile.RAW_RUBY, Tile.RAW_SAPPHIRE))
                //, TaggedJoise("Growing emerald clusters", noiseBlobs(0.9f, 0.9f), 1.7f, Tile.STONE, Tile.STONE, Tile.RAW_EMERALD)
                //, TaggedJoise("Growing hearts of white", noiseBlobs(0.9f, 0.9f), 1.83f, Tile.STONE, Tile.STONE, Tile.RAW_DIAMOND)
                //, TaggedJoise("Growing hearts of violet", noiseRidged(2.5f, 2.5f), 1.75f, Tile.STONE, Tile.STONE, Tile.RAW_AMETHYST)
//
                //, TaggedJoise("Cutting over-grown hearts", noiseBlobs(0.7f, 0.7f), 0.17f, Tile.RAW_AMETHYST, Tile.STONE, Tile.STONE)
        )
        processNoiseLayers(noiseArray)

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
                world.wireArray[i][j] = 0
            }
        }

        // Free some memories
        System.gc()
    }

    /* 1. Raise */

    private fun noiseRidged(xStretch: Float, yStretch: Float): Joise {
        val ridged = ModuleFractal()
        ridged.setType(ModuleFractal.FractalType.RIDGEMULTI)
        ridged.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.QUINTIC)
        ridged.setNumOctaves(4)
        ridged.setFrequency(1.0)
        ridged.seed = SEED xor random.nextLong()

        val ridged_autocorrect = ModuleAutoCorrect()
        ridged_autocorrect.setRange(0.0, 1.0)
        ridged_autocorrect.setSource(ridged)

        val ridged_scale = ModuleScaleDomain()
        ridged_scale.setScaleX(xStretch.toDouble())
        ridged_scale.setScaleY(yStretch.toDouble())
        ridged_scale.setSource(ridged_autocorrect)

        return Joise(ridged_scale)
    }

    private fun noiseBlobs(xStretch: Float, yStretch: Float): Joise {
        val gradval = ModuleBasisFunction()
        gradval.seed = SEED xor random.nextLong()
        gradval.setType(ModuleBasisFunction.BasisType.GRADVAL)
        gradval.setInterpolation(ModuleBasisFunction.InterpolationType.QUINTIC)

        val gradval_scale = ModuleScaleDomain()
        gradval_scale.setScaleX(1.0 / xStretch)
        gradval_scale.setScaleY(1.0 / yStretch)
        gradval_scale.setSource(gradval)

        return Joise(gradval_scale)
    }

    /**
     * Note:
     * * Threshold 1.4 for rarer gem clusters, 1.35 for ores
     */
    private fun noiseSimplex(xStretch: Float, yStretch: Float): Joise {
        val simplex = ModuleFractal()
        simplex.seed = SEED
        simplex.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.SIMPLEX)
        simplex.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.LINEAR)
        simplex.setNumOctaves(2)
        simplex.setFrequency(1.0)

        val simplex_scale = ModuleScaleDomain()
        simplex_scale.setScaleX(1.0 / xStretch)
        simplex_scale.setScaleY(1.0 / yStretch)
        simplex_scale.setSource(simplex)

        return Joise(simplex_scale)
    }

    private fun generateOcean(noiseArrayLocal: IntArray): IntArray {
        val oceanLeftP1 = noiseArrayLocal[OCEAN_WIDTH]
        val oceanRightP1 = noiseArrayLocal[noiseArrayLocal.size - OCEAN_WIDTH]

        /**
         * Add ocean so that:

         * +1|       -   -
         *  0|      -  --  ...
         * -1|______  -

         * interpolated to

         * +1|        -   -
         *  0|   _---  --  ...
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

    val TWO_PI = Math.PI * 2.0

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
        lowland_autocorrect.setRange(0.0, 1.0)
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
        highland_autocorrect.setRange(0.0, 1.0)

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
        mountain_autocorrect.setRange(0.0, 1.0)

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
        terrain_autocorrect.setRange(0.0, 1.0)

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
        println("[mapgenerator] Raising and eroding terrain...")
        for (y in 0..(TERRAIN_UNDULATION - 1)) {
            for (x in 0..WIDTH) {
                // straight-line sampling
                /*val map: Boolean = (
                        joise.get(
                                x / SCALE_X,
                                y / SCALE_Y
                        ) == 1.0)*/
                // circular sampling
                // Mapping function:
                //      World(x, y) -> Joise(sin x, y, cos x)
                val sampleTheta = (x.toDouble() / WIDTH) * TWO_PI
                val sampleOffset = (WIDTH / SCALE_X) / 4.0
                val sampleX = Math.sin(sampleTheta) * sampleOffset + sampleOffset // plus sampleOffset to make only
                val sampleZ = Math.cos(sampleTheta) * sampleOffset + sampleOffset // positive points are to be sampled
                val sampleY = y / SCALE_Y
                val map: Boolean = (
                        joise.get(sampleX, sampleY, sampleZ) == 1.0
                                   )
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
                    world.setTileTerrain(x, i + pillarOffset, Tile.DIRT)
                    world.setTileWall(x, i + pillarOffset, Tile.DIRT)
                } else {
                    world.setTileTerrain(x, i + pillarOffset, Tile.STONE)
                    world.setTileWall(x, i + pillarOffset, Tile.STONE)
                }

            }
        }
    }

    private fun fillMapByNoiseMap() {
        println("[mapgenerator] Shaping world...")
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
            // println("Spline[$x] x: ${splineControlPoints[x].first}, " +
            //         "y: ${splineControlPoints[x].second}")
        }

        // do interpolation
        for (x in 0..dirtStoneLine.size - 1) {
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
                        -0.3f,//0.01f,
                        splineP0,
                        splineP1,
                        splineP2,
                        splineP3
                ))
            }
            else {
                dirtStoneLine[x] = Math.round(FastMath.interpolateCatmullRom(
                        (x - splineX1) / POINTS_GAP.toFloat(),
                        -0.3f,//0.01f,
                        splineP0,
                        splineP1,
                        splineP2,
                        splineP3
                ))
            }
        }

        // scan vertically
        for (x in 0..WIDTH - 1) {
            for (y in 0..HEIGHT - 1) {
                if (terrainMap[clamp(y + DIRT_LAYER_DEPTH, 0, HEIGHT - 1)].get(x)) {
                    // map.setTileTerrain(x, y, Tile.DIRT)
                    // map.setTileWall(x, y, Tile.DIRT)
                    val tile =
                            if (y < dirtStoneLine[x]) Tile.DIRT
                            else                      Tile.STONE
                    world.setTileTerrain(x, y, tile)
                    world.setTileWall(x, y, tile)
                }
            }
        }
    }

    /* 2. Carve */

    /**
     * Carve (place specified block) by noisemap, inversed gradation filter applied.
     * @param map noisemap
     * *
     * @param scarcity higher == rarer
     * * 1.0 is a default value. This value works as a multiplier to the gradient filter.
     * @param tile
     * *
     * @param message
     */
    private fun carveByMap(noisemap: Any, scarcity: Float, tile: Int, message: String,
                           filter: NoiseFilter = NoiseFilterQuadratic,
                           filterStart: Float = NOISE_GRAD_START,
                           filterEnd: Float = NOISE_GRAD_END) {
        println("[mapgenerator] " + message)

        for (y in 0..HEIGHT - 1) {
            for (x in 0..WIDTH - 1) {
                val noise: Float = when (noisemap) {
                    is Joise ->
                        noisemap.get(
                                x.toDouble() / 48.0, // 48: Fixed value
                                y.toDouble() / 48.0
                        ).toFloat()

                    is TaggedSimplexNoise -> noisemap.noiseModule.getNoise(
                            Math.round(x / noisemap.xStretch),
                            Math.round(y / noisemap.yStretch)
                    )

                    else -> throw(IllegalArgumentException("[mapgenerator] Unknown noise module type '${noisemap.javaClass.simpleName}': Only the 'Joise' or 'TaggedSimplexNoise' is valid."))
                }

                if (noise > filter.getGrad(y, filterStart, filterEnd) * scarcity) {
                    world.setTileTerrain(x, y, tile)
                }
            }
        }
    }

    private fun fillByMap(noisemap: Any, scarcity: Float, replaceFrom: Int, replaceTo: Int, message: String,
                          filter: NoiseFilter = NoiseFilterQuadratic,
                          filterStart: Float = NOISE_GRAD_START,
                          filterEnd: Float = NOISE_GRAD_END) {
        println("[mapgenerator] " + message)

        for (y in 0..HEIGHT - 1) {
            for (x in 0..WIDTH - 1) {
                val noise: Float = when (noisemap) {
                    is Joise ->
                        noisemap.get(
                                x.toDouble() / 48.0, // 48: Fixed value
                                y.toDouble() / 48.0
                        ).toFloat()

                    is TaggedSimplexNoise -> noisemap.noiseModule.getNoise(
                            Math.round(x / noisemap.xStretch),
                            Math.round(y / noisemap.yStretch)
                    )

                    else -> throw(IllegalArgumentException("[mapgenerator] Unknown noise module type '${noisemap.javaClass.simpleName}': Only the 'Joise' or 'TaggedSimplexNoise' is valid."))
                }

                if (noise > filter.getGrad(y, filterStart, filterEnd) * scarcity
                    && world.getTileFromTerrain(x, y) == replaceFrom) {
                    world.setTileTerrain(x, y, replaceTo)
                }
            }
        }
    }

    private fun fillByMap(noisemap: Any, scarcity: Float, replaceFrom: Int, tile: IntArray, message: String,
                          filter: NoiseFilter = NoiseFilterQuadratic,
                          filterStart: Float = NOISE_GRAD_START,
                          filterEnd: Float = NOISE_GRAD_END) {
        println("[mapgenerator] " + message)

        for (y in 0..HEIGHT - 1) {
            for (x in 0..WIDTH - 1) {
                val noise: Float = when (noisemap) {
                    is Joise ->
                        noisemap.get(
                                x.toDouble() / 48.0, // 48: Fixed value
                                y.toDouble() / 48.0
                        ).toFloat()

                    is TaggedSimplexNoise -> noisemap.noiseModule.getNoise(
                            Math.round(x / noisemap.xStretch),
                            Math.round(y / noisemap.yStretch)
                    )

                    else -> throw(IllegalArgumentException("[mapgenerator] Unknown noise module type '${noisemap.javaClass.simpleName}': Only the 'Joise' or 'TaggedSimplexNoise' is valid."))
                }

                if (noise > filter.getGrad(y, filterStart, filterEnd) * scarcity && world.getTileFromTerrain(x, y) == replaceFrom) {
                    world.setTileTerrain(x, y, tile[random.nextInt(tile.size)])
                }
            }
        }
    }

    private fun processNoiseLayers(noiseRecords: Array<TaggedJoise>) {
        if (Terrarum.MULTITHREAD) {
            // set up indices
            for (i in 0..Terrarum.THREADS - 1) {
                ThreadParallel.map(
                        i,
                        ThreadProcessNoiseLayers(
                                HEIGHT.toFloat().div(Terrarum.THREADS).times(i).roundInt(),
                                HEIGHT.toFloat().div(Terrarum.THREADS).times(i.plus(1)).roundInt() - 1,
                                noiseRecords
                        ),
                        "SampleJoiseMap"
                )
            }

            ThreadParallel.startAll()
            // FIXME game starts prematurely
            /* Console:
            [mapgenerator] Seed: 85336530
            [mapgenerator] Raising and eroding terrain...
            [mapgenerator] Shaping world...
            [mapgenerator] Carving caves...
            [mapgenerator] Carving caves...
            [mapgenerator] Carving caves...
            [mapgenerator] Flooding bottom lava...
            [mapgenerator] Carving caves...
            [mapgenerator] Planting grass...
            [mapgenerator] Placing floating islands...
            [UIHandler] Creating UI 'ConsoleWindow'
            Mon Jun 13 00:43:57 KST 2016 INFO:Offscreen Buffers FBO=true PBUFFER=true PBUFFERRT=false
            Mon Jun 13 00:43:57 KST 2016 DEBUG:Creating FBO 2048x256
            [UIHandler] Creating UI 'BasicDebugInfoWindow'
            Mon Jun 13 00:43:57 KST 2016 INFO:Offscreen Buffers FBO=true PBUFFER=true PBUFFERRT=false
            Mon Jun 13 00:43:57 KST 2016 DEBUG:Creating FBO 2048x1024
            [UIHandler] Creating UI 'Notification'
            Mon Jun 13 00:43:57 KST 2016 INFO:Offscreen Buffers FBO=true PBUFFER=true PBUFFERRT=false
            Mon Jun 13 00:43:57 KST 2016 DEBUG:Creating FBO 512x64
            [mapgenerator] Collapsing caves...
            [mapgenerator] Collapsing caves...
            [mapgenerator] Collapsing caves...
            [mapgenerator] Collapsing caves...
             */
        }
        else {
            ThreadProcessNoiseLayers(0, HEIGHT - 1, noiseRecords).run()
        }
    }

    private fun generateFloatingIslands() {
        println("[mapgenerator] Placing floating islands...")

        val nIslandsMax = Math.round(world.width * 6f / 8192f)
        val nIslandsMin = Math.max(2, Math.round(world.width * 4f / 8192f))
        val nIslands = random.nextInt(nIslandsMax - nIslandsMin) + nIslandsMin
        val prevIndex = -1

        val tiles = intArrayOf(Tile.AIR, Tile.STONE, Tile.DIRT, Tile.GRASS)

        for (i in 0..nIslands - 1) {
            var currentIndex = random.nextInt(FloatingIslandsPreset.PRESETS)
            while (currentIndex == prevIndex) {
                currentIndex = random.nextInt(FloatingIslandsPreset.PRESETS)
            }
            val island = FloatingIslandsPreset.generatePreset(currentIndex, random)

            val startingPosX = random.nextInt(world.width - 2048) + 1024
            val startingPosY = minimumFloatingIsleHeight + random.nextInt(minimumFloatingIsleHeight)

            for (j in island.indices) {
                for (k in 0..island[0].size - 1) {
                    if (island[j][k] > 0) {
                        world.setTileTerrain(k + startingPosX, j + startingPosY, tiles[island[j][k]])
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
                if (world.terrainArray[i][j].toInt() == 0) {
                    world.setTileTerrain(j, i, Tile.LAVA)
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
            for (x in 0..world.width - 1) {

                val thisTile = world.getTileFromTerrain(x, y)

                for (i in 0..8) {
                    var nearbyWallTile: Int?
                    nearbyWallTile = world.getTileFromWall(x + i % 3 - 1, y + i / 3 - 1)

                    if (nearbyWallTile == null) break;

                    if (i != 4 && thisTile == Tile.DIRT && nearbyWallTile == Tile.AIR) {
                        world.setTileTerrain(x, y, Tile.GRASS)
                        break
                    }
                }
            }
        }

    }

    private fun isGrassOrDirt(x: Int, y: Int): Boolean {
        return world.getTileFromTerrain(x, y) == Tile.GRASS || world.getTileFromTerrain(x, y) == Tile.DIRT
    }

    private fun replaceIfTerrain(ifTileRaw: Int, x: Int, y: Int, replaceTileRaw: Int) {
        if (world.getTileFromTerrain(x, y) == ifTileRaw) {
            world.setTileTerrain(x, y, replaceTileRaw)
        }
    }

    private fun replaceIfWall(ifTileRaw: Int, x: Int, y: Int, replaceTileRaw: Int) {
        if (world.getTileFromWall(x, y) == ifTileRaw) {
            world.setTileWall(x, y, replaceTileRaw)
        }
    }

    /* Post-process */

    private fun fillOcean() {
        val thisSandList = intArrayOf(
                Tile.SAND, Tile.SAND, Tile.SAND, Tile.SAND,
                Tile.SAND_WHITE, Tile.SAND_WHITE, Tile.SAND_WHITE,
                Tile.SAND_BLACK, Tile.SAND_BLACK, Tile.SAND_GREEN
                )
        val thisRand = HQRNG(SEED xor random.nextLong())
        val thisSand = thisSandList[thisRand.nextInt(thisSandList.size)]
        // val thisSand = Tile.SAND_GREEN

        val thisSandStr = if (thisSand == Tile.SAND_BLACK)
            "black"
        else if (thisSand == Tile.SAND_GREEN)
            "green"
        else if (thisSand == Tile.SAND)
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
                        world.setTileTerrain(ix, y, Tile.WATER)
                    }
                } else if (worldOceanPosition == TYPE_OCEAN_RIGHT) {
                    for (y in getTerrainHeightFromHeightMap(world.width - 1 - OCEAN_WIDTH)..getTerrainHeightFromHeightMap(world.width - 1 - ix) - 1) {
                        world.setTileTerrain(world.width - 1 - ix, y, Tile.WATER)
                    }
                }
            }
            //sand
            // linearly increase thickness of the sand sheet
            for (iy in 0..40 - ix * 40 / (OCEAN_WIDTH + SHORE_WIDTH) - 1) {
                if (worldOceanPosition == TYPE_OCEAN_LEFT) {
                    val terrainPoint = getTerrainHeightFromHeightMap(ix)


                    world.setTileTerrain(ix, terrainPoint + iy, thisSand)
                    // clear grass and make the sheet thicker
                    world.setTileTerrain(ix, terrainPoint + iy - 1, thisSand)
                } else if (worldOceanPosition == TYPE_OCEAN_RIGHT) {
                    val terrainPoint = getTerrainHeightFromHeightMap(world.width - 1 - ix)

                    world.setTileTerrain(world.width - 1 - ix, terrainPoint + iy, thisSand)
                    // clear grass and make the sheet thicker
                    world.setTileTerrain(world.width - 1 - ix, terrainPoint + iy - 1, thisSand)
                }
            }
            ix++
        }
    }

    private fun freeze() {
        for (y in 0..world.height - 1 - 1) {
            for (x in 0..getFrozenAreaWidth(y) - 1) {
                if (worldOceanPosition == TYPE_OCEAN_RIGHT) {
                    replaceIfTerrain(Tile.DIRT, x, y, Tile.SNOW)
                    replaceIfTerrain(Tile.STONE, x, y, Tile.ICE_NATURAL)

                    replaceIfWall(Tile.DIRT, x, y, Tile.SNOW)
                    replaceIfWall(Tile.STONE, x, y, Tile.ICE_NATURAL)
                } else {
                    replaceIfTerrain(Tile.DIRT, world.width - 1 - x, y, Tile.SNOW)
                    replaceIfTerrain(Tile.STONE, world.width - 1 - x, y, Tile.ICE_NATURAL)

                    replaceIfWall(Tile.DIRT, world.width - 1 - x, y, Tile.SNOW)
                    replaceIfWall(Tile.STONE, world.width - 1 - x, y, Tile.ICE_NATURAL)
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
            height = getTerrainHeightFromHeightMap(world.width - 1 - width)
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
                        if (world.terrainArray[Math.round(i + pointerY - halfBrushSize)][Math.round(j + pointerX - halfBrushSize)] == fillFrom.toByte()) {
                            world.terrainArray[Math.round(i + pointerY - halfBrushSize)][Math.round(j + pointerX - halfBrushSize)] = fill.toByte()
                        }
                    }
                }
            }
        }
    }

    private fun clamp(x: Int, min: Int, max: Int): Int = if (x < min) min else if (x > max) max else x

    data class TaggedSimplexNoise(var noiseModule: SimplexNoise, var xStretch: Float, var yStretch: Float)

    data class TaggedJoise(var message: String,
                           var noiseModule: Joise, var scarcity: Float,
                           var replaceFromTerrain: Any, var replaceFromWall: Int,
                           var replaceTo: Any,
                           var filter: NoiseFilter = NoiseFilterQuadratic,
                           var filterArg1: Float = NOISE_GRAD_START,
                           var filterArg2: Float = NOISE_GRAD_END)
}