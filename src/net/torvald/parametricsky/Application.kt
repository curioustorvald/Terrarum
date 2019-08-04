package net.torvald.parametricsky

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.EMDASH
import net.torvald.colourutil.CIEXYZUtil.toColorRaw
import net.torvald.colourutil.CIEXYZUtil.toXYZ
import net.torvald.colourutil.CIEYXY
import net.torvald.terrarum.inUse
import java.awt.Dimension
import javax.swing.*


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
    //var thetaOfSun = 0.0

    override fun getScreen(): Screen {
        return super.getScreen()
    }

    override fun setScreen(screen: Screen?) {
        super.setScreen(screen)
    }

    override fun render() {
        Gdx.graphics.setTitle("Daylight Model $EMDASH F: ${Gdx.graphics.framesPerSecond}")

        genTexLoop(turbidity)


        val tex = Texture(oneScreen)
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)

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

    val outTexWidth = 32
    val outTexHeight = 16

    /**
     * Generated texture is as if you took the panorama picture of sky: up 70deg to horizon, east-south-west;
     * with sun not moving (sun is at exact south, sun's height is adjustable)
     */
    private fun genTexLoop(T: Double) {

        fun normaliseY(y: Double): Float {
            var v = y.coerceAtLeast(0.0)

            if (v < 0) println("$y -> $v (should not be negative)")

            return v.toFloat()
        }

        val theta = Math.toRadians(45.0) // of observer

        // loop DAY
        for (x in 0 until outTexWidth) { // theta_s (time of day)
            for (y in 0 until outTexHeight) { // gamma
                val theta_s = Math.toRadians(x * (90.0 / outTexWidth.toDouble()))
                val gamma = Math.toRadians((outTexHeight - y) * (90.0 / outTexHeight.toDouble())) // of observer

                val Y_z = Model.getAbsoluteZenithLuminance(T, theta_s).coerceAtLeast(0.0) / 88.0
                val x_z = Model.getZenithChromaX(T, theta_s)
                val y_z = Model.getZenithChromaY(T, theta_s)

                val Y_p = Y_z * Model.getFforLuma(theta, gamma, T) / Model.getFforLuma(0.0, theta_s, T)
                val Y_oc = Y_z * (1.0 + 2.0 * Math.cos(theta)) / 3.0
                val x_p = (x_z * Model.getFforChromaX(theta, gamma, T) / Model.getFforChromaX(0.0, theta_s, T)).coerceIn(0.0, 1.0)
                val y_p = (y_z * Model.getFforChromaY(theta, gamma, T) / Model.getFforChromaY(0.0, theta_s, T)).coerceIn(0.0, 1.0)

                val normalisedY = normaliseY(Y_p)

                //println("$Y_p -> $normalisedY, $x_p, $y_p")

                val rgbColour = CIEYXY(normalisedY, x_p.toFloat(), y_p.toFloat()).toXYZ().toColorRaw()

                oneScreen.setColor(rgbColour)
                oneScreen.drawPixel(x, y)
            }
        }
        // end loop DAY

        // loop NIGHT
        for (x in outTexWidth until outTexWidth * 2) {
            for (y in 0 until outTexHeight) {
                val theta_s = Math.toRadians(90.0 - (x - outTexWidth) * (90.0 / outTexWidth.toDouble())) // 90 downTo 0
                val theta_sReal = Math.toRadians(120.0)
                val gamma = Math.toRadians((outTexHeight - y) * (90.0 / outTexHeight.toDouble())) // of observer

                val Y_z = Model.getAbsoluteZenithLuminance(T, theta_sReal)
                val x_z = Model.getZenithChromaX(T, theta_s)
                val y_z = Model.getZenithChromaY(T, theta_s)

                val Y_p = Y_z * Model.getFforLuma(theta, gamma, T) / Model.getFforLuma(0.0, theta_sReal, T)
                val Y_oc = Y_z * (1.0 + 2.0 * Math.cos(theta)) / 3.0
                val x_p = (x_z * Model.getFforChromaX(theta, gamma, T) / Model.getFforChromaX(0.0, theta_s, T)).coerceIn(0.0, 1.0)
                val y_p = (y_z * Model.getFforChromaY(theta, gamma, T) / Model.getFforChromaY(0.0, theta_s, T)).coerceIn(0.0, 1.0)

                val normalisedY = normaliseY(Y_p)

                //println("$Y_p -> $normalisedY, $x_p, $y_p")

                val rgbColour = CIEYXY(normalisedY, x_p.toFloat(), y_p.toFloat()).toXYZ().toColorRaw()

                oneScreen.setColor(rgbColour)
                oneScreen.drawPixel(x, y)
            }
        }
        // end loop NIGHT
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


        ApplicationController(this)
    }



    class ApplicationController(app: Application) : JFrame() {

        val mainPanel = JPanel()

        val turbidityControl = JSlider(2, 64, 5)
        //val theta_sControl = JSlider(0, 15, 0)

        val turbidityValueDisp = JLabel()
        //val theta_sValueDisp = JLabel()

        //val theta_sValue: Double
        //    get() = theta_sControl.value * (90.0 / theta_sControl.maximum)

        init {
            val turbidityPanel = JPanel()
            val theta_sPanel = JPanel()

            turbidityPanel.add(JLabel("Turbidity"))
            turbidityPanel.add(turbidityControl)
            turbidityPanel.add(turbidityValueDisp)

            turbidityValueDisp.text = turbidityControl.value.toString()
            //theta_sValueDisp.text = theta_sValue.toString()

            //theta_sPanel.add(JLabel("Theta_s"))
            //theta_sPanel.add(theta_sControl)
            //theta_sPanel.add(theta_sValueDisp)

            mainPanel.add(turbidityPanel)
            mainPanel.add(theta_sPanel)

            this.isVisible = true
            this.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            this.size = Dimension(300, 400)

            this.add(mainPanel)


            turbidityControl.addChangeListener {
                turbidityValueDisp.text = turbidityControl.value.toString()
                app.turbidity = turbidityControl.value.toDouble()
            }

            //theta_sControl.addChangeListener {
            //    theta_sValueDisp.text = theta_sValue.toString()
            //    app.thetaOfSun = Math.toRadians(theta_sValue)
            //}

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