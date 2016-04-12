package net.torvald

import net.torvald.terrarum.Terrarum

import javax.imageio.ImageIO
import java.awt.*
import java.awt.color.ColorSpace
import java.awt.image.*
import java.io.File
import java.io.IOException

/**
 * Created by minjaesong on 16-03-04.
 */
object RasterWriter {

    val BANDOFFSET_RGB = intArrayOf(0, 1, 2)
    val BANDOFFSET_RGBA = intArrayOf(0, 1, 2, 3)
    val BANDOFFSET_ARGB = intArrayOf(3, 0, 1, 2)
    val BANDOFFSET_MONO = intArrayOf(0)

    val COLORSPACE_SRGB = ColorSpace.CS_sRGB
    val COLORSPACE_GRAY = ColorSpace.CS_GRAY
    val COLORSPACE_GREY = COLORSPACE_GRAY
    val COLORSPACE_CIEXYZ = ColorSpace.CS_CIEXYZ
    val COLORSPACE_RGB_LINEAR_GAMMA = ColorSpace.CS_LINEAR_RGB

    @Throws(IOException::class)
    fun writePNG_RGB(w: Int, h: Int, rasterData: ByteArray, path: String) {
        writePNG(w, h, rasterData, BANDOFFSET_RGB, COLORSPACE_SRGB, path)
    }

    @Throws(IOException::class)
    fun writePNG_Mono(w: Int, h: Int, rasterData: ByteArray, path: String) {
        writePNG(w, h, rasterData, BANDOFFSET_MONO, COLORSPACE_GREY, path)
    }

    @Throws(IOException::class)
    fun writePNG(w: Int, h: Int, rasterData: ByteArray, bandOffsets: IntArray, awt_colorspace: Int, path: String) {
        val buffer = DataBufferByte(rasterData, rasterData.size)
        val raster = Raster.createInterleavedRaster(
                buffer, w, h, bandOffsets.size * w, bandOffsets.size, bandOffsets, null)

        val colorModel = ComponentColorModel(ColorSpace.getInstance(awt_colorspace), false, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE)

        val image = BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied, null)

        ImageIO.write(image, "PNG", File(path))
    }

}
