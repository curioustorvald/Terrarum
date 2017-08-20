package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.GdxRuntimeException
import net.torvald.terrarum.langpack.Lang

object ErrorDisp : Screen {

    val logoTex = TerrarumAppLoader.logo
    val font = TerrarumAppLoader.fontGame


    var title = Lang["ERROR_GENERIC_TEXT"]
    var text = arrayListOf<String>("")

    lateinit var batch: SpriteBatch


    val textPosX = 45f

    lateinit var camera: OrthographicCamera


    val titleTextLeftMargin = 8
    val titleText = "${TerrarumAppLoader.GAME_NAME} ${TerrarumAppLoader.getVERSION_STRING()}"


    override fun show() {
        batch = SpriteBatch()


        camera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.setToOrtho(true)
        camera.update()
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(.094f, .094f, .094f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)



        // draw background


        // draw logo
        batch.inUse {
            batch.projectionMatrix = camera.combined

            val headerLogoW = (logoTex.texture.width / 4f).toInt()
            val headerLogoH = (logoTex.texture.height / 4f).toInt()
            val headerTextWidth = font.getWidth(titleText)
            val headerTotalWidth = headerLogoW + titleTextLeftMargin + headerTextWidth
            val headerLogoXPos = (Gdx.graphics.width - headerTotalWidth).div(2)
            val headerLogoYPos = 25 + headerLogoH
            batch.color = Color.WHITE
            batch.draw(logoTex.texture, headerLogoXPos.toFloat(), headerLogoYPos.toFloat(), headerLogoW.toFloat(), -headerLogoH.toFloat())
            font.draw(batch, titleText, headerLogoXPos + headerLogoW.toFloat() + titleTextLeftMargin,  headerLogoYPos -(headerLogoH / 2f) - font.lineHeight + 4)


            // draw error text
            batch.color = Color(0xff8080ff.toInt())
            font.draw(batch, title, textPosX, logoTex.texture.height / 4f * 2 + 50f)

            batch.color = Color.WHITE
            text.forEachIndexed { index, s ->
                font.draw(batch, s, textPosX, logoTex.texture.height / 4f * 2 + 130f + index * font.lineHeight)
            }
        }

    }

    override fun hide() {
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun dispose() {

    }
}

class TerrarumRuntimeException(messageTitle: String, message: List<String>? = null) : RuntimeException() {

    init {

    }

}