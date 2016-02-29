package com.Torvald.Terrarum.TileStat;

import com.Torvald.Terrarum.Actors.Player;
import com.Torvald.Terrarum.GameMap.GameMap;
import com.Torvald.Terrarum.GameMap.MapLayer;
import com.Torvald.Terrarum.MapDrawer.MapCamera;
import com.Torvald.Terrarum.MapDrawer.MapDrawer;
import com.Torvald.Terrarum.Terrarum;
import com.jme3.math.FastMath;

import java.util.Arrays;

/**
 * Created by minjaesong on 16-02-01.
 */
public class TileStat {

    private static short[] tilestat = new short[MapLayer.TILES_SUPPORTED];

    private static final int TSIZE = MapDrawer.TILE_SIZE;

    /**
     * Update tile stats from tiles on screen
     */
    public static void update() {
        Arrays.fill(tilestat, (short) 0);

        // Get stats on no-zoomed screen area. In other words, will behave as if screen zoom were 1.0
        // no matter how the screen is zoomed.
        GameMap map = Terrarum.game.map;
        Player player = Terrarum.game.getPlayer();

        float zoom = Terrarum.game.screenZoom;

        int renderWidth = FastMath.ceil(Terrarum.WIDTH);
        int renderHeight = FastMath.ceil(Terrarum.HEIGHT);

        int noZoomCameraX = MapCamera.clamp(
                Math.round(player.getHitbox().getPointedX() - (renderWidth / 2))
                , map.width * TSIZE - renderWidth
        );
        int noZoomCameraY = MapCamera.clamp(
                Math.round(player.getHitbox().getPointedY() - (renderHeight / 2))
                , map.height * TSIZE - renderHeight
        );

        int for_x_start = MapCamera.div16(noZoomCameraX);
        int for_y_start = MapCamera.div16(noZoomCameraY);
        int for_y_end = MapCamera.clampHTile(for_y_start + MapCamera.div16(renderHeight) + 2);
        int for_x_end = MapCamera.clampWTile(for_x_start + MapCamera.div16(renderWidth) + 2);

        for (int y = for_y_start; y < for_y_end; y++) {
            for (int x = for_x_start; x < for_x_end; x++) {
                int tileWall = map.getTileFromWall(x, y);
                int tileTerrain = map.getTileFromTerrain(x, y);
                tilestat[tileWall] += 1;
                tilestat[tileTerrain] += 1;
            }
        }
    }

    public static int getCount(byte... tile) {
        int sum = 0;
        for (int i = 0; i < tile.length; i++) {
            int newArgs = Byte.toUnsignedInt(tile[i]);
            sum += Short.toUnsignedInt(tilestat[ newArgs ]);
        }

        return sum;
    }

    public static int getCount(int... tile) {
        int sum = 0;
        for (int i = 0; i < tile.length; i++) {
            sum += Short.toUnsignedInt(tilestat[ tile[i] ]);
        }
        return sum;
    }

    /**
     *
     * @return copy of the stat data
     */
    public static short[] getStatCopy() {
        return Arrays.copyOf(tilestat, MapLayer.TILES_SUPPORTED);
    }

}
