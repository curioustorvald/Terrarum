package net.torvald.terrarum.modulebasegame.worldgenerator

import net.torvald.terrarum.LoadScreen

/**
 * Created by minjaesong on 2016-06-13.
 */
class ThreadProcessNoiseLayers(val startIndex: Int, val endIndex: Int,
                               val noiseRecords: Array<WorldGenerator.TaggedJoise>) : Runnable {


    override fun run() {
        for (record in noiseRecords) {
            println("[mapgenerator] ${record.message}...")
            LoadScreen.addMessage("${record.message}...")

            for (y in startIndex..endIndex) {
                for (x in 0..WorldGenerator.WIDTH - 1) {
                    // straight-line sampling
                    /*val noise: Float = record.noiseModule.get(
                            x.toDouble() / 48.0, // 48: Fixed value
                            y.toDouble() / 48.0
                    ).toFloat()*/
                    // circular sampling
                    // Mapping function:
                    //      World(x, y) -> Joise(sin x, y, cos x)
                    val sampleDensity = 48.0 / 2 // 48.0: magic number from old code
                    val sampleTheta = (x.toDouble() / WorldGenerator.WIDTH) * WorldGenerator.TWO_PI
                    val sampleOffset = (WorldGenerator.WIDTH / sampleDensity) / 8.0
                    val sampleX = Math.sin(sampleTheta) * sampleOffset + sampleOffset // plus sampleOffset to make only
                    val sampleZ = Math.cos(sampleTheta) * sampleOffset + sampleOffset // positive points are to be sampled
                    val sampleY = y / sampleDensity
                    val noise: Double = record.noiseModule.get(sampleX, sampleY, sampleZ)

                    val fromTerr = record.replaceFromTerrain
                    val fromWall = record.replaceFromWall

                    val to: Int = when (record.replaceTo) {
                    // replace to designated tile
                        is Int      -> record.replaceTo as Int
                    // replace to randomly selected tile from given array of tile IDs
                        is IntArray -> (record.replaceTo as IntArray)[WorldGenerator.random.nextInt((record.replaceTo as IntArray).size)]
                        else        -> throw IllegalArgumentException("[mapgenerator] Unknown replaceTo tile type '${record.replaceTo.javaClass.canonicalName}': Only 'kotlin.Int' and 'kotlin.IntArray' is valid.")
                    }
                    // replace to ALL? this is bullshit
                    if (to == WorldGenerator.TILE_MACRO_ALL) throw IllegalArgumentException("[mapgenerator] Invalid replaceTo: TILE_MACRO_ALL")

                    // filtered threshold
                    val threshold = record.filter.getGrad(y, record.filterArg1, record.filterArg2)

                    if (noise > threshold * record.scarcity) {
                        if (fromTerr is IntArray) {
                            for (i in 0..fromTerr.size - 1) {
                                val fromTerrVariable = fromTerr[i]

                                if ((WorldGenerator.world.getTileFromTerrain(x, y) == fromTerrVariable || fromTerrVariable == WorldGenerator.TILE_MACRO_ALL)
                                    && (WorldGenerator.world.getTileFromWall(x, y) == fromWall || fromWall == WorldGenerator.TILE_MACRO_ALL)) {
                                    WorldGenerator.world.setTileTerrain(x, y, to)
                                }
                            }
                        }
                        else if ((WorldGenerator.world.getTileFromTerrain(x, y) == fromTerr || fromTerr == WorldGenerator.TILE_MACRO_ALL)
                            && (WorldGenerator.world.getTileFromWall(x, y) == fromWall || fromWall == WorldGenerator.TILE_MACRO_ALL)) {
                            WorldGenerator.world.setTileTerrain(x, y, to)
                        }
                    }
                }
            }
        }
    }
}