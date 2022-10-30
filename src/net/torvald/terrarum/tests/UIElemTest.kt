package net.torvald.terrarum.tests

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import net.torvald.unicode.EMDASH
import net.torvald.terrarum.*
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextLineInput
import net.torvald.terrarum.ui.UIItemToggleButton
import net.torvald.terrarumsansbitmap.gdx.TerrarumSansBitmap

/**
 * Created by Torvald on 2019-10-16.
 */

class UIElemTest : ApplicationAdapter() {


    private lateinit var batch: SpriteBatch
    private lateinit var camera: OrthographicCamera
    private lateinit var ui: UICanvas

    override fun create() {
        App.fontGame = TerrarumSansBitmap(App.FONT_DIR, false, true, false,
                 false,
                256, false, 0.5f, false
        )

        batch = SpriteBatch()
        camera = OrthographicCamera()
        camera.setToOrtho(true, 800f, 600f)
        camera.update()
        Gdx.gl20.glViewport(0, 0, 800, 600)

        ui = DummyTogglePane()
        ui.isVisible = true

        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                ui.touchUp(screenX, screenY, pointer, button)
                return true
            }

            override fun keyTyped(character: Char): Boolean {
                ui.keyTyped(character)
                return true
            }

            override fun scrolled(amountX: Float, amountY: Float): Boolean {
                ui.scrolled(amountX, amountY)
                return true
            }

            override fun keyUp(keycode: Int): Boolean {
                ui.keyUp(keycode)
                return true
            }

            override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                ui.touchDragged(screenX, screenY, pointer)
                return true
            }

            override fun keyDown(keycode: Int): Boolean {
                ui.keyDown(keycode)
                return true
            }

            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                ui.touchDown(screenX, screenY, pointer, button)
                return true
            }
        }
    }


    override fun render() {
        gdxClearAndEnableBlend(0.1f, 0.1f, 0.1f, 1f)

        ui.update(Gdx.graphics.deltaTime)
        ui.render(batch, camera)

        Gdx.graphics.setTitle("Terrarum UIElemTest $EMDASH F: ${Gdx.graphics.framesPerSecond}")
    }



    override fun dispose() {
        batch.dispose()
    }
}

class DummyTogglePane : UICanvas() {
    private val button1 = UIItemToggleButton(this, 400, 100)
    private val textin = UIItemTextLineInput(this, 400, 160, 400)

    override var width = 800
    override var height = 600

    override var openCloseTime: Second = 0f

    private var timer = 0f

    init {
        button1.clickOnceListener = { _,_,_ ->
            button1.toggle()
        }
        uiItems.add(button1)
        uiItems.add(textin)
    }

    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        batch.inUse {
            batch.color = Color.CORAL
            Toolkit.fillArea(batch, 0f, 0f, 800f, 600f)

            batch.color = Color.WHITE
            uiItems.forEach { it.render(batch, camera) }
        }
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        uiItems.forEach { it.touchDown(screenX, screenY, pointer, button) }
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        uiItems.forEach { it.touchUp(screenX, screenY, pointer, button) }
        return true
    }

    override fun keyDown(keycode: Int): Boolean {
        uiItems.forEach { it.keyDown(keycode) }
        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        uiItems.forEach { it.keyUp(keycode) }
        return true
    }

    override fun keyTyped(character: Char): Boolean {
        uiItems.forEach { it.keyTyped(character) }
        return true
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
}

fun main(args: Array<String>) {
    ShaderProgram.pedantic = false

    val appConfig = Lwjgl3ApplicationConfiguration()
    appConfig.useVsync(false)
    appConfig.setResizable(false)
    appConfig.setWindowedMode(800, 600)
    appConfig.setForegroundFPS(60)
    App.scr = TerrarumScreenSize(800, 600)


    Lwjgl3Application(UIElemTest(), appConfig)
}