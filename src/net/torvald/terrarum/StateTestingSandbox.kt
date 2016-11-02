package net.torvald.terrarum


import com.jme3.math.FastMath
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

    val lightning_start = Point2d(50.0, 200.0)
    val lightning_end = Point2d(750.0, 200.0)

    val bolt = LightingBolt(lightning_start, lightning_end, 50)

    val noiseGen = TileableValueNoise(12, 0.5f, 128)

    override fun init(container: GameContainer?, game: StateBasedGame?) {
        noiseGen.generate(seed)
    }

    override fun update(container: GameContainer?, game: StateBasedGame?, delta: Int) {
    }

    override fun getID() = Terrarum.STATE_ID_TEST_SHIT


    private var regenTime = 17
    private var seed = 1L

    override fun render(container: GameContainer, game: StateBasedGame, g: Graphics) {
        g.color = Color.white
        g.lineWidth = 3f

        //g.drawLine(lightning_start, lightning_end)
        //bolt.draw(g)


        val amp = 60f
        val xoff = 10f
        val yoff = 300f

        for (x in noiseGen.width downTo 1) {
            val pStart = noiseGen[x] * amp + yoff
            val pEnd = noiseGen[x - 1] * amp + yoff
            val step = 6

            g.drawLine((noiseGen.width - x) * step + xoff, pStart,
                    (noiseGen.width - x +1) * step + xoff, pEnd)
        }

        g.color = Color.red
        g.lineWidth = 1f

        g.drawLine(xoff, yoff, xoff + noiseGen.width * 6, yoff)

    }

    override fun keyPressed(key: Int, c: Char) {
        if (c == ' ') noiseGen.generate(++seed)
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