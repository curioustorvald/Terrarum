package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.toInt
import kotlin.math.roundToInt

/**
 * Created by Torvald on 2019-10-17.
 */
class UIItemToggleButton(
        parent: UICanvas,
        initialX: Int,
        initialY: Int,
        private var status: Boolean = false
) : UIItem(parent, initialX, initialY) {

    init {
        CommonResourcePool.addToLoadingList("ui_item_toggler_base") {
            val t = TextureRegion(Texture(Gdx.files.internal("./assets/graphics/gui/toggler_back.tga")))
            t.flip(false, false)
            t
        }
        CommonResourcePool.addToLoadingList("ui_item_toggler_handle") {
            val t = TextureRegion(Texture(Gdx.files.internal("./assets/graphics/gui/toggler_switch.tga")))
            t.flip(false, false)
            t
        }
        CommonResourcePool.loadAll()
    }

    override val width: Int
        get() = togglerBase.regionWidth
    override val height: Int
        get() = togglerBase.regionHeight

    private var togglerBase = CommonResourcePool.getAsTextureRegion("ui_item_toggler_base")
    private var togglerHandle = CommonResourcePool.getAsTextureRegion("ui_item_toggler_handle")

    private var handleTravelDist = togglerBase.regionWidth - togglerHandle.regionWidth
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

    // define clickOnceListener by yourself!

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