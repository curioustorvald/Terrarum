package com.Torvald.Terrarum.ConsoleCommand;

import com.Torvald.ColourUtil.Col4096;
import com.Torvald.Terrarum.Terrarum;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.*;
import java.util.Hashtable;

/**
 * Created by minjaesong on 16-01-17.
 */
public class ExportMap implements ConsoleCommand {

    private byte[] mapData;
    private int mapDataPointer = 0;

    private static final byte AIR = 0;

    private static final byte STONE = 1;
    private static final byte DIRT = 2;
    private static final byte GRASS = 3;

    private static final byte SAND = 13;
    private static final byte GRAVEL = 14;

    private static final byte COPPER = 15;
    private static final byte IRON = 16;
    private static final byte GOLD = 17;
    private static final byte SILVER = 18;
    private static final byte ILMENITE = 19;
    private static final byte AURICHALCUM = 20;

    private static final byte DIAMOND = 25;
    private static final byte RUBY = 21;
    private static final byte EMERALD = 22;
    private static final byte SAPPHIRE = 23;
    private static final byte TOPAZ = 24;
    private static final byte AMETHYST = 26;

    private static final byte SNOW = 27;
    private static final byte ICE_FRAGILE = 28;
    private static final byte ICE_NATURAL = 29;
    private static final byte ICE_MAGICAL = 30;

    private static final byte WATER = (byte) 239;
    private static final byte LAVA = (byte) 255;

    private Hashtable<Byte, Col4096> colorTable = new Hashtable<>();

    @Override
    public void execute(String[] args) {
        if (args.length == 2) {
            buildColorTable();

            mapData = new byte[Terrarum.game.map.width * Terrarum.game.map.height * 3];

            for (byte tile : Terrarum.game.map.getLayerTerrain()) {
                byte[] colArray = colorTable.getOrDefault(tile, new Col4096(0xFFF))
                        .toByteArray();

                for (int i = 0; i < 3; i++) {
                    mapData[mapDataPointer + i] = colArray[i];
                }

                mapDataPointer += 3;
            }

            String dir = Terrarum.defaultDir + "/Exports/";
            File dirAsFile = new File(dir);
            if (!dirAsFile.exists()) {
                dirAsFile.mkdir();
            }

            try {
                int[] bandOffsets = {0, 1, 2}; // RGB
                DataBuffer buffer = new DataBufferByte(mapData, mapData.length);
                WritableRaster raster = Raster.createInterleavedRaster(
                        buffer
                        , Terrarum.game.map.width
                        , Terrarum.game.map.height
                        , 3 * Terrarum.game.map.width
                        , 3
                        , bandOffsets
                        , null);

                ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);

                BufferedImage image = new BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(), null);

                ImageIO.write(image, "PNG", new File(dir + args[1] + ".png"));

                new Echo().execute("ExportMap: exported to " + args[1] + ".png");

            } catch (IOException e) {
                new Echo().execute("ExportMap: IOException raised.");
                e.printStackTrace();
            }

            mapData = null;
            mapDataPointer = 0;

            // Free up some memory
            System.gc();
        }
        else{
            printUsage();
        }
    }

    @Override
    public void printUsage() {
        Echo echo = new Echo();
        echo.execute("Usage: export <name>");
        echo.execute("Exports current map into visible image.");
        echo.execute("The image can be found at %adddata%/Terrarum/Exports");
    }

    private void buildColorTable() {
        colorTable.put(AIR, new Col4096(0xCEF));
        colorTable.put(STONE, new Col4096(0x887));
        colorTable.put(DIRT, new Col4096(0x763));
        colorTable.put(GRASS, new Col4096(0x251));

        colorTable.put(COPPER, new Col4096(0x6A8));
        colorTable.put(IRON, new Col4096(0xC75));
        colorTable.put(GOLD, new Col4096(0xCB6));
        colorTable.put(ILMENITE, new Col4096(0x8AB));
        colorTable.put(AURICHALCUM, new Col4096(0xD92));
        colorTable.put(SILVER, new Col4096(0xDDD));

        colorTable.put(DIAMOND, new Col4096(0x9CE));
        colorTable.put(RUBY, new Col4096(0xB10));
        colorTable.put(EMERALD, new Col4096(0x0B1));
        colorTable.put(SAPPHIRE, new Col4096(0x01B));
        colorTable.put(TOPAZ, new Col4096(0xC70));
        colorTable.put(AMETHYST, new Col4096(0x70C));

        colorTable.put(WATER, new Col4096(0x038));
        colorTable.put(LAVA, new Col4096(0xF50));

        colorTable.put(SAND, new Col4096(0xDCA));
        colorTable.put(GRAVEL, new Col4096(0x664));

        colorTable.put(ICE_NATURAL, new Col4096(0x9AB));
        colorTable.put(ICE_MAGICAL, new Col4096(0x7AC));
        colorTable.put(ICE_FRAGILE, new Col4096(0x6AF));
        colorTable.put(SNOW, new Col4096(0xCDE));


    }

}
