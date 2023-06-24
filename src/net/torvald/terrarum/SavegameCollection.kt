package net.torvald.terrarum

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