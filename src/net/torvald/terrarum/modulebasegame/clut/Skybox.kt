package net.torvald.terrarum.modulebasegame.clut

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import com.jme3.math.FastMath
import com.jme3.math.Vector2f
import net.torvald.colourutil.CIEXYZ
import net.torvald.colourutil.toColor
import net.torvald.colourutil.toRGB
import net.torvald.parametricsky.ArHosekSkyModel
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.abs
import net.torvald.terrarum.modulebasegame.worldgenerator.HALF_PI
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import kotlin.math.*

/**
 * Created by minjaesong on 2023-07-09.
 */
object Skybox : Disposable {

    const val gradSize = 64

    private lateinit var gradTexBinLowAlbedo: Array<TextureRegion>
    private lateinit var gradTexBinHighAlbedo: Array<TextureRegion>

    private lateinit var tex: Texture
    private lateinit var texRegions: TextureRegionPack
    private lateinit var texStripRegions: TextureRegionPack

    fun loadlut() {
        tex = Texture(ModMgr.getGdxFile("basegame", "weathers/main_skybox.png"))
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        texRegions = TextureRegionPack(tex, 2, gradSize - 2, 0, 2, 0, 1)
        texStripRegions = TextureRegionPack(tex, elevCnt - 2, gradSize - 2, 2, 2, 1, 1)
    }

    // use internal LUT
    /*operator fun get(elevationDeg: Double, turbidity: Double, albedo: Double): TextureRegion {
        val elev = elevationDeg.coerceIn(-75.0, 75.0).times(2.0).roundToInt().plus(150)
        val turb = turbidity.coerceIn(1.0, 10.0).minus(1.0).times(3.0).roundToInt()
        val alb = albedo.coerceIn(0.1, 0.9).minus(0.1).times(5.0).roundToInt()
        return gradTexBinLowAlbedo[elev * turbCnt + turb]
    }*/

    // use external LUT
    operator fun get(elevationDeg: Double, turbidity: Double, albedo: Double): TextureRegion {
        val elev = elevationDeg.coerceIn(-75.0, 75.0).roundToInt().plus(75)
        val turb = turbidity.coerceIn(1.0, 10.0).minus(1.0).times(5.0).roundToInt()
        val alb = albedo.coerceIn(0.1, 0.9).minus(0.1).times(5.0).roundToInt()
        //printdbg(this, "elev $elevationDeg->$elev; turb $turbidity->$turb; alb $albedo->$alb")
        return texRegions.get(alb * elevCnt + elev, turb)
    }

    fun getUV(elevationDeg: Double, turbidity: Double, albedo: Double): Pair<Texture, FloatArray> {
        val turb = turbidity.coerceIn(1.0, 10.0).minus(1.0).times(3.0).roundToInt()
        val alb = albedo.coerceIn(0.1, 0.9).minus(0.1).times(5.0).roundToInt()
        val region = texStripRegions.get(alb, turb)

        val elev = elevationDeg.coerceIn(-75.0, 75.0).plus(75.0).div(150.0)

        val u = region.u + (elev / albedoCnt).toFloat()

        return tex to floatArrayOf(
            u,
            region.v,
            u,
            region.v2
        )
    }

    private val texcoordEpsilon = 1f / 131072f

    private fun Float.scaleFun() =
        (1f - 1f / 2f.pow(this/6f)) * 0.97f

    internal fun CIEXYZ.scaleToFit(elevationDeg: Double): CIEXYZ {
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

    val elevations = (0..150) //
    val elevationsD = elevations.map { -75.0 + it } // -75, -74, -73, ..., 74, 75 // (specifically using whole number of angles because angle units any finer than 1.0 would make "hack" sunsut happen too fast)
    val turbidities = (0..45) // 1, 1.2, 1.4, 1.6, ..., 10.0
    val turbiditiesD = turbidities.map { 1.0 + it / 5.0 }
    val albedos = arrayOf(0.1, 0.3, 0.5, 0.7, 0.9)
    val elevCnt = elevations.count()
    val turbCnt = turbidities.count()
    val albedoCnt = albedos.size
    val albedoLow = 0.1
    val albedoHight = 0.8 // for theoretical "winter wonderland"?
    val gamma = HALF_PI

    internal fun Double.mapCircle() = sin(HALF_PI * this)

    internal fun initiate() {
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
    internal fun polynomialDecay(p: Double, q: Int, x: Double): Double {
        val sign = if (q % 2 == 1) -1 else 1
        val a1 = -1.0 / p
        val a2 = 1.0 / (1.0 - p)
        val q = q.toDouble()
        return if (x < p)
            sign * a1.pow(q - 1.0) * x.pow(q) + 1.0
        else
            sign * a2.pow(q - 1.0) * (x - 1.0).pow(q)
    }

    internal fun polynomialDecay2(p: Double, q: Int, x: Double): Double {
        val sign = if (q % 2 == 1) 1 else -1
        val a1 = -1.0 / p
        val a2 = 1.0 / (1.0 - p)
        val q = q.toDouble()
        return if (x < p)
            sign * a1.pow(q - 1.0) * x.pow(q)
        else
            sign * a2.pow(q - 1.0) * (x - 1.0).pow(q) + 1.0
    }

    internal fun superellipsoidDecay(p: Double, x: Double): Double {
        return 1.0 - (1.0 - (1.0 - x).pow(1.0 / p)).pow(p)
    }

    internal fun Double.coerceInSmoothly(low: Double, high: Double): Double {
        val x = this.coerceIn(low, high)
        val x2 = ((x - low) * (high - low).pow(-1.0))
//        return FastMath.interpolateLinear(polynomialDecay2(0.5, 2, x2), low, high)
        return FastMath.interpolateLinear(smoothLinear(0.2, x2), low, high)
    }

    /**
     * To get the idea what the fuck is going on here, please refer to https://www.desmos.com/calculator/snqglcu2wl
     */
    internal fun smoothLinear(p: Double, x0: Double): Double {
        val x = x0 - 0.5
        val p1 = sqrt(1.0 - 2.0 * p)
        val t = 0.5 * p1
        val y0 = if (x < -t)
            (1.0 / p) * (x + 0.5).pow(2) - 0.5
        else if (x > t)
            -(1.0 / p) * (x - 0.5).pow(2) + 0.5
        else
            x * 2.0 / (1.0 + p1)

        return y0 + 0.5
    }

    private fun getTexturmaps(albedo: Double): Array<TextureRegion> {
        return Array(elevCnt * turbCnt) {

            val elevationDeg = elevationsD[it / turbCnt]
            val elevationRad = Math.toRadians(elevationDeg)
            val turbidity = turbiditiesD[it % turbCnt]

            val state = ArHosekSkyModel.arhosek_xyz_skymodelstate_alloc_init(turbidity, albedo, elevationRad.abs())
            val pixmap = Pixmap(1, gradSize, Pixmap.Format.RGBA8888)

//            printdbg(this, "elev $elevationDeg turb $turbidity")

            for (yp in 0 until gradSize) {
                val yi = yp - 3
                val xf = -elevationDeg / 90.0
                var yf = (yi / 58.0).coerceIn(0.0, 1.0).mapCircle().coerceInSmoothly(0.0, 0.95)

                // experiments visualisation: https://www.desmos.com/calculator/5crifaekwa
//                if (elevationDeg < 0) yf *= 1.0 - pow(xf, 0.333)
//                if (elevationDeg < 0) yf *= -2.0 * asin(xf - 1.0) / PI
                if (elevationDeg < 0) yf *= superellipsoidDecay(1.0 / 3.0, xf)
                val theta = yf * HALF_PI
                // vertical angle, where 0 is zenith, ±90 is ground (which is odd)

//                println("$yp\t$theta")

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
            TextureRegion(texture)
        }
    }

    override fun dispose() {
        if (::gradTexBinLowAlbedo.isInitialized) gradTexBinLowAlbedo.forEach { it.texture.dispose() }
        if (::gradTexBinHighAlbedo.isInitialized) gradTexBinHighAlbedo.forEach { it.texture.dispose() }
        if (::tex.isInitialized) tex.dispose()
    }
}