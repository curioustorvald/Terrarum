package net.torvald.terrarum.serialise

import net.torvald.terrarum.App
import net.torvald.terrarum.IngameInstance
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameworld.*
import net.torvald.terrarum.modulebasegame.IngameRenderer
import java.io.File
import java.io.Reader

/**
 * Created by minjaesong on 2022-09-03.
 */
object ReadTitlescreenGameWorld {

    operator fun invoke(worldDataStream: Reader, origin: File?): TitlescreenGameWorld =
        Common.jsoner.fromJson(TitlescreenGameWorld::class.java, worldDataStream).also {
            fillInDetails(origin, it)
        }

    private fun fillInDetails(origin: File?, world: TitlescreenGameWorld) {
        world.tileNumberToNameMap.forEach { l, s ->
            world.tileNameToNumberMap[s] = l.toInt()
        }
        world.layerOres = BlockLayerInMemoryI16I8(world.width, world.height)
        world.layerFluids = BlockLayerInMemoryI16F16(world.width, world.height)

        ItemCodex.loadFromSave(origin, world.dynamicToStaticTable, world.dynamicItemInventory)
    }

    fun readWorldAndSetNewWorld(ingame: IngameInstance, worldDataStream: Reader, origin: File?): TitlescreenGameWorld {
        val world = invoke(worldDataStream, origin)
        ingame.world = world

        return world
    }

}


object WriteTitlescreenGameWorld {

    private fun preWrite(ingame: IngameInstance, time_t: Long, world: TitlescreenGameWorld, actorsList: List<Actor>) {
        val currentPlayTime_t = time_t - ingame.loadedTime_t

        world.comp = Common.getCompIndex()

//        world.actors.clear()
//        world.actors.addAll(actorsList.map { it.referenceID }.sorted().distinct())
    }

    operator fun invoke(ingame: IngameInstance, world: TitlescreenGameWorld, actorsList: List<Actor>): String {
        val time_t = App.getTIME_T()
        preWrite(ingame, time_t, world, actorsList)
        val s = Common.jsoner.toJson(world)
        return """{"genver":${Common.GENVER},${s.substring(1)}"""
    }
}