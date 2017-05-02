package net.torvald.terrarum.gameactors

import org.newdawn.slick.SlickException
import java.io.IOException

/**
 * Created by minjaesong on 16-02-05.
 */

object CreatureBuilder {

    /**
     * @Param jsonFileName with extension
     */
    @Throws(IOException::class, SlickException::class)
    operator fun invoke(module: String, jsonFileName: String): ActorWithPhysics {
        val actor = ActorWithPhysics(Actor.RenderOrder.MIDDLE)
        InjectCreatureRaw(actor.actorValue, module, jsonFileName)


        actor.actorValue[AVKey.__ACTION_TIMER] = 0.0

        return actor
    }
}