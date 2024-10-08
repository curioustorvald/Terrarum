package net.torvald.terrarum.modulebasegame.serialise

import com.badlogic.gdx.graphics.Pixmap
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.modulebasegame.IngameRenderer
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.savegame.*
import net.torvald.terrarum.worlddrawer.WorldCamera
import java.io.File

/**
 * It's your responsibility to create a new VirtualDisk if your save is new, and create a backup for modifying existing save.
 *
 * Created by minjaesong on 2021-09-03.
 */
object WriteSavegame {

    enum class SaveMode {
        META, PLAYER, WORLD, SHARED, QUICK_WORLD
    }

    @Volatile var savingStatus = -1 // -1: not started, 0: saving in progress, 255: saving finished
    @Volatile var saveProgress = 0f
    @Volatile var saveProgressMax = 1f

    private fun getSaveThread(time_t: Long, mode: SaveMode, disk: VirtualDisk, outFile: File, ingame: TerrarumIngame, isAuto: Boolean, errorHandler: (Throwable) -> Unit, callback: () -> Unit) = when (mode) {
        SaveMode.WORLD -> WorldSavingThread(time_t, disk, outFile, ingame, isAuto, callback, errorHandler)
        SaveMode.PLAYER -> PlayerSavingThread(time_t, disk, outFile, ingame, isAuto, callback, errorHandler)
        SaveMode.QUICK_WORLD -> QuickSingleplayerWorldSavingThread(time_t, disk, outFile, ingame, isAuto, callback, errorHandler)
        else -> throw IllegalArgumentException("$mode")
    }

    private fun installScreencap() {
        IngameRenderer.screencapExportCallback = { fb ->
            printdbg(this, "Generating thumbnail...")

            val w = 960
            val h = 640

            val cx = /*1-*/(WorldCamera.x % 2)
            val cy = /*1-*/(WorldCamera.y % 2)

            val x = (fb.width - w) / 2 - cx // force the even-numbered position
            val y = (fb.height - h) / 2 - cy // force the even-numbered position

            val p = Pixmap.createFromFrameBuffer(x, y, w, h)
            IngameRenderer.fboRGBexport = p
            //PixmapIO2._writeTGA(gzout, p, true, true)
            //p.dispose()

            printdbg(this, "Done thumbnail generation")
        }
    }

    operator fun invoke(time_t: Long, mode: SaveMode, disk: VirtualDisk, outFile: File, ingame: TerrarumIngame, isAuto: Boolean, errorHandler: (Throwable) -> Unit, callback: () -> Unit) {
        savingStatus = 0
        printdbg(this, "Save queued")

        installScreencap()
        try { printdbg(this, "ScreencapExport installed: ${IngameRenderer.screencapExportCallback}") }
        catch (e: UninitializedPropertyAccessException) { printdbg(this, "ScreencapExport installed: no") }
        IngameRenderer.requestScreencap()

        val savingThread = Thread(getSaveThread(time_t, mode, disk, outFile, ingame, isAuto, errorHandler, callback), "TerrarumBasegameGameSaveThread")
        savingThread.start()

        // it is caller's job to keep the game paused or keep a "save in progress" ui up
        // use callback to fire the after-the-saving-progress job
    }


    fun immediate(time_t: Long, mode: SaveMode, disk: VirtualDisk, outFile: File, ingame: TerrarumIngame, isAuto: Boolean, errorHandler: (Throwable) -> Unit, callback: () -> Unit) {
        savingStatus = 0
        printdbg(this, "Immediate save fired")

        installScreencap()
        try { printdbg(this, "ScreencapExport installed: ${IngameRenderer.screencapExportCallback}") }
        catch (e: UninitializedPropertyAccessException) { printdbg(this, "ScreencapExport installed: no") }
        IngameRenderer.requestScreencap()

        val savingThread = Thread(getSaveThread(time_t, mode, disk, outFile, ingame, isAuto, errorHandler, callback), "TerrarumBasegameGameSaveThread")
        savingThread.start()

        // it is caller's job to keep the game paused or keep a "save in progress" ui up
        // use callback to fire the after-the-saving-progress job
    }

}
