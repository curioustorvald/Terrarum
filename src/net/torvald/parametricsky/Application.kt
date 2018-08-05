package net.torvald.parametricsky

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Affine2
import net.torvald.colourutil.CIEYXY
import net.torvald.colourutil.CIEXYZUtil.toXYZ
import net.torvald.colourutil.CIEXYZUtil.toColorRaw
import net.torvald.colourutil.CIEXYZUtil.toColor
import net.torvald.terrarum.inUse
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import kotlin.math.pow


const val WIDTH = 720
const val HEIGHT = 720

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
     *      theta in 70 downTo 0 step 10   (8 entries)   // canvas
     *      gamma in -90..90 step 12  (16 entries)  // canvas
     *      theta_s in 0..70 step 10 (8 entries)   // time of the day
     *      turbidity in {1.5, 2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64} (12 entries) // weather of the day
     */


    private lateinit var oneScreen: Pixmap
    private lateinit var batch: SpriteBatch

    private lateinit var testTex: Texture

    var turbidity = 7.0
    var thetaOfSun = 0.0

    override fun getScreen(): Screen {
        return super.getScreen()
    }

    override fun setScreen(screen: Screen?) {
        super.setScreen(screen)
    }

    override fun render() {
        Gdx.graphics.setTitle("Daylight Model — F: ${Gdx.graphics.framesPerSecond}")

        genTexLoop(turbidity, thetaOfSun)


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

    /**
     * Generated texture is as if you took the panorama picture of sky: up 70deg to horizon, east-south-west;
     * with sun not moving (sun is at exact south, sun's height is adjustable)
     */
    private fun genTexLoop(T: Double, theta_s: Double) {
        // loop thru gamma and theta
        for (y in 0..90) { // theta
            for (x in 0..90) { // gamma
                val theta = Math.toRadians(y.toDouble()) // of observer
                val gamma = Math.toRadians(90 - x.toDouble()) // of observer

                val Y_z = Model.getAbsoluteZenithLuminance(T, theta_s)
                val x_z = Model.getZenithChromaX(T, theta_s)
                val y_z = Model.getZenithChromaY(T, theta_s)

                val Y_p = Y_z * Model.getFforLuma(theta, gamma, T) / Model.getFforLuma(0.0, theta_s, T)
                val x_p = (x_z * Model.getFforChromaX(theta, gamma, T) / Model.getFforChromaX(0.0, theta_s, T)).coerceIn(0.0, 1.0)
                val y_p = (y_z * Model.getFforChromaY(theta, gamma, T) / Model.getFforChromaY(0.0, theta_s, T)).coerceIn(0.0, 1.0)

                val normalisedY = Y_p.toFloat().pow(0.5f).div(10f)

                //println("$Y_p -> $normalisedY, $x_p, $y_p")

                val rgbColour = CIEYXY(normalisedY, x_p.toFloat(), y_p.toFloat()).toXYZ().toColor()
                //val rgbColour = CIEYXY(normalisedY, 0.3128f, 0.3290f).toXYZ().toColorRaw()

                oneScreen.setColor(rgbColour)
                oneScreen.drawPixel(x, y)
            }
        }
        // end loop
    }

    override fun create() {
        batch = SpriteBatch()
        testTex = Texture(Gdx.files.internal("assets/test_texture.tga"))

        oneScreen = Pixmap(90, 90, Pixmap.Format.RGBA8888)


        ApplicationController(this)
    }



    class ApplicationController(app: Application) : JFrame() {

        val mainPanel = JPanel()

        val turbidityControl = JSlider(2, 25, 7)
        val theta_sControl = JSlider(0, 85, 0)

        val turbidityValueDisp = JLabel()
        val theta_sValueDisp = JLabel()

        init {
            val turbidityPanel = JPanel()
            val theta_sPanel = JPanel()

            turbidityPanel.add(JLabel("Turbidity"))
            turbidityPanel.add(turbidityControl)
            turbidityPanel.add(turbidityValueDisp)

            turbidityValueDisp.text = turbidityControl.value.toString()
            theta_sValueDisp.text = theta_sControl.value.toString()

            theta_sPanel.add(JLabel("Theta_s"))
            theta_sPanel.add(theta_sControl)
            theta_sPanel.add(theta_sValueDisp)

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

            theta_sControl.addChangeListener {
                theta_sValueDisp.text = theta_sControl.value.toString()
                app.thetaOfSun = Math.toRadians(theta_sControl.value.toDouble())
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