package com.Torvald.Terrarum.UserInterface;

import com.Torvald.ImageFont.GameFontBlack;
import com.Torvald.ImageFont.GameFontWhite;
import org.newdawn.slick.*;

/**
 * Created by minjaesong on 16-01-27.
 */
public class Message implements UICanvas {

    private Image segmentLeft, segmentRight, segmentBody;

    private String[] messagesList;
    private int messagesShowingIndex = 0;

    private int width;
    private int height;
    private int messageWindowRadius;

    private Font uiFont;
    private final int GLYPH_HEIGHT = 20;

    public Message(int width, boolean isBlackVariant) throws SlickException {
        if (!isBlackVariant) {
            segmentLeft = new Image("./res/graphics/gui/message_twoline_white_left.png");
            segmentRight = new Image("./res/graphics/gui/message_twoline_white_right.png");
            segmentBody = new Image("./res/graphics/gui/message_twoline_white_body.png");
            uiFont = new GameFontBlack();
        }
        else {
            segmentLeft = new Image("./res/graphics/gui/message_twoline_black_left.png");
            segmentRight = new Image("./res/graphics/gui/message_twoline_black_right.png");
            segmentBody = new Image("./res/graphics/gui/message_twoline_black_body.png");
            uiFont = new GameFontWhite();
        }

        this.width = width;
        height = segmentLeft.getHeight();
        messageWindowRadius = segmentLeft.getWidth();
    }

    public void setMessage(String[] messagesList) {
        this.messagesList = messagesList;
    }

    @Override
    public void update(GameContainer gc, int delta_t) {

    }

    @Override
    public void render(GameContainer gc, Graphics g) {
        g.drawImage(segmentLeft, 0, 0);
        Image scaledSegCentre = segmentBody.getScaledCopy(
                width - (segmentRight.getWidth() + segmentLeft.getWidth())
                , segmentLeft.getHeight()
        );
        g.drawImage(scaledSegCentre, segmentLeft.getWidth(), 0);
        g.drawImage(segmentRight, width - segmentRight.getWidth(), 0);

        g.setFont(uiFont);
        g.setDrawMode(Graphics.MODE_NORMAL);
        for (int i = messagesShowingIndex; i < messagesShowingIndex + 2; i++) {
            g.drawString(messagesList[i]
                    , messageWindowRadius + 4
                    , messageWindowRadius + (GLYPH_HEIGHT * (i - messagesShowingIndex))
            );
        }
        g.setDrawMode(Graphics.MODE_NORMAL);
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
