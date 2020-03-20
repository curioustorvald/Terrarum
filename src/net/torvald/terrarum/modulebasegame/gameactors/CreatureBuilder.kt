package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameworld.GameWorld


/**
 * Created by minjaesong on 2016-02-05.
 */

object CreatureBuilder {

    /**
     * @Param jsonFileName with extension
     */
    operator fun invoke(module: String, jsonFileName: String): ActorWithBody {
        val actor = ActorWithBody(Actor.RenderOrder.MIDDLE)
        InjectCreatureRaw(actor.actorValue, module, jsonFileName)


        actor.actorValue[AVKey.__ACTION_TIMER] = 0.0

        return actor
    }
}