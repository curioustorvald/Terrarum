package net.torvald.terrarum.serialise

import net.torvald.spriteanimation.HasAssembledSprite
import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.spriteassembler.ADProperties
import net.torvald.terrarum.NoSuchActorWithIDException
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.modulebasegame.gameactors.Pocketed
import net.torvald.terrarum.tvda.*
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
 * Player-specific [WriteActor].
 *
 * Created by minjaesong on 2021-10-07.
 */
object WritePlayer {
    operator fun invoke(player: IngamePlayer, disk: VirtualDisk) {
        val actorJson = WriteActor.encodeToByteArray64(player)
        val adl = player.animDesc?.getRawADL() // NULLABLE!
        val adlGlow = player.animDescGlow?.getRawADL() // NULLABLE!
    }

    operator fun invoke(player: IngamePlayer, skimmer: DiskSkimmer) {

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

    operator fun invoke(disk: SimpleFileSystem, worldDataStream: Reader): Actor =
            fillInDetails(disk, Common.jsoner.fromJson(null, worldDataStream))

    fun readActorBare(worldDataStream: Reader): Actor =
            Common.jsoner.fromJson(null, worldDataStream)

    private fun fillInDetails(disk: SimpleFileSystem, actor: Actor): Actor {
        actor.actorValue.actor = actor

        if (actor is Pocketed)
            actor.inventory.actor = actor

        if (actor is ActorWithBody && actor is HasAssembledSprite) {
            val animFile = disk.getFile(-2L)
            val animFileGlow = disk.getFile(-3L)

            actor.sprite = SpriteAnimation(actor)
            if (animFileGlow != null) actor.spriteGlow = SpriteAnimation(actor)
            actor.reassembleSprite(
                    actor.sprite!!,
                    ADProperties(ByteArray64Reader(animFile!!.bytes, Common.CHARSET)),
                    actor.spriteGlow,
                    if (animFileGlow == null) null else ADProperties(ByteArray64Reader(animFileGlow.bytes, Common.CHARSET))
            )
        }

        return actor
    }

    fun readActorAndAddToWorld(ingame: TerrarumIngame, disk: SimpleFileSystem, worldDataStream: Reader): Actor {
        val actor = invoke(disk, worldDataStream)

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