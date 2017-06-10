package net.torvald.terrarum.ui

import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.ActorHumanoid
import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.gameworld.WorldTime
import net.torvald.terrarum.worlddrawer.LightmapRenderer
import net.torvald.terrarum.worlddrawer.LightmapRenderer.normaliseToColour
import org.newdawn.slick.*

/**
 * Created by minjaesong on 2017-06-11.
 */
class UITierOneWatch(private val player: ActorHumanoid?) : UICanvas {
    override var width = 85
    override var height = 52
    override var handler: UIHandler? = null
    override var openCloseTime: Millisec = 0

    private var ELuptimer = 9999 // to make the light turned off by default
    private val ELuptime = 5000
    private var ELon = false

    private var atlas = SpriteSheet(ModMgr.getPath("basegame", "gui/watchface_atlas.tga"), width, height)

    private var littleFont = SpriteSheetFont(SpriteSheet(ModMgr.getPath("basegame", "fonts/7seg_small.tga"), 9, 12), ' ')
    private var timeFont = SpriteSheetFont(SpriteSheet(ModMgr.getPath("basegame", "fonts/7segnum.tga"), 11, 18), '/')
    private var textFont = SpriteSheetFont(SpriteSheet(ModMgr.getPath("basegame", "fonts/watch_dotalph.tga"), 12, 10), '@')
    private var moonDial = SpriteSheet(ModMgr.getPath("basegame", "fonts/watch_17pxmoondial.tga"), 17, 17)
    private var moonDialCount = moonDial.horizontalCount

    private val lcdLitCol = Color(20,20,20)

    private val worldTime: WorldTime
        get() = Terrarum.ingame!!.world.time


    override fun update(gc: GameContainer, delta: Int) {
        if (ELon) {
            ELuptimer += delta
        }

        if (mouseUp || gc.input.isKeyDown(Terrarum.getConfigInt("keyinteract"))) {
            ELuptimer = 0
            ELon = true
        }

        if (ELuptimer >= ELuptime) {
            ELon = false
        }
    }


    override fun render(gc: GameContainer, g: Graphics) {
        atlas.startUse()

        // backplate
        g.drawImage(atlas.getSubImage(0, 0), 0f, 0f)

        // because what the fuck
        blendScreen()
        g.drawImage(atlas.getSubImage(0, 1), 0f, 0f, Color(12, 12, 12))

        // light overlay or EL
        if (ELon) {
            blendNormal()
            g.drawImage(atlas.getSubImage(0, 2), 0f, 0f)
        }
        else {
            var lightLevel = Color.black

            if (player != null) {
                val playerPos = player.tilewiseHitbox
                lightLevel = (LightmapRenderer.getLight(playerPos.canonicalX.toInt(), playerPos.canonicalY.toInt()) ?:
                              Terrarum.ingame!!.world.globalLight
                             ).normaliseToColour()
            }
            else {
                lightLevel = Terrarum.ingame!!.world.globalLight.normaliseToColour()
            }
            blendMul()
            g.drawImage(atlas.getSubImage(0, 1), 0f, 0f, lightLevel)
        }

        // LCD back
        blendNormal()
        g.drawImage(atlas.getSubImage(0, 3), 0f, 0f)

        atlas.endUse()


        // day name
        g.color = lcdLitCol
        g.font = textFont
        g.drawString(worldTime.getDayNameShort().toUpperCase(), 7f, 7f)

        // month
        g.font = littleFont
        g.drawString(worldTime.months.toString().padStart(2, ' '), 40f, 6f)
        // day
        g.drawString(worldTime.days.toString().padStart(2, ' '), 62f, 6f)

        // hour
        g.font = timeFont
        g.drawString(worldTime.hours.toString().padStart(2, '/'), 30f, 28f)
        // minute
        g.drawString(worldTime.minutes.toString().padStart(2, '0'), 58f, 28f)


        // moon dial
        val moonPhase = (worldTime.moonPhase * moonDialCount).roundInt() % moonDialCount
        g.drawImage(moonDial.getSubImage(moonPhase, 0), 4f, 22f, lcdLitCol)
    }

    override fun processInput(gc: GameContainer, delta: Int, input: Input) {
    }

    override fun doOpening(gc: GameContainer, delta: Int) {
    }

    override fun doClosing(gc: GameContainer, delta: Int) {
    }

    override fun endOpening(gc: GameContainer, delta: Int) {
    }

    override fun endClosing(gc: GameContainer, delta: Int) {
    }
}