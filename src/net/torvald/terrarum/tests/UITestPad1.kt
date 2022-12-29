package net.torvald.terrarum.tests

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.GdxRuntimeException
import net.torvald.colourutil.HUSLColorConverter
import net.torvald.terrarum.*
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent
import net.torvald.terrarum.modulebasegame.TerrarumIngame

val UITEST1_WIDTH = 1280
val UITEST1_HEIGHT = 720

/**
 * Created by minjaesong on 2018-12-09.
 */
class UITestPad1 : TerrarumGamescreen {


    lateinit var batch: SpriteBatch
    lateinit var camera: OrthographicCamera
    override fun inputStrobed(e: TerrarumKeyboardEvent) {
    }



    lateinit var colourPickerPixmap: Pixmap
    lateinit var colourPickerTex: Texture

    private var hue = 0f

    override fun show() {
        Gdx.input.inputProcessor = UITestPad1Controller(this)


        batch = FlippingSpriteBatch()
        camera = OrthographicCamera(UITEST1_WIDTH.toFloat(), UITEST1_HEIGHT.toFloat())

        camera.setToOrtho(true, UITEST1_WIDTH.toFloat(), UITEST1_HEIGHT.toFloat())
        camera.update()
        Gdx.gl20.glViewport(0, 0, UITEST1_WIDTH, UITEST1_HEIGHT)

        resize(UITEST1_WIDTH, UITEST1_HEIGHT)

        colourPickerPixmap = Pixmap(cmapSize, cmapSize, Pixmap.Format.RGBA8888)
        colourPickerTex = Texture(colourPickerPixmap)


    }

    val bgCol = Color(.62f, .79f, 1f, 1f)



    private val cmapSize = 16
    private val cmapSize2 = cmapSize * 2f
    private val cmapDrawSize = 256f

    override fun render(delta: Float) {
        Gdx.graphics.setTitle(TerrarumIngame.getCanonicalTitle())


        hue += delta * 8f
        if (hue > 360f) hue -= 360f

        for (y in 0 until cmapSize) { for (x in 0 until cmapSize) {
            val saturation = x * 100f / (cmapSize - 1f)
            val luma = 100f - y * 100f / (cmapSize - 1f)
            val rgb = HUSLColorConverter.hsluvToRgb(floatArrayOf(hue, saturation, luma))
            colourPickerPixmap.setColor(rgb[0], rgb[1], rgb[2], 1f)
            colourPickerPixmap.drawPixel(x, y)
        }}


        batch.inUse {
            colourPickerTex.dispose()
            colourPickerTex = Texture(colourPickerPixmap)
            colourPickerTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            batch.draw(colourPickerTex, 10f, 10f,
                    cmapDrawSize, cmapDrawSize,
                    1f / cmapSize2, 1f - 1f / cmapSize2,
                    1f - 1f / cmapSize2, 1f / cmapSize2
            )
        }
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
    }

    override fun dispose() {
        try { colourPickerTex.dispose() } catch (e: GdxRuntimeException) {}
        try { colourPickerPixmap.dispose() } catch (e: GdxRuntimeException) {}
    }


}

class UITestPad1Controller(val host: UITestPad1) : InputAdapter() {
    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return true
    }
}


fun main(args: Array<String>) {
    ShaderProgram.pedantic = false

    val appConfig = Lwjgl3ApplicationConfiguration()
    appConfig.useVsync(false)
    appConfig.setResizable(false)
    appConfig.setWindowedMode(UITEST1_WIDTH, UITEST1_HEIGHT)
    //appConfig.useGL30 = false; // https://stackoverflow.com/questions/46753218/libgdx-should-i-use-gl30
    appConfig.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 2)

    App.scr = TerrarumScreenSize(UITEST1_WIDTH, UITEST1_HEIGHT)

    Lwjgl3Application(App(appConfig, UITestPad1()), appConfig)
}