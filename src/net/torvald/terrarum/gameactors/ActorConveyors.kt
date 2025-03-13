package net.torvald.terrarum.gameactors

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.worlddrawer.WorldCamera
import kotlin.math.*

/**
 * Created by minjaesong on 2025-03-10.
 */
class ActorConveyors : ActorWithBody {

    // make it savegame-reloadable
    private constructor() {
        axleX1 = -1
        axleY1 = -1
        axleX2 = -1
        axleY2 = -1
    }

    val axleX1: Int // can be negative when the conveyor crosses the world border
    val axleY1: Int
    val axleX2: Int // always positive
    val axleY2: Int

    /**
     * xy1 is always the starting point, and the starting point's x-value is always lower than the end points',
     * even when the conveyor crosses the edge of the world border, in which case the x-value is negative.
     */
    constructor(x1: Int, y1: Int, x2: Int, y2: Int) {
        axleX1 = x1
        axleY1 = y1
        axleX2 = x2
        axleY2 = y2
    }

    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        val inclination = atan2(axleY2.toDouble() - axleY1, axleX2.toDouble() - axleX1)
        val declination = atan2(axleY1.toDouble() - axleY2, axleX1.toDouble() - axleX2)

        val wxSpinCntrStart = (axleX1 + 0.5) * TILE_SIZED
        val wySpinCntrStart = (axleY1 + 0.5) * TILE_SIZED
        val wxSpinCntrEnd = (axleX2 + 0.5) * TILE_SIZED
        val wySpinCntrEnd = (axleY2 + 0.5) * TILE_SIZED

        val r = 0.5 * TILE_SIZED.minus(2)

        val wxBeltTopStart = wxSpinCntrStart + r * sin(inclination)
        val wyBeltTopStart = wySpinCntrStart + r * cos(inclination)
        val wxBeltTopEnd = wxSpinCntrEnd + r * sin(inclination)
        val wyBeltTopEnd = wySpinCntrEnd + r * cos(inclination)

        val wxBeltBtmStart = wxSpinCntrStart + r * sin(declination)
        val wyBeltBtmStart = wySpinCntrStart + r * cos(declination)
        val wxBeltBtmEnd = wxSpinCntrEnd + r * sin(declination)
        val wyBeltBtmEnd = wySpinCntrEnd + r * cos(declination)


        val segmentCount = max(1.0, (6 * cbrt(r).toFloat() * (180.0 / 360.0f)).toInt().toDouble())

        // belt top
        drawLineOnWorld(wxBeltTopStart, wyBeltTopStart, wxBeltTopEnd, wyBeltTopEnd)
        // belt bottom
        drawLineOnWorld(wxBeltBtmStart, wyBeltBtmStart, wxBeltBtmEnd, wyBeltBtmEnd)
        // left arc
        drawArcOnWorld(wxSpinCntrStart, wySpinCntrStart, r, declination, 180.0)
        // right arc
        drawArcOnWorld(wxSpinCntrEnd, wySpinCntrEnd, r, inclination, 180.0)
    }

    private fun drawLineOnWorld(x1: Double, y1: Double, x2: Double, y2: Double) {
        val w = 2.0f
        App.shapeRender.rectLine(
            x1.toFloat() - WorldCamera.x, y1.toFloat() - WorldCamera.y,
            x2.toFloat() - WorldCamera.x, y2.toFloat() - WorldCamera.y,
            w
        )
    }

    private fun drawArcOnWorld(xc: Double, yc: Double, r: Double, arcStart: Double, arcDegrees: Double) {
        val w = 2.0f
        App.shapeRender.arc(
            xc.toFloat() - WorldCamera.x,
            yc.toFloat() - WorldCamera.y,
            r.toFloat(), arcStart.toFloat(), arcDegrees.toFloat()
        )
    }
}