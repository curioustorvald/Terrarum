package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.toInt
import kotlin.math.roundToInt

/**
 * Created by Torvald on 2019-10-17.
 */
class UIItemToggleButton(
        parent: UICanvas,
        override var posX: Int,
        override var posY: Int,
        private var status: Boolean = false
) : UIItem(parent) {

    init {
        CommonResourcePool.addToLoadingList("ui_item_toggler_base") {
            Texture(Gdx.files.internal("./assets/graphics/gui/toggler_back.tga"))
        }
        CommonResourcePool.addToLoadingList("ui_item_toggler_handle") {
            Texture(Gdx.files.internal("./assets/graphics/gui/toggler_switch.tga"))
        }
        CommonResourcePool.loadAll()
    }

    override val width: Int
        get() = togglerBase.width
    override val height: Int
        get() = togglerBase.height
    override var oldPosX = posX
    override var oldPosY = posY

    private var togglerBase = CommonResourcePool.getAsTexture("ui_item_toggler_base")
    private var togglerHandle = CommonResourcePool.getAsTexture("ui_item_toggler_handle")

    private var handleTravelDist = togglerBase.width - togglerHandle.width
    private var handlePos = handleTravelDist * status.toInt()

    private var animTimer = 0f
    private var animLength = 0.1f

    private var animCalled = false

    fun getStatus() = status

    fun setAsTrue() {
        animCalled = true
        animTimer = 0f
        status = true
    }

    fun setAsFalse() {
        animCalled = true
        animTimer = 0f
        status = false
    }

    fun toggle() {
        if (status) setAsFalse() else setAsTrue()
    }

    override fun update(delta: Float) {
        super.update(delta)

        // make things move
        if (animCalled) {
            handlePos = if (status)
                ((animTimer / animLength) * handleTravelDist).roundToInt()
            else
                handleTravelDist - ((animTimer / animLength) * handleTravelDist).roundToInt()

            animTimer += delta
        }

        if (animTimer >= animLength) {
            handlePos = handleTravelDist * status.toInt()
            animCalled = false
        }

        oldPosX = posX
        oldPosY = posY
    }

    override fun render(batch: SpriteBatch, camera: Camera) {
        batch.color = Color.WHITE
        blendNormal(batch)

        batch.draw(togglerBase, posX.toFloat(), posY.toFloat())
        batch.draw(togglerHandle, (posX + handlePos).toFloat(), posY.toFloat())


        super.render(batch, camera)
    }

    override fun dispose() {
    }
}