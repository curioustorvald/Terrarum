package net.torvald.spriteassembler

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.io.StringReader
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * Created by minjaesong on 2019-01-05.
 */
class SpriteAssemblerApp : JFrame() {

    private val panelPreview = ImagePanel()
    private val panelProperties = JTree()
    private val panelAnimationsList = JList<String>()
    private val panelBodypartsList = JList<String>()
    private val panelImageFilesList = JList<String>()
    private val panelSkeletonsList = JList<String>()
    private val panelCode = JTextPane()
    private val statBar = JTextArea("Null.")

    private var adProperties: ADProperties? = null

    private val props = Properties()
    private val lang = Properties()

    private val captionProperties = "" + // dummy string to make IDE happy with the auto indent

                                    "id=ID of this block\n" +
                                    "drop=ID of the block this very block should drop when mined\n" +
                                    "name=String identifier of the block\n" +
                                    "shdr=Shade Red (light absorption). Valid range 0.0-4.0\n" +
                                    "shdg=Shade Green (light absorption). Valid range 0.0-4.0\n" +
                                    "shdb=Shade Blue (light absorption). Valid range 0.0-4.0\n" +
                                    "shduv=Shade UV (light absorbtion). Valid range 0.0-4.0\n" +
                                    "lumr=Luminosity Red (light intensity). Valid range 0.0-4.0\n" +
                                    "lumg=Luminosity Green (light intensity). Valid range 0.0-4.0\n" +
                                    "lumb=Luminosity Blue (light intensity). Valid range 0.0-4.0\n" +
                                    "lumuv=Luminosity UV (light intensity). Valid range 0.0-4.0\n" +
                                    "str=Strength of the block\n" +
                                    "dsty=Density of the block. Water have 1000 in the in-game scale\n" +
                                    "mate=Material of the block\n" +
                                    "solid=Whether the file has full collision\n" +
                                    "plat=Whether the block should behave like a platform\n" +
                                    "wall=Whether the block can be used as a wall\n" +
                                    "fall=Whether the block should fall through the empty space\n" +
                                    "dlfn=Dynamic Light Function. 0=Static. Please see <strong>notes</strong>\n" +
                                    "fv=Vertical friction when player slide on the cliff. 0 means not slide-able\n" +
                                    "fr=Horizontal friction. &lt;16:slippery 16:regular &gt;16:sticky\n"

    /**
     * ¤ is used as a \n marker
     */
    private val translations = "" +
                               "WARNING_CONTINUE=Continue?\n" +
                               "WARNING_YOUR_DATA_WILL_GONE=Existing edits will be lost.\n" +
                               "OPERATION_CANCELLED=Operation cancelled.\n" +
                               "NO_SUCH_FILE=No such file exists, operation cancelled.\n" +
                               "NEW_ROWS=Enter the number of rows to initialise the new CSV.¤Remember, you can always add or delete rows later.\n" +
                               "ADD_ROWS=Enter the number of rows to add:\n" +
                               "WRITE_FAIL=Writing to file has failed:\n" +
                               "STAT_INIT=Creating a new CSV. You can still open existing file.\n" +
                               "STAT_SAVE_SUCCESSFUL=File saved successfully.\n" +
                               "STAT_NEW_FILE=New CSV created.\n" +
                               "STAT_LOAD_SUCCESSFUL=File loaded successfully.\n" +
                               "ERROR_INTERNAL=Something went wrong.\n" +
                               "ERROR_PARSE_FAIL=Parsing failed\n" +
                               "SPRITE_DEF_LOAD_SUCCESSFUL=Sprite definition loaded."

    init {
        // setup application properties //
        try {
            props.load(StringReader(captionProperties))
            lang.load(StringReader(translations))
        }
        catch (e: Throwable) {

        }

        panelPreview.preferredSize = Dimension(512,512)

        panelAnimationsList.model = DefaultListModel()
        panelBodypartsList.model = DefaultListModel()
        panelImageFilesList.model = DefaultListModel()
        panelSkeletonsList.model = DefaultListModel()

        val panelPartsList = JTabbedPane(JTabbedPane.TOP)
        panelPartsList.add("Animations", JScrollPane(panelAnimationsList))
        panelPartsList.add("Bodyparts", JScrollPane(panelBodypartsList))
        panelPartsList.add("Images", JScrollPane(panelImageFilesList))
        panelPartsList.add("Skeletons", JScrollPane(panelSkeletonsList))

        val panelDataView = JSplitPane(JSplitPane.VERTICAL_SPLIT, JScrollPane(panelProperties), panelPartsList)

        val panelTop = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, JScrollPane(panelPreview), panelDataView)

        val panelMain = JSplitPane(JSplitPane.VERTICAL_SPLIT, panelTop, JScrollPane(panelCode))

        val menu = JMenuBar()
        menu.add(JMenu("File"))
        menu.add(JMenu("Parse")).addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                try {
                    adProperties = ADProperties(StringReader(panelCode.text))
                    statBar.text = lang.getProperty("SPRITE_DEF_LOAD_SUCCESSFUL")

                    val propRoot = DefaultMutableTreeNode("Properties")

                    adProperties?.forEach { s, list ->
                        // build tree node for the properties display
                        val propNode = DefaultMutableTreeNode(s)
                        propRoot.add(propNode)
                        list.forEach {
                            propNode.add(DefaultMutableTreeNode(it.toString()))
                        }
                    }

                    panelProperties.model = DefaultTreeModel(propRoot)

                    // clean the data views
                    panelAnimationsList.model = DefaultListModel()
                    panelBodypartsList.model = DefaultListModel()
                    panelSkeletonsList.model = DefaultListModel()

                    // populate animations view
                    adProperties!!.animations.forEach {
                        (panelAnimationsList.model as DefaultListModel).addElement("${it.value}")
                    }
                    // populate bodyparts view
                    adProperties!!.bodyparts.forEach { partName ->
                        (panelBodypartsList.model as DefaultListModel).addElement(partName)
                    }
                    // populate image file list view
                    adProperties!!.bodypartFiles.forEach { partName ->
                        (panelImageFilesList.model as DefaultListModel).addElement(partName)
                    }
                    // populate skeletons view
                    adProperties!!.skeletons.forEach {
                        (panelSkeletonsList.model as DefaultListModel).addElement("${it.value}")
                    }
                }
                catch (fehler: Throwable) {
                    displayError("ERROR_PARSE_FAIL", fehler)
                    fehler.printStackTrace()
                }

            }
        })
        menu.add(JMenu("Run"))

        this.layout = BorderLayout()
        this.add(menu, BorderLayout.NORTH)
        this.add(panelMain, BorderLayout.CENTER)
        this.add(statBar, BorderLayout.SOUTH)
        this.title = "Terrarum Sprite Assembler and Viewer"
        this.isVisible = true
        this.setSize(1154, 768)
        this.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    }

    private fun displayMessage(messageKey: String) {
        JOptionPane.showOptionDialog(
                null,
                lang.getProperty(messageKey), null,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null,
                arrayOf("OK", "Cancel"),
                "Cancel"
        )
    }

    private fun displayError(messageKey: String, cause: Throwable) {
        JOptionPane.showOptionDialog(null,
                lang.getProperty(messageKey) + "\n" + cause.toString(), null,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.ERROR_MESSAGE, null,
                arrayOf("OK", "Cancel"),
                "Cancel"
        )
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpriteAssemblerApp()
        }
    }
}

internal class ImagePanel : JPanel() {

    private var image: BufferedImage? = null

    init {
        try {
            image = ImageIO.read(File("image name and path"))
        }
        catch (ex: IOException) {
            // handle exception...
        }

    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        g.drawImage(image, 0, 0, this) // see javadoc for more info on the parameters
    }

}