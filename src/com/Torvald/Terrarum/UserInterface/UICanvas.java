package com.Torvald.Terrarum.UserInterface;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;

import java.util.LinkedList;

/**
 * Created by minjaesong on 15-12-31. <br>
 * <br>
 * Methods: <br>
 * &nbsp; &nbsp; &nbsp; &nbsp; UICanvas() <br>
 * &nbsp; &nbsp; &nbsp; &nbsp; update(GameContainer gc, int delta_t) <br>
 * &nbsp; &nbsp; &nbsp; &nbsp; render(GameContainer gc, Graphics g) <br>
 * &nbsp; &nbsp; &nbsp; &nbsp; keyPressed(int key, char c) <br>
 * &nbsp; &nbsp; &nbsp; &nbsp; keyReleased(int key, char c) <br>
 * &nbsp; &nbsp; &nbsp; &nbsp; mouseMoved(int oldx, int oldy, int newx, int newy) <br>
 * &nbsp; &nbsp; &nbsp; &nbsp; mouseDragged(int oldx, int oldy, int newx, int newy) <br>
 * &nbsp; &nbsp; &nbsp; &nbsp; mousePressed(int button, int x, int y) <br>
 * &nbsp; &nbsp; &nbsp; &nbsp; mouseReleased(int button, int x, int y) <br>
 * &nbsp; &nbsp; &nbsp; &nbsp; mouseWheelMoved(int change) <br>
 * &nbsp; &nbsp; &nbsp; &nbsp; controllerButtonPressed(int controller, int button) <br>
 * &nbsp; &nbsp; &nbsp; &nbsp; controllerButtonReleased(int controller, int button) <br>
 * &nbsp; &nbsp; &nbsp; &nbsp; processInput(Input input) <br>
 * <br>
 */
public interface UICanvas {

    int getWidth();

    int getHeight();

    void update(GameContainer gc, int delta_t);

    void render(GameContainer gc, Graphics g);

    void keyPressed(int key, char c);

    void keyReleased(int key, char c);

    void mouseMoved(int oldx, int oldy, int newx, int newy);

    void mouseDragged(int oldx, int oldy, int newx, int newy);

    void mousePressed(int button, int x, int y);

    void mouseReleased(int button, int x, int y);

    void mouseWheelMoved(int change);

    void controllerButtonPressed(int controller, int button);

    void controllerButtonReleased(int controller, int button);

    void processInput(Input input);

}
