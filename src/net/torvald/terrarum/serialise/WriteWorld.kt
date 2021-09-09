package net.torvald.terrarum.serialise

import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ReferencingRanges
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.ActorID
import net.torvald.terrarum.gameactors.BlockMarkerActor
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64Writer
import java.io.Reader

/**
 * Created by minjaesong on 2021-08-23.
 */
object WriteWorld {

    fun actorAcceptable(actor: Actor): Boolean {
        return actor.referenceID !in ReferencingRanges.ACTORS_WIRES &&
               actor.referenceID !in ReferencingRanges.ACTORS_WIRES_HELPER &&
               actor != (CommonResourcePool.get("blockmarking_actor") as BlockMarkerActor)
    }

    private fun preWrite(ingame: TerrarumIngame): GameWorld {
        val world = ingame.world
        world.genver = Common.GENVER
        world.comp = Common.COMP_GZIP

        val actorIDbuf = ArrayList<ActorID>()
        ingame.actorContainerActive.filter { actorAcceptable(it) }.forEach { actorIDbuf.add(it.referenceID) }
        ingame.actorContainerInactive.filter { actorAcceptable(it) }.forEach { actorIDbuf.add(it.referenceID) }

        world.actors.clear()
        world.actors.addAll(actorIDbuf.sorted().distinct())

        return world
    }

    operator fun invoke(ingame: TerrarumIngame): String {
        return Common.jsoner.toJson(preWrite(ingame))
    }

    fun encodeToByteArray64(ingame: TerrarumIngame): ByteArray64 {
        val baw = ByteArray64Writer(Common.CHARSET)

        Common.jsoner.toJson(preWrite(ingame), baw)
        baw.flush(); baw.close()

        return baw.toByteArray64()
    }

}



/**
 * Created by minjaesong on 2021-08-25.
 */
object ReadWorld {

    operator fun invoke(worldDataStream: Reader): GameWorld =
            fillInDetails(Common.jsoner.fromJson(GameWorld::class.java, worldDataStream))

    private fun fillInDetails(world: GameWorld): GameWorld {
        world.tileNumberToNameMap.forEach { l, s ->
            world.tileNameToNumberMap[s] = l.toInt()
        }

        return world
    }

    fun readWorldAndSetNewWorld(ingame: TerrarumIngame, worldDataStream: Reader): GameWorld {
        val world = invoke(worldDataStream)
        ingame.world = world
        return world
    }

}