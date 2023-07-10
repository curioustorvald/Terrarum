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
import kotlin.math.*

/**
 * Created by minjaesong on 2023-07-09.
 */
object Skybox : Disposable {

    const val gradSize = 128

    private val gradTexBin: Array<Texture>

    operator fun get(elevationDeg: Double, turbidity: Double): Texture {
//        if (elevationDeg !in elevationsD) {
//            throw IllegalArgumentException("Elevation not in ±75° (got $elevationDeg)")
//        }
//        if (turbidity !in turbiditiesD) {
//            throw IllegalArgumentException("Turbidity not in 1..10 (got $turbidity)")
//        }

        val elev = elevationDeg.coerceIn(elevationsD).toInt() - elevations.first
        val turb = ((turbidity.coerceIn(turbiditiesD) - turbiditiesD.start) / (turbidities.step / 100.0)).toInt()

//        printdbg(this, "$elevationDeg $turbidity ; $elev $turb")

        return gradTexBin[elev * turbCnt + turb]
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
            val elevation1 = -elevationDeg
            val elevation2 = -elevationDeg / 28.5
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

    private val elevations = (-75..75) // 151
    private val elevationsD = (elevations.first.toDouble() .. elevations.last.toDouble())
    private val turbidities = (1_00..10_00 step 50) // 19
    private val turbiditiesD = (turbidities.first / 100.0..turbidities.last / 100.0)
    private val elevCnt = elevations.count()
    private val turbCnt = turbidities.count()
    private val albedo = 0.1
    private val gamma = HALF_PI

    private fun Double.mapCircle() = sin(HALF_PI * this)

    init {
        printdbg(this, "Initialising skybox model")

        gradTexBin = Array(elevCnt * turbCnt) {

            val elevationDeg = (it / turbCnt).plus(elevations.first).toDouble()
            val elevationRad = Math.toRadians(elevationDeg)
            val turbidity = 1.0 + (it % turbCnt) / 2.0

            val state = ArHosekSkyModel.arhosek_xyz_skymodelstate_alloc_init(turbidity, albedo, elevationRad.abs())
            val pixmap = Pixmap(1, gradSize, Pixmap.Format.RGBA8888)

//            printdbg(this, "elev $elevationDeg turb $turbidity")

            for (y in 0 until gradSize) {
                val theta = (y.toDouble() / gradSize * 1.0).coerceIn(0.0, 1.0).mapCircle() * HALF_PI
                // vertical angle, where 0 is zenith, ±90 is ground (which is odd)

                val xyz = CIEXYZ(
                    ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 0).toFloat(),
                    ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 1).toFloat(),
                    ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 2).toFloat()
                )
                val xyz2 = xyz.scaleToFit(elevationDeg)
                val rgb = xyz2.toRGB().toColor()

                pixmap.setColor(rgb)
                pixmap.drawPixel(0, gradSize - 1 - y)
            }

            val texture = Texture(pixmap).also {
                it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            }
            pixmap.dispose()
            texture
        }

        App.disposables.add(this)

        printdbg(this, "Skybox model generated!")
    }

    override fun dispose() {
        gradTexBin.forEach { it.dispose() }
    }
}