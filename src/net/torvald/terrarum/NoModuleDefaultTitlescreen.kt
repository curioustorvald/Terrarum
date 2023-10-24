package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.utils.OpenFile
import java.io.File

/**
 * Created by minjaesong on 2021-12-11.
 */
class NoModuleDefaultTitlescreen(batch: FlippingSpriteBatch) : IngameInstance(batch) {

    private val wot = "${Lang["APP_NOMODULE_1"]}\n${Lang["APP_NOMODULE_2"]}\n\u115F\n\u115F".split('\n')

    private val maxtw = wot.maxOf { App.fontGameFBO.getWidth(it) }

    private val fbo = FrameBuffer(Pixmap.Format.RGBA8888, App.scr.width, App.scr.height, false)

    private var init = false

    private val pathText = App.loadOrderDir.let { if (App.operationSystem == "WINDOWS") it.replace('/','\\') else it.replace('\\','/') }
    private val pathFile = File(App.loadOrderDir)//.parentFile
    private val pathButtonW = App.fontGameFBO.getWidth(pathText)
    private val pathButtonH = 20
    private var pathButtonX = 0f
    private var pathButtonY = 0f

    private var gamemode = 0

    private val fbatch = FlippingSpriteBatch(1000)
    private val camera = OrthographicCamera(App.scr.wf, App.scr.hf)


    private val genericBackdrop = Toolkit.Theme.COL_CELL_FILL.cpy().add(0f,0f,0f,1f)
    private val winTenBackdrop = Color(0x1070AAFF)
    private val winSevenBackdrop = Color(0x000080FF)
    private val osxBackdrop = Color(0x222222FF)

    private val backdrop = if (App.operationSystem == "WINDOWS" && App.OSVersion.substringBefore('.').toInt() > 7)
        winTenBackdrop
    else if (App.operationSystem == "WINDOWS")
        winSevenBackdrop
    else if (App.operationSystem == "OSX")
        osxBackdrop
    else
        genericBackdrop

    init {
        fbatch.projectionMatrix = camera.combined
        App.setConfig("screenmagnifyingfilter", "bilinear")
    }

    override fun render(updateRate: Float) {
        super.render(updateRate)

        gdxClearAndEnableBlend(0f, 0f, 0f, 0f)

        if (!init) {
            val lh = 28f
            val pbreak = 8f
            val th = lh * wot.size

            val heights = FloatArray(wot.size)

            // build y-pos map for strings
            wot.tail().forEachIndexed { index, s ->
                heights[index + 1] = heights[index] + (if (s.isBlank()) pbreak else lh)
            }

            // vertically centre the above
            val centering = (App.scr.hf - heights.last() - App.fontGame.lineHeight) / 2f

            fbo.inAction(camera, fbatch) {
                gdxClearAndEnableBlend(backdrop)
                fbatch.inUse {
                    it.color = Color.WHITE
                    wot.forEachIndexed { index, s ->
                        if (index == wot.lastIndex) {
                            pathButtonX = (Toolkit.drawWidth - pathButtonW) / 2f
                            pathButtonY = (heights[index] + centering)
                        }

                        it.color = Color.WHITE
                        App.fontGame.draw(it, s, (Toolkit.drawWidth - maxtw) / 2f, (heights[index] + centering))
                    }
                }
            }

//            fbatch.projectionMatrix = camera.combined
        }

        if (gamemode == 0) {
            val mouseOnLink = (Gdx.input.x / App.scr.magn in pathButtonX - 48..pathButtonX + 48 + pathButtonW &&
                    Gdx.input.y / App.scr.magn in pathButtonY - 12..pathButtonY + pathButtonH + 12)

            if (mouseOnLink && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                OpenFile(pathFile)
            }

            fbatch.inUse {
                it.color = Color.WHITE
                it.draw(fbo.colorBufferTexture, -App.scr.halfwf, App.scr.halfhf, App.scr.wf, -App.scr.hf)
                it.color = if (mouseOnLink) Toolkit.Theme.COL_SELECTED else Toolkit.Theme.COL_MOUSE_UP
                App.fontGame.draw(it, pathText, pathButtonX - App.scr.halfwf, pathButtonY - App.scr.halfhf)
            }

//            if (Gdx.input.isKeyPressed(Keys.ESCAPE)) gamemode = 1
        }
        else if (gamemode == 1) {

        }
    }

    override fun dispose() {
        super.dispose()
        fbatch.dispose()

        fbo.dispose()
    }
}
