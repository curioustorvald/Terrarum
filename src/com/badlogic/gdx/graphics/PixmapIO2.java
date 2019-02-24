package com.badlogic.gdx.graphics;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.StreamUtils;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by minjaesong on 2019-01-07.
 */
public class PixmapIO2 {

    // REMEMBER: to the GL's perspective, this game's FBOs are always Y-flipped. //

    public static int HEADER_FOOTER_SIZE = 18 + 26;

    public static void writeTGAHappy(FileHandle file, Pixmap pixmap, boolean flipY) throws IOException {
        OutputStream output = file.write(false);

        try {
            _writeTGA(output, pixmap, false, flipY);
        } finally {
            StreamUtils.closeQuietly(output);
        }
    }

    public static void writeTGA(FileHandle file, Pixmap pixmap, boolean flipY) throws IOException {
        OutputStream output = file.write(false);

        try {
            _writeTGA(output, pixmap, true, flipY);
        } finally {
            StreamUtils.closeQuietly(output);
        }
    }

    public static void _writeTGA(OutputStream out, Pixmap pixmap, boolean verbatim, boolean flipY) throws IOException {
        byte[] width = toShortLittle(pixmap.getWidth());
        byte[] height = toShortLittle(pixmap.getHeight());
        byte[] zero = toShortLittle(0);

        out.write(0); // ID field: empty
        out.write(0); // no colour map, but should be ignored anyway as it being unmapped RGB
        out.write(2); // 2 means unmapped RGB
        out.write(new byte[]{0,0,0,0,0}); // color map spec: empty
        out.write(zero); // x origin: 0
        out.write(zero); // y origin: 0
        out.write(width); // width
        out.write(height); // height
        out.write(32); // image pixel size: we're writing 32-bit image (8bpp BGRA)
        out.write(8); // image descriptor: dunno, Photoshop writes 8 in there

        // write actual image data
        // since we're following Photoshop's conventional header, we also follows Photoshop's
        // TGA saving scheme, that is:
        //     1. BGRA order
        //     2. Y-Flipped but not X-Flipped

        if (!flipY) {
            for (int y = pixmap.getHeight() - 1; y >= 0; y--) {
                for (int x = 0; x < pixmap.getWidth(); x++) {
                    writeTga(x, y, verbatim, pixmap, out);
                }
            }
        }
        else {
            for (int y = 0; y < pixmap.getHeight(); y++) {
                for (int x = 0; x < pixmap.getWidth(); x++) {
                    writeTga(x, y, verbatim, pixmap, out);
                }
            }
        }


        // write footer
        // 00 00 00 00 00 00 00 00 TRUEVISION-XFILE 2E 00
        out.write(new byte[]{0,0,0,0,0,0,0,0});
        if (verbatim)
            out.write("TRUEVISION-XFILE".getBytes());
        else
            out.write("TerrarumHappyTGA".getBytes());
        out.write(new byte[]{0x2E,0});


        out.flush();
        out.close();
    }

    private static byte[] zeroalpha = new byte[]{0,0,0,0};
    private static void writeTga(int x, int y, boolean verbatim, Pixmap pixmap, OutputStream out) throws IOException {
        int color = pixmap.getPixel(x, y);

        // if alpha == 0, write special value instead
        if (verbatim && (color & 0xFF) == 0) {
            out.write(zeroalpha);
        }
        else {
            out.write(RGBAtoBGRA(color));
        }
    }
    
    private static byte[] toShortLittle(int i) {
        return new byte[]{
                (byte) (i & 0xFF),
                (byte) ((i >>> 8) & 0xFF)
        };
    }

    private static byte[] RGBAtoBGRA(int rgba) {
        return new byte[]{
                (byte) ((rgba >>> 8) & 0xFF),
                (byte) ((rgba >>> 16) & 0xFF),
                (byte) ((rgba >>> 24) & 0xFF),
                (byte) (rgba & 0xFF)
        };
    }
}
