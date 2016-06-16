package net.torvald.terrarum.mapgenerator

/**
 * Created by minjaesong on 16-06-13.
 */
class ThreadProcessNoiseLayers(val startIndex: Int, val endIndex: Int,
                               val noiseRecords: Array<MapGenerator.TaggedJoise>) : Runnable {


    override fun run() {
        for (record in noiseRecords) {
            println("[mapgenerator] ${record.message}...")
            for (y in startIndex..endIndex) {
                for (x in 0..MapGenerator.WIDTH - 1) {
                    val noise: Float = record.noiseModule.get(
                            x.toDouble() / 48.0, // 48: Fixed value
                            y.toDouble() / 48.0
                    ).toFloat()

                    val fromTerr = record.replaceFromTerrain
                    val fromWall = record.replaceFromWall

                    val to: Int = when (record.replaceTo) {
                    // replace to designated tile
                        is Int      -> record.replaceTo as Int
                    // replace to randomly selected tile from given array of tile IDs
                        is IntArray -> (record.replaceTo as IntArray)[MapGenerator.random.nextInt((record.replaceTo as IntArray).size)]
                        else        -> throw IllegalArgumentException("[mapgenerator] Unknown replaceTo tile type '${record.replaceTo.javaClass.canonicalName}': Only 'kotlin.Int' and 'kotlin.IntArray' is valid.")
                    }
                    // replace to ALL? this is bullshit
                    if (to == MapGenerator.TILE_MACRO_ALL) throw IllegalArgumentException("[mapgenerator] Invalid replaceTo: TILE_MACRO_ALL")

                    // filtered threshold
                    val threshold = record.filter.getGrad(y, record.filterArg1, record.filterArg2)

                    if (noise > threshold * record.scarcity) {
                        if (fromTerr is IntArray) {
                            for (i in 0..fromTerr.size - 1) {
                                val fromTerrVariable = fromTerr[i]

                                if ((MapGenerator.map.getTileFromTerrain(x, y) == fromTerrVariable || fromTerrVariable == MapGenerator.TILE_MACRO_ALL)
                                    && (MapGenerator.map.getTileFromWall(x, y) == fromWall || fromWall == MapGenerator.TILE_MACRO_ALL)) {
                                    MapGenerator.map.setTileTerrain(x, y, to)
                                }
                            }
                        }
                        else if ((MapGenerator.map.getTileFromTerrain(x, y) == fromTerr || fromTerr == MapGenerator.TILE_MACRO_ALL)
                            && (MapGenerator.map.getTileFromWall(x, y) == fromWall || fromWall == MapGenerator.TILE_MACRO_ALL)) {
                            MapGenerator.map.setTileTerrain(x, y, to)
                        }
                    }
                }
            }
        }
    }
}