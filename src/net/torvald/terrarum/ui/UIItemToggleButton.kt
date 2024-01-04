package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.blendNormalStraightAlpha
import net.torvald.terrarum.toInt
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import kotlin.math.roundToInt

/**
 * Created by Torvald on 2019-10-17.
 */
class UIItemToggleButton(
    parent: UICanvas,
    initialX: Int,
    initialY: Int,
    override val width: Int,
    private var status: Boolean = false,
) : UIItem(parent, initialX, initialY) {


    init {
        CommonResourcePool.addToLoadingList("gui_toggler_icons") {
            TextureRegionPack(Texture(Gdx.files.internal("assets/graphics/gui/toggler_icons.tga")), 14, 14)
        }
        CommonResourcePool.loadAll()
    }


    override val height = 24

    private var handleWidth = 30

    private var handleTravelDist = width - handleWidth
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

    private var mouseOnHandle = false

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


        mouseOnHandle = itemRelativeMouseX in handlePos until handlePos + handleWidth && itemRelativeMouseY in 0 until height


        oldPosX = posX
        oldPosY = posY
    }

    private val togglerIcon = CommonResourcePool.getAsTextureRegionPack("gui_toggler_icons")

    private val renderJobs = arrayOf(
        // trough fill
        { batch: SpriteBatch ->
            batch.color = UIItemTextLineInput.TEXTINPUT_COL_BACKGROUND
            Toolkit.fillArea(batch, posX, posY, width, height)
            batch.color = togglerIconCol[1]
            batch.draw(togglerIcon.get(1,0), posX.toFloat() + (handleWidth - 14)/2, posY.toFloat() + (height - 14)/2)
            batch.color = togglerIconCol[0]
            batch.draw(togglerIcon.get(0,0), posX.toFloat() + width - handleWidth + (handleWidth - 14)/2, posY.toFloat() + (height - 14)/2)
        },
        // trough border
        { batch: SpriteBatch ->
            batch.color = troughBorderCol
            Toolkit.drawBoxBorder(batch, posX - 1, posY - 1, width + 2, height + 2)
        },
        // handle fill
        { batch: SpriteBatch ->
            batch.color = handleCol.cpy().mul(Color.LIGHT_GRAY)
            Toolkit.fillArea(batch, posX + handlePos, posY, handleWidth, height)
        },
        // handle border
        { batch: SpriteBatch ->
            batch.color = handleCol
            Toolkit.drawBoxBorder(batch, posX + handlePos - 1, posY - 1, handleWidth + 2, height + 2)
        },
    )

    val troughBorderCol: Color; get() = if (mouseUp && !mouseOnHandle && mousePushed) Toolkit.Theme.COL_SELECTED
    else if (mouseUp && !mouseOnHandle) Toolkit.Theme.COL_MOUSE_UP else Toolkit.Theme.COL_INACTIVE
    val handleCol: Color; get() = if (mouseOnHandle && mousePushed) Toolkit.Theme.COL_SELECTED
    else if (mouseOnHandle) Toolkit.Theme.COL_MOUSE_UP else Color.WHITE

    private val renderOrderMouseUp = arrayOf(0,2,3,1).map { renderJobs[it] }
    private val renderOrderNormal = arrayOf(0,1,2,3).map { renderJobs[it] }

    private val togglerIconCol = arrayOf(
        Color(0xFF8888FF.toInt()),
        Color(0x00E800FF)
    )

    override fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        blendNormalStraightAlpha(batch)


        if (mouseUp && !mouseOnHandle)
            renderOrderMouseUp.forEach { it(batch) }
        else
            renderOrderNormal.forEach { it(batch) }


        super.render(frameDelta, batch, camera)
    }

    override fun dispose() {
    }
}