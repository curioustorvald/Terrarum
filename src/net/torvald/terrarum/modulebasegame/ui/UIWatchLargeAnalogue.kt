package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.jme3.math.FastMath
import net.torvald.terrarum.*
import net.torvald.terrarum.gameworld.WorldTime
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Created by minjaesong on 2023-09-09.
 */
class UIWatchLargeAnalogue() : UICanvas() {

    override var width = 76
    override var height = 76
    override var openCloseTime: Second = 0f

    private val atlas = TextureRegionPack(ModMgr.getGdxFile("basegame", "gui/watchface_analogue_atlas.tga"), 16, 11)

    private val faceBack1 = TextureRegion(atlas.texture, 48, 121, 76, 33)
    private val faceBack2 = TextureRegion(atlas.texture, 48, 154, 76, 44)
    private val faceFore = TextureRegion(atlas.texture, 48, 44, 76, 76)
    private val moon = TextureRegion(atlas.texture, 80, 0, 18, 18)
    private val dialPin = TextureRegion(atlas.texture, 80, 22, 2, 2)

    private val dayNumbers = Array(30) { atlas.get(it / 11, it % 11) } // key: worldTime.calendarDay - 1 (0..29)
    private val weekTexts = Array(8) { atlas.get(3 + it/4, it % 4) } // key: worldTime.dayOfWeek (0..7)

    private val minuteHandCol = Toolkit.Theme.COL_MOUSE_UP
    private val hourHandCol = Toolkit.Theme.COL_SELECTED
    private val seasonHandCol = Color(0xec468aff.toInt())

    private val moondialOffX = 38f
    private val moondialOffY = 38f

    private val seasonHandOffX = 53f
    private val seasonHandOffY = 43f

    private val TWO_PI = 2.0 * Math.PI

    override fun renderUI(batch: SpriteBatch, camera: OrthographicCamera) {
        TerrarumIngame.setCameraPosition(batch, App.shapeRender, camera, posX.toFloat(), posY.toFloat())

        val day = INGAME.world.worldTime.calendarDay - 1
        val week = INGAME.world.worldTime.dayOfWeek

        val moonAngle = INGAME.world.worldTime.moonPhase * Math.PI
        val moonX = -(15.0 * cos(moonAngle)).roundToInt().toFloat() + moondialOffX - 9
        val moonY = -(15.0 * sin(moonAngle)).roundToInt().toFloat() + moondialOffY - 9

        val seasonAngle = ((INGAME.world.worldTime.TIME_T % WorldTime.YEAR_SECONDS) / (WorldTime.YEAR_SECONDS.toDouble()) - 0.125) * TWO_PI
        val seasonX = -(7.0 * cos(seasonAngle)).toFloat() + seasonHandOffX
        val seasonY = -(7.0 * sin(seasonAngle)).toFloat() + seasonHandOffY

        val minAngle = ((INGAME.world.worldTime.TIME_T % WorldTime.HOUR_SEC) / WorldTime.HOUR_SEC.toDouble() + 0.25) * TWO_PI
        val minX = -(28.0 * cos(minAngle)).toFloat() + moondialOffX
        val minY = -(28.0 * sin(minAngle)).toFloat() + moondialOffY

        val hourAngle = ((INGAME.world.worldTime.TIME_T % (WorldTime.HOUR_SEC * 12)) / (WorldTime.HOUR_SEC * 12).toDouble() + 0.25) * TWO_PI
        val hourX = -(21.0 * cos(hourAngle)).toFloat() + moondialOffX
        val hourY = -(21.0 * sin(hourAngle)).toFloat() + moondialOffY

        batch.draw(faceBack1, 0f, 11f)
        batch.draw(moon, moonX, moonY)
        batch.draw(faceBack2, 0f, 33f)
        batch.draw(dayNumbers[day], 16f, 38f)
        batch.draw(weekTexts[week], 30f, 55f)
        batch.draw(faceFore, 0f, 0f)

        batch.end()

        // draw hands
        App.shapeRender.inUse {
            it.color = seasonHandCol
            it.rectLine(seasonHandOffX, seasonHandOffY, seasonX, seasonY, 1.5f)
        }
        batch.inUse {
            batch.draw(dialPin, seasonHandOffX - 1, seasonHandOffY - 1)
        }

        App.shapeRender.inUse {
            it.color = hourHandCol
            it.rectLine(moondialOffX, moondialOffY, hourX, hourY, 2.5f)
            it.color = minuteHandCol
            it.rectLine(moondialOffX, moondialOffY, minX, minY, 2f)
        }

        batch.begin()

        batch.draw(dialPin, moondialOffX - 1, moondialOffY - 1)
    }

    override fun updateUI(delta: Float) {
    }

    override fun dispose() {
        atlas.dispose()
    }


}