package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*

/**
 * Nextstep-themed menu bar with mandatory title line
 *
 * Created by minjaesong on 2018-12-08.
 */
class UINSMenu(
        var title: String = "",
        val minimumWidth: Int,
        /** Optional instance of YamlInvokable can be used */
        treeRepresentation: Yaml,

        val titleBackCol: Color = DEFAULT_TITLEBACKCOL,
        val titleTextCol: Color = DEFAULT_TITLETEXTCOL,
        val titleBlendMode: String = BlendMode.NORMAL,

        val allowDrag: Boolean = true
) : UICanvas() {

    companion object {
        val DEFAULT_TITLEBACKCOL = Color(0f, 0f, 0f, .77f)
        val DEFAULT_TITLETEXTCOL = Color.WHITE
    }

    override var openCloseTime: Second = 0f
    val LINE_HEIGHT = 24
    val TEXT_OFFSETX = 3f
    val TEXT_OFFSETY = (LINE_HEIGHT - App.fontGame.lineHeight) / 2f
    val CHILD_ARROW = "${0x2023.toChar()}"


    val tree = treeRepresentation.parseAsYamlInvokable()
    override var width = 0
    override var height = 0
    //override var width = maxOf(minimumWidth, tree.getLevelData(1).map { AppLoader.fontGame.getWidth(it ?: "") }.max() ?: 0)
    //override var height = LINE_HEIGHT * (tree.children.size + 1)


    private val listStack = ArrayList<MenuPack>()
    /** cached version of listStack.size */
    private var currentDepth = 0

    private data class MenuPack(val title: String, val ui: UIItemTextButtonList)

    private fun ArrayList<MenuPack>.push(item: MenuPack) {
        currentDepth += 1
        this.add(item)
    }
    private fun ArrayList<MenuPack>.pop(): MenuPack {
        currentDepth -= 1
        return this.removeAt(this.lastIndex)
    }
    private fun ArrayList<MenuPack>.peek() = this.last()


    val selectedIndex: Int?
        get() = listStack.peek().ui.selectedIndex

    var invocationArgument: Array<Any> = arrayOf(this)

    init {
        addSubMenu(tree)
    }

    override val mouseUp: Boolean
        get() {
            for (sp in 0 until currentDepth) {
                val subList = listStack[sp].ui

                val _mouseUp = relativeMouseX in subList.posX..subList.posX + subList.width &&
                                relativeMouseY in subList.posY - LINE_HEIGHT..subList.posY + subList.height

                if (_mouseUp) return true
            }

            return false
        }

    // FIXME mouseUp doesn't work here

    private fun addSubMenu(tree: QNDTreeNode<Pair<String, YamlInvokable?>>) {
        val menuTitle = tree.data?.first ?: title
        val stringsFromTree = Array<String>(tree.children.size) {
            tree.children[it].data?.first + if (tree.children[it].children.isNotEmpty()) "  $CHILD_ARROW" else ""
        }

        val listWidth = maxOf(
                App.fontGame.getWidth(menuTitle), minimumWidth,
                stringsFromTree.map { App.fontGame.getWidth(it) }.maxOrNull() ?: 0
        )
        val uiWidth = listWidth + (2 * TEXT_OFFSETX.toInt())
        val listHeight = stringsFromTree.size * LINE_HEIGHT

        val list = UIItemTextButtonList(
                this,
                UIItemTextButtonList.DEFAULT_LINE_HEIGHT,
                stringsFromTree,
                width, LINE_HEIGHT,
                uiWidth, listHeight,
                textAreaWidth = listWidth,
                alignment = UIItemTextButton.Companion.Alignment.LEFT,
                inactiveCol = Color(.94f, .94f, .94f, 1f),
                itemHitboxSize = LINE_HEIGHT

        )

        // List selection change listener
        list.selectionChangeListener = { old, new ->
            // if the selection has a child...

            //println("new sel: ${tree.children[new]}")


            // 1. pop as far as possible
            // 2. push the new menu

            // 1. pop as far as possible
            while (listStack.peek().ui != list) {
                popSubMenu()
            }

            // 2. push the new menu
            if (tree.children[new].children.isNotEmpty()) {
                addSubMenu(tree.children[new])
            }

            // invoke whatever command there is
            //printdbg(this, "Selected: ${tree.children[new].data?.second}")
            tree.children[new].data?.second?.invoke(invocationArgument)
        }
        // END List selection change listener


        // push the processed list
        listStack.push(MenuPack(menuTitle, list))
        // increment the memoized width
        width += uiWidth
    }

    private fun popSubMenu() {
        if (listStack.size == 1) {
            System.err.println("[UINSMenu] Tried to pop root menu")
            printStackTrace(this, System.err)
            return
        }

        val poppedUIItem = listStack.pop()
        width -= poppedUIItem.ui.width
    }

    override fun updateUI(delta: Float) {
        /*listStack.forEach {
            it.list.update(delta)
        }*/ // fucking concurrent modification

        var c = 0
        while (c < listStack.size) {
            listStack[c].ui.update(delta)
            c += 1
        }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        listStack.forEach {
            // draw title bar
            batch.color = titleBackCol
            BlendMode.resolve(titleBlendMode, batch)
            Toolkit.fillArea(batch, it.ui.posX.toFloat(), it.ui.posY.toFloat() - LINE_HEIGHT, it.ui.width.toFloat(), LINE_HEIGHT.toFloat())

            batch.color = titleTextCol
            blendNormal(batch)
            App.fontGame.draw(batch, it.title, TEXT_OFFSETX + it.ui.posX, TEXT_OFFSETY + it.ui.posY - LINE_HEIGHT)

            // draw the list
            batch.color = Color.WHITE
            it.ui.render(batch, camera)
        }

    }

    override fun dispose() {
        listStack.forEach { it.ui.dispose() }
    }

    fun mouseOnTitleBar() =
            relativeMouseX in 0 until width && relativeMouseY in 0 until LINE_HEIGHT

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
    }


    private var dragOriginX = 0 // relative mousepos
    private var dragOriginY = 0 // relative mousepos
    private var dragForReal = false

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (!allowDrag) return false

        if (mouseInScreen(screenX, screenY)) {
            if (dragForReal) {
                handler.setPosition(screenX - dragOriginX, screenY - dragOriginY)
                //println("drag $screenX, $screenY")
            }
        }

        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (mouseOnTitleBar()) {
            dragOriginX = relativeMouseX
            dragOriginY = relativeMouseY
            dragForReal = true
        }
        else {
            dragForReal = false
        }

        return true
    }

    override fun resize(width: Int, height: Int) {
    }
}