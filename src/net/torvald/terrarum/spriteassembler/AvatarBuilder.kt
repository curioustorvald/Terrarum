package net.torvald.terrarum.spriteassembler

import net.torvald.terrarum.App
import net.torvald.terrarum.savegame.*
import net.torvald.terrarum.savegame.VDFileID.BODYPARTEMISSIVE_TO_ENTRY_MAP
import net.torvald.terrarum.savegame.VDFileID.BODYPARTGLOW_TO_ENTRY_MAP
import net.torvald.terrarum.savegame.VDFileID.BODYPART_TO_ENTRY_MAP
import net.torvald.terrarum.savegame.VDFileID.PLAYER_SCREENSHOT
import net.torvald.terrarum.savegame.VDFileID.ROOT
import net.torvald.terrarum.savegame.VDFileID.SAVEGAMEINFO
import net.torvald.terrarum.savegame.VDFileID.SPRITEDEF
import net.torvald.terrarum.savegame.VDFileID.SPRITEDEF_EMSV
import net.torvald.terrarum.savegame.VDFileID.SPRITEDEF_GLOW
import net.torvald.terrarum.serialise.Common
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*

/**
 * Created by minjaesong on 2024-09-25.
 */
class AvatarBuilder : JFrame() {

    private val infilePath = JTextField().also { it.preferredSize = Dimension(400, 24) }
    private val outfilePath = JTextField().also { it.preferredSize = Dimension(400, 24) }
    private val buttonReset = JButton("Clear")
    private val buttonGo = JButton("Export!")

    init {
        val panelPlayer = JPanel().also {
            it.add(JLabel("Player File", SwingConstants.RIGHT).also { it.preferredSize = Dimension(80, 24) })
            it.add(infilePath)
        }

        val panelOutput = JPanel().also {
            it.add(JLabel("Output File", SwingConstants.RIGHT).also { it.preferredSize = Dimension(80, 24) })
            it.add(outfilePath)
        }

        val controlPanel = JPanel().also {
            it.add(buttonReset)
            it.add(buttonGo)
        }

        val panelMain = JPanel().also {
            it.layout = GridLayout(2, 1)
            it.add(panelPlayer, 0)
            it.add(panelOutput, 1)
        }

        this.layout = BorderLayout()
        this.add(panelMain, BorderLayout.CENTER)
        this.add(controlPanel, BorderLayout.SOUTH)
        this.title = "Terrarum Avatar Generator"
        this.isVisible = true
        this.setSize(512, 128)
        this.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE

        buttonReset.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                infilePath.text = ""
                outfilePath.text = ""
            }
        })

        buttonGo.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                if (infilePath.text.isNotBlank() && outfilePath.text.isNotBlank()) {
                    try {
                        invoke(File(infilePath.text), File(outfilePath.text))
                        popupMessage("Exported successfully!")
                    }
                    catch (e: Throwable) {
                        e.printStackTrace()
                        popupError(e.toString())
                    }
                }
            }
        })
    }

    private fun popupMessage(message: String, title: String = "") {
        JOptionPane.showOptionDialog(
            null,
            message,
            title,
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null, null, null
        )
    }

    private fun popupError(message: String, title: String = "Uh oh…") {
        JOptionPane.showOptionDialog(
            null,
            message,
            title,
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.ERROR_MESSAGE,
            null, null, null
        )
    }

    private var lastBodypartIndex = 0L

    operator fun invoke(playerSavegameFile: File, outFile: File) {
        val dom = VDUtil.readDiskArchive(playerSavegameFile)

        val playerInfoFile = dom.getEntry(SAVEGAMEINFO)!!
        val spritedefFile = dom.getEntry(SPRITEDEF)!!
        val spritedefGlowFile = dom.getEntry(SPRITEDEF_GLOW)
        val spritedefEmsvFile = dom.getEntry(SPRITEDEF_EMSV)
        val screencap = dom.getEntry(PLAYER_SCREENSHOT)
        val playerName = dom.diskName.toCanonicalString(Common.CHARSET)
        val outDisk = VDUtil.createNewDisk(4294967295L, playerName, Common.CHARSET)

        val timeNow = App.getTIME_T()

        // initialise outDisk DOM
        VDUtil.addFile(outDisk, playerInfoFile)
        VDUtil.addFile(outDisk, spritedefFile)
        spritedefGlowFile?.let { VDUtil.addFile(outDisk, it) }
        spritedefEmsvFile?.let { VDUtil.addFile(outDisk, it) }
        screencap?.let {VDUtil.addFile(outDisk, it) }



        putBodyparts(timeNow, outDisk, spritedefFile, BODYPART_TO_ENTRY_MAP)
        spritedefGlowFile?.let { putBodyparts(timeNow, outDisk, it, BODYPARTGLOW_TO_ENTRY_MAP) }
        spritedefEmsvFile?.let { putBodyparts(timeNow, outDisk, it, BODYPARTEMISSIVE_TO_ENTRY_MAP) }

        outDisk.saveKind = VDSaveKind.PLAYER_DATA

        VDUtil.dumpToRealMachine(outDisk, outFile)
    }

    // will write: bodypart images, spritedef, to the disk
    private fun putBodyparts(timeNow: Long, outDisk: VirtualDisk, spritedefFile: DiskEntry, bodypartsToEntryFileID: Long) {
        val bodypartsToEntry = StringBuilder()
        val adp = ADProperties(ByteArray64Reader(spritedefFile.contents.getContent() as ByteArray64, Common.CHARSET))
        val images = adp.bodyparts.map { // Pair: "HEAD" to "mods/basegame/sprites/***/prefix_HEAD.tga"
            it.uppercase() to adp.toFilename(it)
        }

        images.forEach { (name, path) ->

            val bodypartFileNative = File("assets/$path")
            if (bodypartFileNative.exists()) {
                lastBodypartIndex += 1L

                // write to bodypartsToEntry file content
                bodypartsToEntry.append("$name=$lastBodypartIndex\n")

                // copy image into new disk
                val bodypartFile = VDUtil.importFile(bodypartFileNative, lastBodypartIndex, Common.CHARSET)
                VDUtil.addFile(outDisk, bodypartFile)
            }
        }

        // write bodypartsToEntry as a file
        val bodypartsToEntryFileContent = EntryFile(ByteArray64.fromByteArray(bodypartsToEntry.toString().toByteArray(Common.CHARSET)))
        val bodypartsToEntryFile = DiskEntry(bodypartsToEntryFileID, ROOT, timeNow, timeNow, bodypartsToEntryFileContent)
        VDUtil.addFile(outDisk, bodypartsToEntryFile)
    }

}

fun main(args: Array<String>) {
    AvatarBuilder()
}
