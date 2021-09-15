package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.Second
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItem
import net.torvald.terrarum.ui.UIItemTextButton

/**
 * Created by minjaesong on 2021-09-15.
 */
class UIKeyboardControlPanel : UICanvas() {

    override var width = 600
    override var height = 600
    override var openCloseTime = 0f

    private val kbx = 61
    private val kby = 95

    private val keycaps = hashMapOf(
        Input.Keys.APOSTROPHE to UIItemKeycap(this, kbx, kby, null),
            // ...
    )

    override fun updateUI(delta: Float) {
        TODO("Not yet implemented")
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        TODO("Not yet implemented")
    }

    override fun doOpening(delta: Float) {
        TODO("Not yet implemented")
    }

    override fun doClosing(delta: Float) {
        TODO("Not yet implemented")
    }

    override fun endOpening(delta: Float) {
        TODO("Not yet implemented")
    }

    override fun endClosing(delta: Float) {
        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }
}



/**
 * @param key LibGDX keycode. Set it to `null` to "disable" the key. Also see [com.badlogic.gdx.Input.Keys]
 */
class UIItemKeycap(parent: UIKeyboardControlPanel, initialX: Int, initialY: Int, val key: Int?) : UIItem(parent, initialX, initialY) {

    override val width = 600
    override val height = 600

    private val labels = CommonResourcePool.getAsTextureRegionPack("inventory_category")

    private val borderKeyForbidden = "0x000000C0"
    private val borderKeyNormal = "0xFFFFFFC0".toInt()
    private val borderKeyPressed = UIItemTextButton.defaultActiveCol

    private val keycapFill = ItemSlotImageFactory.CELLCOLOUR_BLACK

    override fun render(batch: SpriteBatch, camera: Camera) {
        super.render(batch, camera)
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }
}

