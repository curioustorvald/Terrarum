package com.Torvald.Terrarum.UserInterface;

import com.Torvald.Terrarum.Terrarum;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.geom.RoundedRectangle;

/**
 * Created by minjaesong on 16-01-23.
 */
public class Bulletin implements UICanvas {

    int width = 400;
    int height;

    int visibleTime = 5000;
    int showupTimeConuter = 0;

    boolean isShowing = false;
    String[] message;

    Color uiColour = new Color(0x90000000);

    final int FRAME_SIZE = 5;

    private RoundedRectangle uiBox = new RoundedRectangle(
            width
            , Terrarum.HEIGHT
            , FRAME_SIZE
            , 2
            , RoundedRectangle.TOP_LEFT + RoundedRectangle.TOP_RIGHT
    );

    @Override
    public void update(GameContainer gc, int delta_t) {
        if (showupTimeConuter >= visibleTime) {
            isShowing = false;
        }

        if (isShowing) {
            showupTimeConuter += delta_t;
        }
    }

    @Override
    public void render(GameContainer gc, Graphics g) {
        /*if (isShowing) {
            int lineHeight = Terrarum.gameFontWhite.getLineHeight();

            g.setColor(uiColour);
            g.fillRect(0
                    , getHeight() - message.length * lineHeight
                            - 10
                    , width
                    , getHeight()
            );

            for (int i = 0; i < message.length; i++) {
                g.drawString(message[i]
                        , 5
                        , getHeight() - message.length * lineHeight
                                + 5
                                + (i * lineHeight)
                );
            }
        }*/
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
        return Terrarum.HEIGHT;
    }

    public void sendBulletin(String[] message) {
        isShowing = true;
        this.message = message;
    }
}
