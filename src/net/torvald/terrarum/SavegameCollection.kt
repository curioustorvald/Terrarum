package net.torvald.terrarum

import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.savegame.DiskSkimmer
import java.io.File

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

}

class SavegameCollectionPair(player: SavegameCollection?, world: SavegameCollection?) {

    private lateinit var manualPlayer: DiskSkimmer
    private lateinit var manualWorld: DiskSkimmer
    private lateinit var autoPlayer: DiskSkimmer
    private lateinit var autoWorld: DiskSkimmer

    var status = 0 // 0: none available, 1: loadable manual save is newer than loadable auto; 2: loadable autosave is newer than loadable manual
        private set

    var newerSaveIsDamaged = false // only when most recent save is corrupted
        private set

    init {
        printdbg(this, "init ($player, $world)")

        if (player != null && world != null) {

            printdbg(this, player.files.joinToString { it.diskFile.name })
            printdbg(this, world.files.joinToString { it.diskFile.name })

            // if a pair of files were saved successfully, they must have identical lastModifiedTime()
            var pc = 0; val pt = player.files[0].getLastModifiedTime()
            var wc = 0; val wt = world.files[0].getLastModifiedTime()
            while (pc < player.files.size && wc < world.files.size) {
                val pcf = player.files[pc]
                val pcm = pcf.getLastModifiedTime()
                val wcf = world.files[wc]
                val wcm = wcf.getLastModifiedTime()

                if (playerDiskNotDamaged(pcf) && worldDiskNotDamaged(wcf)) {
                    when (pcf.isAutosaved().toInt(1) or wcf.isAutosaved().toInt()) {
                        3 -> {
                            autoPlayer = pcf
                            autoWorld = wcf
                            pc += 1
                            wc += 1
                        }
                        0 -> {
                            manualPlayer = pcf
                            manualWorld = wcf
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



                if (::manualPlayer.isInitialized && ::manualWorld.isInitialized && ::autoPlayer.isInitialized && ::autoWorld.isInitialized)
                    break
            }

            if (::manualPlayer.isInitialized && ::manualWorld.isInitialized && ::autoPlayer.isInitialized && ::autoWorld.isInitialized) {
                status = if (manualPlayer.getLastModifiedTime() > autoPlayer.getLastModifiedTime()) 1 else 2

                printdbg(this, "manualPlayer = ${manualPlayer.diskFile.path}")
                printdbg(this, "manualWorld = ${manualWorld.diskFile.path}")
                printdbg(this, "autoPlayer = ${autoPlayer.diskFile.path}")
                printdbg(this, "autoWorld = ${autoWorld.diskFile.path}")
            }
            else if (::manualPlayer.isInitialized && ::manualWorld.isInitialized || ::autoPlayer.isInitialized && ::autoWorld.isInitialized) {
                status = 1
                if (::manualPlayer.isInitialized) {
                    printdbg(this, "manualPlayer = ${manualPlayer.diskFile.path}")
                    printdbg(this, "manualWorld = ${manualWorld.diskFile.path}")
                }
                else {
                    printdbg(this, "autoPlayer = ${autoPlayer.diskFile.path}")
                    printdbg(this, "autoWorld = ${autoWorld.diskFile.path}")
                }
            }
            else {
                status = 0
            }
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

    fun getManualSave(): Pair<DiskSkimmer, DiskSkimmer>? {
        if (status == 0) return null
        return manualPlayer to manualWorld
    }

    fun getAutoSave(): Pair<DiskSkimmer, DiskSkimmer>? {
        if (status != 2) return null
        return autoPlayer to autoWorld
    }

}