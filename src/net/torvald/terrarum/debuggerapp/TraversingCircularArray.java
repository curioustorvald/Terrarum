package net.torvald.terrarum.debuggerapp;

/**
 * To be used as undo/redo buffer. Which means, current position can go backward and rewrite objects ahead.
 *
 * The most recent item will be same as the current edit.
 *
 * Created by minjaesong on 2019-01-09.
 */
public class TraversingCircularArray<T> {

    private T[] buf;
    private int size;

    public TraversingCircularArray(int size) {
        buf = (T[]) new Object[size]; // create array of nulls
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    private int tail = 0;
    private int head = -1;

    private int unreliableAddCount = 0;

    /**
     * Adds new item.
     * @param item
     */
    public void undoNew(T item) {
        if (unreliableAddCount <= size) unreliableAddCount += 1;

        head = (head + 1) % size;
        if (unreliableAddCount > size) {
            tail = (tail + 1) % size;
        }

        buf[head] = item; // overwrites oldest item when eligible
    }

    /**
     * Pops existing item. This function is analogous to rewinding the tape. Existing data will be untouched.
     * @return
     */
    public T redo() {
        tail -= 1;
        return buf[tail];
    }

    /**
     * Undo the redo. This function is analogous to fast-forwarding the tape, without touching already recorded data.
     * If head of the tape reached, will do nothing.
     */
    public T undoAgain() {
        return null;
    }
}
