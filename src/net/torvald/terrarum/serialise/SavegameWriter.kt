package net.torvald.terrarum.serialise

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.modulebasegame.gameworld.GameWorldExtension
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.DiskEntry
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.DiskSkimmer
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.VDUtil
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.VirtualDisk
import java.io.File
import java.nio.charset.Charset

/**
 * Created by minjaesong on 2018-10-03.
 */
object SavegameWriter {

    // TODO create temporary files (worldinfo), create JSON files on RAM, pack those into TEVd as per Savegame container.txt

    private val charset = Charset.forName("UTF-8")

    operator fun invoke(): Boolean {
        val diskImage = generateDiskImage(null)

        return false
    }


    private fun generateDiskImage(oldDiskFile: File?): VirtualDisk {
        val disk = VDUtil.createNewDisk(0x7FFFFFFFFFFFFFFFL, "TerrarumSave", charset)
        val oldDiskSkimmer = oldDiskFile?.let { DiskSkimmer(oldDiskFile) }

        val ROOT = disk.root.entryID
        val ingame = Terrarum.ingame!!
        val gameworld = ingame.world

        // serialise current world (stage)
        val world = WriteLayerDataZip() // filename can be anything that is "tmp_world[n]" where [n] is any number
        val worldFile = VDUtil.importFile(world!!, gameworld.worldIndex, charset)

        // add current world (stage) to the disk
        VDUtil.addFile(disk, ROOT, worldFile)

        // put other worlds (stages) to the disk (without loading whole oldDiskFile onto the disk)
        oldDiskSkimmer?.let {
            // skim-and-write other worlds
            for (c in 1..ingame.gameworldCount) {
                if (c != gameworld.worldIndex) {
                    val oldWorldFile = oldDiskSkimmer.requestFile(c)
                    VDUtil.addFile(disk, ROOT, oldWorldFile!!)
                }
            }
        }

        // TODO world[n] is done, needs whole other things



        return disk
    }
}