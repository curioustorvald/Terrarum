package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.random.HQRNG
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.AppLoader.printdbgerr
import net.torvald.terrarum.LoadScreen
import net.torvald.terrarum.Second
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.modulebasegame.Ingame
import net.torvald.terrarum.modulebasegame.gameactors.PlayerBuilderSigrid
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextButton
import net.torvald.terrarum.ui.UIItemTextButtonList
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by minjaesong on 2018-08-29.
 */
open class UIRemoCon(treeRepresentation: QNDTreeNode<String>) : UICanvas() {

    override var openCloseTime = 0f

    private var remoConTray: UIRemoConElement // this remocon is dynamically generated
    private var currentRemoConContents = treeRepresentation
    private var currentlySelectedRemoConItem = treeRepresentation.data

    override var width: Int
        get() = remoConWidth // somehow NOT making this constant causes a weird issue
        set(value) {}        // where the remocon widens to screen width
    override var height: Int
        get() = remoConTray.height
        set(value) {}

    private val screens = ArrayList<Pair<String, UICanvas>>()

    private val yamlSep = Regex(" : ")

    init {
        remoConTray = generateNewRemoCon(currentRemoConContents)

        treeRepresentation.traversePreorder { node, _ ->
            val splittedNodeName = node.data?.split(yamlSep)

            if (splittedNodeName?.size == 2) {
                try {
                    val attachedClass = loadClass(splittedNodeName[1])

                    attachedClass.posX = 0
                    attachedClass.posY = 0

                    screens.add((node.data ?: "(null)") to attachedClass)
                }
                catch (e: java.lang.ClassNotFoundException) {
                    printdbgerr(this, "class '${splittedNodeName[1]}' was not found, skipping")
                }
            }
        }
    }

    private fun loadClass(name: String): UICanvas {
        val newClass = Class.forName(name)
        val newClassConstructor = newClass.getConstructor(/* no args defined */)
        val newClassInstance = newClassConstructor.newInstance(/* no args defined */)
        return newClassInstance as UICanvas
    }

    private var mouseActionAvailable = true

    private fun generateNewRemoCon(node: QNDTreeNode<String>): UIRemoConElement {
        val dynamicStrArray = Array(node.children.size, { node.children[it].data?.split(yamlSep)?.get(0) ?: "(null)" })
        return UIRemoConElement(this, dynamicStrArray)
    }

    // currently there's no resetter for this!
    private var startNewGameCalled = false

    override fun updateUI(delta: Float) {
        if (mouseActionAvailable) {
            remoConTray.update(delta)

            mouseActionAvailable = false
        }

        val selectedItem = remoConTray.selectedItem
        val selectedIndex = remoConTray.selectedIndex

        selectedItem?.let {
            // selection change
            if (it.labelText == "MENU_LABEL_QUIT") {
                System.exit(0)
            }
            else if (it.labelText == "MENU_LABEL_RETURN") {
                if (currentRemoConContents.parent != null) {
                    remoConTray.consume()

                    currentRemoConContents = currentRemoConContents.parent!!
                    currentlySelectedRemoConItem = currentRemoConContents.data
                    remoConTray = generateNewRemoCon(currentRemoConContents)
                }
                else {
                    throw NullPointerException("No parent node to return")
                }
            }
            else if (it.labelText.contains("Start New Random Game")) {
                if (!startNewGameCalled) {
                    startNewGameCalled = true


                    printdbg(this, 1)

                    val ingame = Ingame(Terrarum.batch)
                    ingame.gameLoadInfoPayload = Ingame.NewWorldParameters(2400, 800, HQRNG().nextLong())
                    //ingame.gameLoadInfoPayload = Ingame.NewWorldParameters(8192, 2048, 0x51621DL)
                    ingame.gameLoadMode = Ingame.GameLoadMode.CREATE_NEW

                    printdbg(this, 2)

                    Terrarum.ingame = ingame
                    LoadScreen.screenToLoad = ingame
                    Terrarum.setScreen(LoadScreen)

                    printdbg(this, 3)
                }
            }
            else {
                // check if target exists
                //println("current node: ${currentRemoConContents.data}")
                //currentRemoConContents.children.forEach { println("- ${it.data}") }

                if (currentRemoConContents.children.size > selectedIndex ?: 0x7FFFFFFF) {


                    val newCurrentRemoConContents = currentRemoConContents.children[selectedIndex!!]

                    // only go deeper if that node has child to navigate
                    if (currentRemoConContents.children[selectedIndex!!].children.size != 0) {
                        remoConTray.consume()
                        remoConTray = generateNewRemoCon(newCurrentRemoConContents)
                        currentRemoConContents = newCurrentRemoConContents
                    }

                    currentlySelectedRemoConItem = newCurrentRemoConContents.data
                }
                else {
                    throw RuntimeException("Index: $selectedIndex, Size: ${currentRemoConContents.children.size}")
                }
            }


            // do something with the actual selection
            //printdbg(this, "$currentlySelectedRemoConItem")

            screens.forEach {
                //printdbg(this, "> ${it.first}")

                if (currentlySelectedRemoConItem == it.first) {
                    it.second.setAsOpen()


                    //printdbg(this, ">> ding - ${it.second.javaClass.canonicalName}")
                }
                else {
                    it.second.setAsClose()
                }
            }
        }


        screens.forEach {
            it.second.update(delta) // update is required anyway
            // but this is not updateUI, so whenever the UI is completely hidden,
            // underlying handler will block any update until the UI is open again
        }



        if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            mouseActionAvailable = true
        }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        remoConTray.render(batch, camera)

        screens.forEach {
            it.second.render(batch, camera) // again, underlying handler will block unnecessary renders
        }
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

    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        screens.forEach {
            it.second.mouseMoved(screenX, screenY) // again, underlying handler will block unnecessary renders
        }

        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        screens.forEach {
            it.second.touchDragged(screenX, screenY, pointer) // again, underlying handler will block unnecessary renders
        }

        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        screens.forEach {
            it.second.touchDown(screenX, screenY, pointer, button) // again, underlying handler will block unnecessary renders
        }

        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        screens.forEach {
            it.second.touchUp(screenX, screenY, pointer, button) // again, underlying handler will block unnecessary renders
        }

        return true
    }

    override fun scrolled(amount: Int): Boolean {
        screens.forEach {
            it.second.scrolled(amount) // again, underlying handler will block unnecessary renders
        }

        return true
    }

    override fun keyDown(keycode: Int): Boolean {
        screens.forEach {
            it.second.keyDown(keycode) // again, underlying handler will block unnecessary renders
        }

        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        screens.forEach {
            it.second.keyUp(keycode) // again, underlying handler will block unnecessary renders
        }

        return true
    }

    override fun keyTyped(character: Char): Boolean {
        screens.forEach {
            it.second.keyTyped(character) // again, underlying handler will block unnecessary renders
        }

        return true
    }



    class UIRemoConElement(uiRemoCon: UIRemoCon, val labels: Array<String>) {


        private val menubar = UIItemTextButtonList(
                uiRemoCon,
                labels,
                0, menubarOffY,
                uiRemoCon.width, getRemoConHeight(labels),
                textAreaWidth = uiRemoCon.width,
                readFromLang = true,
                activeBackCol = Color(0),
                highlightBackCol = Color(0),
                backgroundCol = Color(0),
                inactiveCol = Color.WHITE,
                defaultSelection = null
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
        val remoConWidth = 280
        fun getRemoConHeight(menu: ArrayList<String>) = 36 * menu.size.plus(1)
        fun getRemoConHeight(menu: Array<String>) = 36 * menu.size.plus(1)
        val menubarOffY: Int; get() = Terrarum.HEIGHT / 2 - (Terrarum.fontGame.lineHeight * 1.5).toInt()
    }
}