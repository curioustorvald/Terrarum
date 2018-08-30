package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.Second
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextButton
import net.torvald.terrarum.ui.UIItemTextButtonList
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by minjaesong on 2018-08-29.
 */
class UIRemoCon(treeRepresentation: QNDTreeNode<String>) : UICanvas() {

    override var openCloseTime = 0f

    private var remoConTray: UIRemoConElement // this remocon is dynamically generated
    private var currentRemoConContents = treeRepresentation

    override var width = remoConWidth
    override var height: Int
        get() = remoConTray.height
        set(value) {}

    init {
        remoConTray = generateNewRemoCon(currentRemoConContents)
    }

    private var mouseActionAvailable = true

    private fun generateNewRemoCon(node: QNDTreeNode<String>): UIRemoConElement {
        val dynamicStrArray = Array(node.children.size, { node.children[it].data ?: "(null)" })
        return UIRemoConElement(this, dynamicStrArray)
    }

    override fun updateUI(delta: Float) {
        if (mouseActionAvailable) {
            remoConTray.update(delta)

            mouseActionAvailable = false
        }

        val selectedItem = remoConTray.selectedItem
        val selectedIndex = remoConTray.selectedIndex

        selectedItem?.let {
            if (it.labelText == "MENU_LABEL_RETURN") {
                if (currentRemoConContents.parent != null) {
                    remoConTray.consume()

                    currentRemoConContents = currentRemoConContents.parent!!
                    remoConTray = generateNewRemoCon(currentRemoConContents)
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

                    // only go deeper if that node has child to navigate
                    if (currentRemoConContents.children[selectedIndex!!].children.size != 0) {
                        remoConTray.consume()
                        currentRemoConContents = currentRemoConContents.children[selectedIndex!!]
                        remoConTray = generateNewRemoCon(currentRemoConContents)
                    }
                }
                else {
                    throw RuntimeException("Index: $selectedIndex, Size: ${currentRemoConContents.children.size}")
                }
            }
        }


        if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            mouseActionAvailable = true
        }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        remoConTray.render(batch, camera)

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