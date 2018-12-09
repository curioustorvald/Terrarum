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
        treeRepresentation: Yaml,

        val titleBackCol: Color = Color(0f,0f,0f,.77f),
        val titleTextCol: Color = Color.WHITE,
        val titleBlendMode: String = BlendMode.NORMAL

) : UICanvas() {

    override var openCloseTime: Second = 0f
    val LINE_HEIGHT = 24
    val TEXT_OFFSETX = 3f
    val TEXT_OFFSETY = (LINE_HEIGHT - Terrarum.fontGame.lineHeight) / 2f
    val CHILD_ARROW = "${0x2023.toChar()}"


    val tree = treeRepresentation.parse()
    override var width = maxOf(minimumWidth, tree.getLevelData(1).map { Terrarum.fontGame.getWidth(it ?: "") }.max() ?: 0)
    override var height = LINE_HEIGHT * (tree.children.size + 1)
    private val treeChildrenLabels = Array<String>(tree.children.size) {
        tree.children[it].toString() + if (tree.children[it].children.isNotEmpty()) "  $CHILD_ARROW" else ""
    }

    private val theRealList = UIItemTextButtonList(
            this,
            treeChildrenLabels,
            posX, posY + LINE_HEIGHT,
            width, height - LINE_HEIGHT,
            textAreaWidth = width - (2 * TEXT_OFFSETX.toInt()),
            alignment = UIItemTextButton.Companion.Alignment.LEFT,
            activeBackCol = Color(0x242424_80),//Color(1f,0f,.75f,1f),
            inactiveCol = Color(.94f,.94f,.94f,1f),
            itemHitboxSize = LINE_HEIGHT

    )

    val selectedIndex: Int?
        get() = theRealList.selectedIndex

    override fun updateUI(delta: Float) {
        theRealList.update(delta)
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        // draw title bar
        batch.color = titleBackCol
        BlendMode.resolve(titleBlendMode, batch)
        batch.fillRect(posX.toFloat(), posY.toFloat(), width.toFloat(), LINE_HEIGHT.toFloat())

        batch.color = titleTextCol
        blendNormal(batch)
        Terrarum.fontGame.draw(batch, title, posX + TEXT_OFFSETX, posY + TEXT_OFFSETY)

        // draw the list
        batch.color = Color.WHITE
        theRealList.render(batch, camera)
    }

    override fun dispose() {
        theRealList.dispose()
    }

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return super.mouseMoved(screenX, screenY)
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return super.touchDragged(screenX, screenY, pointer)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return super.touchDown(screenX, screenY, pointer, button)
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return super.touchUp(screenX, screenY, pointer, button)
    }

    override fun scrolled(amount: Int): Boolean {
        return super.scrolled(amount)
    }

    override fun keyDown(keycode: Int): Boolean {
        return super.keyDown(keycode)
    }

    override fun keyUp(keycode: Int): Boolean {
        return super.keyUp(keycode)
    }

    override fun keyTyped(character: Char): Boolean {
        return super.keyTyped(character)
    }

    override fun resize(width: Int, height: Int) {
    }
}