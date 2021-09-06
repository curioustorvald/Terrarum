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

        val classDef = """{"class":"${actor.javaClass.canonicalName}""""
        baw.write(classDef)
        Common.jsoner.toJson(actor, actor.javaClass, baw)
        baw.flush(); baw.close()
        // by this moment, contents of the baw will be:
        //  {"class":"some.class.Name"{"actorValue":{},......}
        //  (note that first bracket is not closed, and another open bracket after "class" property)
        // and we want to turn it into this:
        //  {"class":"some.class.Name","actorValue":{},......}
        val ba = baw.toByteArray64()
        ba[classDef.toByteArray(Common.CHARSET).size.toLong()] = ','.code.toByte()

        return ba
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

    operator fun invoke(worldDataStream: Reader): Actor =
            fillInDetails(Common.jsoner.fromJson(null, worldDataStream))


    private fun fillInDetails(actor: Actor): Actor {
        actor.actorValue.actor = actor

        if (actor is Pocketed)
            actor.inventory.actor = actor

        if (actor is ActorWithBody && actor is HasAssembledSprite) {
            actor.sprite = SpriteAnimation(actor)
            if (actor.animDescPathGlow != null) actor.spriteGlow = SpriteAnimation(actor)
            actor.reassembleSprite(actor.sprite!!, actor.spriteGlow)
        }

        return actor
    }

    fun readActorAndAddToWorld(ingame: TerrarumIngame, worldDataStream: Reader): Actor {
        val actor = invoke(worldDataStream)

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