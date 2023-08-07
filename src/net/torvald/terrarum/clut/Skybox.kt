package net.torvald.terrarum.clut

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import com.jme3.math.FastMath
import net.torvald.colourutil.CIEXYZ
import net.torvald.colourutil.toColor
import net.torvald.colourutil.toRGB
import net.torvald.parametricsky.ArHosekSkyModel
import net.torvald.terrarum.App
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.abs
import net.torvald.terrarum.floorToInt
import net.torvald.terrarum.toInt
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import kotlin.math.*

/**
 * Created by minjaesong on 2023-07-09.
 */
object Skybox : Disposable {

    private const val HALF_PI = 1.5707963267948966
    private const val PI = 3.141592653589793
    private const val TWO_PI = 6.283185307179586

    const val gradSize = 78

    private lateinit var gradTexBinLowAlbedo: Array<TextureRegion>
    private lateinit var gradTexBinHighAlbedo: Array<TextureRegion>

    private lateinit var tex: Texture
    private lateinit var texRegions: TextureRegionPack
    private lateinit var texStripRegions: TextureRegionPack

    fun loadlut() {
        tex = Texture(Gdx.files.internal("assets/clut/skybox.png"))
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        tex.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
        texRegions = TextureRegionPack(tex, 2, gradSize - 2, 0, 2, 0, 1)
        texStripRegions = TextureRegionPack(tex, elevCnt, gradSize - 2, 0, 2, 0, 1)
    }

    // use internal LUT
    /*operator fun get(elevationDeg: Double, turbidity: Double, albedo: Double): TextureRegion {
        val elev = elevationDeg.coerceIn(-elevBias, elevBias).times(2.0).roundToInt().plus(150)
        val turb = turbidity.coerceIn(1.0, 10.0).minus(1.0).times(turbDivisor).roundToInt()
        val alb = albedo.coerceIn(0.1, 0.9).minus(0.1).times(turbDivisor).roundToInt()
        return gradTexBinLowAlbedo[elev * turbCnt + turb]
    }*/

    // use external LUT
    operator fun get(elevationDeg: Double, turbidity: Double, albedo: Double, isAfternoon: Boolean): TextureRegion {
       TODO()
    }

    data class SkyboxRenderInfo(
        val texture: Texture,
        val uvs: FloatArray,
        val turbidityPoint: Float,
        val albedoPoint: Float,
    )

    fun getUV(elevationDeg: Double, turbidity: Double, albedo: Double): SkyboxRenderInfo {
        val turb = turbidity.coerceIn(turbiditiesD.first(), turbiditiesD.last()).minus(1.0).times(turbDivisor)
        val turbLo = turb.floorToInt()
        val turbHi = min(turbCnt - 1, turbLo + 1)
        val alb = albedo.coerceIn(albedos.first(), albedos.last()).times(5.0)
        val albLo = alb.floorToInt()
        val albHi = min(albedoCnt - 1, albLo + 1)
        val elev = elevationDeg.coerceIn(-elevMax, elevMax).plus(elevMax).div(elevations.last.toDouble()).div(albedoCnt * 2).times((elevCnt - 1.0) / elevCnt)

        // A: morn, turbLow, albLow
        // B: noon, turbLow, albLow
        // C: morn, turbHigh, albLow
        // D: noon, turbHigh, albLow
        // E: morn, turbLow, albHigh
        // F: noon, turbLow, albHigh
        // G: morn, turbHigh, albHigh
        // H: noon, turbHigh, albHigh

        val regionA = texStripRegions.get(albLo + albedoCnt * 0, turbLo)
        val regionB = texStripRegions.get(albLo + albedoCnt * 1, turbLo)
        val regionC = texStripRegions.get(albLo + albedoCnt * 0, turbHi)
        val regionD = texStripRegions.get(albLo + albedoCnt * 1, turbHi)
        val regionE = texStripRegions.get(albHi + albedoCnt * 0, turbLo)
        val regionF = texStripRegions.get(albHi + albedoCnt * 1, turbLo)
        val regionG = texStripRegions.get(albHi + albedoCnt * 0, turbHi)
        val regionH = texStripRegions.get(albHi + albedoCnt * 1, turbHi)
        // (0.5f / tex.width): because of the nature of bilinear interpolation, half pixels from the edges must be discarded
        val uA = regionA.u + (0.5f / tex.width) + elev.toFloat()
        val uB = regionB.u + (0.5f / tex.width) + elev.toFloat()
        val uC = regionC.u + (0.5f / tex.width) + elev.toFloat()
        val uD = regionD.u + (0.5f / tex.width) + elev.toFloat()
        val uE = regionE.u + (0.5f / tex.width) + elev.toFloat()
        val uF = regionF.u + (0.5f / tex.width) + elev.toFloat()
        val uG = regionG.u + (0.5f / tex.width) + elev.toFloat()
        val uH = regionH.u + (0.5f / tex.width) + elev.toFloat()

        return SkyboxRenderInfo(
            tex,
            floatArrayOf(
                uA, regionA.v, uA, regionA.v2,
                uB, regionB.v, uB, regionB.v2,
                uC, regionC.v, uC, regionC.v2,
                uD, regionD.v, uD, regionD.v2,
                uE, regionE.v, uE, regionE.v2,
                uF, regionF.v, uF, regionF.v2,
                uG, regionG.v, uG, regionG.v2,
                uH, regionH.v, uH, regionH.v2,
            ),
            (turb - turbLo).toFloat(),
            (alb - albLo).toFloat(),
        )
    }

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
            // maths model: https://www.desmos.com/calculator/cwi7iyzygg

            val x = -elevationDeg.toFloat()
//            val elevation2 = elevationDeg.toFloat() / 28.5f
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

    val elevations = (0..150)
    val elevMax = elevations.last / 2.0
    val elevationsD = elevations.map { -elevMax + it } // -75, -74, -73, ..., 74, 75 // (specifically using whole number of angles because angle units any finer than 1.0 would make "hack" sunsut happen too fast)
    val turbidities = (0..25) // 1, 1.2, 1.4, 1.6, ..., 6.0
    val turbDivisor = 5.0
    val turbiditiesD = turbidities.map { 1.0 + it / turbDivisor }
    val albedos = arrayOf(0.0, 0.2, 0.4, 0.6, 0.8, 1.0)
    val elevCnt = elevations.count()
    val turbCnt = turbidities.count()
    val albedoCnt = albedos.size
    val gamma = HALF_PI

    internal fun Double.mapCircle() = sin(HALF_PI * this)

    internal fun initiate() {
        printdbg(this, "Initialising skybox model")

        TODO()

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
                val yi = yp - 10
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
        if (Skybox::gradTexBinLowAlbedo.isInitialized) gradTexBinLowAlbedo.forEach { it.texture.dispose() }
        if (Skybox::gradTexBinHighAlbedo.isInitialized) gradTexBinHighAlbedo.forEach { it.texture.dispose() }
        if (Skybox::tex.isInitialized) tex.dispose()
    }
}