package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.ActorHumanoid
import net.torvald.terrarum.gameactors.Second
import net.torvald.terrarum.gameactors.abs
import net.torvald.terrarum.imagefont.Watch7SegSmall
import net.torvald.terrarum.worlddrawer.LightmapRenderer
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2017-06-10.
 */
class UIBasicNotifier(private val player: ActorHumanoid?) : UICanvas() {
    override var width = 116
    override var height = 24
    override var handler: UIHandler? = null
    override var openCloseTime: Second = 0f

    private var ELuptimer = 10f // to make the light turned off by default
    private val ELuptime = 4f
    private var ELon = false

    private var atlas = TextureRegionPack(ModMgr.getPath("basegame", "gui/basic_meter_atlas.tga"), width, height)

    private var font = Watch7SegSmall

    override fun update(delta: Float) {
        if (ELon) {
            ELuptimer += delta
        }

        if (mouseUp || Gdx.input.isKeyPressed(Terrarum.getConfigInt("keyinteract"))) {
            ELuptimer = 0f
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

    private val lcdLitCol = Color(0x141414_ff)

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

    override fun render(batch: SpriteBatch) {
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
                              Terrarum.ingame!!.world.globalLight
                             )
            }
            else {
                lightLevel = Terrarum.ingame!!.world.globalLight
            }


            // backplate
            batch.color = Color(lightLevel.r, lightLevel.g, lightLevel.b, 1f)
            batch.draw(atlas.get(0, 0), 0f, 0f)
        }

        // LCD back
        blendNormal()
        batch.draw(atlas.get(0, 3), 0f, 0f)


        // LCD contents
        batch.color = lcdLitCol
        font.draw(batch, getTempStr(), 21f, 5f)
        font.draw(batch, getMailStr(), 93f, 5f)
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