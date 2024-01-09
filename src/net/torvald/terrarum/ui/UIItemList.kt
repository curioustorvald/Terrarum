package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.BlendMode
import net.torvald.terrarum.Second
import net.torvald.terrarum.Terrarum


/**
 * Created by minjaesong on 2017-08-01.
 */
class UIItemList<Item: UIItem>(
        parentUI: UICanvas,
        val itemList: ArrayList<Item>,
        initialX: Int,
        initialY: Int,
        override val width: Int,
        override val height: Int,

        var selectable: Boolean = false,
        val defaultSelection: Int? = null, // negative: INVALID, positive: valid, null: no select

        // copied directly from UIItemTextButton
        val activeCol: Color = Toolkit.Theme.COL_MOUSE_UP,
        val activeBackCol: Color = Color(0),
        val activeBackBlendMode: String = BlendMode.NORMAL,
        val highlightCol: Color = Toolkit.Theme.COL_SELECTED,
        val highlightBackCol: Color = Color(0xb0b0b0_ff.toInt()),
        val highlightBackBlendMode: String = BlendMode.MULTIPLY,
        val inactiveCol: Color = Toolkit.Theme.COL_INACTIVE,
        val backgroundCol: Color = UIItemTextButtonList.DEFAULT_BACKGROUNDCOL,
        val backgroundBlendMode: String = BlendMode.NORMAL,
        val kinematic: Boolean = false,
        val border: Int = 0
) : UIItem(parentUI, initialX, initialY) {

    init {
        itemList.forEachIndexed { index, item ->
            item.posX = this.posX + border
            item.posY = if (index == 0) this.posY + border else itemList[index - 1].posY + itemList[index - 1].height
        }
    }


    var selectedIndex: Int? = defaultSelection
    val selectedButton: UIItem?
        get() = if (selectedIndex != null) itemList[selectedIndex!!] else null
    private var highlightY: Float? = if (selectedIndex != null) itemList[selectedIndex!!].posY.toFloat() else null
    private val highlighterMoveDuration: Second = 0.1f
    private var highlighterMoveTimer: Second = 0f
    private var highlighterMoving = false
    private var highlighterYStart = highlightY
    private var highlighterYEnd = highlightY

    /** (oldIndex: Int?, newIndex: Int) -> Unit */
    var selectionChangeListener: ((Int?, Int) -> Unit)? = null

    private var clickLatch = MouseLatch()

    override fun show() {
        clickLatch.forceLatch()
    }


    override fun update(delta: Float) {
        val posXDelta = posX - oldPosX
        itemList.forEach { it.posX += posXDelta }


        if (highlighterMoving) {
            highlighterMoveTimer += delta

            if (selectedIndex != null) {
                highlightY = Movement.moveQuick(
                        highlighterYStart!!,
                        highlighterYEnd!!,
                        highlighterMoveTimer,
                        highlighterMoveDuration
                )
            }

            if (highlighterMoveTimer > highlighterMoveDuration) {
                highlighterMoveTimer = 0f
                highlighterYStart = highlighterYEnd
                highlightY = highlighterYEnd
                highlighterMoving = false
            }
        }

        itemList.forEachIndexed { index, item ->
            item.update(delta)


            if (item.mouseUp) clickLatch.latchNoRelease {
                val oldIndex = selectedIndex

                if (kinematic) {
                    highlighterYStart = itemList[selectedIndex!!].posY.toFloat()
                    selectedIndex = index
                    highlighterMoving = true
                    highlighterYEnd = itemList[selectedIndex!!].posY.toFloat()
                }
                else {
                    selectedIndex = index
                    highlightY = itemList[selectedIndex!!].posY.toFloat()
                }

                selectionChangeListener?.invoke(oldIndex, index)
            }
            //item.highlighted = (index == selectedIndex) // forcibly highlight if this.highlighted != null

        }

        clickLatch.latch {  } // update to unlatch

        oldPosX = posX
    }

    override fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        batch.color = backgroundCol
        BlendMode.resolve(backgroundBlendMode, batch)
        Toolkit.fillArea(batch, posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())

        batch.color = highlightBackCol
        BlendMode.resolve(highlightBackBlendMode, batch)
        if (highlightY != null) {
            Toolkit.fillArea(batch, posX.toFloat(), highlightY!!.toFloat(), width.toFloat(), UIItemTextButton.height.toFloat())
        }

        itemList.forEach { it.render(frameDelta, batch, camera) }

        batch.color = backgroundCol
    }

    override fun dispose() {
        itemList.forEach { it.dispose() }
    }
}