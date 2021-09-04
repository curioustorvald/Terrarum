package net.torvald.terrarum.serialise

import net.torvald.terrarum.*
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.*
import net.torvald.terrarum.serialise.Common.getUnzipInputStream
import net.torvald.terrarum.serialise.Common.zip
import net.torvald.terrarum.serialise.WriteWorld.actorAcceptable
import java.io.File
import java.io.Reader

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
        val blockCodexContent = EntryFile(zip(ByteArray64.fromByteArray(Common.jsoner.toJson(BlockCodex).toByteArray(Common.CHARSET))))
        val blocks = DiskEntry(-16, 0, "blocks".toByteArray(), creation_t, time_t, blockCodexContent)
        addFile(disk, blocks)

        // Write ItemCodex//
        val itemCodexContent = EntryFile(zip(ByteArray64.fromByteArray(Common.jsoner.toJson(ItemCodex).toByteArray(Common.CHARSET))))
        val items = DiskEntry(-17, 0, "items".toByteArray(), creation_t, time_t, itemCodexContent)
        addFile(disk, items)

        // Write WireCodex//
        val wireCodexContent = EntryFile(zip(ByteArray64.fromByteArray(Common.jsoner.toJson(WireCodex).toByteArray(Common.CHARSET))))
        val wires = DiskEntry(-18, 0, "wires".toByteArray(), creation_t, time_t, wireCodexContent)
        addFile(disk, wires)

        // Write MaterialCodex//
        val materialCodexContent = EntryFile(zip(ByteArray64.fromByteArray(Common.jsoner.toJson(MaterialCodex).toByteArray(Common.CHARSET))))
        val materials = DiskEntry(-19, 0, "materials".toByteArray(), creation_t, time_t, materialCodexContent)
        addFile(disk, materials)

        // Write FactionCodex//
        val factionCodexContent = EntryFile(zip(ByteArray64.fromByteArray(Common.jsoner.toJson(FactionCodex).toByteArray(Common.CHARSET))))
        val factions = DiskEntry(-20, 0, "factions".toByteArray(), creation_t, time_t, factionCodexContent)
        addFile(disk, factions)

        // Write Apocryphas//
        val apocryphasContent = EntryFile(zip(ByteArray64.fromByteArray(Common.jsoner.toJson(Apocryphas).toByteArray(Common.CHARSET))))
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

    private fun getFileBytes(disk: VirtualDisk, id: Int): ByteArray64 = VDUtil.getAsNormalFile(disk, id).getContent()
    private fun getFileReader(disk: VirtualDisk, id: Int): Reader = ByteArray64Reader(getFileBytes(disk, id), Common.CHARSET)

    operator fun invoke(disk: VirtualDisk) {
        val meta = ReadMeta(disk)
        val player = ReadActor.readActorOnly(getFileReader(disk, 9545698)) as IngamePlayer
        val world = ReadWorld.readWorldOnly(getFileReader(disk, player.worldCurrentlyPlaying))
        val actors = world.actors.map { ReadActor.readActorOnly(getFileReader(disk, it)) }
        val block = Common.jsoner.fromJson(BlockCodex.javaClass, getUnzipInputStream(getFileBytes(disk, -16)))
        val item = Common.jsoner.fromJson(ItemCodex.javaClass, getUnzipInputStream(getFileBytes(disk, -17)))
        val wire = Common.jsoner.fromJson(WireCodex.javaClass, getUnzipInputStream(getFileBytes(disk, -18)))
        val material = Common.jsoner.fromJson(MaterialCodex.javaClass, getUnzipInputStream(getFileBytes(disk, -19)))
        val faction = Common.jsoner.fromJson(FactionCodex.javaClass, getUnzipInputStream(getFileBytes(disk, -20)))
        val apocryphas = Common.jsoner.fromJson(Apocryphas.javaClass, getUnzipInputStream(getFileBytes(disk, -1024)))

        val ingame = TerrarumIngame(AppLoader.batch)
        val worldParam = TerrarumIngame.Codices(meta, block, item, wire, material, faction, apocryphas)
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