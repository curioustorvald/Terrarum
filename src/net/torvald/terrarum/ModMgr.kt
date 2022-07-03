package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.JsonValue
import net.torvald.terrarum.App.*
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.blockproperties.WireCodex
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.itemproperties.MaterialCodex
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.utils.CSVFetcher
import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.terrarum.utils.forEachSiblings
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.FileSystems
import java.util.*





/**
 * Modules (or Mods) Resource Manager
 *
 * The very first mod on the load set must have a title screen
 *
 * NOTE!!: Usage of Groovy is only temporary; if Kotlin's "JSR 223" is no longer experimental and
 *         is readily available, ditch that Groovy.
 *
 *
 * Created by minjaesong on 2017-04-17.
 */
object ModMgr {

    val metaFilename = "metadata.properties"
    val defaultConfigFilename = "default.json"

    data class ModuleMetadata(
            val order: Int,
            val isDir: Boolean,
            val iconFile: FileHandle,
            val properName: String,
            val description: String,
            val author: String,
            val packageName: String,
            val entryPoint: String,
            val releaseDate: String,
            val version: String,
            val jar: String,
            val dependencies: Array<String>,
            val isInternal: Boolean
    ) {
        override fun toString() =
                "\tModule #$order -- $properName | $version | $author\n" +
                "\t$description | $releaseDate\n" +
                "\tEntry point: $entryPoint\n" +
                "\tJarfile: $jar\n" +
                "\tDependencies: ${dependencies.joinToString("\n\t")}"
    }

    data class ModuleErrorInfo(
            val type: LoadErrorType,
            val moduleName: String,
            val cause: Throwable? = null,
    )

    enum class LoadErrorType {
        YOUR_FAULT,
        MY_FAULT,
        NOT_EVEN_THERE
    }

    const val modDirInternal = "./assets/mods"
    val modDirExternal = "${App.defaultDir}/Modules"

    /** Module name (directory name), ModuleMetadata */
    val moduleInfo = HashMap<String, ModuleMetadata>()
    val moduleInfoErrored = HashMap<String, ModuleMetadata>()
    val entryPointClasses = ArrayList<ModuleEntryPoint>()

    val moduleClassloader = HashMap<String, URLClassLoader>()

    val loadOrder = ArrayList<String>()

    val errorLogs = ArrayList<ModuleErrorInfo>()

    fun logError(type: LoadErrorType, moduleName: String, cause: Throwable? = null) {
        errorLogs.add(ModuleErrorInfo(type, moduleName, cause))
    }

    private val digester = DigestUtils.getSha256Digest()

    /**
     * Try to create an instance of a "titlescreen" from the current load order set.
     */
    fun getTitleScreen(batch: FlippingSpriteBatch): IngameInstance? = entryPointClasses.getOrNull(0)?.getTitleScreen(batch)

    private fun List<String>.toVersionNumber() = 0L or
            (this[0].replaceFirst('*','0').removeSuffix("+").toLong().shl(24)) or
            (this.getOrElse(1) {"0"}.replaceFirst('*','0').removeSuffix("+").toLong().shl(16)) or
            (this.getOrElse(2) {"0"}.replaceFirst('*','0').removeSuffix("+").toLong().coerceAtMost(65535))


    init {
        val loadOrderFile = FileSystems.getDefault().getPath("${App.defaultDir}/LoadOrder.txt").toFile()
        if (loadOrderFile.exists()) {

            // load modules
            val loadOrderCSVparser = CSVParser.parse(
                    loadOrderFile,
                    Charsets.UTF_8,
                    CSVFormat.DEFAULT.withCommentMarker('#')
            )
            val loadOrder = loadOrderCSVparser.records
            loadOrderCSVparser.close()


            loadOrder.forEachIndexed { index, it ->
                val moduleName = it[0]
                this.loadOrder.add(moduleName)
                printmsg(this, "Loading module $moduleName")

                try {
                    val modMetadata = Properties()

                    val _internalFile = File("$modDirInternal/$moduleName/$metaFilename")
                    val _externalFile = File("$modDirExternal/$moduleName/$metaFilename")

                    // external mod has precedence over the internal
                    val isInternal = if (_externalFile.exists()) false else if (_internalFile.exists()) true else throw FileNotFoundException()
                    val file = if (isInternal) _internalFile else _externalFile
                    val modDir = if (isInternal) modDirInternal else modDirExternal

                    fun getGdxFile(path: String) = if (isInternal) Gdx.files.internal(path) else Gdx.files.absolute(path)

                    modMetadata.load(FileInputStream(file))

                    if (File("$modDir/$moduleName/$defaultConfigFilename").exists()) {
                        try {
                            val defaultConfig = JsonFetcher("$modDir/$moduleName/$defaultConfigFilename")

                            // read config and store it to the game
                            var entry: JsonValue? = defaultConfig.child
                            while (entry != null) {
                                setToGameConfig(entry, moduleName)
                                entry = entry.next
                            } // copied from App.java

                            // write to user's config file
                        }
                        catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }



                    val properName = modMetadata.getProperty("propername")
                    val description = modMetadata.getProperty("description")
                    val author = modMetadata.getProperty("author")
                    val packageName = modMetadata.getProperty("package")
                    val entryPoint = modMetadata.getProperty("entrypoint")
                    val releaseDate = modMetadata.getProperty("releasedate")
                    val version = modMetadata.getProperty("version")
                    val jar = modMetadata.getProperty("jar")
                    val jarHash = modMetadata.getProperty("jarhash").uppercase()
                    val dependency = modMetadata.getProperty("dependency").split(Regex(""";[ ]*""")).filter { it.isNotEmpty() }.toTypedArray()
                    val isDir = FileSystems.getDefault().getPath("$modDir/$moduleName").toFile().isDirectory


                    val versionNumeral = version.split('.')
                    val versionNumber = versionNumeral.toVersionNumber()

                    dependency.forEach { nameAndVersionStr ->
                        val (moduleName, moduleVersionStr) = nameAndVersionStr.split(' ')
                        val numbers = moduleVersionStr.split('.')
                        val checkVersionNumber = numbers.toVersionNumber() // version number required
                        var operator = numbers.last().last() // can be '+', '*', or a number

                        val checkAgainstStr = moduleInfo[moduleName]?.version ?: throw ModuleDependencyNotSatisfied(nameAndVersionStr, "(module not installed)")
                        val checkAgainst =checkAgainstStr.split('.').toVersionNumber() // version number of what's installed

                        when (operator) {
                            '+', '*' -> if (checkVersionNumber > checkAgainst) throw ModuleDependencyNotSatisfied(nameAndVersionStr, "$moduleName $checkAgainstStr")
                            else -> if (checkVersionNumber != checkAgainst) throw ModuleDependencyNotSatisfied(nameAndVersionStr, "$moduleName $checkAgainstStr")
                        }
                    }


                    moduleInfo[moduleName] = ModuleMetadata(index, isDir, getGdxFile("$modDir/$moduleName/icon.png"), properName, description, author, packageName, entryPoint, releaseDate, version, jar, dependency, isInternal)

                    printdbg(this, moduleInfo[moduleName])

                    // do retexturing if retextures directory exists
                    if (hasFile(moduleName, "retextures")) {
                        printdbg(this, "Trying to load Retextures on ${moduleName}")
                        GameRetextureLoader(moduleName)
                    }

                    // run entry script in entry point
                    if (entryPoint.isNotBlank()) {
                        var newClass: Class<*>? = null
                        try {
                            // for modules that has JAR defined
                            if (jar.isNotBlank()) {
                                val urls = arrayOf<URL>()

                                val jarFilePath = "${File(modDir).absolutePath}/$moduleName/$jar"
                                val cl = JarFileLoader(urls)
                                cl.addFile(jarFilePath)
                                moduleClassloader[moduleName] = cl

                                // check for hash
                                digester.reset()
                                val hash = digester.digest(File(jarFilePath).readBytes()).joinToString("","","") { it.toInt().and(255).toString(16).uppercase().padStart(2,'0') }

                                if (jarHash != hash) {
                                    printdbg(this, "Hash expected: $jarHash, got: $hash")
                                    throw IllegalStateException("Module Jarfile hash mismatch")
                                }

                                // check for module-info.java
                                val moduleInfoPath = cl.getResources("module-info.class").toList().filter { it.toString().contains("$moduleName/$jar!/module-info.class") && it.toString().endsWith("module-info.class")}
                                if (moduleInfoPath.isEmpty()) {
                                    throw IllegalStateException("module-info not found on $moduleName")
                                }

                                newClass = cl.loadClass(entryPoint)
                            }
                            // for modules that are not (meant to be used by the "basegame" kind of modules)
                            else {
                                newClass = Class.forName(entryPoint)
                            }
                        }
                        catch (e: Throwable) {
                            printdbgerr(this, "$moduleName failed to load, skipping...")
                            printdbgerr(this, "\t$e")
                            print(App.csiR); e.printStackTrace(System.out); print(App.csi0)

                            logError(LoadErrorType.YOUR_FAULT, moduleName, e)

                            moduleInfo.remove(moduleName)?.let { moduleInfoErrored[moduleName] = it }
                        }

                        if (newClass != null) {
                            val newClassConstructor = newClass.getConstructor(/* no args defined */)
                            val newClassInstance = newClassConstructor.newInstance(/* no args defined */)

                            entryPointClasses.add(newClassInstance as ModuleEntryPoint)
                            (newClassInstance as ModuleEntryPoint).invoke()

                            printdbg(this, "$moduleName loaded successfully")
                        }
                        else {
                            moduleInfo.remove(moduleName)?.let { moduleInfoErrored[moduleName] = it }
                            printdbg(this, "$moduleName did not load...")
                        }

                    }

                    printmsg(this, "Module $moduleName processed")
                }
                catch (noSuchModule: FileNotFoundException) {
                    printmsgerr(this, "No such module: $moduleName, skipping...")

                    logError(LoadErrorType.NOT_EVEN_THERE, moduleName, noSuchModule)

                    moduleInfo.remove(moduleName)?.let { moduleInfoErrored[moduleName] = it }
                }
                catch (noSuchModule2: ModuleDependencyNotSatisfied) {
                    printmsgerr(this, noSuchModule2.message)

                    logError(LoadErrorType.NOT_EVEN_THERE, moduleName, noSuchModule2)

                    moduleInfo.remove(moduleName)?.let { moduleInfoErrored[moduleName] = it }
                }
                catch (e: Throwable) {
                    // TODO: Instead of skipping module with error, just display the error message onto the face?


                    printmsgerr(this, "There was an error while loading module $moduleName")
                    printmsgerr(this, "\t$e")
                    print(App.csiR); e.printStackTrace(System.out); print(App.csi0)

                    logError(LoadErrorType.YOUR_FAULT, moduleName, e)

                    moduleInfo.remove(moduleName)?.let { moduleInfoErrored[moduleName] = it }
                }
                finally {

                }
            }
        }
    }

    private class ModuleDependencyNotSatisfied(want: String, have: String) :
            RuntimeException("Required: $want, Installed: $have")

    operator fun invoke() { }

    /*fun reloadModules() {
        loadOrder.forEach {
            val moduleName = it

            printmsg(this, "Reloading module $moduleName")

            try {
                checkExistence(moduleName)
                val modMetadata = moduleInfo[it]!!
                val entryPoint = modMetadata.entryPoint


                // run entry script in entry point
                if (entryPoint.isNotBlank()) {
                    var newClass: Class<*>? = null
                    try {
                        newClass = Class.forName(entryPoint)
                    }
                    catch (e: ClassNotFoundException) {
                        printdbgerr(this, "$moduleName has nonexisting entry point, skipping...")
                        printdbgerr(this, "\t$e")
                        moduleInfo.remove(moduleName)
                    }

                    newClass?.let {
                        val newClassConstructor = newClass!!.getConstructor(/* no args defined */)
                        val newClassInstance = newClassConstructor.newInstance(/* no args defined */)

                        entryPointClasses.add(newClassInstance as ModuleEntryPoint)
                        (newClassInstance as ModuleEntryPoint).invoke()
                    }
                }

                printdbg(this, "$moduleName reloaded successfully")
            }
            catch (noSuchModule: FileNotFoundException) {
                printdbgerr(this, "No such module: $moduleName, skipping...")
                moduleInfo.remove(moduleName)
            }
            catch (e: Throwable) {
                printdbgerr(this, "There was an error while loading module $moduleName")
                printdbgerr(this, "\t$e")
                print(App.csiR); e.printStackTrace(System.out); print(App.csi0)
                moduleInfo.remove(moduleName)
            }
        }
    }*/

    private fun checkExistence(module: String) {
        if (!moduleInfo.containsKey(module))
            throw FileNotFoundException("No such module: $module")
    }
    private fun String.sanitisePath() = if (this[0] == '/' || this[0] == '\\')
        this.substring(1..this.lastIndex)
    else this



    /*fun getPath(module: String, path: String): String {
        checkExistence(module)
        return "$modDirInternal/$module/${path.sanitisePath()}"
    }*/
    /** Returning files are read-only */
    fun getGdxFile(module: String, path: String): FileHandle {
        checkExistence(module)
        return if (moduleInfo[module]!!.isInternal)
            Gdx.files.internal("$modDirInternal/$module/$path")
        else
            Gdx.files.absolute("$modDirExternal/$module/$path")
    }
    fun getFile(module: String, path: String): File {
        checkExistence(module)
        return if (moduleInfo[module]!!.isInternal)
            FileSystems.getDefault().getPath("$modDirInternal/$module/$path").toFile()
        else
            FileSystems.getDefault().getPath("$modDirExternal/$module/$path").toFile()
    }
    fun hasFile(module: String, path: String): Boolean {
        if (!moduleInfo.containsKey(module)) return false
        return getFile(module, path).exists()
    }
    fun getFiles(module: String, path: String): Array<File> {
        checkExistence(module)
        val dir = getFile(module, path)
        if (!dir.isDirectory) {
            throw FileNotFoundException("The path is not a directory")
        }
        else {
            return dir.listFiles()
        }
    }

    /** Get a common file (literal file or directory) from all the installed mods. Files are guaranteed to exist. If a mod does not
     * contain the file, the mod will be skipped.
     *
     * @return List of pairs<modname, file>
     */
    fun getFilesFromEveryMod(path: String): List<Pair<String, File>> {
        val path = path.sanitisePath()
        val moduleNames = moduleInfo.keys.toList()

        val filesList = ArrayList<Pair<String, File>>()
        moduleNames.forEach {
            val file = getFile(it, path)
            if (file.exists()) filesList.add(it to file)
        }

        return filesList.toList()
    }

    /** Get a common file (literal file or directory) from all the installed mods. Files are guaranteed to exist. If a mod does not
     * contain the file, the mod will be skipped.
     *
     * Returning files are read-only.
     * @return List of pairs<modname, filehandle>
     */
    fun getGdxFilesFromEveryMod(path: String): List<Pair<String, FileHandle>> {
        val path = path.sanitisePath()
        val moduleNames = moduleInfo.keys.toList()

        val filesList = ArrayList<Pair<String, FileHandle>>()
        moduleNames.forEach {
            val file = getGdxFile(it, path)
            if (file.exists()) filesList.add(it to file)
        }

        return filesList.toList()
    }

    fun disposeMods() {
        entryPointClasses.forEach { it.dispose() }
    }


    object GameBlockLoader {
        init {
            Terrarum.blockCodex = BlockCodex()
            Terrarum.wireCodex = WireCodex()
        }

        @JvmStatic operator fun invoke(module: String) {
            Terrarum.blockCodex.fromModule(module, "blocks/blocks.csv")
            Terrarum.wireCodex.fromModule(module, "wires/")
        }
    }

    object GameItemLoader {
        const val itemPath = "items/"

        init {
            Terrarum.itemCodex = ItemCodex()
        }

        @JvmStatic operator fun invoke(module: String) {
            register(module, CSVFetcher.readFromModule(module, itemPath + "itemid.csv"))
        }

        fun fromCSV(module: String, csvString: String) {
            val csvParser = org.apache.commons.csv.CSVParser.parse(
                    csvString,
                    CSVFetcher.terrarumCSVFormat
            )
            val csvRecordList = csvParser.records
            csvParser.close()
            register(module, csvRecordList)
        }

        private fun register(module: String, csv: List<CSVRecord>) {
            csv.forEach {
                val className: String = it["classname"].toString()
                val internalID: Int = it["id"].toInt()
                val itemName: String = "item@$module:$internalID"

                printdbg(this, "Reading item  ${itemName} <<- internal #$internalID with className $className")

                moduleClassloader[module].let {
                    if (it == null) {
                        val loadedClass = Class.forName(className)
                        val loadedClassConstructor = loadedClass.getConstructor(ItemID::class.java)
                        val loadedClassInstance = loadedClassConstructor.newInstance(itemName)
                        ItemCodex[itemName] = loadedClassInstance as GameItem
                    }
                    else {
                        val loadedClass = it.loadClass(className)
                        val loadedClassConstructor = loadedClass.getConstructor(ItemID::class.java)
                        val loadedClassInstance = loadedClassConstructor.newInstance(itemName)
                        ItemCodex[itemName] = loadedClassInstance as GameItem
                    }
                }
            }
        }
    }

    object GameLanguageLoader {
        const val langPath = "locales/"

        @JvmStatic operator fun invoke(module: String) {
            Lang.load(getFile(module, langPath))
        }
    }

    object GameMaterialLoader {
        const val matePath = "materials/"

        init {
            Terrarum.materialCodex = MaterialCodex()
        }

        @JvmStatic operator fun invoke(module: String) {
            Terrarum.materialCodex.fromModule(module, matePath + "materials.csv")
        }
    }

    /**
     * A sugar-library for easy texture pack creation
     */
    object GameRetextureLoader {
        const val retexturesPath = "retextures/"
        val retexables = listOf("blocks","wires")
        val altFilePaths = HashMap<String, FileHandle>()
        val retexableCallbacks = HashMap<String, () -> Unit>()

        init {
            retexableCallbacks["blocks"] = {
                App.tileMaker(true)
            }

        }

        @JvmStatic operator fun invoke(module: String) {
            val targetModNames = getFiles(module, retexturesPath).filter { it.isDirectory }
            targetModNames.forEach { baseTargetModDir ->
                // modules/<module>/retextures/basegame
//                printdbg(this, "baseTargetModDir = $baseTargetModDir")

                retexables.forEach { category ->
                    val dir = File(baseTargetModDir, category)
                    // modules/<module>/retextures/basegame/blocks

//                    printdbg(this, "cats: ${dir.path}")

                    if (dir.isDirectory && dir.exists()) {
                        dir.listFiles { it: File ->
                            it.name.contains('-')
                        }?.forEach {
                            // <other modname>-<hopefully a number>.tga or .png
                            val tokens = it.name.split('-')
                            if (tokens.size > 1) {
                                val modname = tokens[0]
                                val filename = tokens.tail().joinToString("-")
                                altFilePaths["$modDirInternal/$modname/$category/$filename"] = getGdxFile(module, "$retexturesPath${baseTargetModDir.name}/$category/${it.name}")
                            }
                        }
                    }

//                    retexableCallbacks[category]?.invoke()
                }
            }

            printdbg(this, "ALT FILE PATHS")
            altFilePaths.forEach { (k, v) -> printdbg(this, "$k -> $v") }
        }
    }

    object GameCraftingRecipeLoader {
        const val recipePath = "crafting/"

        @JvmStatic operator fun invoke(module: String) {
            getFile(module, recipePath).listFiles { it: File -> it.name.lowercase().endsWith(".json") }?.forEach { jsonFile ->
                Terrarum.craftingCodex.addFromJson(JsonFetcher(jsonFile), module, jsonFile.name)
            }
        }
    }

}

private class JarFileLoader(urls: Array<URL>) : URLClassLoader(urls) {
    @Throws(MalformedURLException::class)
    fun addFile(path: String) {
        val urlPath = "jar:file://$path!/"
        addURL(URL(urlPath))
    }
}