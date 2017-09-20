package net.torvald.terrarum.utils

import javax.imageio.ImageIO
import java.awt.*
import java.awt.color.ColorSpace
import java.awt.image.*
import java.io.File
import java.io.IOException

/**
 * Created by minjaesong on 2016-03-04.
 */
object RasterWriter {

    val BANDOFFSET_RGB = intArrayOf(0, 1, 2)
    val BANDOFFSET_RGBA = intArrayOf(0, 1, 2, 3)
    val BANDOFFSET_ARGB = intArrayOf(3, 0, 1, 2)
    val BANDOFFSET_MONO = intArrayOf(0)

    val COLORSPACE_SRGB = java.awt.color.ColorSpace.CS_sRGB
    val COLORSPACE_GRAY = java.awt.color.ColorSpace.CS_GRAY
    val COLORSPACE_GREY = net.torvald.terrarum.utils.RasterWriter.COLORSPACE_GRAY
    val COLORSPACE_CIEXYZ = java.awt.color.ColorSpace.CS_CIEXYZ
    val COLORSPACE_RGB_LINEAR_GAMMA = java.awt.color.ColorSpace.CS_LINEAR_RGB

    @Throws(java.io.IOException::class)
    fun writePNG_RGB(w: Int, h: Int, rasterData: ByteArray, path: String) {
        net.torvald.terrarum.utils.RasterWriter.writePNG(w, h, rasterData, BANDOFFSET_RGB, COLORSPACE_SRGB, path)
    }

    @Throws(java.io.IOException::class)
    fun writePNG_Mono(w: Int, h: Int, rasterData: ByteArray, path: String) {
        net.torvald.terrarum.utils.RasterWriter.writePNG(w, h, rasterData, BANDOFFSET_MONO, COLORSPACE_GREY, path)
    }

    @Throws(java.io.IOException::class)
    fun writePNG(w: Int, h: Int, rasterData: ByteArray, bandOffsets: IntArray, awt_colorspace: Int, path: String) {
        val buffer = java.awt.image.DataBufferByte(rasterData, rasterData.size)
        val raster = java.awt.image.Raster.createInterleavedRaster(
                buffer, w, h, bandOffsets.size * w, bandOffsets.size, bandOffsets, null)

        val colorModel = java.awt.image.ComponentColorModel(java.awt.color.ColorSpace.getInstance(awt_colorspace), false, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE)

        val image = java.awt.image.BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied, null)

        javax.imageio.ImageIO.write(image, "PNG", java.io.File(path))
    }

}
