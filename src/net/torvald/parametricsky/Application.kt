package net.torvald.parametricsky

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.unicode.EMDASH
import net.torvald.colourutil.*
import net.torvald.parametricsky.datasets.DatasetCIEXYZ
import net.torvald.parametricsky.datasets.DatasetRGB
import net.torvald.parametricsky.datasets.DatasetSpectral
import net.torvald.terrarum.abs
import net.torvald.terrarum.inUse
import net.torvald.terrarum.modulebasegame.worldgenerator.HALF_PI
import net.torvald.terrarum.modulebasegame.worldgenerator.TWO_PI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import javax.swing.*
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt


/**
 * Created by minjaesong on 2018-08-01.
 */
class Application(val WIDTH: Int, val HEIGHT: Int) : Game() {

    private val HW = WIDTH / 2
    private val HH = HEIGHT / 2

    private val wf = WIDTH.toFloat()
    private val hf = HEIGHT.toFloat()
    private val hwf = HW.toFloat()
//    private val hhf = HH.toFloat()

    /* Variables:
     * 1. Canvas Y (theta)
     * 2. Gamma (180deg - solar_azimuth; Canvas X)
     * 3. Solar angle (theta_s)
     * 4. Turbidity
     *
     * Sampling rate:
     *      theta in 0..90 total 32 entries // canvas
     *      gamma in 0..90 total 32 entries // canvas
     *      theta_s in 0..90 total 16 entries // time of the day
     *      turbidity in {1.5, 2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64} total 12 entries // weather of the day
     *
     *
     * out atlas dimension:
     *      X = (32 * 16) = 512
     *      Y = (32 * 12) = 384
     */


    private lateinit var oneScreen: Pixmap
    private lateinit var batch: SpriteBatch

    var turbidity = 5.0
    var albedo = 0.1
    var elevation = Math.toRadians(45.0)

    var solarBearing = Math.toRadians(90.0)
    var cameraHeading = Math.toRadians(90.0)

    override fun getScreen(): Screen {
        return super.getScreen()
    }

    override fun setScreen(screen: Screen?) {
        super.setScreen(screen)
    }

    override fun render() {
        Gdx.graphics.setTitle("Daylight Model $EMDASH F: ${Gdx.graphics.framesPerSecond}")

        if (turbidity <= 0) throw IllegalStateException()

        // we need to use different modelstate to accomodate different albedo for each spectral band but oh well...
        genTexLoop(ArHosekSkyModel.arhosek_xyz_skymodelstate_alloc_init(turbidity, albedo, elevation.abs()))


        val tex = Texture(oneScreen)
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

        batch.inUse {
            batch.draw(tex, hwf, 0f, hwf, hf)
            batch.draw(tex, hwf, 0f, -hwf, hf)

        }

        tex.dispose()
    }

    override fun pause() {
        super.pause()
    }

    override fun resume() {
        super.resume()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
    }

    override fun dispose() {
        oneScreen.dispose()
    }

    val outTexWidth = 64
    val outTexHeight = 64

    private fun Float.scaleFun() =
            (1f - 1f / 2f.pow(this/6f)) * 0.97f

    private fun Float.negativeElevationScale() =
        minOf(
            (1f - 1f / 2f.pow(this/6f)) * 0.97f,
            1f - (1f - 1f / 2f.pow(this/6f)) * 0.97f
        )

    private fun CIEXYZ.scaleToFit(elevation: Double): CIEXYZ {
        return if (elevation >= 0) {

            val xr = this.X / this.Y
            val zr = this.Z / this.Y
            val scale = this.Y.scaleFun() / this.Y


            CIEXYZ(
                this.X.scaleFun(),
                this.Y.scaleFun(),
                this.Z.scaleFun(),
                this.alpha
            )
        }
        else {
            val elevation1 = -Math.toDegrees(elevation)
            val elevation2 = -Math.toDegrees(elevation) / 28.5
            val scale = (1f - (1f - 1f / 1.8.pow(elevation1)) * 0.97f).toFloat()
            val scale2 = (1.0 - (elevation2.pow(E) / E.pow(elevation2))*0.8).toFloat()
            CIEXYZ(
                this.X.scaleFun() * scale * scale2,
                this.Y.scaleFun() * scale * scale2,
                this.Z.scaleFun() * scale * scale2,
                this.alpha
            )
        }
    }

    /**
     * Generated texture is as if you took the panorama picture of sky: up 70deg to horizon, east-south-west;
     * with sun not moving (sun is at exact south, sun's height is adjustable)
     */
    private fun genTexLoop(state: ArHosekSkyModelState) {

        fun normaliseY(y: Double): Float {
            var v = y.coerceAtLeast(0.0)

            if (v < 0) println("$y -> $v (should not be negative)")

            return v.toFloat()
        }

        val ys = ArrayList<Float>()

        for (y in 0 until oneScreen.height) {
            for (x in 0 until oneScreen.width) {
                val gamma = (x / oneScreen.width.toDouble()) * PI // bearing, where 0 is right at the sun
                val theta = (y / oneScreen.height.toDouble()) * HALF_PI // vertical angle, where 0 is zenith, ±90 is ground (which is odd)

                val xyz = CIEXYZ(
                        ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 0).toFloat(),
                        ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 1).toFloat(),
                        ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 2).toFloat()
                )
                ys.add(xyz.Y)
                val rgb = xyz.scaleToFit(elevation).toRGB().toColor()
                rgb.a = 1f

                val rgb2 = Color(
                    ((rgb.r * 255f).roundToInt() xor 0xAA) / 255f,
                    ((rgb.g * 255f).roundToInt() xor 0xAA) / 255f,
                    ((rgb.b * 255f).roundToInt() xor 0xAA) / 255f,
                    rgb.a
                )

                oneScreen.setColor(rgb)
                oneScreen.drawPixel(x, y)

                //println("x: ${xyz.X}, y: ${xyz.Y}, z: ${xyz.Z}")
            }

        }

        ymaxDisp.text = "${ys.max()}" // ~750.0
        ymaxDisp2.text = "${ys.maxOf { it.scaleFun() }}" // ~1.0

        //System.exit(0)
    }

    override fun create() {
        batch = SpriteBatch()

        oneScreen = Pixmap(outTexWidth, outTexHeight, Pixmap.Format.RGBA8888)

        DatasetSpectral
        DatasetCIEXYZ
        DatasetRGB

        ApplicationController(this)
    }

    val ymaxDisp = JTextField().also {
        it.preferredSize = Dimension(64, 20)
    }
    val ymaxDisp2 = JTextField().also {
        it.preferredSize = Dimension(64, 20)
    }

    class ApplicationController(val app: Application) : JFrame() {

        val dialSize = Dimension(45, 20)

        val turbidityControl = JSpinner(SpinnerNumberModel(5.0, 1.0, 10.0, 0.1)).also {
            it.preferredSize = dialSize
            it.addChangeListener { _ ->
                app.turbidity = it.value as Double
            }
        }
        val albedoControl = JSpinner(SpinnerNumberModel(0.1, 0.0, 1.0, 0.05)).also {
            it.preferredSize = dialSize
            it.addChangeListener { _ ->
                app.albedo = it.value as Double
            }
        }
        val elevationControl = JSpinner(SpinnerNumberModel(45.0, -75.0, 75.0, 0.5)).also {
            it.preferredSize = dialSize
            it.addChangeListener { _ ->
                app.elevation = Math.toRadians(it.value as Double)
            }
        }
        val solarBearing = JSpinner(SpinnerNumberModel(90.0, 0.0, 180.0, 1.0)).also {
            it.preferredSize = dialSize
            it.addChangeListener { _ ->
                app.solarBearing = (it.value as Double)
            }
        }
        val cameraHeading = JSpinner(SpinnerNumberModel(90.0, 0.0, 180.0, 1.0)).also {
            it.preferredSize = dialSize
            it.addChangeListener { _ ->
                app.cameraHeading = (it.value as Double)
            }
        }

        init {
            val atmosPanel = JPanel()
            val turbidityPanel = JPanel().also {
                it.add(JLabel("Turbidity"))
                it.add(turbidityControl)
                atmosPanel.add(it)
            }
            val albedoPanel = JPanel().also {
                it.add(JLabel("Albedo"))
                it.add(albedoControl)
                atmosPanel.add(it)
            }

            val sunPanel = JPanel()
            val elevationPanel = JPanel().also {
                it.add(JLabel("Elevation"))
                it.add(elevationControl)
                sunPanel.add(it)
            }
            val scalefactorPanel = JPanel().also {
                it.add(JLabel("Bearing"))
                it.add(solarBearing)
                sunPanel.add(it)
            }

            val cameraPanel = JPanel()
            val headingPanel = JPanel().also {
                it.add(JLabel("Heading"))
                it.add(cameraHeading)
                cameraPanel.add(it)
            }

            val statsPanel = JPanel()
            val ymaxPanel = JPanel().also {
                it.add(JLabel("Ymax (CIEXYZ)"))
                it.add(app.ymaxDisp)
                statsPanel.add(it)
            }
            val ymaxPanel2 = JPanel().also {
                it.add(JLabel("Ymax (scaled)"))
                it.add(app.ymaxDisp2)
                statsPanel.add(it)
            }

            val mainPanel = JPanel()

            mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)
            JPanel().also {
                it.layout = BorderLayout()
                it.add(JPanel().also { it.add(JLabel("Atmosphere")) }, BorderLayout.NORTH)
                it.add(atmosPanel, BorderLayout.CENTER)
                it.add(JSeparator(), BorderLayout.SOUTH)
                mainPanel.add(it)
            }
            JPanel().also {
                it.layout = BorderLayout()
                it.add(JPanel().also { it.add(JLabel("Sun")) }, BorderLayout.NORTH)
                it.add(sunPanel, BorderLayout.CENTER)
                it.add(JSeparator(), BorderLayout.SOUTH)
                mainPanel.add(it)
            }
            JPanel().also {
                it.layout = BorderLayout()
                it.add(JPanel().also { it.add(JLabel("Camera")) }, BorderLayout.NORTH)
                it.add(cameraPanel, BorderLayout.CENTER)
                it.add(JSeparator(), BorderLayout.SOUTH)
                mainPanel.add(it)
            }
            JPanel().also {
                it.layout = BorderLayout()
                it.add(JPanel().also { it.add(JLabel("Statistics")) }, BorderLayout.NORTH)
                it.add(statsPanel, BorderLayout.CENTER)
                mainPanel.add(it)
            }

            this.isVisible = true
            this.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            this.size = Dimension(300, 600)
            this.add(mainPanel, BorderLayout.CENTER)

        }

    }
}


fun main(args: Array<String>) {
    val config = Lwjgl3ApplicationConfiguration()

    val WIDTH = 1600
    val HEIGHT = 960

    config.setWindowedMode(WIDTH, HEIGHT)
    Lwjgl3Application(Application(WIDTH, HEIGHT), config)
}