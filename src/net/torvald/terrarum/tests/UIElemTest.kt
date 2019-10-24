package net.torvald.terrarum.tests

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.jme3.math.FastMath
import net.torvald.terrarum.*
import net.torvald.terrarum.ui.*
import kotlin.math.roundToInt

/**
 * Created by Torvald on 2019-10-16.
 */

class UIElemTest : ApplicationAdapter() {


    private lateinit var batch: SpriteBatch
    private lateinit var camera: OrthographicCamera
    private lateinit var ui: UICanvas

    override fun create() {
        batch = SpriteBatch()
        camera = OrthographicCamera()
        camera.setToOrtho(false, 800f, 600f)
        camera.update()
        ui = DummyTogglePane()
        ui.isVisible = true
    }


    override fun render() {
        ui.update(Gdx.graphics.rawDeltaTime)
        ui.render(batch, camera)
    }



    override fun dispose() {
        batch.dispose()
    }
}

class DummyTogglePane : UICanvas() {
    private val button1 = UIItemToggleButton(this, 0, 0)

    private val key1 = UIItemConfigKeycap(this, 0, 20, 4, Input.Keys.A)
    private val key2 = UIItemConfigKeycap(this, 36, 20, 4, Input.Keys.S)
    private val key3 = UIItemConfigKeycap(this, 36*2, 20, 4, Input.Keys.D)
    private val key4 = UIItemConfigKeycap(this, 36*3, 20, 4, Input.Keys.F)

    override var width = 100
    override var height = 25

    override var openCloseTime: Second = 0f

    private var timer = 0f

    init {
        uiItems.add(button1)
        uiItems.add(key1)
        uiItems.add(key2)
        uiItems.add(key3)
        uiItems.add(key4)
    }

    override fun updateUI(delta: Float) {
        timer += delta

        if (timer >= 1f) {
            timer -= 1f
            button1.toggle()
        }

        uiItems.forEach { it.update(delta) }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        batch.inUse {

            uiItems.forEach { it.render(batch, camera) }
        }
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

    val appConfig = LwjglApplicationConfiguration()
    appConfig.vSyncEnabled = false
    appConfig.resizable = false
    appConfig.width = 800
    appConfig.height = 600
    appConfig.backgroundFPS = 60
    appConfig.foregroundFPS = 60
    appConfig.forceExit = false

    LwjglApplication(UIElemTest(), appConfig)
}