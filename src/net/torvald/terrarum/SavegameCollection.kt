package net.torvald.terrarum

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.JsonWriter
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.savegame.*
import net.torvald.terrarum.savegame.VDFileID.PLAYER_SCREENSHOT
import net.torvald.terrarum.savegame.VDFileID.ROOT
import net.torvald.terrarum.savegame.VDFileID.SAVEGAMEINFO
import net.torvald.terrarum.savegame.VDFileID.WORLD_SCREENSHOT
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.terrarum.utils.forEachSiblings
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.zip.GZIPInputStream
import kotlin.io.path.Path
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2023-06-24.
 */
class SavegameCollection(files0: List<DiskSkimmer>) {

    /** Sorted in reverse by the last modified time of the files, index zero being the most recent */
    val files = files0.sortedBy { it.diskFile.name }.sortedByDescending {
        it.getLastModifiedTime().shl(2) or
        it.diskFile.extension.matches(Regex("^[abc]${'$'}")).toLong(1) or
        it.diskFile.extension.isBlank().toLong(0)
    }
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

    fun getBaseFile(): DiskSkimmer {
        return files.first { it.diskFile.extension.isBlank() }
    }

    fun getUUID(): UUID {
        var uuid: UUID? = null
        loadable().getFile(SAVEGAMEINFO)!!.let {
            JsonFetcher.readFromJsonString(ByteArray64Reader(it.bytes, Common.CHARSET)).forEachSiblings { name, value ->
                if (name == "worldIndex" || name == "uuid") uuid = UUID.fromString(value.asString())
            }
        }
        return uuid!!
    }

    fun renamePlayer(name: String) {
        files.forEach { skimmer ->
            skimmer.rebuild()
            skimmer.getFile(SAVEGAMEINFO)!!.let { file ->
                val json = JsonFetcher.readFromJsonString(ByteArray64Reader(file.bytes, Common.CHARSET))
                json.getChild("actorValue").getChild(AVKey.NAME).set(name)

                val jsonBytes = json.prettyPrint(JsonWriter.OutputType.json, 0).encodeToByteArray().toByteArray64()
                val newEntry = DiskEntry(SAVEGAMEINFO, ROOT, skimmer.requestFile(SAVEGAMEINFO)!!.creationDate, App.getTIME_T(), EntryFile(jsonBytes))

                skimmer.appendEntry(newEntry)

                skimmer.setDiskName(name, Common.CHARSET)
            }
        }
    }

    fun renameWorld(name: String) {
        files.forEach { skimmer ->
            skimmer.setDiskName(name, Common.CHARSET)
        }
    }

    fun getThumbnail(width: Int, height: Int, shrinkage: Double) = this.loadable().getThumbnail(width, height, shrinkage)
}

fun DiskSkimmer.getTgaGz(vid: EntryID, width: Int, height: Int, shrinkage: Double): TextureRegion? {
    return this.requestFile(vid).let { file ->
        if (file != null) {
            val zippedTga = (file.contents as EntryFile).bytes
            val gzin = GZIPInputStream(ByteArray64InputStream(zippedTga))
            val tgaFileContents = gzin.readAllBytes(); gzin.close()
            val pixmap = Pixmap(tgaFileContents, 0, tgaFileContents.size)
            TextureRegion(Texture(pixmap)).also {
                App.disposables.add(it.texture)
                // do cropping and resizing
                it.setRegion(
                    ((pixmap.width - width*2) / shrinkage).roundToInt(),
                    ((pixmap.height - height*2) / shrinkage).roundToInt(),
                    (width * shrinkage).roundToInt(),
                    (height * shrinkage).roundToInt()
                )
            }
        }
        else {
            null
        }
    }
}
fun DiskSkimmer.getTgaGzPixmap(vid: EntryID, width: Int, height: Int, shrinkage: Double): Pixmap? {
    return this.requestFile(vid).let { file ->
        if (file != null) {
            val zippedTga = (file.contents as EntryFile).bytes
            val gzin = GZIPInputStream(ByteArray64InputStream(zippedTga))
            val tgaFileContents = gzin.readAllBytes(); gzin.close()
            val pixmap = Pixmap(tgaFileContents, 0, tgaFileContents.size)
            return pixmap
        }
        else {
            null
        }
    }
}
fun DiskSkimmer.getThumbnail(width: Int, height: Int, shrinkage: Double) =
    when (this.getSaveKind()) {
        1 -> this.getTgaGz(PLAYER_SCREENSHOT, width, height, shrinkage)
        2 -> this.getTgaGz(WORLD_SCREENSHOT, width, height, shrinkage)
        else -> throw IllegalArgumentException("Unknown save kind: ${this.getSaveKind()}")
    }
fun DiskSkimmer.getThumbnailPixmap(width: Int, height: Int, shrinkage: Double) =
    when (this.getSaveKind()) {
        1 -> this.getTgaGzPixmap(PLAYER_SCREENSHOT, width, height, shrinkage)
        2 -> this.getTgaGzPixmap(WORLD_SCREENSHOT, width, height, shrinkage)
        else -> throw IllegalArgumentException("Unknown save kind: ${this.getSaveKind()}")
    }

class SavegameCollectionPair(private val player: SavegameCollection?, private val world: SavegameCollection?) {

//    private var manualPlayer: DiskSkimmer? = null
//    private var manualWorld: DiskSkimmer? = null
//    private var autoPlayer: DiskSkimmer? = null
//    private var autoWorld: DiskSkimmer? = null

    /* removing auto/manual discrimination: on Local Asynchronous Multiplayer, if newer autosave is available, there is
     * no choice but loading one to preserve the data; then why bother having two? */
    private var playerDisk: DiskSkimmer? = null; private set
    private var worldDisk: DiskSkimmer? = null; private set

    var status = 0 // 0: none available, 1: loadable manual save is newer than loadable auto; 2: loadable autosave is newer than loadable manual
        private set

    val newerSaveIsDamaged: Boolean // only when most recent save is corrupted

    init {
        if (player != null && world != null) {
            printdbg(this, "player files: " + player.files.joinToString { it.diskFile.name })
            printdbg(this, "world files:" + world.files.joinToString { it.diskFile.name })

            var pc = 0
            var wc = 0

            playerDisk = player.files[pc]
            worldDisk = world.files[wc]

            while (pc < player.files.size && wc < world.files.size) {
                // 0b pw
                val dmgflag = playerDiskNotDamaged(playerDisk!!).toInt(1) or worldDiskNotDamaged(worldDisk!!).toInt()

                when (dmgflag) {
                    3 -> break
                    2 -> {
                        worldDisk = world.files[++wc]
                    }
                    1 -> {
                        playerDisk = player.files[++pc]
                    }
                    0 -> {
                        worldDisk = world.files[++wc]
                        playerDisk = player.files[++pc]
                    }
                }

                // if it's time to exit the loop and all tested saves were damaged:
                if (pc == player.files.size) playerDisk = null
                if (wc == world.files.size) worldDisk = null
            }

            newerSaveIsDamaged = (pc + wc > 0)
        }
        else {
            newerSaveIsDamaged = false
        }

        status = if (playerDisk != null && worldDisk != null && (playerDisk!!.isAutosaved() || worldDisk!!.isAutosaved()))
                2
        else (player != null && world != null).toInt()

        printdbg(this, "playerDisk = ${playerDisk?.diskFile?.path}")
        printdbg(this, "worldDisk = ${worldDisk?.diskFile?.path}")
        printdbg(this, "status = $status")
    }

    /*init {
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
                            // world is modified after another player playing on the same world but only left an autosave
                            // there is no choice but loading the autosave in such scenario to preserve the data
                            else {
                                wc += 1
                            }
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
    } */

    private fun DiskSkimmer.isAutosaved() = this.getSaveMode().and(0b0000_0010) != 0

    private fun playerDiskNotDamaged(disk: DiskSkimmer): Boolean {
        return true
    }

    private fun worldDiskNotDamaged(disk: DiskSkimmer): Boolean {
        return true
    }

    fun moreRecentAutosaveAvailable() = (status == 2)
    fun saveAvaliable() = (status > 0)

    /*fun getManualSave(): DiskPair? {
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
    }*/

    fun getLoadableSave(): DiskPair? {
        return if (status == 0) null
        else DiskPair(playerDisk!!, worldDisk!!)
    }
}

data class DiskPair(val player: DiskSkimmer, val world: DiskSkimmer) {

}