package com.Torvald.Terrarum.GameControl;

import com.Torvald.Terrarum.Actors.Controllable;
import com.Torvald.Terrarum.Actors.Player;
import com.Torvald.Terrarum.MapDrawer.MapCamera;
import com.Torvald.Terrarum.MapDrawer.MapDrawer;
import com.Torvald.Terrarum.Terrarum;
import com.Torvald.Terrarum.TileProperties.TileNameCode;
import com.Torvald.Terrarum.TileProperties.TilePropCodex;
import com.Torvald.Terrarum.UserInterface.UIHandler;
import org.newdawn.slick.Input;

/**
 * Created by minjaesong on 15-12-31.
 */
public class GameController {

    private static KeyMap keyMap;

    public GameController() {

    }

    public static void setKeyMap(KeyMap map) {
        keyMap = map;
    }

    public static void processInput(Input input) {
        int mouseTileX = (int) ((MapCamera.getCameraX() + input.getMouseX() / Terrarum.game.screenZoom)
                / MapDrawer.TILE_SIZE);
        int mouseTileY = (int) ((MapCamera.getCameraY() + input.getMouseY() / Terrarum.game.screenZoom)
                / MapDrawer.TILE_SIZE);


        KeyToggler.update(input);


        if (!Terrarum.game.consoleHandler.isTakingControl()) {
            if (Terrarum.game.getPlayer().vehicleRiding != null) {
                Terrarum.game.getPlayer().vehicleRiding.processInput(input);
            }

            Terrarum.game.getPlayer().processInput(input);

            for (UIHandler ui : Terrarum.game.uiContainer) {
                ui.processInput(input);
            }
        }
        else {
            Terrarum.game.consoleHandler.processInput(input);
        }


        if (input.isMouseButtonDown(Input.MOUSE_LEFT_BUTTON)) {
            // test tile remove
            try {
                Terrarum.game.map.setTileTerrain(mouseTileX, mouseTileY, TileNameCode.AIR);
                // Terrarum.game.map.setTileWall(mouseTileX, mouseTileY, TileNameCode.AIR);
            }
            catch (ArrayIndexOutOfBoundsException e) {
            }
        }
        else if (input.isMouseButtonDown(Input.MOUSE_RIGHT_BUTTON)) {
            // test tile place
            try {
                Terrarum.game.map.setTileTerrain(mouseTileX, mouseTileY
                        , Terrarum.game.getPlayer().getActorValue().getAsInt("selectedtile"));
            }
            catch (ArrayIndexOutOfBoundsException e) {
            }
        }
    }

    public static void keyPressed(int key, char c) {
        if (keyPressedByCode(key, EnumKeyFunc.UI_CONSOLE)) {
            Terrarum.game.consoleHandler.toggleOpening();
        }
        else if (keyPressedByCode(key, EnumKeyFunc.UI_BASIC_INFO)) {
            Terrarum.game.debugWindow.toggleOpening();
        }



        if (!Terrarum.game.consoleHandler.isTakingControl()) {
            if (Terrarum.game.getPlayer().vehicleRiding != null) {
                Terrarum.game.getPlayer().vehicleRiding.keyPressed(key, c);
            }

            Terrarum.game.getPlayer().keyPressed(key, c);
        }
        else {
            Terrarum.game.consoleHandler.keyPressed(key, c);
        }

        //System.out.println(String.valueOf(key) + ", " + String.valueOf(c));
    }

    public static void keyReleased(int key, char c) {

    }

    public static void mouseMoved(int oldx, int oldy, int newx, int newy) {

    }

    public static void mouseDragged(int oldx, int oldy, int newx, int newy) {

    }

    public static void mousePressed(int button, int x, int y) {

    }

    public static void mouseReleased(int button, int x, int y) {

    }

    public static void mouseWheelMoved(int change) {

    }

    public static void controllerButtonPressed(int controller, int button) {

    }

    public static void controllerButtonReleased(int controller, int button) {

    }

    private static boolean keyPressedByCode(int key, EnumKeyFunc fn) {
        return (KeyMap.getKeyCode(fn) == key);
    }

}
