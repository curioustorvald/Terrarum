package com.Torvald.Terrarum.UserInterface;

import com.Torvald.Terrarum.Terrarum;
import org.jetbrains.annotations.Nullable;
import org.newdawn.slick.*;
import org.newdawn.slick.geom.RoundedRectangle;

/**
 * Created by minjaesong on 16-01-23.
 */
public class Notification implements UICanvas {

    int width;
    int height;

    int visibleTime;
    int showupTimeConuter = 0;

    boolean isShowing = false;
    String[] message;

    Message msgUI;

    public Notification() throws SlickException {
        width = 400;
        msgUI = new Message(width, true);
        height = msgUI.getHeight();
        visibleTime = Terrarum.getConfigInt("notificationshowuptime");
    }

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
        if (isShowing) {
            msgUI.render(gc, g);
        }
    }

    @Override
    public void processInput(Input input) {

    }

    @Nullable
    @Override
    public Integer getWidth() {
        return width;
    }

    @Override
    public void setWidth(@Nullable Integer integer) {
        width = integer;
    }

    @Nullable
    @Override
    public Integer getHeight() {
        return height;
    }

    @Override
    public void setHeight(@Nullable Integer integer) {
        height = integer;
    }

    public void sendNotification(String[] message) {
        isShowing = true;
        this.message = message;
        showupTimeConuter = 0;
        msgUI.setMessage(this.message);
    }
}
