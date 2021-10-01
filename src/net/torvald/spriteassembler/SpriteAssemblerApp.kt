package net.torvald.spriteassembler

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.EMDASH
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

    private val lang = Properties()


    /**
     * ¤ is used as a \n marker
     */
    private val translations = """
        WARNING_CONTINUE=Continue?
        WARNING_YOUR_DATA_WILL_GONE=Existing edits will be lost.
        OPERATION_CANCELLED=Operation cancelled.
        NO_SUCH_FILE=No such file exists, operation cancelled.
        NEW_ROWS=Enter the number of rows to initialise the new CSV.¤Remember, you can always add or delete rows later.
        ADD_ROWS=Enter the number of rows to add:
        WRITE_FAIL=Writing to file has failed:
        STAT_INIT=Creating a new CSV. You can still open existing file.
        STAT_SAVE_TGA_SUCCESSFUL=Spritesheet exported successfully.
        STAT_LOAD_SUCCESSFUL=File loaded successfully.
        ERROR_INTERNAL=Something went wrong.
        ERROR_PARSE_FAIL=Parsing failed
        SPRITE_DEF_LOAD_SUCCESSFUL=Sprite definition loaded.
        SPRITE_ASSEMBLE_SUCCESSFUL=Sprite assembled.
        PROPERTIES_GO_HERE=Properties will be shown here.
    """.trimIndent()

    private var panelCodeInit = true

    init {
        // setup application properties //
        try {
            lang.load(StringReader(translations))
        }
        catch (e: Throwable) {

        }

        panelCode.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        panelCode.text = """Terrarum Sprite Assembler
            |Copyright 2019${EMDASH} CuriousTorvald (minjaesong)
            |
            |This program is free software: you can redistribute it and/or modify
            |it under the terms of the GNU General Public License as published by
            |the Free Software Foundation, either version 3 of the License, or
            |(at your option) any later version.
            |
            |This program is distributed in the hope that it will be useful,
            |but WITHOUT ANY WARRANTY; without even the implied warranty of
            |MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
            |GNU General Public License for more details.
            |
            |You should have received a copy of the GNU General Public License
            |along with this program.  If not, see <https://www.gnu.org/licenses/>.""".trimMargin()
        panelCode.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (panelCodeInit) {
                    panelCodeInit = false
                    panelCode.text = ""
                }
            }
        })

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

        panelProperties.model = DefaultTreeModel(DefaultMutableTreeNode(lang.getProperty("PROPERTIES_GO_HERE")))
        val panelDataView = JSplitPane(JSplitPane.VERTICAL_SPLIT, JScrollPane(panelProperties), panelPartsList)
        panelDataView.resizeWeight = 0.333

        // to disable text wrap
        //val panelCodeNoWrap = JPanel(BorderLayout())
        //panelCodeNoWrap.add(panelCode)

        val panelMain = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, JScrollPane(panelCode), panelDataView)
        panelMain.resizeWeight = 0.666

        val menu = JMenuBar()
        menu.add(JMenu("Update")).addMouseListener(object : MouseAdapter() {
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



                    gdxWindow.requestAssemblyTest(adProperties)
                    statBar.text = lang.getProperty("SPRITE_ASSEMBLE_SUCCESSFUL")
                }
                catch (fehler: Throwable) {
                    displayError("ERROR_PARSE_FAIL", fehler)
                    fehler.printStackTrace()
                }

            }
        })
        menu.add(JMenu("Zoom 1x")).addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                gdxWindow.zoom = 1f
            }
        })
        menu.add(JMenu("2x")).addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                gdxWindow.zoom = 2f
            }
        })
        menu.add(JMenu("3x")).addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                gdxWindow.zoom = 3f
            }
        })
        menu.add(JMenu("4x")).addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                gdxWindow.zoom = 4f
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

    var zoom = 1f

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
            batch.draw(renderTexture, 0f, 0f, renderTexture.width * zoom, renderTexture.height * zoom)
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
    val appConfig = Lwjgl3ApplicationConfiguration()
    appConfig.setWindowedMode(800, 800)
    appConfig.setIdleFPS(5)
    appConfig.setForegroundFPS(5)
    appConfig.setResizable(false)

    val gdxWindow = SpriteAssemblerPreview()

    SpriteAssemblerApp(gdxWindow)
    Lwjgl3Application(gdxWindow, appConfig)
}