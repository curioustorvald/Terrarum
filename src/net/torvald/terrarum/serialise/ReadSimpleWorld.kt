package net.torvald.terrarum.serialise

import net.torvald.terrarum.App
import net.torvald.terrarum.IngameInstance
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.SimpleGameWorld
import java.io.File
import java.io.Reader

/**
 * Created by minjaesong on 2022-09-03.
 */
object ReadSimpleWorld {

    operator fun invoke(worldDataStream: Reader, origin: File?): GameWorld =
            fillInDetails(Common.jsoner.fromJson(SimpleGameWorld::class.java, worldDataStream), origin)

    private fun fillInDetails(world: GameWorld, origin: File?): GameWorld {
        world.tileNumberToNameMap.forEach { l, s ->
            world.tileNameToNumberMap[s] = l.toInt()
        }

        ItemCodex.loadFromSave(origin, world.dynamicToStaticTable, world.dynamicItemInventory)

        return world
    }

    fun readWorldAndSetNewWorld(ingame: IngameInstance, worldDataStream: Reader, origin: File?): GameWorld {
        val world = invoke(worldDataStream, origin)
        ingame.world = world
        return world
    }

}


object WriteSimpleWorld {

    private fun preWrite(ingame: IngameInstance, time_t: Long, world: SimpleGameWorld, actorsList: List<Actor>) {
        val currentPlayTime_t = time_t - ingame.loadedTime_t

        world.comp = Common.COMP_GZIP
        world.lastPlayTime = time_t
        world.totalPlayTime += currentPlayTime_t

        world.actors.clear()
        world.actors.addAll(actorsList.map { it.referenceID }.sorted().distinct())
    }

    operator fun invoke(ingame: IngameInstance, world: SimpleGameWorld, actorsList: List<Actor>): String {
        val time_t = App.getTIME_T()
        val s = Common.jsoner.toJson(preWrite(ingame, time_t, world, actorsList))
        return """{"genver":${Common.GENVER},${s.substring(1)}"""
    }
}