package net.torvald.terrarum.serialise

import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import java.io.InputStream
import java.io.Reader

/**
 * Created by minjaesong on 2021-08-25.
 */
open class ReadWorld(val ingame: TerrarumIngame) {

    open fun invoke(worldDataStream: InputStream) {
        postRead(WriteWorld.jsoner.fromJson(GameWorld::class.java, worldDataStream))
    }

    open fun invoke(worldDataStream: Reader) {
        postRead(WriteWorld.jsoner.fromJson(GameWorld::class.java, worldDataStream))
    }

    private fun postRead(world: GameWorld) {
        world.postLoad()

        ingame.world = world
        //ingame.actorNowPlaying?.setPosition(3.0, 3.0)
    }

}