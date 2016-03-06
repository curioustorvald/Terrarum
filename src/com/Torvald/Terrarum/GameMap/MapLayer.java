package com.Torvald.Terrarum.GameMap;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * Created by minjaesong on 16-01-17.
 */
public class MapLayer implements Iterable<Byte> {

    byte[][] data;

    public int width;
    public int height;

    public static final int RANGE = 256;

    public MapLayer(int width, int height) {
        this.width = width;
        this.height = height;

        data = new byte[height][width];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                data[i][j] = 0;
            }
        }
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<Byte> iterator() {
        return new Iterator<Byte>() {

            private int iteratorCount = 0;

            @Override
            public boolean hasNext() {
                return iteratorCount < width * height;
            }

            @Override
            public Byte next() {
                int y = iteratorCount / width;
                int x = iteratorCount % width;
                // advance counter
                iteratorCount += 1;

                return data[y][x];
            }
        };
    }

    /**
     * Performs the given action for each element of the {@code Iterable}
     * until all elements have been processed or the action throws an
     * exception.  Unless otherwise specified by the implementing class,
     * actions are performed in the order of iteration (if an iteration order
     * is specified).  Exceptions thrown by the action are relayed to the
     * caller.
     *
     * @param action The action to be performed for each element
     * @throws NullPointerException if the specified action is null
     * @implSpec <p>The default implementation behaves as if:
     * <pre>{@code
     *     for (T t : this)
     *         action.accept(t);
     * }</pre>
     * @since 1.8
     */
    @Override
    public void forEach(Consumer action) {
        for (Byte b : this) {
            action.accept(b);
        }
    }

    /**
     * Creates a {@link java.util.Spliterator} over the elements described by this
     * {@code Iterable}.
     *
     * @return a {@code Spliterator} over the elements described by this
     * {@code Iterable}.
     * @implSpec The default implementation creates an
     * <em><a href="Spliterator.html#binding">early-binding</a></em>
     * spliterator from the iterable's {@code Iterator}.  The spliterator
     * inherits the <em>fail-fast</em> properties of the iterable's iterator.
     * @implNote The default implementation should usually be overridden.  The
     * spliterator returned by the default implementation has poor splitting
     * capabilities, is unsized, and does not report any spliterator
     * characteristics. Implementing classes can nearly always provide a
     * better implementation.
     * @since 1.8
     */
    @Override
    public Spliterator spliterator() {
        throw new UnsupportedOperationException();
    }

    int getTile(int x, int y) {
        return uint8ToInt32(data[y][x]);
    }

    void setTile(int x, int y, byte tile) {
        data[y][x] = tile;
    }

    private int uint8ToInt32(byte x) {
        int ret;
        if ((x & 0b1000_0000) != 0) {
            ret = (x & 0b0111_1111) | (x & 0b1000_0000);
        }
        else {
            ret = x;
        }
        return ret;
    }
}

