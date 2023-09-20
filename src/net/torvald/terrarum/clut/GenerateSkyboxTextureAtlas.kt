package net.torvald.terrarum.clut

import net.torvald.colourutil.CIEXYZ
import net.torvald.colourutil.toColor
import net.torvald.colourutil.toRGB
import net.torvald.parametricsky.ArHosekSkyModel
import net.torvald.terrarum.abs
import net.torvald.terrarum.clut.Skybox.coerceInSmoothly
import net.torvald.terrarum.clut.Skybox.mapCircle
import net.torvald.terrarum.clut.Skybox.scaleToFit
import net.torvald.terrarum.modulebasegame.worldgenerator.HALF_PI
import net.torvald.terrarum.serialise.toLittle
import java.io.File

/**
 * Created by minjaesong on 2023-08-01.
 */
fun main() {
    // y: increasing turbidity (1.0 .. 10.0, in steps of 0.333)
    // x: elevations (-75 .. 75 in steps of 1, then albedo of [0.1, 0.3, 0.5, 0.7, 0.9])
    val texh = Skybox.gradSize * Skybox.turbCnt
    val texw = Skybox.elevCnt * Skybox.albedoCnt * 2
    val TGA_HEADER_SIZE = 18

    val bytes = ByteArray(TGA_HEADER_SIZE + texw * texh * 4 + 26)
    // write header
    byteArrayOf(
        0, // ID field
        0, // colour map (none)
        2, // colour type (unmapped RGB)
        0,0,0,0,0, // colour map spec (empty)
        0,0, // x origin (0)
        0,0, // y origin (0)
        (texw and 255).toByte(),(texw.ushr(8) and 255).toByte(), // width
        (texh and 255).toByte(),(texh.ushr(8) and 255).toByte(), // height
        32, // bits-per-pixel (8bpp RGBA)
        8 // image descriptor
    ).forEachIndexed { i,b -> bytes[i] = b }
    // write footer
    "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000TRUEVISION-XFILE\u002E\u0000".forEachIndexed { i, c -> bytes[18 + texw * texh * 4 + i] =
        c.code.toByte()
    }

    println("Generating texture atlas ($texw x $texh)...")

    // write pixels
    for (gammaPair in 0..1) {
        val gamma = if (gammaPair == 0) HALF_PI else 3* HALF_PI

        for (albedo0 in 0 until Skybox.albedoCnt) {
            val albedo = Skybox.albedos[albedo0]
            println("Albedo=$albedo")
            for (turb0 in 0 until Skybox.turbCnt) {
                val turbidity = Skybox.turbiditiesD[turb0]
                println("....... Turbidity=$turbidity")
                for (elev0 in 0 until Skybox.elevCnt) {
                    val elevationDeg = Skybox.elevationsD[elev0]
                    val elevationRad = Math.toRadians(elevationDeg)
//                println("... Elevation: $elevationDeg")

                    val state =
                        ArHosekSkyModel.arhosek_xyz_skymodelstate_alloc_init(turbidity, albedo, elevationRad.abs())

                    for (yp in 0 until Skybox.gradSize) {
                        val yi = yp - 10
                        val xf = -elevationDeg / 90.0
                        var yf = (yi / 58.0).coerceIn(0.0, 1.0).mapCircle().coerceInSmoothly(0.0, 0.95)

                        // experiments visualisation: https://www.desmos.com/calculator/5crifaekwa
//                    if (elevationDeg < 0) yf *= 1.0 - pow(xf, 0.333)
//                    if (elevationDeg < 0) yf *= -2.0 * asin(xf - 1.0) / PI
                        if (elevationDeg < 0) yf *= Skybox.superellipsoidDecay(1.0 / 3.0, xf)
                        val theta = yf * HALF_PI
                        // vertical angle, where 0 is zenith, ±90 is ground (which is odd)

//                    println("$yp\t$theta")

                        val xyz = CIEXYZ(
                            ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 0).toFloat(),
                            ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 1).toFloat(),
                            ArHosekSkyModel.arhosek_tristim_skymodel_radiance(state, theta, gamma, 2).toFloat()
                        )
                        val xyz2 = xyz.scaleToFit(elevationDeg)
                        val rgb = xyz2.toRGB().toColor()
                        val colour = rgb.toIntBits().toLittle()

                        val imgOffX = albedo0 * Skybox.elevCnt + elev0 + Skybox.elevCnt * Skybox.albedoCnt * gammaPair
                        val imgOffY = texh - 1 - (Skybox.gradSize * turb0 + yp)
                        val fileOffset = TGA_HEADER_SIZE + 4 * (imgOffY * texw + imgOffX)
                        for (i in 0..3) {
                            bytes[fileOffset + i] = colour[bytesLut[i]]
                        }
                    }
                }
            }
        }
    }

    println("Atlas generation done!")

    File("./assets/clut/skybox.tga").writeBytes(bytes)
}

private val bytesLut = arrayOf(2,1,0,3,2,1,0,3) // For some reason BGRA order is what makes it work