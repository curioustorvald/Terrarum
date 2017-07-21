package net.torvald.terrarum.gameactors

import net.torvald.terrarum.gameworld.GameWorld


/**
 * Created by minjaesong on 16-02-05.
 */

object CreatureBuilder {

    /**
     * @Param jsonFileName with extension
     */
    operator fun invoke(world: GameWorld, module: String, jsonFileName: String): ActorWithPhysics {
        val actor = ActorWithPhysics(world, Actor.RenderOrder.MIDDLE)
        InjectCreatureRaw(actor.actorValue, module, jsonFileName)


        actor.actorValue[AVKey.__ACTION_TIMER] = 0.0

        return actor
    }
}