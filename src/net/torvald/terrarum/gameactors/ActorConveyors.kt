package net.torvald.terrarum.gameactors

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

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

        val r = 0.5 * TILE_SIZED

        val wxBeltTopStart = wxSpinCntrStart + r * sin(inclination)
        val wyBeltTopStart = wySpinCntrStart + r * cos(inclination)
        val wxBeltTopEnd = wxSpinCntrEnd + r * sin(inclination)
        val wyBeltTopEnd = wySpinCntrEnd + r * cos(inclination)

        val wxBeltBtmStart = wxSpinCntrStart + r * sin(declination)
        val wyBeltBtmStart = wySpinCntrStart + r * cos(declination)
        val wxBeltBtmEnd = wxSpinCntrEnd + r * sin(declination)
        val wyBeltBtmEnd = wySpinCntrEnd + r * cos(declination)


        // belt top
        drawLineAtWorld(wxBeltTopStart, wyBeltTopStart, wxBeltTopEnd, wyBeltTopEnd)
        // belt bottom
        drawLineAtWorld(wxBeltBtmStart, wyBeltBtmStart, wxBeltBtmEnd, wyBeltBtmEnd)
        // left arc

        // right arc



    }
}