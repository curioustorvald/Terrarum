package net.torvald.terrarum.tests

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
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
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItem
import net.torvald.terrarum.ui.UIItemTextButton
import net.torvald.terrarum.ui.UIItemToggleButton
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
    private val button1 = UIItemToggleButton(this, 10, 10)

    override var width = 100
    override var height = 25

    override var openCloseTime: Second = 0f

    private var timer = 0f

    override fun updateUI(delta: Float) {
        timer += delta

        if (timer >= 1.5f) {
            timer -= 1.5f
            button1.toggle()
        }

        button1.update(delta)
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        batch.inUse {
            batch.color = Color(0x404040ff)
            button1.render(batch, camera)
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