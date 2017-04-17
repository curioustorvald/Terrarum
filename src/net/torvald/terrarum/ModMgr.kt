package net.torvald.terrarum

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.FileSystems

/**
 * Modules Resource Manager
 *
 * Created by SKYHi14 on 2017-04-17.
 */
object ModMgr {

    data class ModuleMetadata(val order: Int, val isDir: Boolean, val desc: String, val libraries: Array<String>) {
        override fun toString() =
            "\tModule #$order -- $desc\n" +
            "\tExternal libraries: ${libraries.joinToString(", ")}"
    }
    const val modDir = "./assets/modules"

    val moduleInfo = HashMap<String, ModuleMetadata>()

    init {
        // load modules
        val loadOrderCSVparser = CSVParser.parse(
                FileSystems.getDefault().getPath("$modDir/LoadOrder.csv").toFile(),
                Charsets.UTF_8,
                CSVFormat.DEFAULT.withCommentMarker('#')
        )
        val loadOrder = loadOrderCSVparser.records
        loadOrderCSVparser.close()


        loadOrder.forEachIndexed { index, it ->
            val moduleName = it[0]
            println("[ModMgr] Loading module $moduleName")

            val description = it[1]
            val libs = it[2].split(';').toTypedArray()
            val isDir = FileSystems.getDefault().getPath("$modDir/$moduleName").toFile().isDirectory
            moduleInfo[moduleName] = ModuleMetadata(index, isDir, description, libs)

            println(moduleInfo[moduleName])
        }
    }

    private fun checkExistence(module: String) {
        if (!moduleInfo.containsKey(module))
            throw FileNotFoundException("No such module: $module")
    }
    private fun String.sanitisePath() = if (this[0] == '/' || this[0] == '\\')
        this.substring(1..this.lastIndex)
    else this



    fun getPath(module: String, path: String): String {
        checkExistence(module)
        return "$modDir/$module/${path.sanitisePath()}"
    }
    fun getFile(module: String, path: String): File {
        checkExistence(module)
        return FileSystems.getDefault().getPath(getPath(module, path)).toFile()
    }
    fun getFiles(module: String, path: String): Array<File> {
        checkExistence(module)
        val dir = FileSystems.getDefault().getPath(getPath(module, path)).toFile()
        if (!dir.isDirectory) {
            throw FileNotFoundException("The path is not a directory")
        }
        else {
            return dir.listFiles()
        }
    }
}