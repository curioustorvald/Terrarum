package net.torvald.terrarum.modulebasegame.clut

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Disposable
import net.torvald.colourutil.CIEXYZ
import net.torvald.colourutil.toColor
import net.torvald.colourutil.toRGB
import net.torvald.parametricsky.ArHosekSkyModel
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.abs
import net.torvald.terrarum.modulebasegame.worldgenerator.HALF_PI
import java.lang.Math.pow
import kotlin.math.*

/**
 * Created by minjaesong on 2023-07-09.
 */
object Skybox : Disposable {

    const val gradSize = 128

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

    private fun getTexturmaps(albedo: Double): Array<Texture> {
        return Array(elevCnt * turbCnt) {

            val elevationDeg = (it / turbCnt).plus(elevations.first).toDouble()
            val elevationRad = Math.toRadians(elevationDeg)
            val turbidity = 1.0 + (it % turbCnt) / (10.0 / turbidityStep)

            val state = ArHosekSkyModel.arhosek_xyz_skymodelstate_alloc_init(turbidity, albedo, elevationRad.abs())
            val pixmap = Pixmap(1, gradSize, Pixmap.Format.RGBA8888)

//            printdbg(this, "elev $elevationDeg turb $turbidity")

            for (y in 0 until gradSize) {
                var yf = (y + 0.5) / gradSize.toDouble()
                if (elevationDeg < 0) yf *= 1.0 - pow(-elevationDeg / 90.0, 0.333)
                val theta = yf.mapCircle() * HALF_PI
                // vertical angle, where 0 is zenith, ±90 is ground (which is odd)

                val xyz = CIEXYZ(
                    ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 0).toFloat(),
                    ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 1).toFloat(),
                    ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 2).toFloat()
                )
                val xyz2 = xyz.scaleToFit(elevationDeg)
                val rgb = xyz2.toRGB().toColor()

                pixmap.setColor(rgb)
                pixmap.drawPixel(0, y)
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