package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.modulebasegame.worldgenerator.HALF_PI
import net.torvald.terrarum.modulebasegame.worldgenerator.TWO_PI
import org.lwjgl.opengl.GL11
import java.util.*
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
        l = 0.0
        c = 0

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

    private var s: Double // belt length (total)
    private var l: Double // belt length (single straight part)
    private var c: Int // total segment counts

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

    private var turn: Double = 0.0 // TODO decide if the max should be 1.0 or TWO_PI. For a numerical precision, 1.0 is recommended though

    companion object {
        private val COL_BELT = Color(0x444444ff)
        private val COL_BELT_TOP = Color(0x666666ff)

        private val COL_BELT_ALT = Color.MAGENTA//Color(0x888888ff.toInt())
        private val COL_BELT_TOP_ALT = Color(0xccccccff.toInt())
    }

    @Transient private var shapeRender = App.makeShapeRenderer()

    override fun reload() {
        super.reload()
        shapeRender = App.makeShapeRenderer()
        s = calcBeltLength(x1, y1, x2, y2)
        l = calcBeltLengthStraightPart(x1, y1, x2, y2)
        c = (s / 32).roundToInt() * 2 // 16px segments rounded towards nearest even number
    }

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
        l = calcBeltLengthStraightPart(x1, y1, x2, y2)

        di = atan2(this.y2.toDouble() - this.y1, this.x1.toDouble() - this.x2)
        dd = atan2(this.y1.toDouble() - this.y2, this.x2.toDouble() - this.x1)

        cx1 = (this.x1 + 0.5) * TILE_SIZED
        cy1 = (this.y1 + 0.5) * TILE_SIZED
        cx2 = (this.x2 + 0.5) * TILE_SIZED
        cy2 = (this.y2 + 0.5) * TILE_SIZED

        r = 0.5 * TILE_SIZED.minus(2)
        c = (s / 4).roundToInt() * 2 // 4px segments rounded towards nearest even number

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
        2 * (calcBeltLengthStraightPart(x1, y1, x2, y2) + Math.PI * TILE_SIZED / 2)

    private fun calcBeltLengthStraightPart(x1: Int, y1: Int, x2: Int, y2: Int) =
        hypot((x2 - x1) * TILE_SIZED, (y2 - y1) * TILE_SIZED)

    override fun updateImpl(delta: Float) {
        super.updateImpl(delta)

        turn += delta / 1
        while (turn >= 1.0) turn -= 1.0

//        turn = 0.0
    }

    @Transient var smu = 0

    override var tooltipText: String? = ""

    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        batch.end()

        shapeRender.projectionMatrix = batch.projectionMatrix
            Gdx.gl.glEnable(GL11.GL_LINE_SMOOTH)
        shapeRender.inUse {

            it.color = COL_BELT

            // belt top
            drawLineOnWorld(btx1, bty1, btx2, bty2, 2f)
            // belt bottom
            drawLineOnWorld(bbx1, bby1, bbx2, bby2, 2f)
            // left arc
            drawArcOnWorld(cx1, cy1, r, dd, -Math.PI, 2f)
            // right arc
            drawArcOnWorld(cx2, cy2, r, di, -Math.PI, 2f)

            // draw belt stripes under
            it.color = COL_BELT_ALT
            val segmentLen = s / c
            var segmentsUsed = 0
            for (i in 0 until c / 2) { // not exact code but whatever
                //= originPoint + segmentLen * (translation terms) * (movement on the belt terms)
                val m = segmentLen * (i + 0.00 + turn)
                val n = segmentLen * (i + 0.25 + turn)
                val x1 = btx1 + m * cos(dd)
                val y1 = bty1 - m * sin(dd)
                val x2 = btx1 + n * cos(dd)
                val y2 = bty1 - n * sin(dd)

                if (x1 in btx1..btx2) {
                    if (bty2 > bty1) {
                        drawLineOnWorld(
                            x1.coerceIn(btx1..btx2),
                            y1.coerceIn(bty1..bty2),
                            x2.coerceIn(btx1..btx2),
                            y2.coerceIn(bty1..bty2),
                            2f
                        )
                    }
                    else {
                        drawLineOnWorld(
                            x1.coerceIn(btx1..btx2),
                            y1.coerceIn(bty2..bty1),
                            x2.coerceIn(btx1..btx2),
                            y2.coerceIn(bty2..bty1),
                            2f
                        )
                    }
                    segmentsUsed++
                }
            }
            smu = segmentsUsed
            // stripes at the right spindle
            // eq: k units/s on straight part == (k / r) rad/s on curve

            val lSegCnt = l / segmentLen
            val cSegCnt = (c / 2.0) - lSegCnt
            val cSegOffset = (cSegCnt fmod 1.0) * segmentLen // [pixels]
            val turnOffset = cSegOffset / r
            tooltipText = "di=$di, dd=$dd\nsegLen=$segmentLen\ntotalSegCnt=$c\nlSegCnt=$lSegCnt\ncSegCnt=$cSegCnt\ncSegOffset=$cSegOffset\nturnOffset=$turnOffset\nr=$r"
            for (i in 0 until 3) {
//                it.color = listOf(Color.LIME, Color.CORAL, Color.CYAN)[i]

                val arcStart = di - turnOffset - segmentLen * (-i + turn) / r // use `di` as the baseline
                val arcSize = -(segmentLen * 0.25) / r
                val arcEnd = arcStart + arcSize
                val arcRange = di-Math.PI..di

                // if the arc overlaps the larger arc...
                if (arcStart in arcRange || arcEnd in arcRange)
                    drawArcOnWorld(cx2, cy2, r,
                        arcStart.coerceIn(arcRange), // this doesn't work due to sign flipping
                        arcSize.coerceIn(arcRange),
                        2f
                    )
            }

            // bottom straight part
            it.color = COL_BELT_ALT
            /*val bottomSectionOffset = (l + Math.PI * r) % segmentLen
            for (i in 0 until c / 2) { // not exact code but whatever
                val x1 = bbx1 + bottomSectionOffset + r * sin(di - HALF_PI) * (i + turn - 0.00) * segmentLen
                val y1 = bby1 + bottomSectionOffset + r * cos(di - HALF_PI) * (i + turn - 0.00) * segmentLen - 1
                val x2 = bbx1 + bottomSectionOffset + r * sin(di - HALF_PI) * (i + turn - 0.25) * segmentLen
                val y2 = bby1 + bottomSectionOffset + r * cos(di - HALF_PI) * (i + turn - 0.25) * segmentLen - 1

                if (x1 in bbx1 - 4..bbx2 + 4) {
                    if (bby2 > bby1)
                        drawLineOnWorld(
                            x1.coerceIn(bbx1..bbx2),
                            y1.coerceIn(bby1..bby2),
                            x2.coerceIn(bbx1..bbx2),
                            y2.coerceIn(bby1..bby2),
                            2f
                        )
                    else
                        drawLineOnWorld(
                            x1.coerceIn(bbx1..bbx2),
                            y1.coerceIn(bby2..bby1),
                            x2.coerceIn(bbx1..bbx2),
                            y2.coerceIn(bby2..bby1),
                            2f
                        )
                }
            }*/



            it.color = COL_BELT_TOP
            // belt top
//            drawLineOnWorld(btx1, bty1 - 0.5f, btx2, bty2 - 0.5f, 1f)
            // belt bottom
//            drawLineOnWorld(bbx1, bby1 - 0.5f, bbx2, bby2 - 0.5f, 1f)
        }

        batch.begin()
    }

    private fun drawLineOnWorld(x1: Double, y1: Double, x2: Double, y2: Double, width: Float) {
        shapeRender.rectLine(
            x1.toFloat(), y1.toFloat(),
            x2.toFloat(), y2.toFloat(),
            width
        )
    }

    private fun drawArcOnWorld(xc: Double, yc: Double, r: Double, arcStart: Double, arcDeg: Double, width: Float) {
        // dissect the circle
        //// estimated number of segments. pathLen divided by sqrt(2)
        val segments = (4 * Math.cbrt(r) * arcDeg.absoluteValue).toInt().coerceAtLeast(1)

        for (i in 0 until segments) {
            val degStart = (i.toDouble() / segments) * arcDeg + arcStart
            val degEnd = ((i + 1.0) / segments) * arcDeg + arcStart

            val x1 = r * sin(degStart) + xc
            val y1 = r * cos(degStart) + yc
            val x2 = r * sin(degEnd) + xc
            val y2 = r * cos(degEnd) + yc

            drawLineOnWorld(x1, y1, x2, y2, width)
        }
    }

    /** Real time, in nanoseconds */
    @Transient var spawnRequestedTime: Long = 0L
        protected set

    internal var actorThatInstalledThisFixture: UUID? = null

    open fun spawn(installerUUID: UUID?): Boolean {
        this.isVisible = true

        val posXtl = minOf(x1, x2).toDouble()
        val posYtl = minOf(y1, y2).toDouble()
        val posXbr = maxOf(x1, x2).toDouble()
        val posYbr = maxOf(y1, y2).toDouble()

        this.hitbox.setFromTwoPoints(posXtl * TILE_SIZED, posYtl * TILE_SIZED, (posXbr+1) * TILE_SIZED, (posYbr+1) * TILE_SIZED)
        this.setHitboxDimension(this.hitbox.width.toInt(), this.hitbox.height.toInt(), 0, 1)
        this.intTilewiseHitbox.setFromTwoPoints(posXtl, posYtl, posXbr, posYbr)

        // actually add this actor into the world
        INGAME.queueActorAddition(this)
        spawnRequestedTime = System.nanoTime()

        actorThatInstalledThisFixture = installerUUID

        //makeNoiseAndDust(posXtl, posYtl)

        onSpawn()

        return true
    }

    /**
     * Callend whenever the fixture was spawned successfully.
     */
    open fun onSpawn() {}

    override fun dispose() {
        shapeRender.tryDispose()
    }
}