package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ceilToInt
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.ui.ControlPanelCommon.makeButton
import net.torvald.terrarum.ui.*
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.unicode.TIMES

/**
 * Created by minjaesong on 2021-10-06.
 */
class UIGraphicsControlPanel(remoCon: UIRemoCon?) : UICanvas() {

    override var width = 560

    init {
        handler.allowESCtoClose = false

        ControlPanelCommon.register(this, width, "basegame.graphicscontrolpanel", arrayOf(
            arrayOf("", { Lang["CREDITS_VFX"] }, "h1"),
                arrayOf("fx_dither", { Lang["MENU_OPTIONS_DITHER"] }, "toggle"),
                arrayOf("fx_backgroundblur", { Lang["MENU_OPTIONS_BLUR"] }, "toggle"),
                arrayOf("maxparticles", { Lang["MENU_OPTIONS_PARTICLES"] }, "spinner,256,1024,256"),
            arrayOf("", { Lang["MENU_OPTIONS_DISPLAY"] }, "h1"),
                arrayOf("screenwidth,screenheight", { Lang["MENU_OPTIONS_RESOLUTION"] }, "typeinres"),
                arrayOf("fullscreen", { Lang["MENU_OPTIONS_FULLSCREEN"] }, "toggle"),
                arrayOf("screenmagnifying", { Lang["GAME_ACTION_ZOOM"] }, "spinnerd,1.0,2.0,0.05"),
                arrayOf("screenmagnifyingfilter", { Lang["MENU_OPTIONS_FILTERING_MODE"] }, "textsel,none=MENU_OPTIONS_NONE,bilinear=MENU_OPTIONS_FILTERING_BILINEAR,hq2x=MENU_OPTIONS_FILTERING_HQ2X_DNT"),
                arrayOf("displayfps", { Lang["MENU_LABEL_FRAMESPERSEC"] }, "spinner,0,300,2"),
                arrayOf("usevsync", { Lang["MENU_OPTIONS_VSYNC"] }, "toggle"),
                arrayOf("", { "(${Lang["MENU_LABEL_RESTART_REQUIRED"]})" }, "p"),
            arrayOf("", { Lang["MENU_LABEL_STREAMING"] }, "h1"),
                arrayOf("fx_streamerslayout", { Lang["MENU_OPTIONS_STREAMERS_LAYOUT"] }, "toggle"),
        ))
    }

    override var height = ControlPanelCommon.getMenuHeight("basegame.graphicscontrolpanel")

    override fun updateUI(delta: Float) {
        uiItems.forEach { it.update(delta) }
    }

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
        ControlPanelCommon.render("basegame.graphicscontrolpanel", width, batch)
        uiItems.forEach { it.render(batch, camera) }

        if (App.getConfigBoolean("fx_streamerslayout")) {
            val xstart = App.scr.width - App.scr.chatWidth

            batch.color = Color(0x00f8ff_40)
            Toolkit.fillArea(batch, xstart + 1, 1, App.scr.chatWidth - 2, App.scr.height - 2)

            batch.color = Toolkit.Theme.COL_MOUSE_UP
            Toolkit.drawBoxBorder(batch, xstart + 1, 1, App.scr.chatWidth - 2, App.scr.height - 2)

            val overlayResTxt = "${(App.scr.chatWidth * App.scr.magn).ceilToInt()}$TIMES${App.scr.windowH}"

            App.fontGame.draw(batch, overlayResTxt,
                    (xstart + (App.scr.chatWidth - App.fontGame.getWidth(overlayResTxt)) / 2).toFloat(),
                    ((App.scr.height - App.fontGame.lineHeight) / 2).toFloat()
            )
        }
    }

    override fun dispose() {
    }
}