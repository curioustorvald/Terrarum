package net.torvald.terrarum.ui

import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.ActorHumanoid
import net.torvald.terrarum.gameactors.abs
import net.torvald.terrarum.worlddrawer.LightmapRenderer
import net.torvald.terrarum.worlddrawer.LightmapRenderer.normaliseToColour
import org.newdawn.slick.*

/**
 * Created by minjaesong on 2017-06-10.
 */
class UIBasicNotifier(private val player: ActorHumanoid?) : UICanvas {
    override var width = 116
    override var height = 24
    override var handler: UIHandler? = null
    override var openCloseTime: Millisec = 0

    private var ELuptimer = 9999 // to make the light turned off by default
    private val ELuptime = 5000
    private var ELon = false

    private var atlas = SpriteSheet(ModMgr.getPath("basegame", "gui/basic_meter_atlas.tga"), width, height)

    private var font = SpriteSheetFont(SpriteSheet(ModMgr.getPath("basegame", "fonts/7seg_small.tga"), 9, 12), ' ')

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


    private val temperature: Int
        get() = -2
    private val mailCount: Int
        get() = 0

    private val lcdLitCol = Color(20,20,20)

    fun getTempStr(): String {
        val sb = StringBuilder()

        if (temperature < 100) {
            if (temperature < 0)
                sb.append("-")
            else
                sb.append(" ")

            if (temperature.abs() < 10)
                sb.append(" ")
        }

        sb.append(temperature.abs())

        sb.append('"') // celsius superscript

        return sb.toString()
    }

    fun getMailStr(): String {
        val sb = StringBuilder()

        if (mailCount < 10)
            sb.append(" ")

        sb.append(mailCount)

        return sb.toString()
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


        // LCD contents
        g.color = lcdLitCol
        g.font = font
        g.drawString(getTempStr(), 21f, 5f)
        g.drawString(getMailStr(), 93f, 5f)
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