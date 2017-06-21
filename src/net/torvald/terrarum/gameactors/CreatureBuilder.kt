package net.torvald.terrarum.gameactors


/**
 * Created by minjaesong on 16-02-05.
 */

object CreatureBuilder {

    /**
     * @Param jsonFileName with extension
     */
    operator fun invoke(module: String, jsonFileName: String): ActorWithPhysics {
        val actor = ActorWithPhysics(Actor.RenderOrder.MIDDLE)
        InjectCreatureRaw(actor.actorValue, module, jsonFileName)


        actor.actorValue[AVKey.__ACTION_TIMER] = 0.0

        return actor
    }
}