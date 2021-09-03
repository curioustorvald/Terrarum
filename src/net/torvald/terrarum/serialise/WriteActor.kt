package net.torvald.terrarum.serialise

import net.torvald.spriteanimation.HasAssembledSprite
import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.terrarum.NoSuchActorWithIDException
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.modulebasegame.gameactors.Pocketed
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64Writer
import java.io.Reader

/**
 * Created by minjaesong on 2021-08-24.
 */
object WriteActor {

    operator fun invoke(actor: Actor): String {
        val s = Common.jsoner.toJson(actor, actor.javaClass)
        return """{"class":"${actor.javaClass.canonicalName}",${s.substring(1)}"""
    }

    fun encodeToByteArray64(actor: Actor): ByteArray64 {
        val baw = ByteArray64Writer(Common.CHARSET)

        Common.jsoner.toJson(actor, actor.javaClass, baw)
        baw.flush(); baw.close()

        return baw.toByteArray64()
    }

}



/**
 * Actor's JSON representation is expected to have "class" property on the root object, such as:
 * ```
 * "class":"net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer"
 * ```
 *
 * Created by minjaesong on 2021-08-27.
 */
object ReadActor {

    fun readActorOnly(worldDataStream: Reader): Actor =
            Common.jsoner.fromJson(null, worldDataStream)

    operator fun invoke(ingame: TerrarumIngame, worldDataStream: Reader): Actor =
            postRead(ingame, readActorOnly(worldDataStream))

    private fun postRead(ingame: TerrarumIngame, actor: Actor): Actor {
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


        return actor
    }

}