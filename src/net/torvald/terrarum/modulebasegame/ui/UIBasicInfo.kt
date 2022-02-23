package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.modulebasegame.imagefont.Watch7SegSmall
import net.torvald.terrarum.modulebasegame.imagefont.WatchFont
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2017-06-10.
 */
class UIBasicInfo() : UICanvas() {

    val player: ActorHumanoid?
        get() = Terrarum.ingame?.actorNowPlaying

    override var width = 93
    override var height = 23
    override var openCloseTime: Second = 0f

    private var ELuptimer = 10f // to make the light turned off by default
    private val ELuptime = 4f
    private var ELon = false

    private var watchFont = WatchFont
    private var atlas = TextureRegionPack(ModMgr.getGdxFile("basegame", "gui/basic_info1.tga"), width, height)

    private var font = Watch7SegSmall

    private val TEMP_AMERICAN = -1
    private val TEMP_KELVIN = 0
    private val TEMP_CELCIUS = 1

    override fun updateUI(delta: Float) {
        if (ELon) {
            ELuptimer += delta
        }

        if (mouseUp || Gdx.input.isKeyPressed(App.getConfigInt("control_key_interact"))) {
            ELuptimer = 0f
            ELon = true
        }

        if (ELuptimer >= ELuptime) {
            ELon = false
        }
    }

    private val temperature: Int
        get() {
            if (player != null) {
                val playerTilePos = player!!.hIntTilewiseHitbox
                val tempCelsius = -273f + ((Terrarum.ingame as? TerrarumIngame)?.world?.getTemperature(playerTilePos.centeredX.toInt(), playerTilePos.centeredY.toInt()) ?: 288f)

                return if (tempCelsius < -10)
                    0
                else if (tempCelsius < 10)
                    1
                else if (tempCelsius < 30)
                    2
                else if (tempCelsius < 50)
                    3
                else
                    4
            }
            else {
                return 2
            }
        }
    private val mailCount: Int
        get() = 0 // cap things at 99

    private val drawCol = Color(1f, 1f, 1f, UIQuickslotBar.DISPLAY_OPACITY)
    private val lcdLitColELoff = Color(0xc0c0c0ff.toInt()) mul drawCol
    private val lcdLitColELon = Color(0x404040ff) mul drawCol

    private val lcdLitCol: Color = lcdLitColELoff

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        batch.color = drawCol
        batch.draw(atlas.get(0, 0), 0f, 0f)

        // mail count
        batch.color = lcdLitCol
        watchFont.draw(batch, mailCount.toString().padStart(2, '0'), 69f, 7f)

        // temperature
        if (temperature != 0) {
            batch.draw(atlas.get(1, temperature - 1), 0f, 0f)
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
        atlas.dispose()
    }
}