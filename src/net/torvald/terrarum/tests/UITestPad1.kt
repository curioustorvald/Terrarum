package net.torvald.terrarum.tests

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import net.torvald.terrarum.*
import net.torvald.terrarum.ui.UINSMenu

/**
 * Created by minjaesong on 2018-12-09.
 */
class UITestPad1 : ScreenAdapter() {

    val treeStr = """
- File
 - New : Ctrl-N
 - Open : Ctrl-O
 - Open Recent
  - yaml_example.yaml
  - Yaml.kt
 - Close : Ctrl-W
 - Settings
 - Line Separators
  - CRLF
  - CR
  - LF
- Edit
 - Undo : Ctrl-Z
 - Redo : Shift-Ctrl-Z
 - Cut : Ctrl-X
 - Copy : Ctrl-C
 - Paste : Ctrl-V
 - Find
  - Find : Ctrl-F
  - Replace : Shift-Ctrl-F
 - Convert Indents
  - To Spaces
   - Set Project Indentation
  - To Tabs
- Refactor
 - Refactor This
 - Rename : Shift-Ctrl-R
 - Extract
  - Variable
  - Property
  - Function
"""


    lateinit var nsMenu: UINSMenu
    lateinit var batch: SpriteBatch
    lateinit var camera: OrthographicCamera

    override fun show() {
        nsMenu = UINSMenu(
                "Menu",
                160,
                Yaml(treeStr)
        )
        batch = SpriteBatch()
        camera = OrthographicCamera(AppLoader.appConfig.width.toFloat(), AppLoader.appConfig.height.toFloat())

        camera.setToOrtho(true, AppLoader.appConfig.width.toFloat(), AppLoader.appConfig.height.toFloat())
        camera.update()
        Gdx.gl20.glViewport(0, 0, AppLoader.appConfig.width, AppLoader.appConfig.height)

        resize(AppLoader.appConfig.width, AppLoader.appConfig.height)

        nsMenu.setPosition(10, 10)
        nsMenu.setAsAlwaysVisible()

    }

    val bgCol = Color(.62f,.79f,1f,1f)

    override fun render(delta: Float) {
        batch.inUse {
            batch.color = bgCol
            batch.fillRect(0f, 0f, 2048f, 2048f)

            nsMenu.render(batch, camera)
        }

        //nsMenu.setPosition(20, 20) // FIXME the prolonged bug, "the entire screen is shifted!" is caused by these kind of operations
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


fun main(args: Array<String>) {
    ShaderProgram.pedantic = false

    val appConfig = LwjglApplicationConfiguration()
    appConfig.vSyncEnabled = false
    appConfig.resizable = false//true;
    //appConfig.width = 1072; // IMAX ratio
    //appConfig.height = 742; // IMAX ratio
    appConfig.width = 1110 // photographic ratio (1.5:1)
    appConfig.height = 740 // photographic ratio (1.5:1)
    appConfig.backgroundFPS = 9999
    appConfig.foregroundFPS = 9999
    appConfig.forceExit = false

    LwjglApplication(AppLoader(appConfig, UITestPad1()), appConfig)
}