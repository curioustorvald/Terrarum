package net.torvald.terrarum.serialise

import com.badlogic.gdx.Gdx
import net.torvald.random.HQRNG
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.*
import net.torvald.terrarum.utils.JsonWriter.getJsonBuilder
import net.torvald.util.SortedArrayList
import java.io.File
import java.nio.charset.Charset
import kotlin.math.roundToInt

internal class RNGPool() {
    private val RNG = HQRNG()
    private val used = SortedArrayList<Int>()

    init {
        for (i in 0 until 32767) {
            used.add(i)
        }
    }

    fun next(): Int {
        var n = RNG.nextLong().ushr(32).toInt()
        while (used.contains(n)) {
            n = RNG.nextLong().ushr(32).toInt()
        }
        used.add(n)
        return n
    }
}

/**
 * Created by minjaesong on 2018-10-03.
 */
object SavegameWriter {

    // TODO create temporary files (worldinfo), create JSON files on RAM, pack those into TEVd as per Savegame container.txt

    private val rngPool = RNGPool()

    private val charset = Charset.forName("UTF-8")

    private lateinit var playerName: String

    operator fun invoke(pnameOverride: String? = null): Boolean {
        playerName = pnameOverride ?: "${Terrarum.ingame!!.actorGamer!!.actorValue[AVKey.NAME]}"
        if (playerName.isEmpty()) playerName = "Test subject ${Math.random().times(0x7FFFFFFF).roundToInt()}"

        try {
            val diskImage = generateNewDiskImage()
            val outFile = File("${AppLoader.defaultSaveDir}/$playerName")
            VDUtil.dumpToRealMachine(diskImage, outFile)

            return true
        }
        catch (e: Throwable) {
            e.printStackTrace()
        }

        return false
    }


    fun generateNewDiskImage(): VirtualDisk {
        val creationDate = System.currentTimeMillis() / 1000L
        val ingame = Terrarum.ingame!!
        val gameworld = ingame.world
        val player = ingame.actorGamer!!
        val disk = VDUtil.createNewDisk(0x7FFFFFFFFFFFFFFFL, "Tesv-$playerName", charset)
        val ROOT = disk.root.entryID

        // serialise current world (stage)

        val worldBytes = WriteLayerDataZip(gameworld) // filename can be anything that is "world[n]" where [n] is any number
        if (worldBytes == null) {
            throw Error("Serialising world failed")
        }

        if (!worldBytes.sliceArray(0..3).contentEquals(WriteLayerDataZip.MAGIC)) {
            worldBytes.forEach {
                print(it.toUInt().and(255u).toString(16).toUpperCase().padStart(2, '0'))
                print(' ')
            }; println()
            throw Error()
        }

        // add current world (stage) to the disk
        VDUtil.registerFile(disk, DiskEntry(
                gameworld.worldIndex, ROOT,
                "world${gameworld.worldIndex}".toByteArray(charset),
                creationDate, creationDate,
                EntryFile(worldBytes)
        ))



        // TODO world[n] is done, needs whole other things


        // worldinfo0..3
        val worldinfoBytes = WriteWorldInfo(ingame)
        worldinfoBytes?.forEachIndexed { index, bytes ->
            VDUtil.registerFile(disk, DiskEntry(
                    32766 - index, ROOT, "worldinfo$index".toByteArray(charset),
                    creationDate, creationDate,
                    EntryFile(bytes)
            ))
        } ?: throw Error("Serialising worldinfo failed")

        // loadorder.txt
        VDUtil.registerFile(disk, DiskEntry(
                32767, ROOT, "load_order.txt".toByteArray(charset),
                creationDate, creationDate,
                EntryFile(ByteArray64.fromByteArray(Gdx.files.internal("./assets/mods/LoadOrder.csv").readBytes()))
        ))

        // actors
        ingame.actorContainerActive.forEach {
            VDUtil.registerFile(disk, DiskEntry(
                    rngPool.next(), ROOT,
                    it.referenceID.toString(16).toUpperCase().toByteArray(charset),
                    creationDate, creationDate,
                    EntryFile(serialiseActor(it))
            ))
        }
        ingame.actorContainerInactive.forEach {
            VDUtil.registerFile(disk, DiskEntry(
                    rngPool.next(), ROOT,
                    it.referenceID.toString(16).toUpperCase().toByteArray(charset),
                    creationDate, creationDate,
                    EntryFile(serialiseActor(it))
            ))
        }

        // items
        ItemCodex.dynamicItemDescription.forEach { dynamicID, item ->
            VDUtil.registerFile(disk, DiskEntry(
                    rngPool.next(), ROOT,
                    dynamicID.toByteArray(charset),
                    creationDate, creationDate,
                    EntryFile(serialiseItem(item))
            ))
        }

        System.gc()

        return disk
    }

    fun modifyExistingSave(savefile: File): VirtualDisk {
        TODO()
    }

    private fun serialiseActor(a: Actor): ByteArray64 {
        val gson = getJsonBuilder().toJson(a).toByteArray(charset)
        return ByteArray64.fromByteArray(gson)
    }

    private fun serialiseItem(i: GameItem): ByteArray64 {
        val gson = getJsonBuilder().toJson(i).toByteArray(charset)
        return ByteArray64.fromByteArray(gson)
    }
}