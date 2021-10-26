package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbgerr
import net.torvald.terrarum.serialise.WriteConfig
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextButton
import net.torvald.terrarum.ui.UIItemTextButtonList
import net.torvald.terrarum.ui.UIItemTextButtonList.Companion.DEFAULT_LINE_HEIGHT

/**
 * Created by minjaesong on 2018-08-29.
 */
open class UIRemoCon(val parent: TitleScreen, treeRepresentation: QNDTreeNode<String>) : UICanvas() {

    override var openCloseTime = 0f

    private var remoConTray: UIRemoConElement // this remocon is dynamically generated
    var currentRemoConContents = treeRepresentation; private set
    private var currentlySelectedRemoConItem = treeRepresentation.data

    override var width: Int
        get() = remoConWidth // somehow NOT making this constant causes a weird issue
        set(value) {}        // where the remocon widens to screen width
    override var height: Int
        get() = remoConTray.height
        set(value) {}

    //private val screens = ArrayList<Pair<String, UICanvas>>()
    private val screenNames = HashMap<String, String>()

    private val yamlSep = Yaml.SEPARATOR
    private val tagSep = "+"
    init {
        remoConTray = generateNewRemoCon(currentRemoConContents)

        treeRepresentation.traversePreorder { node, _ ->
            val splittedNodeName = node.data?.split(yamlSep)

            if (splittedNodeName?.size == 2 && node.data != null) {
                try {
                    val attachedClass = loadClass(splittedNodeName[1]) // check existance
                    screenNames[node.data!!] = splittedNodeName[1]
                }
                catch (e: java.lang.ClassNotFoundException) {
                    printdbgerr(this, "class '${splittedNodeName[1]}' was not found, skipping")
                }
            }
        }
    }

    private fun loadClass(name: String): UICanvas {
        val newClass = Class.forName(name)
        val newClassConstructor = newClass.getConstructor(this.javaClass)
        val newClassInstance = newClassConstructor.newInstance(this)
        return newClassInstance as UICanvas
    }

    private var mouseActionAvailable = true

    private fun generateNewRemoCon(node: QNDTreeNode<String>): UIRemoConElement {
        val labels = Array(node.children.size) { node.children[it].data?.split(yamlSep)?.get(0)?.split(tagSep)?.get(0) ?: "(null)" }
        val tags = Array(node.children.size) { arrayOf(node.children[it].data?.split(yamlSep)?.get(0)?.split(tagSep)?.getOrNull(1) ?: "") }
        return UIRemoConElement(this, labels, tags)
    }


    private var oldSelectedItem: UIItemTextButton? = null
    private var openUI: UICanvas? = null

    override fun updateUI(delta: Float) {
        if (mouseActionAvailable && Terrarum.mouseDown) {
            mouseActionAvailable = false

            remoConTray.update(delta)

            val selectedItem = remoConTray.selectedItem
            val selectedIndex = remoConTray.selectedIndex

            if (!handler.uiToggleLocked) {
                selectedItem?.let { if (selectedItem != oldSelectedItem) {
                    oldSelectedItem?.highlighted = false

                    // selection change
                    if (it.labelText == "MENU_LABEL_QUIT") {
                        //System.exit(0)
                        Gdx.app.exit()
                    }
                    else if (it.labelText.startsWith("MENU_LABEL_RETURN")) {
                        val tag = it.tags
                        if (tag.contains("WRITETOCONFIG")) WriteConfig()


                        if (currentRemoConContents.parent != null) {
                            remoConTray.consume()

                            currentRemoConContents = currentRemoConContents.parent!!
                            currentlySelectedRemoConItem = currentRemoConContents.data
                            remoConTray = generateNewRemoCon(currentRemoConContents)

                            parent.uiFakeBlurOverlay.setAsClose()
                        }
                        else {
                            throw NullPointerException("No parent node to return")
                        }
                    }
                    else {
                        // check if target exists
                        //println("current node: ${currentRemoConContents.data}")
                        //currentRemoConContents.children.forEach { println("- ${it.data}") }

                        if (currentRemoConContents.children.size > selectedIndex ?: 0x7FFFFFFF) {
                            setNewRemoConContents(currentRemoConContents.children[selectedIndex!!])
                        }
                        else {
                            throw RuntimeException("Index: $selectedIndex, Size: ${currentRemoConContents.children.size}")
                        }
                    }


                    // do something with the actual selection
                    //printdbg(this, "$currentlySelectedRemoConItem")
                    openUI?.setAsClose()
                    openUI?.dispose()

                    screenNames[currentlySelectedRemoConItem]?.let {
                        val ui = loadClass(it)
                        ui.setPosition(0,0)
                        parent.uiFakeBlurOverlay.setAsOpen()
                        ui.setAsOpen()
                        openUI = ui
                    }
                } }
            }


            oldSelectedItem = remoConTray.selectedItem
        }


        openUI?.update(delta)


        if (!Terrarum.mouseDown) {
            mouseActionAvailable = true
        }
    }

    fun setNewRemoConContents(newCurrentRemoConContents: QNDTreeNode<String>, forceSetParent: QNDTreeNode<String>? = null) {

        if (forceSetParent != null) {
            newCurrentRemoConContents.parent = forceSetParent
        }

        // only go deeper if that node has child to navigate
        if (newCurrentRemoConContents.children.size != 0) {
            remoConTray.consume()
            remoConTray = generateNewRemoCon(newCurrentRemoConContents)
            currentRemoConContents = newCurrentRemoConContents
        }

        currentlySelectedRemoConItem = newCurrentRemoConContents.data
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        remoConTray.render(batch, camera)
        openUI?.render(batch, camera)
    }

    override fun doOpening(delta: Float) {

    }

    override fun doClosing(delta: Float) {

    }

    override fun endOpening(delta: Float) {

    }

    override fun endClosing(delta: Float) {

    }

    override fun dispose() {
        openUI?.dispose()
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        openUI?.touchDragged(screenX, screenY, pointer)
        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        openUI?.touchDown(screenX, screenY, pointer, button)
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        openUI?.touchUp(screenX, screenY, pointer, button)
        return true
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        openUI?.scrolled(amountX, amountY)
        return true
    }

    override fun keyDown(keycode: Int): Boolean {
        openUI?.keyDown(keycode)
        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        openUI?.keyUp(keycode)
        return true
    }

    override fun keyTyped(character: Char): Boolean {
        openUI?.keyTyped(character)
        return true
    }



    class UIRemoConElement(uiRemoCon: UIRemoCon, val labels: Array<String>, val tags: Array<Array<String>>) {

        private val lineHeight = 36
        private val paddingLeft = 48

        private val menubar = UIItemTextButtonList(
                uiRemoCon,
                lineHeight,
                labels,
                menubarOffX - paddingLeft,
                menubarOffY - lineHeight * labels.size + 16,
                uiRemoCon.width + paddingLeft, getRemoConHeight(labels),
                textAreaWidth = uiRemoCon.width,
                readFromLang = true,
                activeBackCol = Color(0),//Color(1f,0f,.75f,1f),
                highlightBackCol = Color(0),
                inactiveCol = Color.WHITE,
                defaultSelection = null,
                itemHitboxSize = lineHeight - 2,
                alignment = UIItemTextButton.Companion.Alignment.LEFT,
                leftPadding = paddingLeft,
                tagsCollection = tags
        )

        fun update(delta: Float) {
            menubar.update(delta)
        }

        fun render(batch: SpriteBatch, camera: Camera) {
            menubar.render(batch, camera)
        }

        // nullifies currently selected item
        fun consume() {
            menubar.selectedIndex = null
        }

        /** null if none are selected */
        val selectedItem: UIItemTextButton?
            get() = menubar.selectedButton
        /** null if none are selected */
        val selectedIndex: Int?
            get() = menubar.selectedIndex

        val height = getRemoConHeight(labels)
    }

    companion object {
        val remoConWidth = 160
        fun getRemoConHeight(menu: ArrayList<String>) = DEFAULT_LINE_HEIGHT * menu.size.plus(1)
        fun getRemoConHeight(menu: Array<String>) = DEFAULT_LINE_HEIGHT * menu.size.plus(1)
        val menubarOffX: Int; get() = (0.11 * Toolkit.drawWidth).toInt()
        val menubarOffY: Int; get() = (0.82 * App.scr.height).toInt()
    }
}