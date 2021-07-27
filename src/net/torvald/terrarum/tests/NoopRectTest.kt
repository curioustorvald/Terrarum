package net.torvald.terrarum.tests

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.EMDASH
import net.torvald.random.HQRNG
import net.torvald.terrarum.Point2i
import net.torvald.terrarum.inUse
import net.torvald.terrarum.worlddrawer.toRGBA

val RECTTEST_WIDTH = 768
val RECTTEST_HEIGHT = 768

/**
 * Created by minjaesong on 2019-02-04.
 */
class NoopRectTest(val appConfig: Lwjgl3ApplicationConfiguration) : Game() {

    private val SIZE = 100

    private val map = Array(SIZE + 2) { Array(SIZE + 2) { Color(0) } }
    private lateinit var pixmap: Pixmap
    private lateinit var texture: Texture

    private lateinit var batch: SpriteBatch
    private lateinit var camera: OrthographicCamera

    private val DECAY_CONST = 16f/256f
    private val DECAY_CONST2 = DECAY_CONST * 1.41421356f

    val rng = HQRNG()

    private val lightSources = Array<Point2i>(6) {
        val rx = rng.nextInt(SIZE) + 1
        val ry = rng.nextInt(SIZE) + 1

        Point2i(rx, ry)
    }

    override fun create() {
        pixmap = Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888)
        pixmap.blending = Pixmap.Blending.None
        texture = Texture(1, 1, Pixmap.Format.RGBA8888)

        batch = SpriteBatch()
        camera = OrthographicCamera(RECTTEST_WIDTH.toFloat(), RECTTEST_HEIGHT.toFloat())
        camera.setToOrtho(true)
    }

    override fun render() {
        // clear
        for (y in 0 until SIZE + 2) {
            for (x in 0 until SIZE + 2) {
                map[y][x] = Color(0)
            }
        }

        // set light sources
        lightSources.forEach {
            map[it.y][it.x] = Color(-1)
        }

        // Round 2
        for (y in SIZE - 1 downTo 0) {
            for (x in 0 until SIZE) {
                calculateAndAssign(x + 1, y + 1, 1)
            }
        }
        // Round 3
        for (y in SIZE - 1 downTo 0) {
            for (x in SIZE - 1 downTo 0) {
                calculateAndAssign(x + 1, y + 1, 2)
            }
        }
        // Round 4
        for (y in 0 until SIZE) {
            for (x in SIZE - 1 downTo 0) {
                calculateAndAssign(x + 1, y + 1, 3)
            }
        }
        // Round 1
        for (y in 0 until SIZE) {
            for (x in 0 until SIZE) {
                calculateAndAssign(x + 1, y + 1, 4)
            }
        }

        // make image
        for (y in 0 until SIZE + 2) {
            for (x in 0 until SIZE + 2) {
                pixmap.drawPixel(x, y, map[y][x].toRGBA())
            }
        }

        // draw image
        texture.dispose()
        texture = Texture(pixmap)

        Gdx.graphics.setTitle("No-op Rectangle Test $EMDASH F: ${Gdx.graphics.framesPerSecond}")

        batch.inUse {
            batch.projectionMatrix = camera.combined
            batch.draw(texture, 0f, 0f, RECTTEST_WIDTH.toFloat(), RECTTEST_HEIGHT.toFloat())
        }

        println()
    }


    private fun calculateAndAssign(x: Int, y: Int, pass: Int) {
        val lightLevelThis = map[y][x]

        lightLevelThis.maxAndAssign(map[y-1][x-1].darken2())
        lightLevelThis.maxAndAssign(map[y-1][x+1].darken2())
        lightLevelThis.maxAndAssign(map[y+1][x-1].darken2())
        lightLevelThis.maxAndAssign(map[y+1][x+1].darken2())

        lightLevelThis.maxAndAssign(map[y-1][x  ].darken())
        lightLevelThis.maxAndAssign(map[y+1][x  ].darken())
        lightLevelThis.maxAndAssign(map[y  ][x-1].darken())
        lightLevelThis.maxAndAssign(map[y  ][x+1].darken())

        if (map[y][x] == lightLevelThis) {
            markDupes(x, y, pass)
        }

        map[y][x] = lightLevelThis
    }

    private fun markDupes(x: Int, y: Int, pass: Int) {
        //println("Duplicate at:\t$x\t$y\t$pass")
    }

    private fun Color.darken(): Color {
        return Color(
                this.r * (1f - DECAY_CONST),
                this.g * (1f - DECAY_CONST),
                this.b * (1f - DECAY_CONST),
                this.a * (1f - DECAY_CONST)
        )
    }

    private fun Color.darken2(): Color {
        return Color(
                this.r * (1f - DECAY_CONST2),
                this.g * (1f - DECAY_CONST2),
                this.b * (1f - DECAY_CONST2),
                this.a * (1f - DECAY_CONST2)
        )
    }

    private fun Color.maxAndAssign(other: Color): Color {
        this.set(
                if (this.r > other.r) this.r else other.r,
                if (this.g > other.g) this.g else other.g,
                if (this.b > other.b) this.b else other.b,
                if (this.a > other.a) this.a else other.a
        )

        return this
    }

}

fun main(args: Array<String>) {
    val appConfig = Lwjgl3ApplicationConfiguration()
    appConfig.setWindowedMode(RECTTEST_WIDTH, RECTTEST_HEIGHT)

    Lwjgl3Application(NoopRectTest(appConfig), appConfig)
}