package com.Torvald.Terrarum.UserInterface;

import com.Torvald.Terrarum.Actors.PlayerDebugger;
import com.Torvald.Terrarum.Actors.Hitbox;
import com.Torvald.Terrarum.LangPack.Lang;
import com.Torvald.Terrarum.MapDrawer.LightmapRenderer;
import com.Torvald.Terrarum.MapDrawer.MapDrawer;
import com.Torvald.Terrarum.Terrarum;
import com.Torvald.Terrarum.MapDrawer.MapCamera;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;

import java.util.Formatter;

/**
 * Created by minjaesong on 16-01-19.
 */
public class BasicDebugInfoWindow implements UICanvas {

    private static PlayerDebugger playerDbg;

    int width;
    int height;

    /**
     * Call AFTER player constuction!
     */
    public BasicDebugInfoWindow() {
        width = Terrarum.WIDTH;
        height = Terrarum.HEIGHT;
    }

    @Override
    public void render(GameContainer gc, Graphics g) {
        if (playerDbg == null) {
            playerDbg = new PlayerDebugger(Terrarum.game.getPlayer());
        }


        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        int mouseTileX = (int) ((MapCamera.getCameraX() + gc.getInput().getMouseX() / Terrarum.game.screenZoom)
                / MapDrawer.TILE_SIZE);
        int mouseTileY = (int) ((MapCamera.getCameraY() + gc.getInput().getMouseY() / Terrarum.game.screenZoom)
                / MapDrawer.TILE_SIZE);

        g.setColor(Color.white);

        Hitbox hitbox = playerDbg.hitbox();
        Hitbox nextHitbox = playerDbg.nextHitbox();

        printLine(g, 1, "posX : "
                + String.valueOf(hitbox.getPointedX())
                + " ("
                + String.valueOf((int) (hitbox.getPointedX() / MapDrawer.TILE_SIZE))
                + ")"
        );
        printLine(g, 2, "posY : "
                + String.valueOf(hitbox.getPointedY())
                + " ("
                + String.valueOf((int) (hitbox.getPointedY() / MapDrawer.TILE_SIZE))
                + ")"
        );
        printLine(g, 3, "veloX : " + String.valueOf(playerDbg.veloX()));
        printLine(g, 4, "veloY : " + String.valueOf(playerDbg.veloY()));
        printLine(g, 5, "grounded : " + String.valueOf(playerDbg.grounded()));
        printLine(g, 6, "noClip : " + String.valueOf(playerDbg.noClip()));
        printLine(g, 7, "mass : " + String.valueOf(playerDbg.mass()) + " [kg]");

        String lightVal;
        String mtX = String.valueOf(mouseTileX), mtY = String.valueOf(mouseTileY);
        try {
            char valRaw = LightmapRenderer.getValueFromMap(mouseTileX, mouseTileY);
            int rawR = LightmapRenderer.getRawR(valRaw);
            int rawG = LightmapRenderer.getRawG(valRaw);
            int rawB = LightmapRenderer.getRawB(valRaw);
            lightVal = String.valueOf((int)valRaw) + " ("
                    + String.valueOf(rawR) + " "
                    + String.valueOf(rawG) + " "
                    + String.valueOf(rawB) + ")";
        }
        catch (ArrayIndexOutOfBoundsException e) {
            lightVal = "out of bounds";
            mtX = "---";
            mtY = "---";
        }
        printLine(g, 8, "light at cursor : " + lightVal
        );

        String tileNo;
        try {
            tileNo = String.valueOf(Terrarum.game.map.getTileFromTerrain(mouseTileX, mouseTileY));
        }
        catch (ArrayIndexOutOfBoundsException e) {
            tileNo = "out of bounds";
        }
        printLine(g, 9, "tile : " + tileNo + " (" + mtX + ", " + mtY + ")");

        /**
         * Second column
         */

        int collisonFlag = playerDbg.eventMoving();
        printLineColumn(g, 2, 1, "Vsync : " + Terrarum.appgc.isVSyncRequested());
        printLineColumn(g, 2, 2, "Env colour temp : " + MapDrawer.getColTemp());

        /**
         * On screen
         */

        // Memory allocation
        long memInUse = Terrarum.game.memInUse;
        long totalVMMem = Terrarum.game.totalVMMem;

        g.setColor(new Color(0xFF7F00));
        g.drawString(
                Lang.get("DEV_MEMORY_SHORT_CAP")
                        + " : "
                        + formatter.format(
                        Lang.get("DEV_MEMORY_A_OF_B")
                        , memInUse
                        , totalVMMem
                )
                , Terrarum.WIDTH - 200, line(1)
        );

        // Hitbox
        float zoom = Terrarum.game.screenZoom;
        g.setColor(new Color(0x007f00));
        g.drawRect(hitbox.getHitboxStart().getX() * zoom
                        - MapCamera.getCameraX() * zoom
                , hitbox.getHitboxStart().getY() * zoom
                        - MapCamera.getCameraY() * zoom
                , hitbox.getWidth() * zoom
                , hitbox.getHeight() * zoom
        );
        // ...and its point
        g.fillRect(
                (hitbox.getPointedX() - 1) * zoom
                        - MapCamera.getCameraX() * zoom
                , (hitbox.getPointedY() - 1) * zoom
                        - MapCamera.getCameraY() * zoom
                , 3
                , 3
        );
        g.drawString(
                Lang.get("DEV_COLOUR_LEGEND_GREEN")
                        + " :  hitbox", Terrarum.WIDTH - 200
                , line(2)
        );

        // Next hitbox
        g.setColor(Color.blue);
        g.drawRect(nextHitbox.getHitboxStart().getX() * zoom
                        - MapCamera.getCameraX() * zoom
                , nextHitbox.getHitboxStart().getY() * zoom
                        - MapCamera.getCameraY() * zoom
                , nextHitbox.getWidth() * zoom
                , nextHitbox.getHeight() * zoom
        );
        // ...and its point
        g.fillRect(
                (nextHitbox.getPointedX() - 1) * zoom
                        - MapCamera.getCameraX() * zoom
                , (nextHitbox.getPointedY() - 1) * zoom
                        - MapCamera.getCameraY() * zoom
                , 3
                , 3
        );
        g.drawString(
                Lang.get("DEV_COLOUR_LEGEND_BLUE")
                        + " :  nextHitbox", Terrarum.WIDTH - 200
                , line(3)
        );
    }

    private static void printLine(Graphics g, int l, String s) {
        g.drawString(s, 20, line(l));
    }

    private static void printLineColumn(Graphics g, int col, int row, String s) {
        g.drawString(s, 20 + column(col), line(row));
    }

    private static int line(int i) {
        return i * 20;
    }

    private static int column(int i) {
        return (250 * (i - 1));
    }

    @Override
    public void update(GameContainer gc, int delta_t) {

    }

    @Override
    public void keyPressed(int key, char c) {

    }

    @Override
    public void keyReleased(int key, char c) {

    }

    @Override
    public void mouseMoved(int oldx, int oldy, int newx, int newy) {

    }

    @Override
    public void mouseDragged(int oldx, int oldy, int newx, int newy) {

    }

    @Override
    public void mousePressed(int button, int x, int y) {

    }

    @Override
    public void mouseReleased(int button, int x, int y) {

    }

    @Override
    public void mouseWheelMoved(int change) {

    }

    @Override
    public void controllerButtonPressed(int controller, int button) {

    }

    @Override
    public void controllerButtonReleased(int controller, int button) {

    }

    @Override
    public void processInput(Input input) {

    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

}
