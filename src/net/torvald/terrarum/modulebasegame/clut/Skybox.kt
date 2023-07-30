package net.torvald.terrarum.modulebasegame.clut

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Disposable
import com.jme3.math.FastMath
import net.torvald.colourutil.CIEXYZ
import net.torvald.colourutil.toColor
import net.torvald.colourutil.toRGB
import net.torvald.parametricsky.ArHosekSkyModel
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.abs
import net.torvald.terrarum.modulebasegame.worldgenerator.HALF_PI
import kotlin.math.*

/**
 * Created by minjaesong on 2023-07-09.
 */
object Skybox : Disposable {

    const val gradSize = 64

    private val gradTexBinLowAlbedo: Array<Texture>
    private val gradTexBinHighAlbedo: Array<Texture>

    operator fun get(elevationDeg: Double, turbidity: Double, highAlbedo: Boolean = false): Texture {
//        if (elevationDeg !in elevationsD) {
//            throw IllegalArgumentException("Elevation not in ±75° (got $elevationDeg)")
//        }
//        if (turbidity !in turbiditiesD) {
//            throw IllegalArgumentException("Turbidity not in 1..10 (got $turbidity)")
//        }

        val elev = elevationDeg.coerceIn(elevationsD).toInt() - elevations.first
        val turb = ((turbidity.coerceIn(turbiditiesD) - turbiditiesD.start) / (turbidities.step / 10.0)).toInt()

//        printdbg(this, "$elevationDeg $turbidity ; $elev $turb")

        return (if (highAlbedo) gradTexBinHighAlbedo else gradTexBinLowAlbedo)[elev * turbCnt + turb]
    }

    private fun Float.scaleFun() =
        (1f - 1f / 2f.pow(this/6f)) * 0.97f

    private fun CIEXYZ.scaleToFit(elevationDeg: Double): CIEXYZ {
        return if (elevationDeg >= 0) {
            CIEXYZ(
                this.X.scaleFun(),
                this.Y.scaleFun(),
                this.Z.scaleFun(),
                this.alpha
            )
        }
        else {
            val deg1 = (-elevationDeg / 75.0).pow(0.93).times(-75.0)
            val elevation1 = -deg1
            val elevation2 = -deg1 / 28.5
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

    private val elevations = (-75..75) //zw 151
    private val elevationsD = (elevations.first.toDouble() .. elevations.last.toDouble())
    private val turbidityStep = 5
    private val turbidities = (1_0..10_0 step turbidityStep) // (100 / turbidityStep) - 1
    private val turbiditiesD = (turbidities.first / 10.0..turbidities.last / 10.0)
    private val elevCnt = elevations.count()
    private val turbCnt = turbidities.count()
    private val albedoLow = 0.1
    private val albedoHight = 0.8 // for theoretical "winter wonderland"?
    private val gamma = HALF_PI

    private fun Double.mapCircle() = sin(HALF_PI * this)

    init {
        printdbg(this, "Initialising skybox model")

        gradTexBinLowAlbedo = getTexturmaps(albedoLow)
        gradTexBinHighAlbedo = getTexturmaps(albedoHight)

        App.disposables.add(this)

        printdbg(this, "Skybox model generated!")
    }

    /**
     * See https://www.desmos.com/calculator/lcvvsju3p1 for mathematical definition
     * @param p decay point. 0.0..1.0
     * @param q polynomial degree. 2+. Larger value means sharper transition around the point p
     * @param x the 'x' value of the function, as in `y=f(x)`. 0.0..1.0
     */
    private fun polynomialDecay(p: Double, q: Int, x: Double): Double {
        val sign = if (q % 2 == 1) -1 else 1
        val a1 = -1.0 / p
        val a2 = 1.0 / (1.0 - p)
        val q = q.toDouble()
        return if (x < p)
            sign * a1.pow(q - 1.0) * x.pow(q) + 1.0
        else
            sign * a2.pow(q - 1.0) * (x - 1.0).pow(q)
    }

    private fun polynomialDecay2(p: Double, q: Int, x: Double): Double {
        val sign = if (q % 2 == 1) 1 else -1
        val a1 = -1.0 / p
        val a2 = 1.0 / (1.0 - p)
        val q = q.toDouble()
        return if (x < p)
            sign * a1.pow(q - 1.0) * x.pow(q)
        else
            sign * a2.pow(q - 1.0) * (x - 1.0).pow(q) + 1.0
    }

    private fun superellipsoidDecay(p: Double, x: Double): Double {
        return 1.0 - (1.0 - (1.0 - x).pow(1.0 / p)).pow(p)
    }

    private fun Double.coerceInSmoothly(low: Double, high: Double): Double {
        val x = this.coerceIn(low, high)
        val x2 = ((x - low) * (high - low).pow(-1.0))
//        return FastMath.interpolateLinear(polynomialDecay2(0.5, 2, x2), low, high)
        return FastMath.interpolateLinear(smoothLinear(0.2, x2), low, high)
    }

    /**
     * To get the idea what the fuck is going on here, please refer to https://www.desmos.com/calculator/snqglcu2wl
     */
    private fun smoothLinear(p: Double, x0: Double): Double {
        val x = x0 - 0.5
        val t = 0.5 * sqrt(1.0 - 2.0 * p)
        val y0 = if (x < -t)
            (1.0 / p) * (x + 0.5).pow(2) - 0.5
        else if (x > t)
            -(1.0 / p) * (x - 0.5).pow(2) + 0.5
        else
            x * 2.0 / (1.0 + sqrt(1.0 - 2.0 * p))

        return y0 + 0.5
    }

    private fun getTexturmaps(albedo: Double): Array<Texture> {
        return Array(elevCnt * turbCnt) {

            val elevationDeg = (it / turbCnt).plus(elevations.first).toDouble()
            val elevationRad = Math.toRadians(elevationDeg)
            val turbidity = 1.0 + (it % turbCnt) / (10.0 / turbidityStep)

            val state = ArHosekSkyModel.arhosek_xyz_skymodelstate_alloc_init(turbidity, albedo, elevationRad.abs())
            val pixmap = Pixmap(1, gradSize, Pixmap.Format.RGBA8888)

//            printdbg(this, "elev $elevationDeg turb $turbidity")

            for (yp in 0 until gradSize) {
                val yi = yp - 3
                val xf = -elevationDeg / 90.0
                var yf = (yi / 58.0).coerceInSmoothly(0.0, 0.95)

                // experiments visualisation: https://www.desmos.com/calculator/5crifaekwa
//                if (elevationDeg < 0) yf *= 1.0 - pow(xf, 0.333)
//                if (elevationDeg < 0) yf *= -2.0 * asin(xf - 1.0) / PI
                if (elevationDeg < 0) yf *= superellipsoidDecay(1.0 / 3.0, xf)
                val theta = (yf.mapCircle() * HALF_PI)
                // vertical angle, where 0 is zenith, ±90 is ground (which is odd)

                val xyz = CIEXYZ(
                    ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 0).toFloat(),
                    ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 1).toFloat(),
                    ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 2).toFloat()
                )
                val xyz2 = xyz.scaleToFit(elevationDeg)
                val rgb = xyz2.toRGB().toColor()

//                pixmap.setColor(if (yp in 17 until 17 + 94) Color.LIME else Color.CORAL)
                pixmap.setColor(rgb)
                pixmap.drawPixel(0, yp)
            }

            val texture = Texture(pixmap).also {
                it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            }
            pixmap.dispose()
            texture
        }
    }

    override fun dispose() {
        gradTexBinLowAlbedo.forEach { it.dispose() }
        gradTexBinHighAlbedo.forEach { it.dispose() }
    }
}