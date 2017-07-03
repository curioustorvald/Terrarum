package net.torvald.terrarum.gameactors

import net.torvald.terrarum.worlddrawer.LightmapRenderer

/**
 * A wrapper to support instant player changing (or possessing other NPCs maybe)
 *
 * @param actor : here you 'attach' the actor you wish to control
 * Created by minjaesong on 16-10-23.
 */
class PlayableActorDelegate(val actor: ActorHumanoid) {

    init {
        if (actor !is Controllable)
            throw IllegalArgumentException("Player must be 'Controllable'!")
    }


    fun update(delta: Float) {
        //val oldTilewisePos = actor.tilewiseHitbox

        actor.update(delta)

        // fire lightmap recalculate event upon tilewise pos change
        //val newTilewisePos = actor.tilewiseHitbox
        //if (oldTilewisePos != newTilewisePos) {
        //    LightmapRenderer.fireRecalculateEvent()
        //}
        // not going to work: think about stationery tiki torches, global lights, etc
    }
}