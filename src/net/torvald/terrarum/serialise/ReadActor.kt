package net.torvald.terrarum.serialise

import net.torvald.spriteanimation.HasAssembledSprite
import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.terrarum.NoSuchActorWithIDException
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.modulebasegame.gameactors.Pocketed
import java.io.InputStream
import java.io.Reader

/**
 * Actor's JSON representation is expected to have "class" property on the root object, such as:
 * ```
 * "class":"net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer"
 * ```
 *
 * Created by minjaesong on 2021-08-27.
 */
class ReadActor(val ingame: TerrarumIngame) {

    open fun invoke(worldDataStream: InputStream) {
        postRead(Common.jsoner.fromJson(null, worldDataStream))
    }

    open fun invoke(worldDataStream: Reader) {
        postRead(Common.jsoner.fromJson(null, worldDataStream))
    }

    private fun postRead(actor: Actor) {
        // filling in Transients
        actor.actorValue.actor = actor

        if (actor is Pocketed)
            actor.inventory.actor = actor

        if (actor is ActorWithBody) {
            actor.sprite = SpriteAnimation(actor)

            if (actor is HasAssembledSprite) {
                if (actor.animDescPathGlow != null) actor.spriteGlow = SpriteAnimation(actor)
                actor.reassembleSprite(actor.sprite!!, actor.spriteGlow)
            }
        }
        // replace existing player
        val oldPlayerID = ingame.actorNowPlaying?.referenceID
        try {
            ingame.forceRemoveActor(ingame.getActorByID(actor.referenceID))
        }
        catch (e: NoSuchActorWithIDException) { /* no actor to delete, you may proceed */ }
        ingame.addNewActor(actor)

        if (actor.referenceID == oldPlayerID)
            ingame.actorNowPlaying = actor as ActorHumanoid
    }

}