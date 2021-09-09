package net.torvald.terrarum.tests

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import net.torvald.terrarum.*
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.ui.UINSMenu

val UITEST1_WIDTH = 1280
val UITEST1_HEIGHT = 720

/**
 * Created by minjaesong on 2018-12-09.
 */
class UITestPad1 : ScreenAdapter() {

    val treeStr = """
- File
 - New
 - Open
 - Open Recent
  - yaml_example.yaml
  - Yaml.kt
 - Close
 - Settings
 - Line Separators
  - CRLF
  - CR
  - LF
- Edit
 - Undo
 - Redo
 - Cut
 - Copy
 - Paste
 - Find
  - Find
  - Replace
 - Convert Indents
  - To Spaces
   - Set Project Indentation
  - To Tabs
- Refactor
 - Refactor This
 - Rename
 - Extract
  - Variable
  - Property
  - Function
"""


    lateinit var nsMenu: UINSMenu
    lateinit var batch: SpriteBatch
    lateinit var camera: OrthographicCamera

    override fun show() {
        Gdx.input.inputProcessor = UITestPad1Controller(this)

        nsMenu = UINSMenu(
                "Menu",
                96,
                Yaml(treeStr)
        )
        batch = SpriteBatch()
        camera = OrthographicCamera(UITEST1_WIDTH.toFloat(), UITEST1_HEIGHT.toFloat())

        camera.setToOrtho(true, UITEST1_WIDTH.toFloat(), UITEST1_HEIGHT.toFloat())
        camera.update()
        Gdx.gl20.glViewport(0, 0, UITEST1_WIDTH, UITEST1_HEIGHT)

        resize(UITEST1_WIDTH, UITEST1_HEIGHT)

        nsMenu.setPosition(0, 0)
        nsMenu.setAsAlwaysVisible()

    }

    val bgCol = Color(.62f, .79f, 1f, 1f)

    var _dct = 0f

    override fun render(delta: Float) {
        Gdx.graphics.setTitle(TerrarumIngame.getCanonicalTitle())


        // UPDATE
        nsMenu.update(delta)

        // RENDER

        batch.inUse {
            batch.color = bgCol
            batch.fillRect(0f, 0f, 2048f, 2048f)

            nsMenu.render(batch, camera)

            batch.color = if (nsMenu.mouseOnTitleBar())
                Color.LIME
            else
                Color.FIREBRICK
            App.fontGame.draw(batch, "Mouse: ${Terrarum.mouseScreenX}, ${Terrarum.mouseScreenY}", 8f, 740 - 28f)
        }

        _dct = (_dct + delta*2) % 10f
        //nsMenu.setPosition(_dct.toInt(), _dct.toInt())
    }




    override fun pause() {
        super.pause()
    }

    override fun resume() {
        super.resume()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
    }

    override fun dispose() {
        super.dispose()
    }


}

class UITestPad1Controller(val host: UITestPad1) : InputAdapter() {
    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        host.nsMenu.touchDragged(screenX, screenY, pointer)
        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        host.nsMenu.touchDown(screenX, screenY, pointer, button)
        return true
    }
}


fun main(args: Array<String>) {
    ShaderProgram.pedantic = false

    val appConfig = Lwjgl3ApplicationConfiguration()
    appConfig.useVsync(false)
    appConfig.setResizable(false)
    appConfig.setWindowedMode(UITEST1_WIDTH, UITEST1_HEIGHT)

    Lwjgl3Application(App(appConfig, UITestPad1()), appConfig)
}