package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.gameactors.ActorID
import net.torvald.terrarum.gameactors.ActorWithBody

/**
 * Created by minjaesong on 2022-02-28.
 */
class ActorMovingPlatform() : ActorWithBody() {

    private val tilewiseLength = 3

    private val actorsRiding = ArrayList<ActorID>() // saving actorID due to serialisation issues

    /**
     * Make the actor its externalV controlled by this platform
     */
    fun mount(actor: ActorWithBody) {
        actorsRiding.add(actor.referenceID)
    }

    /**
     * Make the actor its externalV no longer controlled by this platform
     */
    fun dismount(actor: ActorWithBody) {
        actorsRiding.remove(actor.referenceID)
    }

}