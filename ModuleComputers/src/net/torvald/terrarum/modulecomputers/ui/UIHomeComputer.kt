package net.torvald.terrarum.modulecomputers.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import net.torvald.terrarum.*
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulecomputers.gameactors.FixtureHomeComputer
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.tsvm.VM
import net.torvald.tsvm.peripheral.GraphicsAdapter
import net.torvald.unicode.*

internal class UIHomeComputer : UICanvas(
        toggleKeyLiteral = null,
        toggleButtonLiteral = "control_gamepad_start",
) {
    override var width = 640
    override var height = 480
    override var openCloseTime = 0f

    private val drawOffX = (width - 560).div(2).toFloat()
    private val drawOffY = (height - 448).div(2).toFloat()

    private var batch: FlippingSpriteBatch
    private var camera: OrthographicCamera

    internal lateinit var vm: VM
    internal lateinit var fixture: FixtureHomeComputer

    init {
        batch = FlippingSpriteBatch()
        camera = OrthographicCamera(width.toFloat(), height.toFloat())
        //val m = Matrix4()
        //m.setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat())
        batch.projectionMatrix = camera.combined
    }

    private val fbo = FrameBuffer(Pixmap.Format.RGBA8888, width, height, false)

    private val controlHelp =
            "${getKeycapPC(ControlPresets.getKey("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}\u3000 " +
            "$KEYCAP_CTRL$KEYCAP_SHIFT$KEYCAP_T$KEYCAP_R Terminate\u3000" +
            "$KEYCAP_CTRL$KEYCAP_SHIFT$KEYCAP_R$KEYCAP_S Reset\u3000" +
            "$KEYCAP_CTRL$KEYCAP_SHIFT$KEYCAP_R$KEYCAP_Q SysRq"

    override fun updateUI(delta: Float) {
    }

    override fun renderUI(otherBatch: SpriteBatch, otherCamera: OrthographicCamera) {
        otherBatch.end()

        fbo.inAction(camera, batch) {
            Gdx.gl.glClearColor(0f,0f,0f,1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT) // to hide the crap might be there

            (vm.peripheralTable[1].peripheral as? GraphicsAdapter)?.let { gpu ->
                val clearCol = gpu.getBackgroundColour()
                Gdx.gl.glClearColor(clearCol.r, clearCol.g, clearCol.b, clearCol.a)
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

                gpu.render(Gdx.graphics.deltaTime, batch, drawOffX, drawOffY, true, fbo) // gpu.render will internally end() the fbo then begin() again before using the batch I've fed in
            }
        }

        otherBatch.begin()
        otherBatch.shader = null
        blendNormalStraightAlpha(otherBatch)
        otherBatch.color = Color.WHITE
        otherBatch.draw(fbo.colorBufferTexture, posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())
        otherBatch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(otherBatch, posX - 1, posY - 1, width + 2, height + 2)

        App.fontGame.draw(otherBatch, controlHelp, posX, posY + height + 4)
    }

    override fun doOpening(delta: Float) {
        super.doOpening(delta)
        fixture.startVM()
    }


    override fun dispose() {
        fbo.dispose()
    }

}