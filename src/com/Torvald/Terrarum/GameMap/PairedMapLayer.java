package com.Torvald.Terrarum.GameMap;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * Created by minjaesong on 16-02-15.
 */
public class PairedMapLayer implements Iterable<Byte> {

    /**
     * 0b_xxxx_yyyy, x for lower index, y for higher index
     *
     * e.g.
     *
     *   0110 1101 is interpreted as
     *   6 for tile 0, 13 for tile 1.
     */
    byte[][] dataPair;

    public int width;
    public int height;

    public static transient final int RANGE = 16;

    public PairedMapLayer(int width, int height) {
        this.width = width / 2;
        this.height = height;

        dataPair = new byte[height][width / 2];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width / 2; j++) {
                dataPair[i][j] = 0;
            }
        }
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     * Note: this iterator will return combined damage, that is 0bxxxx_yyyy as whole.
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

                return dataPair[y][x];
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
     * Note: this iterator will return combined damage, that is 0bxxxx_yyyy as whole.
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
     * Creates a {@link Spliterator} over the elements described by this
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
    public Spliterator<Byte> spliterator() {
        throw new UnsupportedOperationException();
    }

    int getData(int x, int y) {
        if ((x & 0x1) == 0)
            // higher four bits for i = 0, 2, 4, ...
            return (dataPair[y][x / 2] & 0xF0) >>> 4;
        else
            // lower four bits for i = 1, 3, 5, ...
            return dataPair[y][x / 2] & 0x0F;
    }

    void setData(int x, int y, int data) {
        if (data < 0 || data >= 16) throw new IllegalArgumentException("[PairedMapLayer] " + data + ": invalid data value.");
        if ((x & 0x1) == 0)
            // higher four bits for i = 0, 2, 4, ...
            dataPair[y][x / 2] = (byte) (dataPair[y][x / 2] & 0x0F | (data & 0xF) << 4);
        else
            // lower four bits for i = 1, 3, 5, ...
            dataPair[y][x / 2] = (byte) (dataPair[y][x / 2] & 0xF0 | (data & 0xF));
    }
}
