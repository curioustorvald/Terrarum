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
        x1 = -1
        y1 = -1
        x2 = -1
        y2 = -1

        s = 0.0

        di = 0.0
        dd = 0.0

        cx1 = 0.0
        cy1 = 0.0
        cx2 = 0.0
        cy2 = 0.0

        r = 0.5 * TILE_SIZED.minus(2)

        btx1 = 0.0
        bty1 = 0.0
        btx2 = 0.0
        bty2 = 0.0

        bbx1 = 0.0
        bby1 = 0.0
        bbx2 = 0.0
        bby2 = 0.0
    }

    val x1: Int // can be negative when the conveyor crosses the world border
    val y1: Int
    val x2: Int // always positive
    val y2: Int

    private val s: Double // belt length

    private val di: Double // inclination deg
    private val dd: Double // declination deg

    private val cx1: Double // centre of the left spindle
    private val cy1: Double
    private val cx2: Double // centre of the right spindle
    private val cy2: Double

    private val r: Double // radius

    private val btx1: Double // line points of the top belt
    private val bty1: Double
    private val btx2: Double
    private val bty2: Double

    private val bbx1: Double // line points of the bottom bolt
    private val bby1: Double
    private val bbx2: Double
    private val bby2: Double

    /**
     * xy1 is always the starting point, and the starting point's x-value is always lower than the end points',
     * even when the conveyor crosses the edge of the world border, in which case the x-value is negative.
     */
    constructor(x1: Int, y1: Int, x2: Int, y2: Int) {
        this.x1 = x1
        this.y1 = y1
        this.x2 = x2
        this.y2 = y2

        s = calcBeltLength(x1, y1, x2, y2)

        di = atan2(this.y2.toDouble() - this.y1, this.x2.toDouble() - this.x1)
        dd = atan2(this.y1.toDouble() - this.y2, this.x1.toDouble() - this.x2)

        cx1 = (this.x1 + 0.5) * TILE_SIZED
        cy1 = (this.y1 + 0.5) * TILE_SIZED
        cx2 = (this.x2 + 0.5) * TILE_SIZED
        cy2 = (this.y2 + 0.5) * TILE_SIZED

        r = 0.5 * TILE_SIZED.minus(2)

        btx1 = cx1 + r * sin(di)
        bty1 = cy1 + r * cos(di)
        btx2 = cx2 + r * sin(di)
        bty2 = cy2 + r * cos(di)

        bbx1 = cx1 + r * sin(dd)
        bby1 = cy1 + r * cos(dd)
        bbx2 = cx2 + r * sin(dd)
        bby2 = cy2 + r * cos(dd)
    }

    private fun calcBeltLength(x1: Int, y1: Int, x2: Int, y2: Int) =
        2 * (hypot((x2 - x1) * TILE_SIZED, (y2 - y1) * TILE_SIZED) + Math.PI * TILE_SIZED / 2)


    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {


        // belt top
        drawLineOnWorld(btx1, bty1, btx2, bty2)
        // belt bottom
        drawLineOnWorld(bbx1, bby1, bbx2, bby2)
        // left arc
        drawArcOnWorld(cx1, cy1, r, dd, Math.PI)
        // right arc
        drawArcOnWorld(cx2, cy2, r, di, Math.PI)
    }

    private fun drawLineOnWorld(x1: Double, y1: Double, x2: Double, y2: Double) {
        val w = 2.0f
        App.shapeRender.rectLine(
            x1.toFloat() - WorldCamera.x, y1.toFloat() - WorldCamera.y,
            x2.toFloat() - WorldCamera.x, y2.toFloat() - WorldCamera.y,
            w
        )
    }

    private fun drawArcOnWorld(xc: Double, yc: Double, r: Double, arcStart: Double, arcDeg: Double) {
        val w = 2.0f
        // dissect the circle
        val pathLen = arcDeg * r
        //// estimated number of segments. pathLen divided by sqrt(2)
        val segments = Math.round(pathLen / Double.fromBits(0x3FF6A09E667F3BCDL)).coerceAtLeast(1L).toInt()

        for (i in 0 until segments) {
            val degStart = (i.toDouble() / segments) * arcDeg + arcStart
            val degEnd = ((i + 1.0) / segments) * arcDeg + arcStart

            val x1 = r * sin(degStart) + xc
            val y1 = r * cos(degStart) + yc
            val x2 = r * sin(degEnd) + xc
            val y2 = r * cos(degEnd) + yc

            drawLineOnWorld(x1, y1, x2, y2)
        }
    }
}