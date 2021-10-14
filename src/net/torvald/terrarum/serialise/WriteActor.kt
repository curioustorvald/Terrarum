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
 * Player-specific [WriteActor]. Will write JSON and Animation Description Languages
 *
 * Created by minjaesong on 2021-10-07.
 */
object WritePlayer {

    /**
     * Will happily overwrite existing entry
     */
    private fun addFile(disk: VirtualDisk, file: DiskEntry) {
        disk.entries[file.entryID] = file
        file.parentEntryID = 0
        val dir = VDUtil.getAsDirectory(disk, 0)
        if (!dir.contains(file.entryID)) dir.add(file.entryID)
    }

    operator fun invoke(player: IngamePlayer, playerDisk: VirtualDisk, ingame: TerrarumIngame, time_t: Long) {
        player.lastPlayTime = time_t
        player.totalPlayTime += time_t - ingame.loadedTime_t


        // restore player prop backup created on load-time for multiplayer
        if (ingame.isMultiplayer) {
            player.setPosition(player.unauthorisedPlayerProps.physics.position)
            player.actorValue = player.unauthorisedPlayerProps.actorValue!!
            player.inventory = player.unauthorisedPlayerProps.inventory!!
        }

        player.worldCurrentlyPlaying = ingame.world.worldIndex


        val actorJson = WriteActor.encodeToByteArray64(player)
        val adl = player.animDesc!!.getRawADL()
        val adlGlow = player.animDescGlow?.getRawADL() // NULLABLE!

        val jsonContents = EntryFile(actorJson)
        val jsonCreationDate = playerDisk.getEntry(-1)?.creationDate ?: time_t
        addFile(playerDisk, DiskEntry(-1L, 0L, jsonCreationDate, time_t, jsonContents))

        val adlContents = EntryFile(ByteArray64.fromByteArray(adl.toByteArray(Common.CHARSET)))
        val adlCreationDate = playerDisk.getEntry(-2)?.creationDate ?: time_t
        addFile(playerDisk, DiskEntry(-2L, 0L, adlCreationDate, time_t, adlContents))

        if (adlGlow != null) {
            val adlGlowContents = EntryFile(ByteArray64.fromByteArray(adlGlow.toByteArray(Common.CHARSET)))
            val adlGlowCreationDate = playerDisk.getEntry(-3)?.creationDate ?: time_t
            addFile(playerDisk, DiskEntry(-3L, 0L, adlGlowCreationDate, time_t, adlGlowContents))
        }


    }

}

/**
 * Player-specific [ReadActor].
 *
 * @param disk disk
 * @param dataStream Reader containing JSON file
 *
 * Created by minjaesong on 2021-10-07.
 */
object ReadPlayer {

    operator fun invoke(disk: SimpleFileSystem, dataStream: Reader): IngamePlayer =
            ReadActor(disk, dataStream) as IngamePlayer

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

    operator fun invoke(disk: SimpleFileSystem, dataStream: Reader): Actor =
            fillInDetails(disk, Common.jsoner.fromJson(null, dataStream))

    fun readActorBare(worldDataStream: Reader): Actor =
            Common.jsoner.fromJson(null, worldDataStream)

    private fun fillInDetails(disk: SimpleFileSystem, actor: Actor): Actor {
        actor.actorValue.actor = actor

        if (actor is Pocketed)
            actor.inventory.actor = actor

        if (actor is ActorWithBody && actor is IngamePlayer) {
            val animFile = disk.getFile(-2L)
            val animFileGlow = disk.getFile(-3L)
            val bodypartsFile = disk.getFile(-1025)

            actor.sprite = SpriteAnimation(actor)
            actor.animDesc = ADProperties(ByteArray64Reader(animFile!!.bytes, Common.CHARSET))
            if (animFileGlow != null) {
                actor.spriteGlow = SpriteAnimation(actor)
                actor.animDescGlow = ADProperties(ByteArray64Reader(animFileGlow.bytes, Common.CHARSET))
            }

            if (bodypartsFile != null)
                actor.reassembleSprite(disk, actor.sprite!!, actor.spriteGlow)
            else
                actor.reassembleSprite(actor.sprite!!, actor.spriteGlow)
        }
        else if (actor is ActorWithBody && actor is HasAssembledSprite) {
            if (actor.animDesc != null) actor.sprite = SpriteAnimation(actor)
            if (actor.animDescGlow != null) actor.spriteGlow = SpriteAnimation(actor)

            actor.reassembleSprite(actor.sprite, actor.spriteGlow)
        }


        return actor
    }

    fun readActorAndAddToWorld(ingame: TerrarumIngame, disk: SimpleFileSystem, dataStream: Reader): Actor {
        val actor = invoke(disk, dataStream)

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