package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.App.*
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.TitleScreen
import net.torvald.terrarum.serialise.TryResize
import net.torvald.terrarum.serialise.WriteConfig
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextButton
import net.torvald.terrarum.ui.UIItemTextButtonList
import net.torvald.terrarum.ui.UIItemTextButtonList.Companion.DEFAULT_LINE_HEIGHT

/**
 * Created by minjaesong on 2018-08-29.
 */
open class UIRemoCon(val parent: TitleScreen, val treeRoot: QNDTreeNode<String>) : UICanvas() {
    init {
        handler.allowESCtoClose = false
    }
    override var openCloseTime = 0f

    private var remoConTray: UIRemoConElement // this remocon is dynamically generated
    var currentRemoConContents = treeRoot; private set
    private var currentlySelectedRemoConItem = treeRoot.data

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

        registerUIclasses(treeRoot)
    }

    var currentRemoConLabelCount = 0; private set

    private fun registerUIclasses(tree: QNDTreeNode<String>) {
        tree.traversePreorder { node, _ ->
            val splittedNodeName = node.data?.split(yamlSep)

            if (splittedNodeName?.size == 2 && node.data != null) {
                try {
                    val tag = splittedNodeName[0].split(tagSep).getOrNull(1)
                    val attachedClass = loadClass("basegame", splittedNodeName[1]) // check existence
                    screenNames[node.data!!] = splittedNodeName[1] // actual loading will by dynamic as some UIs need to be re-initialised as they're called
                }
                catch (e: java.lang.ClassNotFoundException) {
                    printdbgerr(this, "class '${splittedNodeName[1]}' was not found, skipping")
                }
            }
        }
    }

    private fun loadClass(module: String, name: String): UICanvas {
        return ModMgr.getJavaClass<UICanvas>(module, name, arrayOf(this.javaClass), arrayOf(this))
    }

    private var mouseActionAvailable = true

    private fun generateNewRemoCon(node: QNDTreeNode<String>): UIRemoConElement {
        val labels = Array(node.children.size) { node.children[it].data?.split(yamlSep)?.get(0)?.split(tagSep)?.get(0) ?: "(null)" }
        val tags = Array(node.children.size) { node.children[it].data?.split(yamlSep)?.get(0)?.split(tagSep).let {
            if (it.isNullOrEmpty() || it.size < 2) emptyArray<String>()
            else it.subList(1, it.size).toTypedArray()
        } }
        currentRemoConLabelCount = labels.size
        return UIRemoConElement(this, labels, tags)
    }


    private var oldSelectedItem: UIItemTextButton? = null
    private var openUI: UICanvas? = null

    override fun updateImpl(delta: Float) {
        if (mouseActionAvailable) {
            mouseActionAvailable = false

            remoConTray.update(delta)

            if (Terrarum.mouseDown) {
                val selectedItem = remoConTray.selectedItem
                val selectedIndex = remoConTray.selectedIndex

                if (!handler.uiToggleLocked) {
                    selectedItem?.let {
                        if (selectedItem != oldSelectedItem) {
                            oldSelectedItem?.highlighted = false

                            // selection change
                            if (it.textfun() == Lang["MENU_LABEL_QUIT"]) {
                                //System.exit(0)
                                Gdx.app.exit()
                            }
                            else if (it.textfun() == Lang["MENU_LABEL_RETURN"]) {
                                val tag = it.tags
                                if (tag.contains("RESIZEIFNEEDED")) TryResize.pre()
                                if (tag.contains("WRITETOCONFIG")) WriteConfig()
                                if (tag.contains("RESIZEIFNEEDED")) TryResize()

                                if (IS_DEVELOPMENT_BUILD) print("[UIRemoCon] Returning from ${currentRemoConContents.data}")

                                if (currentRemoConContents.parent != null) {
                                    remoConTray.consume()

                                    currentRemoConContents = currentRemoConContents.parent!!
                                    currentlySelectedRemoConItem = currentRemoConContents.data
                                    remoConTray = generateNewRemoCon(currentRemoConContents)

                                    parent.uiFakeBlurOverlay.setAsClose()

                                    if (IS_DEVELOPMENT_BUILD) println(" to ${currentlySelectedRemoConItem}")
                                }
                                else {
                                    throw NullPointerException("No parent node to return")
                                }
                            }
                            else {
                                // check if target exists
                                if (IS_DEVELOPMENT_BUILD) {
                                    //println("current node: ${currentRemoConContents.data}")
                                    //currentRemoConContents.children.forEach { println("- ${it.data}") }
                                }

                                if (currentRemoConContents.children.size > selectedIndex ?: 0x7FFFFFFF) {
                                    setNewRemoConContents(currentRemoConContents.children[selectedIndex!!])
                                }
                                else {
                                    throw RuntimeException("Index: $selectedIndex, Size: ${currentRemoConContents.children.size}")
                                }
                            }


                            // do something with the actual selection
                            //printdbg(this, "$currentlySelectedRemoConItem")
                            openUI(currentlySelectedRemoConItem)
                        }
                    }
                }


                oldSelectedItem = remoConTray.selectedItem
            }
        }

        openUI?.update(delta)


        if (!Terrarum.mouseDown) {
            mouseActionAvailable = true
        }
    }

    fun setNewRemoConContents(newCurrentRemoConContents: QNDTreeNode<String>, forceSetParent: QNDTreeNode<String>? = null) {

        printdbg(this, "setting new remocon contents: ${newCurrentRemoConContents.data}")

        if (forceSetParent != null) {
            newCurrentRemoConContents.parent = forceSetParent
        }


        // only go deeper if that node has child to navigate
        if (newCurrentRemoConContents.children.size != 0) {
            remoConTray.consume()
            remoConTray = generateNewRemoCon(newCurrentRemoConContents)
            currentRemoConContents = newCurrentRemoConContents
        }

        registerUIclasses(newCurrentRemoConContents)

        currentlySelectedRemoConItem = newCurrentRemoConContents.data
    }

    fun openUI(menuString: String?) {
        openUI?.let {
            it.setAsClose()
            it.dispose()
        }


        printdbg(this, "$menuString has screen: ${screenNames.containsKey(menuString)}")
        screenNames[menuString]?.let {
            val ui = loadClass("basegame", it)
            ui.setPosition(0,0)
            parent.uiFakeBlurOverlay.setAsOpen()
            ui.setAsOpen()
            ui.handler.allowESCtoClose = false
            openUI = ui
        }
    }

    fun openUI(external: UICanvas) {
        openUI?.let {
            it.setAsClose()
            it.dispose()
        }


        printdbg(this, "Displaying external UI ${external.javaClass.canonicalName}")
        external.setPosition(0,0)
        parent.uiFakeBlurOverlay.setAsOpen()
        external.setAsOpen()
        openUI = external
    }

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        remoConTray.render(frameDelta, batch, camera)
        openUI?.render(frameDelta, batch, camera)
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

    override fun inputStrobed(e: TerrarumKeyboardEvent) {
        openUI?.inputStrobed(e)
    }

    class UIRemoConElement(uiRemoCon: UIRemoCon, val labels: Array<String>, val tags: Array<Array<String>>) {

        companion object {
            const val lineHeight = 36
            const val paddingLeft = 48
        }

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

        fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
            menubar.render(frameDelta, batch, camera)
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