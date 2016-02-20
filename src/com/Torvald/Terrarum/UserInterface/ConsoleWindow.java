package com.Torvald.Terrarum.UserInterface;

import com.Torvald.Terrarum.LangPack.Lang;
import com.Torvald.Terrarum.Terrarum;
import com.Torvald.Terrarum.ConsoleCommand.CommandInterpreter;
import com.Torvald.Terrarum.GameControl.Key;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;

/**
 * Created by minjaesong on 15-12-31.
 */
public class ConsoleWindow implements UICanvas {

    Color UIColour = new Color(0x90000000);

    private StringBuffer commandInputPool;
    private String prevCommand;

    private int inputCursorPos;

    private final int MESSAGES_MAX = 5000;
    private String[] messages;
    private int messageDisplayPos;
    private int messagesCount;

    private final int LINE_HEIGHT = 20;
    private final int MESSAGES_DISPLAY_COUNT = 9;

    int width;
    int height;

    public ConsoleWindow() {
        super();
        reset();
    }

    @Override
    public void update(GameContainer gc, int delta_t) {

    }

    @Override
    public void render(GameContainer gc, Graphics g) {
        g.setColor(UIColour);
        g.fillRect(0, 0, width, height);
        g.fillRect(0, 0, width, LINE_HEIGHT);

        String input = commandInputPool.toString();
        int inputDrawWidth = g.getFont().getWidth(input);
        int inputDrawHeight = g.getFont().getLineHeight();

        g.setColor(Color.white);
        g.drawString(input, 1, 0);
        g.fillRect(inputDrawWidth, 0, 2, inputDrawHeight);

        for (int i = 0; i < MESSAGES_DISPLAY_COUNT; i++) {
            String message = messages[messageDisplayPos + i];
            if (message != null) {
                g.drawString(message, 1, LINE_HEIGHT * (i + 1));
            }
        }
    }


    
    public void keyPressed(int key, char c) {
        // execute
        if (key == Key.RET && commandInputPool.length() > 0) {
            prevCommand = commandInputPool.toString();
            executeCommand();
            commandInputPool = new StringBuffer();
        }
        // backspace
        else if (key == Key.BKSP && commandInputPool.length() > 0) {
            commandInputPool.deleteCharAt(commandInputPool.length() - 1);
        }
        // get input
        else if ((key >= 2 && key <= 13)
                || (key >= 16 && key <= 27)
                || (key >= 30 && key <= 40)
                || (key >= 44 && key <= 53)
                || (commandInputPool.length() > 0 && key == 57)){
            commandInputPool.append(c);
            inputCursorPos += 1;
        }
        // prev command
        else if (key == Key.UP) {
            commandInputPool = new StringBuffer();
            commandInputPool.append(prevCommand);
        }
        // scroll up
        else if (key == Key.PGUP) {
            setDisplayPos(-MESSAGES_DISPLAY_COUNT + 1);
        }
        // scroll down
        else if (key == Key.PGDN) {
            setDisplayPos(MESSAGES_DISPLAY_COUNT - 1);
        }
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

    private void executeCommand() {
        sendMessage("> " + commandInputPool.toString());
        CommandInterpreter.execute(commandInputPool.toString());
    }

    public void sendMessage(String msg) {
        messages[messagesCount] = new String(msg);
        messagesCount += 1;
        if (messagesCount > MESSAGES_DISPLAY_COUNT) {
            messageDisplayPos = messagesCount - MESSAGES_DISPLAY_COUNT;
        }
    }

    private void setDisplayPos(int change) {
        int lowLim = 0;
        int highLim = (messagesCount <= MESSAGES_DISPLAY_COUNT)
                ? 0
                : messagesCount - MESSAGES_DISPLAY_COUNT;
        int newVal = messageDisplayPos + change;

        if (newVal < lowLim) {
            messageDisplayPos = lowLim;
        }
        else if (newVal > highLim) {
            messageDisplayPos = highLim;
        }
        else {
            messageDisplayPos = newVal;
        }

    }

    public void reset() {
        width = Terrarum.WIDTH;
        height = 200;

        messages = new String[MESSAGES_MAX];
        messageDisplayPos = 0;
        messagesCount = 0;
        inputCursorPos = 0;
        prevCommand = "";
        commandInputPool = new StringBuffer();

        if (Terrarum.game.auth.b()) sendMessage(Lang.get("DEV_MESSAGE_CONSOLE_CODEX"));
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
