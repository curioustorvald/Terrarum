package net.torvald.parametricsky

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.EMDASH
import net.torvald.colourutil.*
import net.torvald.parametricsky.datasets.DatasetCIEXYZ
import net.torvald.parametricsky.datasets.DatasetRGB
import net.torvald.parametricsky.datasets.DatasetSpectral
import net.torvald.terrarum.inUse
import net.torvald.terrarum.modulebasegame.worldgenerator.HALF_PI
import net.torvald.terrarum.modulebasegame.worldgenerator.TWO_PI
import java.awt.Dimension
import javax.swing.*
import kotlin.math.PI
import kotlin.math.pow


const val WIDTH = 1200
const val HEIGHT = 600

/**
 * Created by minjaesong on 2018-08-01.
 */
class Application : Game() {

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

    private lateinit var testTex: Texture

    var turbidity = 5.0
    var albedo = 0.1
    var elevation = 0.0
    var scalefactor = 1f

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
        genTexLoop(ArHosekSkyModel.arhosek_xyz_skymodelstate_alloc_init(turbidity, albedo, elevation))


        val tex = Texture(oneScreen)
        tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

        batch.inUse {
            batch.draw(tex, 0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat())
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

    val outTexWidth = 256
    val outTexHeight = 256

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


        for (y in 0 until oneScreen.height) {
            for (x in 0 until oneScreen.width) {
                val gamma = (x / oneScreen.width.toDouble()) * TWO_PI // 0deg..360deg
                val theta = (1.0 - (y / oneScreen.height.toDouble())) * HALF_PI // 90deg..0deg

                val xyz = CIEXYZ(
                        ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 0).toFloat().times(scalefactor / 10f),
                        ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 1).toFloat().times(scalefactor / 10f),
                        ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 2).toFloat().times(scalefactor / 10f)
                )
                val rgb = xyz.toRGB().toColor()
                rgb.a = 1f

                oneScreen.setColor(rgb)
                oneScreen.drawPixel(x, y)

                //println("x: ${xyz.X}, y: ${xyz.Y}, z: ${xyz.Z}")
            }

        }

        //System.exit(0)
    }

    /**
     * Generated texture is as if you took the panorama picture of sky: up 70deg to horizon, east-south-west;
     * with sun not moving (sun is at exact south, sun's height is adjustable)
     */
    /*private fun genTexLoop2(T: Double, theta_s: Double) {

        fun hazeFun(T: Double): Double {
            val T = T - 1
            if (T >= 10) return 1.0
            else return 2.0.pow(T).div(1024.0)
        }

        // loop thru gamma and theta
        for (y in 0..outTexDim) { // theta
            for (x in 0..outTexDim) { // gamma
                val theta = Math.toRadians(y * (90.0 / outTexDim.toDouble())) // of observer
                val gamma = Math.toRadians(x * (90.0 / outTexDim.toDouble())) // of observer

                val Y_z = Model.getAbsoluteZenithLuminance(T, theta_s)
                val x_z = Model.getZenithChromaX(T, theta_s)
                val y_z = Model.getZenithChromaY(T, theta_s)

                val Y_p = Y_z * Model.getFforLuma(theta, gamma, T) / Model.getFforLuma(0.0, theta_s, T)
                val Y_oc = Y_z * (1.0 + 2.0 * Math.cos(theta)) / 3.0
                val x_p = (x_z * Model.getFforChromaX(theta, gamma, T) / Model.getFforChromaX(0.0, theta_s, T)).coerceIn(0.0, 1.0)
                val y_p = (y_z * Model.getFforChromaY(theta, gamma, T) / Model.getFforChromaY(0.0, theta_s, T)).coerceIn(0.0, 1.0)

                val normalisedY = Y_p.toFloat().pow(0.5f).div(10f)
                val normalisedY_oc = Y_oc.toFloat().pow(0.5f).div(10f)

                //println("$Y_p -> $normalisedY, $x_p, $y_p")

                if (T < 11) {
                    val rgbColour = CIEYXY(normalisedY, x_p.toFloat(), y_p.toFloat()).toXYZ().toColorRaw()
                    val hazeColour = CIEYXY(normalisedY_oc, 0.3128f, 0.3290f).toXYZ().toColorRaw()

                    val hazeAmount = hazeFun(T).toFloat()
                    val newColour = Color(
                            FastMath.interpolateLinear(hazeAmount, rgbColour.r, hazeColour.r),
                            FastMath.interpolateLinear(hazeAmount, rgbColour.g, hazeColour.g),
                            FastMath.interpolateLinear(hazeAmount, rgbColour.b, hazeColour.b),
                            1f
                    )

                    oneScreen.setColor(newColour)
                    oneScreen.drawPixel(x, y)
                }
                else {
                    val hazeColour = CIEYXY(normalisedY_oc, 0.3128f, 0.3290f).toXYZ().toColorRaw()
                    oneScreen.setColor(hazeColour)
                    oneScreen.drawPixel(x, y)
                }
            }
        }
        // end loop
    }*/

    override fun create() {
        batch = SpriteBatch()
        testTex = Texture(Gdx.files.internal("assets/test_texture.tga"))

        oneScreen = Pixmap(outTexWidth * 2, outTexHeight, Pixmap.Format.RGBA8888)

        DatasetSpectral
        DatasetCIEXYZ
        DatasetRGB

        ApplicationController(this)
    }



    class ApplicationController(app: Application) : JFrame() {

        val mainPanel = JPanel()

        val turbidityControl = JSpinner(SpinnerNumberModel(5.0, 1.0, 10.0, 0.1))
        val albedoControl = JSpinner(SpinnerNumberModel(0.1, 0.0, 1.0, 0.05))
        val elevationControl = JSpinner(SpinnerNumberModel(0.0, 0.0, 90.0, 0.5))
        val scalefactorControl = JSpinner(SpinnerNumberModel(1.0, 0.0, 2.0, 0.01))

        init {
            val turbidityPanel = JPanel()
            val albedoPanel = JPanel()
            val elevationPanel = JPanel()
            val scalefactorPanel = JPanel()

            turbidityControl.preferredSize = Dimension(45, 18)
            albedoControl.preferredSize = Dimension(45, 18)
            elevationControl.preferredSize = Dimension(45, 18)
            scalefactorControl.preferredSize = Dimension(45, 18)

            turbidityPanel.add(JLabel("Turbidity"))
            turbidityPanel.add(turbidityControl)

            albedoPanel.add(JLabel("Albedo"))
            albedoPanel.add(albedoControl)

            elevationPanel.add(JLabel("Elevation"))
            elevationPanel.add(elevationControl)

            scalefactorPanel.add(JLabel("Scaling Factor"))
            scalefactorPanel.add(scalefactorControl)

            mainPanel.add(turbidityPanel)
            mainPanel.add(albedoPanel)
            mainPanel.add(elevationPanel)
            mainPanel.add(scalefactorPanel)

            this.isVisible = true
            this.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            this.size = Dimension(300, 400)

            this.add(mainPanel)


            turbidityControl.addChangeListener {
                app.turbidity = turbidityControl.value as Double
            }

            albedoControl.addChangeListener {
                app.albedo = albedoControl.value as Double
            }

            elevationControl.addChangeListener {
                app.elevation = Math.toRadians(elevationControl.value as Double)
            }

            scalefactorControl.addChangeListener {
                app.scalefactor = (scalefactorControl.value as Double).toFloat()
            }

        }

    }
}


fun main(args: Array<String>) {
    val config = LwjglApplicationConfiguration()
    config.width = WIDTH
    config.height = HEIGHT
    config.foregroundFPS = 0

    LwjglApplication(Application(), config)
}