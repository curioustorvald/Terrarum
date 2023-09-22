package net.torvald.terrarum.clut

import com.badlogic.gdx.graphics.Color
import net.torvald.colourutil.CIEXYZ
import net.torvald.colourutil.HUSLColorConverter
import net.torvald.colourutil.toColor
import net.torvald.colourutil.toRGB
import net.torvald.gdx.graphics.Cvec
import net.torvald.parametricsky.ArHosekSkyModel
import net.torvald.terrarum.abs
import net.torvald.terrarum.clut.Skybox.coerceInSmoothly
import net.torvald.terrarum.clut.Skybox.mapCircle
import net.torvald.terrarum.clut.Skybox.scaleToFit
import net.torvald.terrarum.modulebasegame.worldgenerator.HALF_PI
import net.torvald.terrarum.serialise.toLittle
import net.torvald.terrarum.serialise.toUint
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2023-08-01.
 */
class GenerateSkyboxTextureAtlas {

    fun generateStrip(
        gammaPair: Int,
        albedo: Double,
        turbidity: Double,
        elevationDeg: Double,
        writefun: (Int, Int, Byte) -> Unit
    ) {
        val elevationRad = Math.toRadians(elevationDeg)
        /*val gamma = if (gammaPair == 0) HALF_PI else {
        Math.toRadians(180 + 114 + 24 * cos(PI * elevationDeg / 40))
    }*/
        val gamma = Math.toRadians(115 + 25 * cos(PI * elevationDeg / 40)) + (gammaPair * PI)
//    println("... Elevation: $elevationDeg")

        val state =
            ArHosekSkyModel.arhosek_xyz_skymodelstate_alloc_init(turbidity, albedo, elevationRad.abs())

        for (yp in 0 until Skybox.gradSize) {
            val yi = yp - 10
            val xf = -elevationDeg / 90.0
            var yf = (yi / 58.0).coerceIn(0.0, 1.0).mapCircle().coerceInSmoothly(0.0, 0.95)

            // experiments visualisation: https://www.desmos.com/calculator/5crifaekwa
//        if (elevationDeg < 0) yf *= 1.0 - pow(xf, 0.333)
//        if (elevationDeg < 0) yf *= -2.0 * asin(xf - 1.0) / PI
            if (elevationDeg < 0) yf *= Skybox.superellipsoidDecay(1.0 / 3.0, xf)
            val theta = yf * HALF_PI
            // vertical angle, where 0 is zenith, ±90 is ground (which is odd)

//        println("$yp\t$theta")

            val xyz = CIEXYZ(
                ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 0).toFloat(),
                ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 1).toFloat(),
                ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 2).toFloat()
            )
            val xyz2 = xyz.scaleToFit(elevationDeg)
            val rgb = xyz2.toRGB().toColor().gamma(1.2f)
            val colour = rgb.toIntBits().toLittle()

            for (i in 0..3) {
                writefun(yp, i, colour[bytesLut[i]])
            }
        }
    }

    private fun Color.gamma(gam: Float): Color {
        this.r = this.r.pow(gam)
        this.g = this.g.pow(gam)
        this.b = this.b.pow(gam)
        return this
    }

    // y: increasing turbidity (1.0 .. 10.0, in steps of 0.333)
    // x: elevations (-75 .. 75 in steps of 1, then albedo of [0.1, 0.3, 0.5, 0.7, 0.9])
    val TGA_HEADER_SIZE = 18
    val texh = Skybox.gradSize * Skybox.turbCnt
    val texh2 = Skybox.turbCnt
    val texw = Skybox.elevCnt * Skybox.albedoCnt * 2
    val bytesSize = texw * texh
    val bytes2Size = texw * texh2
    val bytes = ByteArray(TGA_HEADER_SIZE + bytesSize * 4 + 26)
    val bytes2 = ByteArray(TGA_HEADER_SIZE + texw * bytes2Size * 4 + 26)

    fun generateMainFile() {
        // write header
        byteArrayOf(
            0, // ID field
            0, // colour map (none)
            2, // colour type (unmapped RGB)
            0, 0, 0, 0, 0, // colour map spec (empty)
            0, 0, 0, 0, // unused for modern purposes
            (texw and 255).toByte(), (texw.ushr(8) and 255).toByte(), // width
            (texh and 255).toByte(), (texh.ushr(8) and 255).toByte(), // height
            32, // bits-per-pixel (8bpp RGBA)
            8 // image descriptor (32bpp, bottom-left origin)
        ).forEachIndexed { i, b -> bytes[i] = b }
        // write footer
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000TRUEVISION-XFILE\u002E\u0000".forEachIndexed { i, c ->
            bytes[TGA_HEADER_SIZE + bytesSize * 4 + i] =
                c.code.toByte()
        }

        println("Generating texture atlas ($texw x $texh)...")

        // write pixels
        for (gammaPair in 0..1) {

            for (albedo0 in 0 until Skybox.albedoCnt) {
                val albedo = Skybox.albedos[albedo0]
                println("Albedo=$albedo")
                for (turb0 in 0 until Skybox.turbCnt) {
                    val turbidity = Skybox.turbiditiesD[turb0]
                    println("....... Turbidity=$turbidity")
                    for (elev0 in 0 until Skybox.elevCnt) {
                        var elevationDeg = Skybox.elevationsD[elev0]
                        if (elevationDeg == 0.0) elevationDeg = 0.5 // dealing with the edge case
                        generateStrip(gammaPair, albedo, turbidity, elevationDeg) { yp, i, colour ->
                            val imgOffX = albedo0 * Skybox.elevCnt + elev0 + Skybox.elevCnt * Skybox.albedoCnt * gammaPair
                            val imgOffY = texh - 1 - (Skybox.gradSize * turb0 + yp)
                            val fileOffset = TGA_HEADER_SIZE + 4 * (imgOffY * texw + imgOffX)
                            bytes[fileOffset + i] = colour
                        }
                    }
                }
            }
        }

        println("Atlas generation done!")
        File("./assets/clut/skybox.tga").writeBytes(bytes)
    }

    private val gradSizes = (0 until Skybox.gradSize)

    private fun getByte(gammaPair: Int, albedo0: Int, turb0: Int, elev0: Int, yp: Int, channel: Int): Byte {
        val imgOffX = albedo0 * Skybox.elevCnt + elev0 + Skybox.elevCnt * Skybox.albedoCnt * gammaPair
        val imgOffY = texh - 1 - (Skybox.gradSize * turb0 + yp)
        val fileOffset = TGA_HEADER_SIZE + 4 * (imgOffY * texw + imgOffX)
        return bytes[fileOffset + channel]
    }

    fun generateCloudColourmap() {
        if (bytes[TGA_HEADER_SIZE].toInt() == 0) throw IllegalStateException("Atlas not generated")

        // write header
        byteArrayOf(
            0, // ID field
            0, // colour map (none)
            2, // colour type (unmapped RGB)
            0, 0, 0, 0, 0, // colour map spec (empty)
            0, 0, 0, 0, // unused for modern purposes
            (texw and 255).toByte(), (texw.ushr(8) and 255).toByte(), // width
            (texh2 and 255).toByte(), (texh2.ushr(8) and 255).toByte(), // height
            32, // bits-per-pixel (8bpp RGBA)
            8 // image descriptor (32bpp, bottom-left origin)
        ).forEachIndexed { i, b -> bytes2[i] = b }
        // write footer
        "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000TRUEVISION-XFILE\u002E\u0000".forEachIndexed { i, c ->
            bytes2[TGA_HEADER_SIZE + bytes2Size * 4 + i] =
                c.code.toByte()
        }

        println("Generating cloud colour atlas ($texw x $texh2)...")

        for (gammaPair in 0..1) {

            for (albedo0 in 0 until Skybox.albedoCnt) {
                val albedo = Skybox.albedos[albedo0]
                println("Albedo=$albedo")
                for (turb0 in 0 until Skybox.turbCnt) {
                    val turbidity = Skybox.turbiditiesD[turb0]
                    println("....... Turbidity=$turbidity")
                    for (elev0 in 0 until Skybox.elevCnt) {

                        val avrB = (gradSizes.sumOf { getByte(gammaPair, albedo0, turb0, elev0, it, 0).toUint() }.toDouble() / Skybox.gradSize).div(255.0).srgbLinearise().toFloat()
                        val avrG = (gradSizes.sumOf { getByte(gammaPair, albedo0, turb0, elev0, it, 1).toUint() }.toDouble() / Skybox.gradSize).div(255.0).srgbLinearise().toFloat()
                        val avrR = (gradSizes.sumOf { getByte(gammaPair, albedo0, turb0, elev0, it, 2).toUint() }.toDouble() / Skybox.gradSize).div(255.0).srgbLinearise().toFloat()
                        val avrA = (gradSizes.sumOf { getByte(gammaPair, albedo0, turb0, elev0, it, 3).toUint() }.toDouble() / Skybox.gradSize).div(255.0).srgbLinearise().toFloat()

                        val colour = Cvec(avrR, avrG, avrB, avrA).saturate(1.6666667f)

                        val colourBytes = arrayOf(
                            colour.b.times(255f).roundToInt().coerceIn(0..255).toByte(),
                            colour.g.times(255f).roundToInt().coerceIn(0..255).toByte(),
                            colour.r.times(255f).roundToInt().coerceIn(0..255).toByte(),
                            colour.a.times(255f).roundToInt().coerceIn(0..255).toByte()
                        )

                        val imgOffX = albedo0 * Skybox.elevCnt + elev0 + Skybox.elevCnt * Skybox.albedoCnt * gammaPair
                        val imgOffY = texh2 - 1 - turb0
                        val fileOffset = TGA_HEADER_SIZE + 4 * (imgOffY * texw + imgOffX)

                        for (i in 0..3) {
                            bytes2[fileOffset + i] = colourBytes[i]
                        }

                    }
                }
            }
        }

        println("Colourmap generation done!")
        File("./assets/clut/skyboxavr.tga").writeBytes(bytes2)
    }


    fun invoke() {
        generateMainFile()
        generateCloudColourmap()
    }

    private fun Double.srgbLinearise(): Double {
        return if (this > 0.0031308)
            1.055 * this.pow(1 / 2.4) - 0.055
        else
            this * 12.92
    }

    private fun Cvec.saturate(intensity: Float): Cvec {
        val luv = HUSLColorConverter.rgbToHsluv(floatArrayOf(this.r, this.g, this.b))
        luv[1] *= intensity
        val rgb = HUSLColorConverter.hsluvToRgb(luv)
        this.r = rgb[0]
        this.g = rgb[1]
        this.b = rgb[2]
        return this
    }

    private val bytesLut = arrayOf(2, 1, 0, 3, 2, 1, 0, 3) // For some reason BGRA order is what makes it work
}

fun main() {
    GenerateSkyboxTextureAtlas().invoke()
}