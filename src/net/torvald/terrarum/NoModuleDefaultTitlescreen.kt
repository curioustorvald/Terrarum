package net.torvald.terrarum

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import net.torvald.terrarum.ui.Toolkit

/**
 * Created by minjaesong on 2021-12-11.
 */
class NoModuleDefaultTitlescreen(batch: SpriteBatch) : IngameInstance(batch) {

    private val wot = listOf(
            "No Module is currently loaded.",
            "Please review your Load Order on",
            "assets/mods/LoadOrder.csv"
    )

    private val maxtw = wot.maxOf { App.fontGame.getWidth(it) }

    private val fbo = FrameBuffer(Pixmap.Format.RGBA8888, App.scr.width, App.scr.height, true)

    private var init = false

    override fun render(updateRate: Float) {
        gdxClearAndSetBlend(0f, 0f, 0f, 0f)

        if (!init) {
            val lh = 36f
            val th = lh * wot.size

            fbo.inAction(null, null) {
                gdxClearAndSetBlend(.094f, .094f, .094f, 1f)
                batch.inUse {
                    batch.color = Color.WHITE
                    wot.forEachIndexed { index, s ->
                        App.fontGame.draw(batch, s, (Toolkit.drawWidth - maxtw) / 2f, (App.scr.height - th) / 2f + lh * index)
                    }
                }
            }
        }

        batch.inUse {
            batch.draw(fbo.colorBufferTexture, 0f, 0f)
        }
    }

    override fun dispose() {
        super.dispose()

        fbo.dispose()
    }
}