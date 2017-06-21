package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.ActorHumanoid
import net.torvald.terrarum.gameactors.Second
import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.gameworld.WorldTime
import net.torvald.terrarum.imagefont.Watch7SegMain
import net.torvald.terrarum.imagefont.Watch7SegSmall
import net.torvald.terrarum.imagefont.WatchDotAlph
import net.torvald.terrarum.worlddrawer.LightmapRenderer
import net.torvald.terrarum.worlddrawer.LightmapRenderer.normaliseToColour
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2017-06-11.
 */
class UITierOneWatch(private val player: ActorHumanoid?) : UICanvas {
    override var width = 85
    override var height = 52
    override var handler: UIHandler? = null
    override var openCloseTime: Second = 0f

    private var ELuptimer = 10f // to make the light turned off by default
    private val ELuptime = 4f
    private var ELon = false

    private var atlas = TextureRegionPack(ModMgr.getPath("basegame", "gui/watchface_atlas.tga"), width, height)

    private var littleFont = Watch7SegSmall
    private var timeFont = Watch7SegMain
    private var textFont = WatchDotAlph
    private var moonDial = TextureRegionPack(ModMgr.getPath("basegame", "fonts/watch_17pxmoondial.tga"), 17, 17)
    private var moonDialCount = moonDial.horizontalCount

    private val lcdLitCol = Color(0x141414_ff)

    private val worldTime: WorldTime
        get() = TerrarumGDX.ingame!!.world.time


    override fun update(delta: Float) {
        if (ELon) {
            ELuptimer += delta
        }

        if (mouseUp || Gdx.input.isKeyPressed(TerrarumGDX.getConfigInt("keyinteract"))) {
            ELuptimer = 0f
            ELon = true
        }

        if (ELuptimer >= ELuptime) {
            ELon = false
        }
    }

    override fun render(batch: SpriteBatch) {
        // backplate
        batch.draw(atlas.get(0, 0), 0f, 0f)

        // because what the fuck (rendered darker than what it supposed to be)
        /*blendScreen()
        batch.color = Color(0x0c0c0c_ff)
        batch.draw(atlas.get(0, 1), 0f, 0f)*/

        // light overlay or EL
        if (ELon) {
            blendNormal()
            batch.draw(atlas.get(0, 2), 0f, 0f)
        }
        else {
            val lightLevel: Color

            if (player != null) {
                val playerPos = player.tilewiseHitbox
                lightLevel = (LightmapRenderer.getLight(playerPos.centeredX.toInt(), playerPos.centeredY.toInt()) ?:
                              TerrarumGDX.ingame!!.world.globalLight
                             ).normaliseToColour()
            }
            else {
                lightLevel = TerrarumGDX.ingame!!.world.globalLight.normaliseToColour()
            }
            blendMul()
            batch.color = lightLevel
            batch.draw(atlas.get(0, 1), 0f, 0f)
        }

        // LCD back
        blendNormal()
        batch.draw(atlas.get(0, 3), 0f, 0f)


        
        // day name
        batch.color = lcdLitCol
        textFont.draw(batch, worldTime.getDayNameShort().toUpperCase(), 7f, 7f)

        // month
        littleFont.draw(batch, worldTime.months.toString().padStart(2, ' '), 40f, 6f)
        // day
        littleFont.draw(batch, worldTime.days.toString().padStart(2, ' '), 62f, 6f)

        // hour
        timeFont.draw(batch, worldTime.hours.toString().padStart(2, '/'), 30f, 28f)
        // minute
        timeFont.draw(batch, worldTime.minutes.toString().padStart(2, '0'), 58f, 28f)


        // moon dial
        val moonPhase = (worldTime.moonPhase * moonDialCount).roundInt() % moonDialCount
        batch.color = lcdLitCol
        batch.draw(moonDial.get(moonPhase, 0), 4f, 22f)
    }

    override fun processInput(delta: Float) {
    }

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
    }
}