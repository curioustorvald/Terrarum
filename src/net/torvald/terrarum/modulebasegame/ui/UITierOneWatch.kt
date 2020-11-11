package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.modulebasegame.gameworld.GameWorldExtension
import net.torvald.terrarum.modulebasegame.gameworld.WorldTime
import net.torvald.terrarum.modulebasegame.imagefont.WatchFont
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2017-06-11.
 */
class UITierOneWatch(private val player: ActorHumanoid?) : UICanvas() {
    override var width = 160
    override var height = 23
    override var openCloseTime: Second = 0f

    private var ELuptimer = 10f // init value higher than uptime: to make the light turned off by default
    private val ELuptime = 4f
    private var ELon = false

    private var atlas = TextureRegionPack(ModMgr.getPath("basegame", "gui/watchface_atlas.tga"), width, height)

    private var watchFont = WatchFont
    private var moonDial = TextureRegionPack(ModMgr.getPath("basegame", "fonts/watch_17pxmoondial.tga"), 17, 17)
    private var moonDialCount = moonDial.horizontalCount

    private val drawCol = Color(1f, 1f, 1f, UIQuickslotBar.DISPLAY_OPACITY)
    private val lcdLitColELoff = Color(0xc0c0c0ff.toInt()) mul drawCol
    private val lcdLitColELon = Color(0x404040ff) mul drawCol

    private val lcdLitCol: Color = lcdLitColELoff
        //get() = if (ELon) lcdLitColELon else lcdLitColELoff

    private val worldTime: WorldTime
        get() = (Terrarum.ingame!!.world as GameWorldExtension).worldTime


    override fun updateUI(delta: Float) {
        if (ELon) {
            ELuptimer += delta
        }

        if (mouseUp || Gdx.input.isKeyPressed(AppLoader.getConfigInt("keyinteract"))) {
            ELuptimer = 0f
            ELon = true
        }

        if (ELuptimer >= ELuptime) {
            ELon = false
        }
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        // light overlay or EL
        /*blendNormal(batch)
        if (ELon) {
            batch.draw(atlas.get(0, 2), 0f, 0f)
        }
        else {
            // backplate
            batch.draw(atlas.get(0, 0), 0f, 0f)
        }*/
        // disabling light button
        batch.color = drawCol
        batch.draw(atlas.get(0, 0), 0f, 0f)

        
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
        batch.draw(atlas.get(1, worldTime.calendarMonth - 1), 0f, 0f)


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