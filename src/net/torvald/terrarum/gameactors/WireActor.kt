package net.torvald.terrarum.gameactors

/**
 * Created by minjaesong on 2021-07-30.
 */
class WireActor(id: ActorID) : ActorWithBody(RenderOrder.WIRES, PhysProperties.IMMOBILE) {
    init {
        this.referenceID = id
    }
}