package net.torvald.terrarum.modulebasegame.serialise

import net.torvald.gdx.graphics.PixmapIO2
import net.torvald.terrarum.App
import net.torvald.terrarum.modulebasegame.IngameRenderer
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.savegame.*
import net.torvald.terrarum.toInt
import java.io.File
import java.util.zip.GZIPOutputStream

/**
 * This function called means the "Avatar" was not externally created and thus has no sprite-bodypart-name-to-entry-number-map
 *
 * Created by minjaesong on 2021-10-08
 */
class PlayerSavingThread(
    val time_t: Long,
    val disk: VirtualDisk,
    val outFile: File,
    val ingame: TerrarumIngame,
    val isAuto: Boolean,
    val callback: () -> Unit,
    val errorHandler: (Throwable) -> Unit
) : SavingThread(errorHandler) {
    /**
     * Will happily overwrite existing entry
     */
    private fun addFile(disk: VirtualDisk, file: DiskEntry) {
        disk.entries[file.entryID] = file
        file.parentEntryID = 0
        val dir = VDUtil.getAsDirectory(disk, 0)
        if (!dir.contains(file.entryID)) dir.add(file.entryID)
    }


    override fun save() {
        App.printdbg(this, "outFile: ${outFile.path}")

        disk.saveMode = 2 * isAuto.toInt() // no quick
        disk.saveKind = VDSaveKind.PLAYER_DATA
        disk.capacity = 0L

        WriteSavegame.saveProgress = 0f

        // wait for screencap
        var emergencyStopCnt = 0
        while (IngameRenderer.screencapBusy) {
//            printdbg(this, "spinning for screencap to be taken")
            Thread.sleep(4L)
            emergencyStopCnt += 1
            if (emergencyStopCnt >= SCREENCAP_WAIT_TRY_MAX) throw InterruptedException("Waiting screencap to be taken for too long")
        }

        // write screencap
        val tgaout = ByteArray64GrowableOutputStream()
        val gzout = GZIPOutputStream(tgaout)
        PixmapIO2._writeTGA(gzout, IngameRenderer.fboRGBexport, true, true)
        IngameRenderer.fboRGBexport.dispose()
        val thumbContent = EntryFile(tgaout.toByteArray64())
        val thumb =
            DiskEntry(VDFileID.PLAYER_SCREENSHOT, VDFileID.ROOT, ingame.world.creationTime, time_t, thumbContent)
        addFile(disk, thumb)



        App.printdbg(this, "Writing The Player...")
        WritePlayer(ingame.actorGamer, disk, ingame, time_t)
        disk.entries[0]!!.modificationDate = time_t
        VDUtil.dumpToRealMachine(disk, outFile)


//        IngameRenderer.screencapBusy = false

        callback()
    }
}