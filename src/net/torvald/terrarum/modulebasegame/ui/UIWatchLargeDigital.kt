package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.*
import net.torvald.terrarum.gameworld.WorldTime
import net.torvald.terrarum.modulebasegame.imagefont.WatchFont
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UINotControllable
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2017-06-11.
 */
@UINotControllable
class UIWatchLargeDigital() : UICanvas() {
    override var width = 162
    override var height = 25
    override var openCloseTime: Second = 0f

    private var ELuptimer = 10f // init value higher than uptime: to make the light turned off by default
    private val ELuptime = 4f
    private var ELon = false

    private val atlas = Texture(ModMgr.getGdxFile("basegame", "gui/watchface_large_digital.tga"))

    private val watchface = TextureRegion(atlas, 0, 0, 162, 25)
    private val seasonIcon = Array(4) {
        TextureRegion(atlas, 12 * it, 25, 12, 7)
    }
    private val seasonPos = arrayOf(
        143 to 4,
        131 to 4,
        131 to 12,
        143 to 12,
    )

    private val watchFont = WatchFont
    private val moonDial = TextureRegionPack(ModMgr.getGdxFile("basegame", "fonts/watch_17pxmoondial.tga"), 17, 17)
    private val moonDialCount = moonDial.horizontalCount

    private val lcdLitColELoff = Color(0xc0c0c0ff.toInt())
    private val lcdLitColELon = Color(0x404040ff)

    private val lcdLitCol: Color = lcdLitColELoff
        //get() = if (ELon) lcdLitColELon else lcdLitColELoff

    private val worldTime: WorldTime
        get() = INGAME.world.worldTime


    override fun updateUI(delta: Float) {
        if (ELon) {
            ELuptimer += delta
        }

        if (mouseUp || Gdx.input.isKeyPressed(ControlPresets.getKey("control_key_interact"))) {
            ELuptimer = 0f
            ELon = true
        }

        if (ELuptimer >= ELuptime) {
            ELon = false
        }
    }

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
        // disabling light button
        batch.color = Color.WHITE
        batch.draw(watchface, -1f, -1f)

        
        // day name
        batch.color = lcdLitCol
        watchFont.draw(batch, worldTime.getDayNameShort().toUpperCase(), 73f, 7f)

        // day
        watchFont.draw(batch, worldTime.calendarDay.toString().padStart(2, '@'), 107f, 7f)

        // hour
        watchFont.draw(batch, worldTime.hours.toString().padStart(2, '@'), 27f, 7f)
        // minute
        watchFont.draw(batch, worldTime.minutes.toString().padStart(2, '0'), 49f, 7f)

        // season marker
        val season = worldTime.calendarMonth - 1
        seasonPos[season].let { (x, y) ->
            batch.draw(seasonIcon[season], x.toFloat(), y.toFloat())
        }


        // moon dial
        val moonPhase = (worldTime.moonPhase * moonDialCount).roundToInt() % moonDialCount
        batch.color = lcdLitCol
        batch.draw(moonDial.get(moonPhase, 0), 6f, 3f)
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
        moonDial.dispose()
    }
}