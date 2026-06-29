package net.torvald.terrarum.modulecomputers.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import net.torvald.terrarum.*
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulecomputers.gameactors.FixtureHomeComputer
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTransitionContainer
import net.torvald.tsvm.VM
import net.torvald.tsvm.peripheral.GraphicsAdapter
import net.torvald.unicode.*
import java.util.Locale

/**
 * The operator console UI for [FixtureHomeComputer].
 *
 * The live VM screen is shown 2x-magnified with the TSVM CRT shader and grabs full keyboard + mouse
 * input (ESC closes the UI). The monitor is the centre page of a [UIItemTransitionContainer]; the
 * left/right pages are reserved placeholders (connection ports / inside chassis).
 *
 * Created by minjaesong on 2021-12-04, rewritten 2026-06-28.
 */
internal class UIHomeComputer : UICanvas(
    toggleKeyLiteral = null,
    toggleButtonLiteral = "control_gamepad_start",
) {
    override var width = App.scr.width
    override var height = App.scr.height
    override var openCloseTime = 0f

    internal lateinit var vm: VM
    internal lateinit var fixture: FixtureHomeComputer

    private val monitor = MonitorPanel(this)
    private val portsPage = PlaceholderPanel(this, "CONNECTION PORTS")
    private val chassisPage = PlaceholderPanel(this, "INSIDE CHASSIS")

    // monitor is the centre page (index 1); index 0 = ports, index 2 = chassis
    private val container = UIItemTransitionContainer(
        this, 0, 0, width, height,
        currentPosition = 1f,
        uis = listOf(portsPage, monitor, chassisPage)
    )

    private val controlHelp =
        "${getKeycapPC(Input.Keys.ESCAPE)} ${Lang["GAME_ACTION_CLOSE"]}　 " +
        "$KEYCAP_CTRL$KEYCAP_SHIFT$KEYCAP_Q Power off　" +
        "$KEYCAP_CTRL$KEYCAP_SHIFT$KEYCAP_R$KEYCAP_S Reset"

    override fun updateImpl(delta: Float) {
        container.update(delta)
    }

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        container.render(frameDelta, batch, camera)

        batch.color = Color.WHITE
        App.fontGame.draw(batch, controlHelp, 8f, App.scr.height - 24f)
    }

    private fun ctrlShift() =
        (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) &&
        (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT))

    override fun keyDown(keycode: Int): Boolean {
        when {
            keycode == Input.Keys.ESCAPE -> { setAsClose(); return true }
            // host-level power-off (Terminate); the in-VM reset/SysRq combos are handled by the VM itself
            ctrlShift() && keycode == Input.Keys.Q -> { fixture.stopVM(); setAsClose(); return true }
            else -> container.keyDown(keycode)
        }
        return true
    }

    override fun keyUp(keycode: Int): Boolean { container.keyUp(keycode); return true }
    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean { container.touchDown(screenX, screenY, pointer, button); return true }
    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean { container.touchUp(screenX, screenY, pointer, button); return true }
    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean { container.touchDragged(screenX, screenY, pointer); return true }
    override fun scrolled(amountX: Float, amountY: Float): Boolean { container.scrolled(amountX, amountY); return true }

    override fun doOpening(delta: Float) {
        super.doOpening(delta)
        fixture.startVM() // power on when the console is first opened
    }

    override fun dispose() {
        container.dispose()
    }

    // ----------------------------------------------------------------- monitor page

    /** Renders the live VM screen 2x with the CRT post-shader and forwards all input to the VM. */
    private inner class MonitorPanel(private val parentUI: UIHomeComputer) : UICanvas() {
        override var width = App.scr.width
        override var height = App.scr.height
        override var openCloseTime = 0f

        private val fbW get() = FixtureHomeComputer.GRAPHICSCONFIG.width
        private val fbH get() = FixtureHomeComputer.GRAPHICSCONFIG.height
        private val magn = 1

        private var crtShader: ShaderProgram? = null
        private var gpuFBO: FrameBuffer? = null
        private var monBatch: FlippingSpriteBatch? = null
        private var monCamera: OrthographicCamera? = null
        private var framecount = 0L

        private fun ensureGL() {
            if (crtShader == null) crtShader = loadCrtShader()
            if (gpuFBO == null) gpuFBO = FrameBuffer(Pixmap.Format.RGBA8888, fbW, fbH, false)
            if (monBatch == null) monBatch = FlippingSpriteBatch()
            if (monCamera == null) monCamera = OrthographicCamera(fbW.toFloat(), fbH.toFloat())
        }

        private fun loadCrtShader(): ShaderProgram {
            // The shader resource lives inside the swappable TerrarumTSVM.jar, which is on the *module*
            // classloader — not the system classpath that Gdx.files.classpath() would search. Read it
            // through a TSVM class's classloader (same trick TSVM uses to load JS_INIT.js).
            val resPath = "net/torvald/tsvm/shader_crt_post.frag"
            val frag0 = (GraphicsAdapter::class.java.classLoader.getResourceAsStream(resPath)
                ?: throw RuntimeException("CRT shader resource not found on the TSVM classloader: $resPath"))
                .bufferedReader().use { it.readText() }
            val frag = if (Gdx.graphics.glVersion.majorVersion >= 4) "#version 400\n$frag0"
                       else "#version 330\n#define fma(a,b,c) (((a)*(b))+(c))\n$frag0"
            val s = ShaderProgram(GraphicsAdapter.DRAW_SHADER_VERT, frag)
            if (s.log.lowercase(Locale.getDefault()).contains("error"))
                throw RuntimeException("CRT shader failed to compile:\n${s.log}")
            return s
        }

        override fun updateImpl(delta: Float) {}

        override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
            ensureGL()
            framecount++

            val gpu = parentUI.fixture.gpu
            val drawW = fbW * magn
            val drawH = fbH * magn
            val monX = (App.scr.width - drawW) / 2f
            val monY = (App.scr.height - drawH) / 2f

            if (gpu == null) {
                batch.color = Color.WHITE
                val msg = "POWERING ON…"
                App.fontGame.draw(batch, msg, (App.scr.width - App.fontGame.getWidth(msg)) / 2f, App.scr.height / 2f)
                return
            }

            // map mouse coordinates onto the 2x monitor rectangle
            parentUI.vm.getIO().let { io ->
                io.inputViewport = null
                io.inputOriginX = monX.toInt()
                io.inputOriginY = monY.toInt()
                io.inputAreaW = drawW
                io.inputAreaH = drawH
            }

            val crt = crtShader!!
            val gpuFbo = gpuFBO!!
            val mb = monBatch!!
            val cam = monCamera!!

            batch.end()

            // 1) render the VM screen into gpuFBO at native resolution
            gpuFbo.inAction(cam, mb) {
                val bg = gpu.getBackgroundColour()
                Gdx.gl.glClearColor(bg.r, bg.g, bg.b, bg.a)
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
                gpu.render(frameDelta, mb, 0f, 0f, true, null, gpuFbo)
            }
            // gpuFbo.inAction restores the bound FBO and leaves `cam`/`mb` at screen-ortho (Y-down)

            // 2) blit gpuFBO onto the screen through the CRT post-shader using the MAIN batch, so the
            //    quad shares the UI's correct camera/projection. The main batch is a FlippingSpriteBatch
            //    whose unpacked-colour vertices match the CRT vertex shader.
            //    (mb/monCamera must NOT be used here: inAction leaves monCamera centred at the origin and
            //    sized to the unmagnified screen, which both mis-positions and scrambles the quad.)
            batch.begin()
            batch.shader = crt
            crt.setUniformf("resolution", drawW.toFloat(), drawH.toFloat())
            crt.setUniformf("interlacer", (framecount % 2).toFloat())
            crt.setUniformf("time", (framecount % 640).toFloat())
            crt.setUniformi("signalMode", 0) // -1 RGB (sharp), 0 S-video, 1 composite
            batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_SRC_ALPHA, GL20.GL_ONE)
            batch.color = Color.WHITE
            batch.draw(gpuFbo.colorBufferTexture, monX, monY, drawW.toFloat(), drawH.toFloat())
            batch.shader = null
            blendNormalStraightAlpha(batch)

            // thin bezel
            batch.color = Toolkit.Theme.COL_INACTIVE
            Toolkit.drawBoxBorder(batch, monX.toInt() - 1, monY.toInt() - 1, drawW + 2, drawH + 2)
            batch.color = Color.WHITE
        }

        override fun keyDown(keycode: Int): Boolean { parentUI.vm.getIO().keyDown(keycode); return true }
        override fun keyUp(keycode: Int): Boolean { parentUI.vm.getIO().keyUp(keycode); return true }
        override fun keyTyped(character: Char): Boolean { parentUI.vm.getIO().keyTyped(character); return true }
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean { parentUI.vm.getIO().touchDown(screenX, screenY, pointer, button); return true }
        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean { parentUI.vm.getIO().touchUp(screenX, screenY, pointer, button); return true }
        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean { parentUI.vm.getIO().touchDragged(screenX, screenY, pointer); return true }
        override fun scrolled(amountX: Float, amountY: Float): Boolean { parentUI.vm.getIO().scrolled(amountX, amountY); return true }

        override fun dispose() {
            crtShader?.dispose()
            gpuFBO?.dispose()
            monBatch?.dispose()
        }
    }

    // ----------------------------------------------------------------- placeholder pages

    /** Reserved left/right pages (connection ports, inside chassis). For now just a labelled panel. */
    private inner class PlaceholderPanel(private val parentUI: UIHomeComputer, private val title: String) : UICanvas() {
        override var width = App.scr.width
        override var height = App.scr.height
        override var openCloseTime = 0f

        private val panelW = 480
        private val panelH = 360

        override fun updateImpl(delta: Float) {}

        override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
            val x = (App.scr.width - panelW) / 2
            val y = (App.scr.height - panelH) / 2

            batch.color = Toolkit.Theme.COL_CELL_FILL
            Toolkit.fillArea(batch, x, y, panelW, panelH)
            batch.color = Toolkit.Theme.COL_INACTIVE
            Toolkit.drawBoxBorder(batch, x, y, panelW, panelH)

            batch.color = Color.WHITE
            App.fontGame.draw(batch, title, x + (panelW - App.fontGame.getWidth(title)) / 2f, (y + panelH / 2).toFloat())
        }

        override fun dispose() {}
    }
}
