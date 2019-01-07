package com.badlogic.gdx.graphics;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.StreamUtils;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by minjaesong on 2019-01-07.
 */
public class PixmapIO2 {

    public static void writeTGA(FileHandle file, Pixmap pixmap) throws IOException {
        OutputStream output = file.write(false);

        try {
            _writeTGA(output, pixmap);
        } finally {
            StreamUtils.closeQuietly(output);
        }
    }

    private static void _writeTGA(OutputStream out, Pixmap pixmap) throws IOException {
        byte[] width = toShortLittle(pixmap.getWidth());
        byte[] height = toShortLittle(pixmap.getHeight());
        byte[] zero = toShortLittle(0);

        byte[] zeroalpha = new byte[]{0,0,0,0};

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

        for (int y = pixmap.getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < pixmap.getWidth(); x++) {
                int color = pixmap.getPixel(x, y);

                // if alpha == 0, write special value instead
                if ((color & 0xFF) == 0) {
                    out.write(zeroalpha);
                }
                else {
                    out.write(RGBAtoBGRA(color));
                }
            }
        }


        // write footer
        // 00 00 00 00 00 00 00 00 TRUEVISION-XFILE 2E 00
        out.write(new byte[]{0,0,0,0,0,0,0,0});
        out.write("TerrarumHappyTGA".getBytes());
        out.write(new byte[]{0x2E,0});


        out.flush();
        out.close();
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
