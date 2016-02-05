package com.Torvald.Terrarum.GameControl;

import com.Torvald.Terrarum.Actors.Controllable;
import com.Torvald.Terrarum.Actors.Player;
import com.Torvald.Terrarum.Game;
import com.Torvald.Terrarum.UserInterface.UIHandler;
import org.newdawn.slick.Input;

/**
 * Created by minjaesong on 15-12-31.
 */
public class GameController {

    private static KeyMap keyMap;

    private static Player player;
    private static Controllable playerVehicle;

    public GameController() {
        player = Game.getPlayer();
    }

    public static void setKeyMap(KeyMap map) {
        keyMap = map;
    }

    public static void processInput(Input input) {
        if (!Game.consoleHandler.isTakingControl()) {
            if (playerVehicle != null) {
                playerVehicle.processInput(input);
            }

            player.processInput(input);

            for (UIHandler ui : Game.uiContainer) {
                ui.processInput(input);
            }
        }
        else {
            Game.consoleHandler.processInput(input);
        }
    }

    public static void keyPressed(int key, char c) {
        if (keyPressedByCode(key, EnumKeyFunc.UI_CONSOLE)) {
            Game.consoleHandler.toggleOpening();
        }
        else if (keyPressedByCode(key, EnumKeyFunc.UI_BASIC_INFO)) {
            Game.debugWindow.toggleOpening();
        }



        if (!Game.consoleHandler.isTakingControl()) {
            if (playerVehicle != null) {
                playerVehicle.keyPressed(key, c);
            }

            player.keyPressed(key, c);
        }
        else {
            Game.consoleHandler.keyPressed(key, c);
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
