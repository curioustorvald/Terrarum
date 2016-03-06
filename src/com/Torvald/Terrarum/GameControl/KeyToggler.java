package com.Torvald.Terrarum.GameControl;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Input;

public class KeyToggler {

    private static boolean[] currentState = new boolean[256];
    private static boolean[] isPressed = new boolean[256];
    private static boolean[] isToggled = new boolean[256];

    public static boolean isOn(int key){
        return currentState[key];
    }

    public static void update(Input input){
        for (int i = 0; i < 256; i++) {
            if (input.isKeyDown(i)) {
                isPressed[i] = true;
            } else {
                isPressed[i] = false;
            }
        }

        for (int i = 0; i < 256; i++){
            if (isPressed[i] && !currentState[i] && !isToggled[i]){
                currentState[i] = true;
                isToggled[i] = true;
            }
            else if(isPressed[i] && currentState[i] && !isToggled[i]){
                currentState[i] = false;
                isToggled[i] = true;
            }

            if (!isPressed[i] && isToggled[i]){
                isToggled[i] = false;
            }
        }
    }

    public static void forceSet(int key, boolean b) {
        currentState[key] = b;
        isToggled[key] = true;
    }
	
}
