package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.utils.JsonValue
import net.torvald.gdx.graphics.Cvec
import net.torvald.terrarum.App.*
import net.torvald.terrarum.App.setToGameConfig
import net.torvald.terrarum.audio.AudioCodex
import net.torvald.terrarum.blockproperties.*
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gamecontroller.IME
import net.torvald.terrarum.gameitems.FixtureInteractionBlocked
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.itemproperties.CraftingCodex
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.itemproperties.MaterialCodex
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameitems.BlockBase
import net.torvald.terrarum.modulebasegame.worldgenerator.OregenParams
import net.torvald.terrarum.modulebasegame.worldgenerator.Worldgen
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.utils.CSVFetcher
import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.terrarum.weather.WeatherCodex
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
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
            val descTranslations: Map<String, String>,
            val author: String,
            val packageName: String,
            val entryPoint: String,
            val releaseDate: String,
            val version: String,
            val jar: String,
            val dependencies: Array<String>,
            val isInternal: Boolean,
            val configPlan: List<String>
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
        val loadOrderFile = File(App.loadOrderDir)
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

                val loadScriptMod = if (App.getConfigBoolean("enablescriptmods")) true else (index == 0)


                val moduleName = it[0]
                this.loadOrder.add(moduleName)
                printmsg(this, "Loading module $moduleName")
                var module: ModuleMetadata? = null

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


                    val descTranslations = HashMap<String, String>()
                    modMetadata.stringPropertyNames().filter { it.startsWith("description_") }.forEach { key ->
                        val langCode = key.substringAfter('_')
                        descTranslations[langCode] = modMetadata.getProperty(key)
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

                    val configPlan = ArrayList<String>()
                    File("$modDir/$moduleName/configplan.csv").let {
                        if (it.exists() && it.isFile) {
                            configPlan.addAll(it.readLines(Common.CHARSET).filter { it.isNotBlank() })
                        }
                    }

                    module = ModuleMetadata(index, isDir, getGdxFile("$modDir/$moduleName/icon.png"), properName, description, descTranslations, author, packageName, entryPoint, releaseDate, version, jar, dependency, isInternal, configPlan)

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


                    moduleInfo[moduleName] = module

                    printdbg(this, module)

                    // do retexturing if retextures directory exists
                    if (hasFile(moduleName, "retextures")) {
                        printdbg(this, "Trying to load Retextures on ${moduleName}")
                        GameRetextureLoader(moduleName)
                    }

                    // add locales if exists
                    if (hasFile(moduleName, "locales")) {
                        printdbg(this, "Trying to load Locales on ${moduleName}")
                        GameLanguageLoader(moduleName)
                    }

                    // add keylayouts if exists
                    if (hasFile(moduleName, "keylayout")) {
                        printdbg(this, "Trying to load Keyboard Layouts on ${moduleName}")
                        GameIMELoader(moduleName)
                    }

                    // run entry script in entry point
                    if (entryPoint.isNotBlank()) {
                        if (!loadScriptMod) {
                            throw ScriptModDisallowedException()
                        }


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

                                if (!App.IS_DEVELOPMENT_BUILD && jarHash != hash) {
                                    printdbg(this, "Hash expected: $jarHash, got: $hash")
                                    throw IllegalStateException("Module Jarfile hash mismatch")
                                }

                                // check for module-info.java
                                /*val moduleInfoPath = cl.getResources("module-info.class").toList().filter { it.toString().contains("$moduleName/$jar!/module-info.class") && it.toString().endsWith("module-info.class")}
                                if (moduleInfoPath.isEmpty()) {
                                    throw IllegalStateException("module-info not found on $moduleName")
                                }*/

                                newClass = cl.loadClass(entryPoint)
                            }
                            // for modules that are not (meant to be used by the "basegame" kind of modules)
                            else {
                                newClass = Class.forName(entryPoint)
                            }
                        }
                        catch (e: Throwable) {
                            printdbgerr(this, "Module failed to load, skipping: $moduleName")
                            printdbgerr(this, "\t$e")
                            print(App.csiR); e.printStackTrace(System.out); print(App.csi0)

                            logError(LoadErrorType.YOUR_FAULT, moduleName, e)

                            moduleInfo.remove(moduleName)
                            moduleInfoErrored[moduleName] = module
                        }

                        if (newClass != null) {
                            val newClassConstructor = newClass.getConstructor(/* no args defined */)
                            val newClassInstance = newClassConstructor.newInstance(/* no args defined */)

                            entryPointClasses.add(newClassInstance as ModuleEntryPoint)
                            (newClassInstance as ModuleEntryPoint).invoke()

                            printdbg(this, "Module loaded successfully: $moduleName")
                        }
                        else {
                            moduleInfo.remove(moduleName)
                            moduleInfoErrored[moduleName] = module
                            printdbg(this, "Module did not load: $moduleName")
                        }

                    }

                    printmsg(this, "Module processed: $moduleName")
                }
                catch (noSuchModule: FileNotFoundException) {
                    printmsgerr(this, "No such module, skipping: $moduleName")

                    logError(LoadErrorType.NOT_EVEN_THERE, moduleName, noSuchModule)

                    moduleInfo.remove(moduleName)
                    if (module != null) moduleInfoErrored[moduleName] = module
                }
                catch (noSuchModule2: ModuleDependencyNotSatisfied) {
                    printmsgerr(this, noSuchModule2.message)

                    logError(LoadErrorType.NOT_EVEN_THERE, moduleName, noSuchModule2)

                    moduleInfo.remove(moduleName)
                    if (module != null) moduleInfoErrored[moduleName] = module
                }
                catch (noScriptModule: ScriptModDisallowedException) {
                    printmsgerr(this, noScriptModule.message)

                    logError(LoadErrorType.MY_FAULT, moduleName, noScriptModule)

                    moduleInfo.remove(moduleName)
                    if (module != null) moduleInfoErrored[moduleName] = module
                }
                catch (e: Throwable) {
                    // TODO: Instead of skipping module with error, just display the error message onto the face?


                    printmsgerr(this, "There was an error while loading module $moduleName")
                    printmsgerr(this, "\t$e")
                    print(App.csiR); e.printStackTrace(System.out); print(App.csi0)

                    logError(LoadErrorType.YOUR_FAULT, moduleName, e)

                    moduleInfo.remove(moduleName)
                    if (module != null) moduleInfoErrored[moduleName] = module
                }
                finally {

                }
            }
        }
    }

    private class ModuleDependencyNotSatisfied(want: String, have: String) :
            RuntimeException("Required: $want, Installed: $have")

    private class ScriptModDisallowedException : RuntimeException("Script Mods disabled")

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
    fun getGdxFiles(module: String, path: String): Array<FileHandle> {
        checkExistence(module)
        val dir = getGdxFile(module, path)
        if (!dir.isDirectory) {
            throw FileNotFoundException("The path is not a directory")
        }
        else {
            return dir.list()
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

    fun getLoadOrderTextForSavegame(): String {
        return loadOrder.filter { moduleInfo[it] != null }.map { "$it ${moduleInfo[it]!!.version}" }.joinToString("\n")
    }


    object GameBlockLoader {
        init {
            Terrarum.blockCodex = BlockCodex()
            Terrarum.wireCodex = WireCodex()
        }

        @JvmStatic operator fun invoke(module: String) {
            Terrarum.blockCodex.fromModule(module, "blocks/blocks.csv") { tile ->
                // register blocks as items
                ItemCodex[tile.id] = makeNewItemObj(tile, false)
                if (IS_DEVELOPMENT_BUILD) print(tile.id+" ")

                if (BlockCodex[tile.id].isWallable) {
                    ItemCodex["wall@" + tile.id] = makeNewItemObj(tile, true)
                    if (IS_DEVELOPMENT_BUILD) print("wall@" + tile.id + " ")
                }


                // crafting recipes: tile -> 2x wall
                if (tile.isWallable && tile.isSolid && !tile.isActorBlock) {
                    CraftingRecipeCodex.addRecipe(
                        CraftingCodex.CraftingRecipe(
                        "",
                        arrayOf(
                            CraftingCodex.CraftingIngredients(
                            tile.id, CraftingCodex.CraftingItemKeyMode.VERBATIM, 1
                        )),
                        2,
                        "wall@"+tile.id,
                        module
                    ))
                }
            }
            Terrarum.wireCodex.fromModule(module, "wires/") { wire ->

            }
            Terrarum.wireCodex.portsFromModule(module, "wires/")
            Terrarum.wireCodex.wireDecaysFromModule(module, "wires/")
        }

        private fun makeNewItemObj(tile: BlockProp, isWall: Boolean) = object : GameItem(
            if (isWall) "wall@"+tile.id else tile.id
        ), FixtureInteractionBlocked {
            override var baseMass: Double = (tile.density / 100.0) * (if (tile.isPlatform) 0.5 else 1.0)
            override var baseToolSize: Double? = null
            override var inventoryCategory = if (isWall) Category.WALL else Category.BLOCK
            override var canBeDynamic = false
            override val materialId = tile.material
            override var equipPosition = EquipPosition.HAND_GRIP
            //        override val itemImage: TextureRegion
//            get() {
//                val itemSheetNumber = App.tileMaker.tileIDtoItemSheetNumber(originalID)
//                val bucket =  if (isWall) BlocksDrawer.tileItemWall else BlocksDrawer.tileItemTerrain
//                return bucket.get(
//                        itemSheetNumber % App.tileMaker.ITEM_ATLAS_TILES_X,
//                        itemSheetNumber / App.tileMaker.ITEM_ATLAS_TILES_X
//                )
//            }

            @Transient private var isWall1: Boolean = true

            init {
                isWall1 = isWall
                tags.addAll(tile.tags)
                if (isWall) tags.add("WALL")
                originalName =
                    if (isWall && tags.contains("UNLIT")) "${tile.nameKey}>>=BLOCK_UNLIT_TEMPLATE>>=BLOCK_WALL_NAME_TEMPLATE"
                    else if (isWall) "${tile.nameKey}>>=BLOCK_WALL_NAME_TEMPLATE"
                    else if (tags.contains("UNLIT")) "${tile.nameKey}>>=BLOCK_UNLIT_TEMPLATE"
                    else tile.nameKey
            }

            override fun getLumCol() =
                if (isWall1) Cvec(0)
                else BlockCodex[originalID].getLumCol(0, 0)

            override fun startPrimaryUse(actor: ActorWithBody, delta: Float): Long {
                return BlockBase.blockStartPrimaryUse(actor, this, dynamicID, delta)
            }

            override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) {
                BlockBase.blockEffectWhenEquipped(actor, delta)
            }
        }
    }

    object GameOreLoader {
        init {
            Terrarum.oreCodex = OreCodex()
        }

        @JvmStatic operator fun invoke(module: String) {
            // register ore codex
            Terrarum.oreCodex.fromModule(module, "ores/ores.csv")

            // register to worldgen
            try {
                CSVFetcher.readFromModule(module, "ores/worldgen.csv").forEach { rec ->
                    val tile = "ores@$module:${rec.get("id")}"
                    val freq = rec.get("freq").toDouble()
                    val power = rec.get("power").toDouble()
                    val scale = rec.get("scale").toDouble()
                    val ratio = rec.get("ratio").toDouble()
                    val tiling = rec.get("tiling")
                    val blockTagNonGrata = rec.get("blocktagnongrata").split(',').map { it.trim().toUpperCase() }.toHashSet()

                    Worldgen.registerOre(OregenParams(tile, freq, power, scale, ratio, tiling, blockTagNonGrata))
                }
            }
            catch (e: IOException) {
                e.printStackTrace()
            }
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
                val tags = it["tags"].split(',').map { it.trim().toUpperCase() }.toHashSet()

                printdbg(this, "Reading item  ${itemName} <<- internal #$internalID with className $className")

                moduleClassloader[module].let {
                    if (it == null) {
                        val loadedClass = Class.forName(className)
                        val loadedClassConstructor = loadedClass.getConstructor(ItemID::class.java)
                        val loadedClassInstance = loadedClassConstructor.newInstance(itemName)
                        ItemCodex[itemName] = loadedClassInstance as GameItem
                        ItemCodex[itemName]!!.tags.addAll(tags)
                    }
                    else {
                        val loadedClass = it.loadClass(className)
                        val loadedClassConstructor = loadedClass.getConstructor(ItemID::class.java)
                        val loadedClassInstance = loadedClassConstructor.newInstance(itemName)
                        ItemCodex[itemName] = loadedClassInstance as GameItem
                        ItemCodex[itemName]!!.tags.addAll(tags)
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

    object GameIMELoader {
        const val keebPath = "keylayout/"

        @JvmStatic operator fun invoke(module: String) {
            val FILE = getFile(module, keebPath)

            FILE.listFiles { file, s -> s.endsWith(".${IME.KEYLAYOUT_EXTENSION}") }.sortedBy { it.name }.forEach {
                printdbg(this, "Registering Low layer ${it.nameWithoutExtension.lowercase()}")
                IME.registerLowLayer(it.nameWithoutExtension.lowercase(), IME.parseKeylayoutFile(it))
            }

            FILE.listFiles { file, s -> s.endsWith(".${IME.IME_EXTENSION}") }.sortedBy { it.name }.forEach {
                printdbg(this, "Registering High layer ${it.nameWithoutExtension.lowercase()}")
                IME.registerHighLayer(it.nameWithoutExtension.lowercase(), IME.parseImeFile(it))
            }

            val iconFile = getFile(module, keebPath + "icons.tga").let {
                if (it.exists()) it else getFile(module, keebPath + "icons.png")
            }

            if (iconFile.exists()) {
                val iconSheet = TextureRegionPack(iconFile.path, 20, 20)
                val iconPixmap = Pixmap(Gdx.files.absolute(iconFile.path))
                for (k in 0 until iconPixmap.height step 20) {
                    val langCode = StringBuilder()
                    for (c in 0 until 20) {
                        val x = c
                        var charnum = 0
                        for (b in 0 until 7) {
                            val y = k + b
                            if (iconPixmap.getPixel(x, y) and 255 != 0) {
                                charnum = charnum or (1 shl b)
                            }
                        }
                        if (charnum != 0) langCode.append(charnum.toChar())
                    }

                    if (langCode.isNotEmpty()) {
                        printdbg(this, "Icon order #${(k+1) / 20} - icons[\"$langCode\"] = iconSheet.get(1, ${k/20})")
                        IME.icons["$langCode"] = iconSheet.get(1, k / 20).also { it.flip(false, false) }
                    }
                }

                App.disposables.add(iconSheet)
                iconPixmap.dispose()
            }

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

    object GameFluidLoader {
        const val fluidPath = "fluid/"

        init {
            Terrarum.fluidCodex = FluidCodex()
        }

        @JvmStatic operator fun invoke(module: String) {
            Terrarum.fluidCodex.fromModule(module, fluidPath + "fluids.csv")
        }
    }

    object GameAudioLoader {
        val audioPath = listOf(
            "audio/music",
            "audio/effects",
            "audio/ambient",
        )

        init {
            Terrarum.audioCodex = AudioCodex()
        }

        private fun loadAudio(basename: String, file: FileHandle) {
            if (file.isDirectory)
                file.list().forEach { loadAudio("$basename.${it.name()}", it) }
            else {
                val id = basename.substringBeforeLast('.').substringBeforeLast('.')
                Terrarum.audioCodex.addToAudioPool(id, file)
                printdbg(this, "Registering audio $id ($file)")
            }
        }

        @JvmStatic operator fun invoke(module: String) {
            audioPath.forEach {
                if (getGdxFile(module, it).let { it.exists() && it.isDirectory }) {
                    getGdxFiles(module, it).forEach { file -> loadAudio("${it.substringAfter("audio/")}.${file.name()}", file) }
                }
            }
        }
    }

    object GameWeatherLoader {
        val weatherPath = "weathers/"

        init {
            Terrarum.weatherCodex = WeatherCodex()
        }

        @JvmStatic operator fun invoke(module: String) {
            getFiles(module, weatherPath).filter { it.isFile && it.name.lowercase().endsWith(".json") }.forEach {
                Terrarum.weatherCodex.readFromJson(module, it)
            }
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
        const val smeltingPath = "smelting/"

        @JvmStatic operator fun invoke(module: String) {
            getFile(module, recipePath).listFiles { it: File -> it.name.lowercase().endsWith(".json") }?.forEach { jsonFile ->
                Terrarum.craftingCodex.addFromJson(JsonFetcher(jsonFile), module, jsonFile.name)
            }

            getFile(module, smeltingPath).listFiles { it: File -> it.name.lowercase().endsWith(".json") }?.forEach { jsonFile ->
                Terrarum.craftingCodex.addSmeltingFromJson(JsonFetcher(jsonFile), module, jsonFile.name)
            }
        }
    }


    object GameExtraGuiLoader {
        internal val guis = ArrayList<(TerrarumIngame) -> UICanvas>()

        @JvmStatic fun register(uiCreationFun: (TerrarumIngame) -> UICanvas) {
            guis.add(uiCreationFun)
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