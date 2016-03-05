package com.Torvald;

import com.Torvald.Terrarum.Terrarum;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;

/**
 * Created by minjaesong on 16-03-04.
 */
public class RasterWriter {

    public static final int[] BANDOFFSET_RGB = {0, 1, 2};
    public static final int[] BANDOFFSET_RGBA = {0, 1, 2, 3};
    public static final int[] BANDOFFSET_ARGB = {3, 0, 1, 2};
    public static final int[] BANDOFFSET_MONO = {0};

    public static final int COLORSPACE_SRGB = ColorSpace.CS_sRGB;
    public static final int COLORSPACE_GRAY = ColorSpace.CS_GRAY;
    public static final int COLORSPACE_GREY = COLORSPACE_GRAY;
    public static final int COLORSPACE_CIEXYZ = ColorSpace.CS_CIEXYZ;
    public static final int COLORSPACE_RGB_LINEAR_GAMMA = ColorSpace.CS_LINEAR_RGB;

    public static void writePNG_RGB(int w, int h, byte[] rasterData, String path) throws IOException {
        writePNG(w, h, rasterData, BANDOFFSET_RGB, COLORSPACE_SRGB, path);
    }

    public static void writePNG_Mono(int w, int h, byte[] rasterData, String path) throws IOException {
        writePNG(w, h, rasterData, BANDOFFSET_MONO, COLORSPACE_GREY, path);
    }

    public static void writePNG(int w, int h, byte[] rasterData, int[] bandOffsets, int awt_colorspace, String path) throws IOException {
        DataBuffer buffer = new DataBufferByte(rasterData, rasterData.length);
        WritableRaster raster = Raster.createInterleavedRaster(
                  buffer
                , w
                , h
                , bandOffsets.length * w
                , bandOffsets.length
                , bandOffsets
                , null
        );

        ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(awt_colorspace), false, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);

        BufferedImage image = new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(), null);

        ImageIO.write(image, "PNG", new File(path));
    }

}
