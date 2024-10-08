package net.torvald.terrarum.modulebasegame

import net.torvald.reflection.extortField
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.archivers.ClusteredFormatDOM
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.archivers.Clustfile
import net.torvald.terrarum.savegame.*
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.serialise.toBig64
import java.io.File

/**
 * Created by minjaesong on 2024-10-08.
 */
object SavegameConverter {

    fun type254toType11(infile: File, outFile: File) {
        val type254DOM = VDUtil.readDiskArchive(infile)
        type254DOMtoType11Disk(type254DOM, outFile)
    }

    private val getGenver = Regex("""(?<="genver" ?: ?)[0-9]+""")

    private fun type254DOMtoType11Disk(type254DOM: VirtualDisk, outFile: File) {
        val version: Long = when (type254DOM.saveKind) {
            VDSaveKind.PLAYER_DATA, VDSaveKind.WORLD_DATA -> {
                val savegameInfo = ByteArray64Reader(type254DOM.getFile(VDFileID.SAVEGAMEINFO)!!.bytes, Common.CHARSET)
                CharArray(128).let {
                    savegameInfo.read(it, 0, 128)
                    getGenver.find(String(it))?.value?.toLong()!!
                }
            }
            else -> 0L
        }

        val newDisk = ClusteredFormatDOM.createNewArchive(
            outFile,
            Common.CHARSET,
            type254DOM.getDiskName(Common.CHARSET),
            ClusteredFormatDOM.MAX_CAPA_IN_SECTORS,
            byteArrayOf(
                -1, // pad
                byteArrayOf(15, -1)[type254DOM.saveOrigin.ushr(4).and(1)], // imported/native
                byteArrayOf(0x00, 0x50, 0x57)[type254DOM.saveKind], // player/world
                byteArrayOf(0x6d, 0x6d, 0x61, 0x61)[type254DOM.saveMode], // manual/auto
                type254DOM.extraInfoBytes[4], type254DOM.extraInfoBytes[5], // snapshot info
                0, 0,// reserved
            ) + version.toBig64() // VERSION_RAW in big-endian
        )
        val DOM = ClusteredFormatDOM(newDisk)
        val root = DOM.getRootFile()

        // do filecopy
        type254DOM.entries.filter { it.key != 0L }.forEach { entryID, diskEntry ->
            val filename = Common.type254EntryIDtoType17Filename(entryID)
            if (diskEntry.contents !is EntryFile) throw IllegalStateException("Entry in the savegame is not a file (${diskEntry.contents.javaClass.simpleName})")

            val entry = diskEntry.contents as EntryFile

            val oldBytes = entry.bytes.toByteArray()

            Clustfile(DOM, root, filename).let { file ->
                // write bytes
                file.createNewFile()
                file.writeBytes(oldBytes)
                // modify attributes
                val FAT = file.extortField<ClusteredFormatDOM.FATEntry>("FAT")!!
                FAT.creationDate = diskEntry.creationDate
                FAT.modificationDate = diskEntry.modificationDate
            }
        }

        DOM.dispose()
    }

}