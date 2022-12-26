package net.torvald.terrarum.modulebasegame.serialise

import net.torvald.spriteanimation.AssembledSpriteAnimation
import net.torvald.spriteanimation.HasAssembledSprite
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.ReferencingRanges.PREFIX_DYNAMICITEM
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.savegame.*
import net.torvald.terrarum.savegame.VDFileID.BODYPARTGLOW_TO_ENTRY_MAP
import net.torvald.terrarum.savegame.VDFileID.BODYPART_TO_ENTRY_MAP
import net.torvald.terrarum.savegame.VDFileID.LOADORDER
import net.torvald.terrarum.savegame.VDFileID.ROOT
import net.torvald.terrarum.savegame.VDFileID.SAVEGAMEINFO
import net.torvald.terrarum.savegame.VDFileID.SPRITEDEF
import net.torvald.terrarum.savegame.VDFileID.SPRITEDEF_GLOW
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.spriteassembler.ADProperties
import java.io.Reader
import java.util.*

/**
 * Created by minjaesong on 2021-08-24.
 */
object WriteActor {

    // genver must be found on fixed location of the JSON string
    operator fun invoke(actor: Actor): String {
        val s = Common.jsoner.toJson(actor, actor.javaClass)
        return """{"genver":${Common.GENVER},"class":"${actor.javaClass.canonicalName}",${s.substring(1)}"""
    }

    fun encodeToByteArray64(actor: Actor): ByteArray64 {
        val baw = ByteArray64Writer(Common.CHARSET)

        val header = """{"genver":${Common.GENVER},"class":"${actor.javaClass.canonicalName}""""
        baw.write(header)
        Common.jsoner.toJson(actor, actor.javaClass, baw)
        baw.flush(); baw.close()
        // by this moment, contents of the baw will be:
        //  {"class":"some.class.Name"{"actorValue":{},......}
        //  (note that first bracket is not closed, and another open bracket after "class" property)
        // and we want to turn it into this:
        //  {"class":"some.class.Name","actorValue":{},......}
        val ba = baw.toByteArray64()
        ba[header.toByteArray(Common.CHARSET).size.toLong()] = ','.code.toByte()

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

    operator fun invoke(player: IngamePlayer, playerDisk: VirtualDisk, ingame: TerrarumIngame?, time_t: Long) {
        player.lastPlayTime = time_t
        player.totalPlayTime += time_t - (ingame?.loadedTime_t ?: time_t)


        // restore player prop backup created on load-time for multiplayer
        if (ingame?.isMultiplayer == true) {
            player.setPosition(player.unauthorisedPlayerProps.physics.position)
            player.actorValue = player.unauthorisedPlayerProps.actorValue!!
            player.inventory = player.unauthorisedPlayerProps.inventory!!
        }

        player.worldCurrentlyPlaying = ingame?.world?.worldIndex ?: UUID(0L,0L)

        // Write subset of Ingame.ItemCodex
        // The existing ItemCodex must be rewritten to clear out obsolete records
        player.dynamicToStaticTable.clear()
        player.dynamicItemInventory.clear()
        player.inventory.forEach { (itemid, _) ->
            if (itemid.startsWith("$PREFIX_DYNAMICITEM:")) {
                player.dynamicToStaticTable[itemid] = ItemCodex.dynamicToStaticID(itemid)
                player.dynamicItemInventory[itemid] = ItemCodex[itemid]!!
            }
        }


        val actorJson = WriteActor.encodeToByteArray64(player)

        val adl = player.animDesc!!.getRawADL()
        val adlGlow = player.animDescGlow?.getRawADL() // NULLABLE!

        val jsonContents = EntryFile(actorJson)
        val jsonCreationDate = playerDisk.getEntry(SAVEGAMEINFO)?.creationDate ?: time_t
        addFile(playerDisk, DiskEntry(SAVEGAMEINFO, ROOT, jsonCreationDate, time_t, jsonContents))

        val adlContents = EntryFile(ByteArray64.fromByteArray(adl.toByteArray(Common.CHARSET)))
        val adlCreationDate = playerDisk.getEntry(SPRITEDEF)?.creationDate ?: time_t
        addFile(playerDisk, DiskEntry(SPRITEDEF, ROOT, adlCreationDate, time_t, adlContents))

        if (adlGlow != null) {
            val adlGlowContents = EntryFile(ByteArray64.fromByteArray(adlGlow.toByteArray(Common.CHARSET)))
            val adlGlowCreationDate = playerDisk.getEntry(SPRITEDEF_GLOW)?.creationDate ?: time_t
            addFile(playerDisk, DiskEntry(SPRITEDEF_GLOW, ROOT, adlGlowCreationDate, time_t, adlGlowContents))
        }

        // write loadorder //
        val loadOrderBa64Writer = ByteArray64Writer(Common.CHARSET)
        loadOrderBa64Writer.write(ModMgr.loadOrder.joinToString("\n"))
        loadOrderBa64Writer.flush(); loadOrderBa64Writer.close()
        val loadOrderText = loadOrderBa64Writer.toByteArray64()
        val loadOrderContents = EntryFile(loadOrderText)
        addFile(playerDisk, DiskEntry(LOADORDER, ROOT, jsonCreationDate, time_t, loadOrderContents))
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

    private fun fillInDetails(disk: SimpleFileSystem, actor: Actor): Actor {
        actor.reload()


        if (actor is ActorWithBody && actor is IngamePlayer) {
            val animFile = disk.getFile(SPRITEDEF)
            val animFileGlow = disk.getFile(SPRITEDEF_GLOW)
            val bodypartsFile = disk.getFile(BODYPART_TO_ENTRY_MAP)

            actor.animDesc = ADProperties(ByteArray64Reader(animFile!!.bytes, Common.CHARSET))
            actor.sprite = AssembledSpriteAnimation(
                actor.animDesc!!,
                actor,
                if (bodypartsFile != null) disk else null,
                if (bodypartsFile != null) BODYPART_TO_ENTRY_MAP else null,
                false
            )
            if (animFileGlow != null) {
                actor.animDescGlow = ADProperties(ByteArray64Reader(animFileGlow.bytes, Common.CHARSET))
                actor.spriteGlow = AssembledSpriteAnimation(
                    actor.animDescGlow!!,
                    actor,
                    if (bodypartsFile != null) disk else null,
                    if (bodypartsFile != null) BODYPARTGLOW_TO_ENTRY_MAP else null,
                    true
                )
            }

            ItemCodex.loadFromSave(disk.getBackingFile(), actor.dynamicToStaticTable, actor.dynamicItemInventory)

//            val heldItem = ItemCodex[actor.inventory.itemEquipped[GameItem.EquipPosition.HAND_GRIP]]

            /*if (bodypartsFile != null)
                actor.reassembleSpriteFromDisk(disk, actor.sprite!!, actor.spriteGlow, heldItem)
            else
                actor.reassembleSprite(actor.sprite!!, actor.spriteGlow, heldItem)*/
        }
        else if (actor is ActorWithBody && actor is HasAssembledSprite) {
            if (actor.animDesc != null) actor.sprite = AssembledSpriteAnimation(actor.animDesc!!, actor, false)
            if (actor.animDescGlow != null) actor.spriteGlow = AssembledSpriteAnimation(actor.animDescGlow!!, actor, true)

            //actor.reassembleSprite(actor.sprite, actor.spriteGlow, null)
        }


        return actor
    }

}