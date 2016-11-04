package net.torvald.terrarum


import com.jme3.math.FastMath
import net.torvald.colourutil.ColourTemp
import net.torvald.point.Point2d
import net.torvald.random.HQRNG
import net.torvald.random.TileableValueNoise
import net.torvald.terrarum.gameactors.floorInt
import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.virtualcomputer.terminal.ALException
import org.apache.commons.csv.CSVRecord
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.util.*
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

/**
 * Created by minjaesong on 16-09-05.
 */
class StateTestingSandbox : BasicGameState() {

    val lightning_start = Point2d(50.0, 100.0)
    val lightning_end = Point2d(750.0, 300.0)

    val bolt = LightingBolt(lightning_start, lightning_end, 50)


    override fun init(container: GameContainer?, game: StateBasedGame?) {
        reseed(genOnly = true)
    }

    override fun update(container: GameContainer?, game: StateBasedGame?, delta: Int) {
        /*timer += delta
        if (timer > regenTime) {
            timer -= regenTime
            reseed()
        }*/
    }

    override fun getID() = Terrarum.STATE_ID_TEST_SHIT


    private var timer = 0
    private var regenTime = 17

    private var seed = System.nanoTime()

    val samples = 128

    val lightningXgen = TileableValueNoise(8, 0.67f, samples, 8)
    val lightningYgen = TileableValueNoise(8, 0.58f, samples, 4)

    override fun render(container: GameContainer, game: StateBasedGame, g: Graphics) {
        g.color = ColourTemp(7500)
        g.lineWidth = 3f

        //g.drawLine(lightning_start, lightning_end)
        //bolt.draw(g)

        // TODO rotational transformmation for the points
        // (newX, newY) = (x cos(theta) - y sin(theta), x sin(theta) + y cos(theta))


        val ampY = 40f
        val ampX = 6
        val xoff = 10f
        val yoff = 300f

        for (x in 0..lightningYgen.width - 1) {
            val pXstart = (x     + lightningXgen[x    ]) * ampX + xoff
            val pXend =   (x + 1 + lightningXgen[x + 1]) * ampX + xoff
            val pYstart = lightningYgen[x    ] * ampY + yoff
            val pYend =   lightningYgen[x + 1] * ampY + yoff

            g.drawLine(pXstart, pYstart, pXend, pYend)
        }

        g.color = Color.red
        g.lineWidth = 1f

        g.drawLine(xoff, yoff, xoff + lightningYgen.width * ampX, yoff)

    }

    override fun keyPressed(key: Int, c: Char) {
        if (c == ' ') reseed()
    }

    private fun reseed(genOnly: Boolean = false) {
        if (!genOnly) seed = System.nanoTime()
        lightningXgen.generate(0x51621DL xor seed)
        lightningYgen.generate(seed)
    }
}

fun Graphics.drawLine(p1: Point2d, p2: Point2d) {
    drawLine(p1.x.toFloat(), p1.y.toFloat(), p2.x.toFloat(), p2.y.toFloat())
}

class LightingBolt(val start: Point2d, val end: Point2d, val segments: Int) {
    val mainBolt = LinkedList<Point2d>() //Pair<Length, Y-Pos>

    val boltYDev = 20.0

    init {
        val length = start.length(end)

        for (i in 0..segments - 1) {
            mainBolt.add(
                    Point2d(
                            start.x + length / segments * i,
                            start.y + HQRNG().nextFloat().times(2.0).minus(1.0).times(boltYDev)
                    )
            )
        }
    }

    fun draw(g: Graphics) {
        for (i in 0..segments - 1) {
            val startpoint = mainBolt[i]
            val endpoint = if (i == segments - 1) end else mainBolt[i + 1]

            g.drawLine(startpoint, endpoint)
        }
    }
}