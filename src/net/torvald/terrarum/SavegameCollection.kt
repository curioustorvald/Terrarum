package net.torvald.terrarum

import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.savegame.DiskSkimmer
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path

/**
 * Created by minjaesong on 2023-06-24.
 */
class SavegameCollection(files0: List<DiskSkimmer>) {

    /** Sorted in reverse by the last modified time of the files, index zero being the most recent */
    val files = files0.sortedByDescending { it.getLastModifiedTime() }
    /** Sorted in reverse by the last modified time of the files, index zero being the most recent */
    val autoSaves = files.filter { it.diskFile.extension.matches(Regex("[a-z]")) }
    /** Sorted in reverse by the last modified time of the files, index zero being the most recent */
    val manualSaves = files.filter { !it.diskFile.extension.matches(Regex("[a-z]")) }

    init {
        files.forEach { it.rebuild() }
    }

    companion object {
        fun collectFromBaseFilename(basedir: File, name: String): SavegameCollection {
            val files = basedir.listFiles().filter { it.name.startsWith(name) }
                .mapNotNull { try { DiskSkimmer(it, true) } catch (e: Throwable) { null } }
            return SavegameCollection(files)
        }
    }

    /**
     * Returns the most recent not-corrupted file
     */
    fun loadable(): DiskSkimmer {
        return files.first()
    }

    fun moveToRecycle(basedir: String) {
        files.forEach {
            try {
                Files.move(it.diskFile.toPath(), Path(basedir, it.diskFile.name), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            }
            catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}

class SavegameCollectionPair(player: SavegameCollection?, world: SavegameCollection?) {

    private var manualPlayer: DiskSkimmer? = null
    private var manualWorld: DiskSkimmer? = null
    private var autoPlayer: DiskSkimmer? = null
    private var autoWorld: DiskSkimmer? = null

    var status = 0 // 0: none available, 1: loadable manual save is newer than loadable auto; 2: loadable autosave is newer than loadable manual
        private set

    var newerSaveIsDamaged = false // only when most recent save is corrupted
        private set

    init {
        printdbg(this, "init ($player, $world)")

        if (player != null && world != null) {

            printdbg(this, "player files: " + player.files.joinToString { it.diskFile.name })
            printdbg(this, "world files:" + world.files.joinToString { it.diskFile.name })

            // if a pair of files were saved successfully, they must have identical lastModifiedTime()
            var pc = 0; val pt = player.files[0].getLastModifiedTime()
            var wc = 0; val wt = world.files[0].getLastModifiedTime()
            while (pc < player.files.size && wc < world.files.size) {
                val pcf = player.files[pc]
                val pcm = pcf.getLastModifiedTime()
                val wcf = world.files[wc]
                val wcm = wcf.getLastModifiedTime()

                printdbg(this, "pc=$pc, wc=$wc, pcm=$pcm, wcm=$wcm")

                if (playerDiskNotDamaged(pcf) && worldDiskNotDamaged(wcf)) {

                    printdbg(this, "pcf.autosaved=${pcf.isAutosaved()}, wcf.autosaved=${wcf.isAutosaved()}")

                    when (pcf.isAutosaved().toInt(1) or wcf.isAutosaved().toInt()) {
                        3 -> {
                            if (autoPlayer == null && autoWorld == null) {
                                autoPlayer = pcf
                                autoWorld = wcf
                            }
                            pc += 1
                            wc += 1
                        }
                        0 -> {
                            if (manualPlayer == null && manualWorld == null) {
                                manualPlayer = pcf
                                manualWorld = wcf
                            }
                            pc += 1
                            wc += 1
                        }
                        else -> {
                            if (pcm > wcm)
                                pc += 1
                            else if (pcm == wcm) {
                                pc += 1
                                wc += 1
                            }
                            else
                                wc += 1
                        }
                    }
                }



                if (manualPlayer != null && manualWorld != null && autoPlayer != null && autoWorld != null)
                    break
            }

            if (manualPlayer != null && manualWorld != null && autoPlayer != null && autoWorld != null) {
                status = if (manualPlayer!!.getLastModifiedTime() > autoPlayer!!.getLastModifiedTime()) 1 else 2
            }
            else if (manualPlayer != null && manualWorld != null || autoPlayer != null && autoWorld != null) {
                status = 1
            }
            else {
                status = 0
            }

            printdbg(this, "manualPlayer = ${manualPlayer?.diskFile?.path}")
            printdbg(this, "manualWorld = ${manualWorld?.diskFile?.path}")
            printdbg(this, "autoPlayer = ${autoPlayer?.diskFile?.path}")
            printdbg(this, "autoWorld = ${autoWorld?.diskFile?.path}")
            printdbg(this, "status = $status")
        }
    }

    private fun DiskSkimmer.isAutosaved() = this.getSaveMode().and(0b0000_0010) != 0

    private fun playerDiskNotDamaged(disk: DiskSkimmer): Boolean {
        return true
    }

    private fun worldDiskNotDamaged(disk: DiskSkimmer): Boolean {
        return true
    }

    fun moreRecentAutosaveAvailable() = (status == 2)
    fun saveAvaliable() = (status > 0)

    fun getManualSave(): DiskPair? {
        if (status == 0) return null
        return DiskPair(manualPlayer!!, manualWorld!!)
    }

    fun getAutoSave(): DiskPair? {
        if (status != 2) return null
        return DiskPair(autoPlayer!!, autoWorld!!)
    }

    fun getLoadableSave(): DiskPair? {
        if (status == 0) return null
        return if (manualPlayer != null && manualWorld != null)
            DiskPair(manualPlayer!!, manualWorld!!)
        else
            DiskPair(autoPlayer!!, autoWorld!!)
    }
}

data class DiskPair(val player: DiskSkimmer, val world: DiskSkimmer)