package net.torvald.terrarum

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.Toolkit

/**
 * Created by minjaesong on 2021-12-11.
 */
class NoModuleDefaultTitlescreen(batch: FlippingSpriteBatch) : IngameInstance(batch) {

    private val wot = "${Lang["APP_NOMODULE_1"]}\n${Lang["APP_NOMODULE_2"]}\n\u115F\n\u115F".split('\n')

    private val maxtw = wot.maxOf { App.fontGameFBO.getWidth(it) }

    private val fbo = FrameBuffer(Pixmap.Format.RGBA8888, App.scr.width, App.scr.height, false)

    private var init = false

    private val pathText = App.loadOrderDir

    override fun render(updateRate: Float) {
        gdxClearAndSetBlend(0f, 0f, 0f, 0f)

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
            val centering = (App.scr.hf - heights.last() - App.fontGameFBO.lineHeight) / 2f

            fbo.inAction(null, null) {
                gdxClearAndSetBlend(.094f, .094f, .094f, 1f)
                batch.inUse {
                    batch.color = Color.WHITE
                    wot.reversed().forEachIndexed { index, s ->
                        if (index == 0) {
                            batch.color = Toolkit.Theme.COL_HIGHLIGHT
                            App.fontGameFBO.draw(batch, pathText, (Toolkit.drawWidth - App.fontGameFBO.getWidth(pathText)) / 2f, heights[index] + centering)
                        }
                        else {
                            batch.color = Color.WHITE
                            App.fontGameFBO.draw(batch, s, (Toolkit.drawWidth - maxtw) / 2f, heights[index] + centering)
                        }
                    }
                }
            }
        }

        batch.inUse {
            batch.color = Color.WHITE
            batch.draw(fbo.colorBufferTexture, 0f, 0f)
        }
    }

    override fun dispose() {
        super.dispose()

        fbo.dispose()
    }
}