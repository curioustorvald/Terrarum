package net.torvald.spriteassembler

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.gdx.graphics.PixmapIO2
import net.torvald.terrarum.gdxClearAndSetBlend
import net.torvald.terrarum.inUse
import java.awt.BorderLayout
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.StringReader
import java.util.*
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * Created by minjaesong on 2019-01-05.
 */
class SpriteAssemblerApp(val gdxWindow: SpriteAssemblerPreview) : JFrame() {

    private val panelProperties = JTree()
    private val panelAnimationsList = JList<String>()
    private val panelBodypartsList = JList<String>()
    private val panelImageFilesList = JList<String>()
    private val panelSkeletonsList = JList<String>()
    private val panelTransformsList = JList<String>()
    private val panelStatList = JList<String>()
    private val panelCode = JTextPane()
    private val statBar = JTextArea("Null.")

    private lateinit var adProperties: ADProperties

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
                               "STAT_SAVE_TGA_SUCCESSFUL=Spritesheet exported successfully.\n" +
                               "STAT_LOAD_SUCCESSFUL=File loaded successfully.\n" +
                               "ERROR_INTERNAL=Something went wrong.\n" +
                               "ERROR_PARSE_FAIL=Parsing failed\n" +
                               "SPRITE_DEF_LOAD_SUCCESSFUL=Sprite definition loaded.\n" +
                               "SPRITE_ASSEMBLE_SUCCESSFUL=Sprite assembled."

    init {
        // setup application properties //
        try {
            props.load(StringReader(captionProperties))
            lang.load(StringReader(translations))
        }
        catch (e: Throwable) {

        }

        panelCode.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        panelCode.text = "Enter your descriptor code here…"

        panelAnimationsList.model = DefaultListModel()
        panelBodypartsList.model = DefaultListModel()
        panelImageFilesList.model = DefaultListModel()
        panelSkeletonsList.model = DefaultListModel()
        panelTransformsList.model = DefaultListModel()
        panelStatList.model = DefaultListModel()

        val panelPartsList = JTabbedPane(JTabbedPane.TOP)
        panelPartsList.add("Animations", JScrollPane(panelAnimationsList))
        panelPartsList.add("Bodyparts", JScrollPane(panelBodypartsList))
        panelPartsList.add("Images", JScrollPane(panelImageFilesList))
        panelPartsList.add("Skeletons", JScrollPane(panelSkeletonsList))
        panelPartsList.add("Transforms", JScrollPane(panelTransformsList))
        panelPartsList.add("Stats", JScrollPane(panelStatList))

        val panelDataView = JSplitPane(JSplitPane.VERTICAL_SPLIT, JScrollPane(panelProperties), panelPartsList)
        panelDataView.resizeWeight = 0.333

        // to disable text wrap
        //val panelCodeNoWrap = JPanel(BorderLayout())
        //panelCodeNoWrap.add(panelCode)

        val panelMain = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, JScrollPane(panelCode), panelDataView)
        panelMain.resizeWeight = 0.666

        val menu = JMenuBar()
        menu.add(JMenu("Parse")).addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                try {
                    adProperties = ADProperties(StringReader(panelCode.text))
                    statBar.text = lang.getProperty("SPRITE_DEF_LOAD_SUCCESSFUL")

                    val propRoot = DefaultMutableTreeNode("Properties")

                    adProperties.forEach { s, list ->
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
                    panelImageFilesList.model = DefaultListModel()
                    panelSkeletonsList.model = DefaultListModel()
                    panelTransformsList.model = DefaultListModel()
                    panelStatList.model = DefaultListModel()

                    // populate animations view
                    adProperties.animations.forEach {
                        (panelAnimationsList.model as DefaultListModel).addElement("${it.value}")
                    }
                    // populate bodyparts view
                    adProperties.bodyparts.forEach { partName ->
                        (panelBodypartsList.model as DefaultListModel).addElement(partName)
                    }
                    // populate image file list view
                    adProperties.bodypartFiles.forEach { partName ->
                        (panelImageFilesList.model as DefaultListModel).addElement(partName)
                    }
                    // populate skeletons view
                    adProperties.skeletons.forEach {
                        (panelSkeletonsList.model as DefaultListModel).addElement("${it.value}")
                    }
                    // populate transforms view
                    adProperties.transforms.forEach {
                        (panelTransformsList.model as DefaultListModel).addElement("$it")
                    }
                    // populate stats
                    (panelStatList.model as DefaultListModel).addElement("Spritesheet rows: ${adProperties.rows}")
                    (panelStatList.model as DefaultListModel).addElement("Spritesheet columns: ${adProperties.cols}")
                    (panelStatList.model as DefaultListModel).addElement("Frame size: ${adProperties.frameWidth}, ${adProperties.frameHeight}")
                    (panelStatList.model as DefaultListModel).addElement("Origin position: ${adProperties.originX}, ${adProperties.originY}")
                }
                catch (fehler: Throwable) {
                    displayError("ERROR_PARSE_FAIL", fehler)
                    fehler.printStackTrace()
                }

            }
        })
        menu.add(JMenu("Run")).addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                try {
                    gdxWindow.requestAssemblyTest(adProperties)
                    statBar.text = lang.getProperty("SPRITE_ASSEMBLE_SUCCESSFUL")
                }
                catch (fehler: Throwable) {
                    displayError("ERROR_PARSE_FAIL", fehler)
                    fehler.printStackTrace()
                }
            }
        })
        menu.add(JMenu("Export")).addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                val fileChooser = JFileChooser()
                fileChooser.showSaveDialog(null)

                if (fileChooser.selectedFile != null) {
                    gdxWindow.requestExport(fileChooser.selectedFile.absolutePath)
                    statBar.text = lang.getProperty("STAT_SAVE_TGA_SUCCESSFUL")
                } // else, do nothing
            }
        })

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
}

class SpriteAssemblerPreview: Game() {
    private lateinit var batch: SpriteBatch

    private lateinit var renderTexture: Texture
    private var image: Pixmap? = null
        set(value) {
            renderTexture.dispose()
            field?.dispose()

            field = value
            renderTexture = Texture(field)
        }

    override fun create() {
        Gdx.graphics.setTitle("Sprite Assembler Preview")
        batch = SpriteBatch()
        renderTexture = Texture(1, 1, Pixmap.Format.RGBA8888)
    }

    private val bgCol = Color(.62f, .79f, 1f, 1f)

    private var doAssemble = false
    private lateinit var assembleProp: ADProperties

    private var doExport = false
    private lateinit var exportPath: String

    override fun render() {
        if (doAssemble) {
            // assembly requires GL context
            doAssemble = false
            assembleImage(assembleProp)
        }

        if (doExport && image != null) {
            doExport = false
            PixmapIO2.writeTGAHappy(Gdx.files.absolute(exportPath), image, false)
        }


        gdxClearAndSetBlend(bgCol)


        batch.inUse {
            batch.color = Color.WHITE
            batch.draw(renderTexture, 0f, 0f, renderTexture.width * 2f, renderTexture.height * 2f)
        }
    }

    private fun assembleImage(prop: ADProperties) {
        image = AssembleSheetPixmap(prop)
    }

    // TODO rename to requestAssembly
    fun requestAssemblyTest(prop: ADProperties) {
        doAssemble = true
        assembleProp = prop
    }

    fun requestExport(path: String) {
        doExport = true
        exportPath = path
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
    }
}

fun main(args: Array<String>) {
    val appConfig = LwjglApplicationConfiguration()
    appConfig.resizable = false
    appConfig.width = 512
    appConfig.height = 1024
    appConfig.foregroundFPS = 5
    appConfig.backgroundFPS = 5

    val gdxWindow = SpriteAssemblerPreview()

    LwjglApplication(gdxWindow, appConfig)
    SpriteAssemblerApp(gdxWindow)
}