package net.torvald.terrarum.serialise

import net.torvald.terrarum.App
import net.torvald.terrarum.IngameInstance
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameworld.BlockLayerI16F16
import net.torvald.terrarum.gameworld.BlockLayerI16I8
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.SimpleGameWorld
import java.io.File
import java.io.Reader

/**
 * Created by minjaesong on 2022-09-03.
 */
object ReadSimpleWorld {

    operator fun invoke(worldDataStream: Reader, origin: File?): GameWorld =
        Common.jsoner.fromJson(SimpleGameWorld::class.java, worldDataStream).also {
            fillInDetails(origin, it)
        }

    private fun fillInDetails(origin: File?, world: GameWorld) {
        world.tileNumberToNameMap.forEach { l, s ->
            world.tileNameToNumberMap[s] = l.toInt()
        }
        world.layerOres = BlockLayerI16I8(world.width, world.height)
        world.layerFluids = BlockLayerI16F16(world.width, world.height)

        ItemCodex.loadFromSave(origin, world.dynamicToStaticTable, world.dynamicItemInventory)
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

        world.comp = Common.getCompIndex()
        world.lastPlayTime = time_t
        world.totalPlayTime += currentPlayTime_t

        world.actors.clear()
        world.actors.addAll(actorsList.map { it.referenceID }.sorted().distinct())
    }

    operator fun invoke(ingame: IngameInstance, world: SimpleGameWorld, actorsList: List<Actor>): String {
        val time_t = App.getTIME_T()
        preWrite(ingame, time_t, world, actorsList)
        val s = Common.jsoner.toJson(world)
        return """{"genver":${Common.GENVER},${s.substring(1)}"""
    }
}