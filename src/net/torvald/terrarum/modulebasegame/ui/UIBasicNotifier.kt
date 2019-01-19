package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.modulebasegame.Ingame
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.modulebasegame.imagefont.Watch7SegSmall
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2017-06-10.
 */
class UIBasicNotifier(private val player: ActorHumanoid?) : UICanvas() {
    override var width = 116
    override var height = 24
    override var openCloseTime: Second = 0f

    private var ELuptimer = 10f // to make the light turned off by default
    private val ELuptime = 4f
    private var ELon = false

    private var atlas = TextureRegionPack(ModMgr.getPath("basegame", "gui/basic_meter_atlas.tga"), width, height)

    private var font = Watch7SegSmall

    private val TEMP_AMERICAN = -1
    private val TEMP_KELVIN = 0
    private val TEMP_CELCIUS = 1

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

    private val temperature: Int
        get() {
            if (player != null) {
                val playerTilePos = player.hIntTilewiseHitbox
                val tempCelsius = -273f + ((Terrarum.ingame as? Ingame)?.world?.getTemperature(playerTilePos.centeredX.toInt(), playerTilePos.centeredY.toInt()) ?: 288f)

                return when (AppLoader.getConfigInt("temperatureunit")) {
                    TEMP_KELVIN -> tempCelsius.times(1.8f).plus(32f).roundInt()
                    TEMP_CELCIUS -> tempCelsius.roundInt()
                    else -> tempCelsius.plus(273.15f).roundInt()
                }
            }
            else {
                return 888
            }
        }
    private val mailCount: Int
        get() = 0

    private val drawCol = Color(1f,1f,1f,0.5f)
    private val lcdLitColELoff = Color(0x141414_aa)
    private val lcdLitColELon = Color(0x141414_ff)

    private val lcdLitCol: Color
        get() = if (ELon) lcdLitColELon else lcdLitColELoff

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

        if (AppLoader.getConfigInt("temperatureunit") == 1) {
            sb.append('"') // celsius superscript
        }
        else if (AppLoader.getConfigInt("temperatureunit") == -1) {
            sb.append('#') // fahrenheit subscript
        }
        else {
            sb.append(' ') // display nothing for kelvin
        }

        return sb.toString()
    }

    fun getMailStr(): String {
        val sb = StringBuilder()

        if (mailCount < 10)
            sb.append(" ")

        sb.append(mailCount)

        return sb.toString()
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        // light overlay or EL
        if (ELon) {
            blendNormal(batch)
            batch.draw(atlas.get(0, 2), 0f, 0f)
        }
        else {
            // backplate
            batch.color = drawCol
            batch.draw(atlas.get(0, 0), 0f, 0f)
        }

        // LCD back
        blendNormal(batch)
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