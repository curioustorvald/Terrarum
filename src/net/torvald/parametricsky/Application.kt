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
import net.torvald.terrarum.abs
import net.torvald.terrarum.clut.Skybox
import net.torvald.terrarum.clut.Skybox.coerceInSmoothly
import net.torvald.terrarum.clut.Skybox.mapCircle
import net.torvald.terrarum.inUse
import net.torvald.terrarum.modulebasegame.worldgenerator.HALF_PI
import java.awt.BorderLayout
import java.awt.Dimension
import java.lang.Math.pow
import javax.swing.*
import kotlin.math.*


val INITIAL_TURBIDITY = 4.0
val INITIAL_ALBEDO = 0.1
val INITIAL_ELEV = 0.0


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

    var turbidity = INITIAL_TURBIDITY
    var albedo = INITIAL_ALBEDO
    var elevation = Math.toRadians(INITIAL_ELEV)

    var solarBearing = Math.toRadians(90.0)
    var cameraHeading = Math.toRadians(90.0)

    override fun getScreen(): Screen {
        return super.getScreen()
    }

    override fun setScreen(screen: Screen?) {
        super.setScreen(screen)
    }

    var model = ArHosekSkyModel.arhosek_xyz_skymodelstate_alloc_init(turbidity, albedo, elevation.abs())

    fun regenerateModel() {
        model = ArHosekSkyModel.arhosek_xyz_skymodelstate_alloc_init(turbidity, albedo, elevation.abs())
    }

    override fun render() {
        Gdx.graphics.setTitle("Daylight Model $EMDASH F: ${Gdx.graphics.framesPerSecond}")

        if (turbidity <= 0) throw IllegalStateException()

        // we need to use different model-state to accommodate different albedo for each spectral band but oh well...
        genTexLoop(model, elevation)
//        println("$elevation\t${ymaxDisp.text}\t${ymaxDisp2.text}")


        /*for (elev in -75..75) {
            val elevation = Math.toRadians(elev.toDouble())
            val model = ArHosekSkyModel.arhosek_xyz_skymodelstate_alloc_init(turbidity, albedo, elevation.abs())
            genTexLoop(model, elevation)
            println("$elev\t${ymaxDisp.text}\t${ymaxDisp2.text}")
        }*/


        val tex = Texture(oneScreen)
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

        batch.inUse {
//            batch.draw(tex, hwf, 0f, hwf, hf)
//            batch.draw(tex, hwf, 0f, -hwf, hf)

            batch.draw(tex, 0f, 0f, wf, hf)
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

    val outTexWidth = 1
    val outTexHeight = 128

    private fun Float.scaleFun() =
            (1f - 1f / 2f.pow(this/6f)) * 0.97f

    private fun CIEXYZ.scaleToFit(elevation: Double): CIEXYZ {
        return if (elevation >= 0) {
            CIEXYZ(
                this.X.scaleFun(),
                this.Y.scaleFun(),
                this.Z.scaleFun(),
                this.alpha
            )
        }
        else {
            // maths model: https://www.desmos.com/calculator/cwi7iyzygg

            val x = -Math.toDegrees(elevation).toFloat()
//            val elevation2 = -Math.toDegrees(elevation) / 28.5
            val p = 3.5f
            val q = 7.5f
            val s = -0.2f
            val f = (1f - (1f - 1f / 1.8f.pow(x)) * 0.97f).toFloat()
//            val g = (1.0 - (elevation2.pow(E) / E.pow(elevation2))*0.8).toFloat()
            val h = ((x / q).pow(p) + 1f).pow(s)
            CIEXYZ(
                this.X.scaleFun() * f * h,
                this.Y.scaleFun() * f * h,
                this.Z.scaleFun() * f * h,
                this.alpha
            )
        }
    }

    private fun Double.mapCircle() = sin(HALF_PI * this)

    /**
     * Generated texture is as if you took the panorama picture of sky: up 70deg to horizon, east-south-west;
     * with sun not moving (sun is at exact south, sun's height is adjustable)
     */
    private fun genTexLoop(state: ArHosekSkyModelState, elevation: Double) {

        fun normaliseY(y: Double): Float {
            var v = y.coerceAtLeast(0.0)

            if (v < 0) println("$y -> $v (should not be negative)")

            return v.toFloat()
        }

        val ys = ArrayList<Float>()
        val ys2 = ArrayList<Float>()

        val halfHeight = oneScreen.height * 0.5
        val elevationDeg = Math.toDegrees(elevation)

        for (x in 0 until oneScreen.width) {
            for (y in 0 until oneScreen.height) {

                // sky-sphere mapping
                /*val xf = ((x + 0.5) / oneScreen.width) * 2.0 - 1.0
                val yf = ((y + 0.5) / oneScreen.height) * 2.0 - 1.0
                val gamma = atan2(yf, xf) + PI
                val theta = sqrt(xf*xf + yf*yf) * HALF_PI*/

                // AM-PM mapping (use with WIDTH=1)
                val yp = y % (oneScreen.height / 2)
                val yi = yp - 3
                val xf = -elevationDeg / 90.0
                var yf = (yi / 58.0).coerceIn(0.0, 1.0).mapCircle().coerceInSmoothly(0.0, 0.95)
                if (elevationDeg < 0) yf *= Skybox.superellipsoidDecay(1.0 / 3.0, xf)
                val theta = yf * HALF_PI
                val gamma = if (y < halfHeight) HALF_PI else 3 * HALF_PI


                val xyz = CIEXYZ(
                        ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 0).toFloat(),
                        ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 1).toFloat(),
                        ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 2).toFloat(),
                )
                val xyz2 = xyz.scaleToFit(elevation)
                ys.add(xyz.Y)
                ys2.add(xyz2.Y)
                val rgb = xyz2.toRGB().toColor()
                rgb.a = 1f

                /*val rgb2 = Color(
                    ((rgb.r * 255f).roundToInt() xor 0xAA) / 255f,
                    ((rgb.g * 255f).roundToInt() xor 0xAA) / 255f,
                    ((rgb.b * 255f).roundToInt() xor 0xAA) / 255f,
                    rgb.a
                )*/

                oneScreen.setColor(rgb)
                oneScreen.drawPixel(x, y)

                //println("x: ${xyz.X}, y: ${xyz.Y}, z: ${xyz.Z}")
            }

        }

        ymaxDisp.text = "${ys.max()}"
        ymaxDisp2.text = "${ys2.max()}"

        //System.exit(0)
    }

    override fun create() {
        batch = SpriteBatch()

        oneScreen = Pixmap(outTexWidth, outTexHeight, Pixmap.Format.RGBA8888)

//        DatasetSpectral
        DatasetCIEXYZ
//        DatasetRGB

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

        val turbidityControl = JSpinner(SpinnerNumberModel(INITIAL_TURBIDITY, 1.0, 10.0, 0.1)).also {
            it.preferredSize = dialSize
            it.addChangeListener { _ ->
                app.turbidity = it.value as Double
                app.regenerateModel()
            }
        }
        val albedoControl = JSpinner(SpinnerNumberModel(INITIAL_ALBEDO, 0.0, 1.0, 0.05)).also {
            it.preferredSize = dialSize
            it.addChangeListener { _ ->
                app.albedo = it.value as Double
                app.regenerateModel()
            }
        }
        val elevationControl = JSpinner(SpinnerNumberModel(INITIAL_ELEV, -75.0, 75.0, 0.5)).also {
            it.preferredSize = dialSize
            it.addChangeListener { _ ->
                app.elevation = Math.toRadians(it.value as Double)
                app.regenerateModel()
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
                it.add(JLabel("Turbidity (log_2)"))
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

    val WIDTH = 2048
    val HEIGHT = 2048

    config.setWindowedMode(WIDTH, HEIGHT)
    Lwjgl3Application(Application(WIDTH, HEIGHT), config)
}