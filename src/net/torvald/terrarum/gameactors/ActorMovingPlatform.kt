package net.torvald.terrarum.gameactors

import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.ActorID
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.PhysProperties

/**
 * Created by minjaesong on 2022-02-28.
 */
open class ActorMovingPlatform() : ActorWithBody() {

    protected var tilewiseWidth = 3
    @Transient protected val actorsRiding = ArrayList<ActorID>() // saving actorID due to serialisation issues

    init {
        physProp = PhysProperties.PHYSICS_OBJECT

        setHitboxDimension(TILE_SIZE * tilewiseWidth, TILE_SIZE, 0, 0)
    }


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