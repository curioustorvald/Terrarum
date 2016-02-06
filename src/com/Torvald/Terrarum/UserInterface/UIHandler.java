package com.Torvald.Terrarum.UserInterface;

import com.Torvald.Terrarum.MapDrawer.MapCamera;
import com.Torvald.Terrarum.Terrarum;
import com.Torvald.Terrarum.Game;
import com.jme3.math.FastMath;
import com.sun.istack.internal.Nullable;
import org.newdawn.slick.*;

/**
 * Created by minjaesong on 15-12-31.
 */
public class UIHandler {

    private UICanvas ui;

    // X/Y Position to the game window.
    private int posX;
    private int posY;
    private boolean visible = false;

    private boolean alwaysVisible = false;

    private Graphics UIGraphicInstance;
    private Image UIDrawnCanvas;

    private boolean opening = false;
    private boolean closing = false;

    /**
     * Construct new UIHandler with given UI attached.
     * Invisible in default.
     * @param ui
     * @throws SlickException
     */
    public UIHandler(UICanvas ui) throws SlickException{
        System.out.println(("[UIHandler] Creating UI '" + ui.getClass().getTypeName()) + "'");

        this.ui = ui;

        UIDrawnCanvas = new Image(
                FastMath.nearestPowerOfTwo(ui.getWidth())
                , FastMath.nearestPowerOfTwo(ui.getHeight())
        );

        UIGraphicInstance = UIDrawnCanvas.getGraphics();

        visible = false;
    }



    public void update(GameContainer gc, int delta_t){
        if (visible || alwaysVisible) {
            ui.update(gc, delta_t);
        }
    }

    public void render(GameContainer gc, Graphics gameGraphicInstance) {
        if (visible || alwaysVisible) {
            UIGraphicInstance.clear();
            UIGraphicInstance.setFont(Terrarum.gameFontWhite);

            ui.render(gc, UIGraphicInstance);
            gameGraphicInstance.drawImage(UIDrawnCanvas
                    // compensate for screenZoom AND camera translation
                    // (see Game.render -> g.translate())
                    , posX + MapCamera.getCameraX() * Terrarum.game.screenZoom
                    , posY + MapCamera.getCameraY() * Terrarum.game.screenZoom
            );
        }
    }

    public void setPosition(int x, int y){
        posX = x;
        posY = y;
    }

    public  void setVisibility(boolean b){
        if (alwaysVisible) {
            throw new RuntimeException("Tried to 'set visibility of' constant UI");
        }
        visible = b;
    }



    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }

    public boolean isVisible() {
        if (alwaysVisible) {
            return true;
        } else {
            return visible;
        }
    }

    public void setAsAlwaysVisible() {
        alwaysVisible = true;
        visible = true;
    }



    public void setAsOpening(){
        if (alwaysVisible) {
            throw new RuntimeException("Tried to 'open' constant UI");
        }
        visible = true;
    }

    public void setAsClosing(){
        if (alwaysVisible) {
            throw new RuntimeException("Tried to 'close' constant UI");
        }
        visible = false;
    }

    public void toggleOpening() {
        if (alwaysVisible) {
            throw new RuntimeException("Tried to 'toggle opening of' constant UI");
        }
        if (visible) {
            if (!closing) {
                setAsClosing();
            }
        }
        else {
            if (!opening) {
                setAsOpening();
            }
        }
    }

    public void processInput(Input input) {
        if (visible) { ui.processInput(input); }
    }

    public void keyPressed(int key, char c) {
        if (visible) { ui.keyPressed(key, c); }
    }

    public void keyReleased(int key, char c) {
        if (visible) { ui.keyReleased(key, c); }
    }

    public void mouseMoved(int oldx, int oldy, int newx, int newy) {
        if (visible) { ui.mouseMoved(oldx, oldy, newx, newy); }
    }

    public void mouseDragged(int oldx, int oldy, int newx, int newy) {
        if (visible) { ui.mouseDragged(oldx, oldy, newx, newy); }
    }

    public void mousePressed(int button, int x, int y) {
        if (visible) { ui.mousePressed(button, x, y); }
    }

    public void mouseReleased(int button, int x, int y) {
        if (visible) { ui.mouseReleased(button, x, y); }
    }

    public void mouseWheelMoved(int change) {
        if (visible) { ui.mouseWheelMoved(change); }
    }

    public void controllerButtonPressed(int controller, int button) {
        if (visible) { ui.controllerButtonPressed(controller, button); }
    }

    public void controllerButtonReleased(int controller, int button) {
        if (visible) { ui.controllerButtonReleased(controller, button); }
    }

    public boolean isTakingControl() {
        if (alwaysVisible) {
            return false; // constant UI can't take control
        }
        return (visible && !opening);
    }

    public UICanvas getUI() {
        return ui;
    }
}
