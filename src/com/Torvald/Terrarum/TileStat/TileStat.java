package com.Torvald.Terrarum.TileStat;

import com.Torvald.Terrarum.Game;
import com.Torvald.Terrarum.GameMap.MapLayer;
import com.Torvald.Terrarum.MapDrawer.MapCamera;

import java.util.Arrays;

/**
 * Created by minjaesong on 16-02-01.
 */
public class TileStat {

    private static short[] tilestat = new short[MapLayer.TILES_SUPPORTED];

    public static void update() {
        Arrays.fill(tilestat, (short) 0);

        int for_x_start = MapCamera.getRenderStartX();
        int for_y_start = MapCamera.getRenderStartY();
        int for_x_end = MapCamera.getRenderEndX();
        int for_y_end = MapCamera.getRenderEndY();

        for (int y = for_y_start; y < for_y_end; y++) {
            for (int x = for_x_start; x < for_x_end; x++) {
                int tileWall = Game.map.getTileFromWall(x, y);
                int tileTerrain = Game.map.getTileFromTerrain(x, y);
                tilestat[tileWall] += 1;
                tilestat[tileTerrain] += 1;
            }
        }
    }

    public static int getCount(int... tile) {
        int sum = 0;
        for (int i = 0; i < tile.length; i++) {
            sum += Short.toUnsignedInt(tilestat[tile[i]]);
        }
        return sum;
    }

}
