package net.torvald.terrarum.modulebasegame.gameactors

/**
 * A wrapper to support instant player changing (or possessing other NPCs maybe)
 *
 * @param actor : here you 'attach' the actor you wish to control
 * Created by minjaesong on 2016-10-23.
 */
@Deprecated("The ingame should discriminate 'theRealGamer' and 'actorNowPlaying'")
class PlayableActorDelegate(@Transient val actor: ActorHumanoid) {

    /*init {
        if (actor !is Controllable)
            throw IllegalArgumentException("Player must be 'Controllable'!")
    }


    fun update(delta: Float) {
        //val oldTilewisePos = actor.hIntTilewiseHitbox

        actor.update(delta)

        // fire lightmap recalculate event upon tilewise pos change
        //val newTilewisePos = actor.hIntTilewiseHitbox
        //if (oldTilewisePos != newTilewisePos) {
        //    LightmapRenderer.fireRecalculateEvent()
        //}
        // not going to work: think about stationery tiki torches, global lights, etc
    }*/
}