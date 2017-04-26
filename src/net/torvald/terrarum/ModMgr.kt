package net.torvald.terrarum

import net.torvald.CSVFetcher
import net.torvald.terrarum.itemproperties.InventoryItem
import net.torvald.terrarum.itemproperties.ItemCodex
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.nio.file.FileSystems
import javax.script.ScriptEngineManager
import javax.script.Invocable



/**
 * Modules Resource Manager
 *
 *
 * NOTE!!: Usage of Groovy is only temporary; if Kotlin's "JSR 223" is no longer experimental and
 *         is readily available, ditch that Groovy.
 *
 *
 * Created by SKYHi14 on 2017-04-17.
 */
object ModMgr {

    data class ModuleMetadata(
            val order: Int,
            val isDir: Boolean,
            val desc: String,
            val entryPoint: String,
            val libraries: Array<String>
    ) {
        override fun toString() =
            "\tModule #$order -- $desc\n" +
            "\tEntry point: $entryPoint\n" +
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
            val entryPoint = it[2]
            val libs = it[3].split(';').toTypedArray()
            val isDir = FileSystems.getDefault().getPath("$modDir/$moduleName").toFile().isDirectory
            moduleInfo[moduleName] = ModuleMetadata(index, isDir, description, entryPoint, libs)

            println(moduleInfo[moduleName])


            // run entry script in entry point
            if (entryPoint.isNotBlank()) {
                val extension = entryPoint.split('.').last()
                val engine = ScriptEngineManager().getEngineByExtension(extension)!!
                val invocable = engine as Invocable
                engine.eval(FileReader(getFile(moduleName, entryPoint)))
                invocable.invokeFunction("invoke", moduleName)
            }


            println("[ModMgr] $moduleName loaded successfully")
        }


        /*val manager = ScriptEngineManager()
        val factories = manager.engineFactories
        for (f in factories) {
            println("engine name:" + f.engineName)
            println("engine version:" + f.engineVersion)
            println("language name:" + f.languageName)
            println("language version:" + f.languageVersion)
            println("names:" + f.names)
            println("mime:" + f.mimeTypes)
            println("extension:" + f.extensions)
            println("-----------------------------------------------")
        }*/


        /*val engine = ScriptEngineManager().getEngineByExtension("groovy")!!
        engine.eval(FileReader(getFile("basegame", "/items/testpick.groovy")))
        val newPick = (engine as Invocable).invokeFunction("invoke", 8449) as InventoryItem
        ItemCodex[8449] = newPick*/
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



    object GameItemLoader {

        val itemPath = "items/"

        @JvmStatic operator fun invoke(module: String) {
            val engine = ScriptEngineManager().getEngineByExtension("groovy")!!
            val invocable = engine as Invocable

            val csv = CSVFetcher.readFromModule(module, itemPath + "itemid.csv")
            csv.forEach {
                val filename = it["filename"].toString()
                val script = getFile(module, itemPath + filename).readText()
                val itemID = it["id"].toInt()

                engine.eval(script)
                ItemCodex[itemID] = invocable.invokeFunction("invoke", itemID) as InventoryItem
            }
        }

    }
}