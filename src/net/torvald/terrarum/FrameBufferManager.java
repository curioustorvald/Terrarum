package net.torvald.terrarum;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;

import java.util.Stack;

/**
 * Nested FBOs are just not a thing in GL!
 *
 * Created by minjaesong on 2018-07-03.
 *
 * @link https://stackoverflow.com/questions/25471727/libgdx-nested-framebuffer
 */
public class FrameBufferManager {
    private static Stack<FrameBuffer> stack = new Stack<FrameBuffer>();

    public static void begin(FrameBuffer buffer) {
        if (!stack.isEmpty()) {
            stack.peek().end();
        }
        stack.push(buffer).begin();
    }

    public static void end() {
        stack.pop().end();
        if (!stack.isEmpty()) {
            stack.peek().begin();
        }
    }

    public static FrameBuffer peek() {
        return (stack.size() > 0) ? stack.peek() : null;
    }
}
