package net.torvald.terrarum.modulebasegame.worldgenerator

import net.torvald.terrarum.App
import net.torvald.terrarum.App.*
import net.torvald.terrarum.BlockCodex
import net.torvald.terrarum.gameworld.GameWorld
import kotlin.math.roundToLong

/**
 * New world generator.
 *
 * Created by minjaesong on 2019-09-02.
 */
object Worldgen {

    private lateinit var world: GameWorld
    lateinit var params: WorldgenParams
        private set

    private val threadLock = java.lang.Object()

    fun attachMap(world: GameWorld, genParams: WorldgenParams) {
        this.world = world
        params = genParams
    }

    fun generateMap() {
        val jobs = listOf(
                Work("Reticulating Splines", Terragen(world, params.seed, params.terragenParams)),
                Work("Adding Vegetations", Biomegen(world, params.seed, params.biomegenParams))
        )


        for (i in jobs.indices) {
            printdbg(this, "Worldgen: job #${i+1}")

            val it = jobs[i]

            App.getLoadScreen().addMessage(it.loadingScreenName)
            it.theWork.getDone()
        }

        // determine spawn point
        world.spawnX = 0
        world.spawnY = 180
        // go up?
        if (BlockCodex[world.getTileFromTerrain(world.spawnX, world.spawnY)].isSolid) {
            // go up!
            while (BlockCodex[world.getTileFromTerrain(world.spawnX, world.spawnY)].isSolid) {
                world.spawnY -= 1
            }
        }
        else {
            // go down!
            while (!BlockCodex[world.getTileFromTerrain(world.spawnX, world.spawnY)].isSolid) {
                world.spawnY += 1
            }
        }

        printdbg(this, "Generation job finished")

    }

    private data class Work(val loadingScreenName: String, val theWork: Gen)

    fun getEstimationSec(width: Int, height: Int): Long {
        return (23.05 * 1.25 * (48600000.0 / bogoflops) * ((width * height) / 40095000.0) * (32.0 / THREAD_COUNT)).roundToLong()
    }
}

abstract class Gen(val world: GameWorld, val seed: Long, val params: Any) {
    open fun getDone() { } // trying to use different name so that it won't be confused with Runnable or Callable
}

data class WorldgenParams(
        val seed: Long,
        // optional parameters
        val terragenParams: TerragenParams = TerragenParams(),
        val biomegenParams: BiomegenParams = BiomegenParams()
)

infix fun Long.shake(other: Long): Long {
    var s0 = this
    var s1 = other

    s1 = s1 xor s0
    s0 = s0 shl 55 or s0.ushr(9) xor s1 xor (s1 shl 14)
    s1 = s1 shl 36 or s1.ushr(28)

    return s0 + s1
}

val TWO_PI = Math.PI * 2.0
val HALF_PI = Math.PI / 2.0
