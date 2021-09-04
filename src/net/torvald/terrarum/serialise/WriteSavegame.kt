package net.torvald.terrarum.serialise

import net.torvald.terrarum.*
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.*
import net.torvald.terrarum.serialise.WriteWorld.actorAcceptable
import java.io.File

/**
 * It's your responsibility to create a new VirtualDisk if your save is new, and create a backup for modifying existing save.
 *
 * Created by minjaesong on 2021-09-03.
 */
object WriteSavegame {

    /**
     * Will happily overwrite existing entry
     */
    private fun addFile(disk: VirtualDisk, file: DiskEntry) {
        disk.entries[file.entryID] = file
        file.parentEntryID = 0
        val dir = VDUtil.getAsDirectory(disk, 0)
        if (!dir.contains(file.entryID)) dir.add(file.entryID)
    }

    operator fun invoke(disk: VirtualDisk, outFile: File, ingame: TerrarumIngame) {
        val creation_t = ingame.world.creationTime
        val time_t = AppLoader.getTIME_T()
        val currentPlayTime_t = time_t - ingame.loadedTime_t


        // Write Meta //
        val metaContent = EntryFile(WriteMeta.encodeToByteArray64(ingame, currentPlayTime_t))
        val meta = DiskEntry(-1, 0, "savegame".toByteArray(), creation_t, time_t, metaContent)
        addFile(disk, meta)

        // Write BlockCodex//
        val blockCodexContent = EntryFile(ByteArray64.fromByteArray(Common.jsoner.toJson(BlockCodex).toByteArray(Common.CHARSET)))
        val blocks = DiskEntry(-16, 0, "blocks".toByteArray(), creation_t, time_t, blockCodexContent)
        addFile(disk, blocks)

        // Write ItemCodex//
        val itemCodexContent = EntryFile(ByteArray64.fromByteArray(Common.jsoner.toJson(ItemCodex).toByteArray(Common.CHARSET)))
        val items = DiskEntry(-17, 0, "items".toByteArray(), creation_t, time_t, itemCodexContent)
        addFile(disk, items)

        // Write WireCodex//
        val wireCodexContent = EntryFile(ByteArray64.fromByteArray(Common.jsoner.toJson(WireCodex).toByteArray(Common.CHARSET)))
        val wires = DiskEntry(-18, 0, "wires".toByteArray(), creation_t, time_t, wireCodexContent)
        addFile(disk, wires)

        // Write MaterialCodex//
        val materialCodexContent = EntryFile(ByteArray64.fromByteArray(Common.jsoner.toJson(MaterialCodex).toByteArray(Common.CHARSET)))
        val materials = DiskEntry(-19, 0, "materials".toByteArray(), creation_t, time_t, materialCodexContent)
        addFile(disk, materials)

        // Write FactionCodex//
        val factionCodexContent = EntryFile(ByteArray64.fromByteArray(Common.jsoner.toJson(FactionCodex).toByteArray(Common.CHARSET)))
        val factions = DiskEntry(-20, 0, "factions".toByteArray(), creation_t, time_t, factionCodexContent)
        addFile(disk, factions)

        // Write Apocryphas//
        val apocryphasContent = EntryFile(ByteArray64.fromByteArray(Common.jsoner.toJson(Apocryphas).toByteArray(Common.CHARSET)))
        val apocryphas = DiskEntry(-1024, 0, "modprops".toByteArray(), creation_t, time_t, apocryphasContent)
        addFile(disk, apocryphas)

        // Write World //
        val worldNum = ingame.world.worldIndex
        val worldContent = EntryFile(WriteWorld.encodeToByteArray64(ingame))
        val world = DiskEntry(worldNum, 0, "world${worldNum}".toByteArray(), creation_t, time_t, worldContent)
        addFile(disk, world)

        // Write Actors //
        listOf(ingame.actorContainerActive, ingame.actorContainerInactive).forEach { actors ->
            actors.forEach {
                if (actorAcceptable(it)) {
                    val actorContent = EntryFile(WriteActor.encodeToByteArray64(it))
                    val actor = DiskEntry(it.referenceID, 0, "actor${it.referenceID}".toByteArray(), creation_t, time_t, actorContent)
                    addFile(disk, actor)
                }
            }
        }

        disk.capacity = 0
        VDUtil.dumpToRealMachine(disk, outFile)
    }
}



/**
 * Load and setup the game for the first load.
 *
 * To load additional actors/worlds, use ReadActor/ReadWorld.
 *
 * Created by minjaesong on 2021-09-03.
 */
object LoadSavegame {

    operator fun invoke(disk: VirtualDisk) {
        val meta = ReadMeta(disk)
        val player = ReadActor.readActorOnly(
                ByteArray64Reader(VDUtil.getAsNormalFile(disk, 9545698).getContent(), Common.CHARSET)
        ) as IngamePlayer
        val world = ReadWorld.readWorldOnly(
                ByteArray64Reader(VDUtil.getAsNormalFile(disk, player.worldCurrentlyPlaying).getContent(), Common.CHARSET)
        )
        val actors = world.actors.map {
            ReadActor.readActorOnly(ByteArray64Reader(VDUtil.getAsNormalFile(disk, it).getContent(), Common.CHARSET))
        }

        val ingame = TerrarumIngame(AppLoader.batch)
        val worldParam = meta
        ingame.world = world
        ingame.gameLoadInfoPayload = worldParam
        ingame.gameLoadMode = TerrarumIngame.GameLoadMode.LOAD_FROM
        ingame.savegameArchive = disk
        actors.forEach { ingame.addNewActor(it) }
        ingame.actorNowPlaying = player

        Terrarum.setCurrentIngameInstance(ingame)
        val loadScreen = SanicLoadScreen
        AppLoader.setLoadScreen(loadScreen)
    }
}