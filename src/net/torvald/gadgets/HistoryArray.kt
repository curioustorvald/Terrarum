package net.torvald.gadgets

import java.util.*
import java.util.function.Consumer

/**
 * Simple ArrayList wrapper that acts as history keeper. You can append any data but cannot delete.
 * Created by minjaesong on 16-07-13.
 */
class HistoryArray<T>(val historyMax: Int) {

    val history = ArrayList<T?>(Math.min(historyMax, 256)) // 256: arbitrary-set upper bound

    fun add(value: T) {
        if (history.size == 0) {
            history.add(value)
            return
        }
        // push existing values to back 1 index
        else {
            for (i in history.size - 1 downTo 0) {
                // if history.size is smaller than historyMax, make room by appending
                if (i == history.size - 1 && i < historyMax - 1)
                    history.add(history[i])
                // actually move if we have some room
                else if (i < historyMax - 1)
                    history[i + 1] = history[i]
            }
        }
        // add new value to the room
        history[0] = value
    }

    /**
     * Get certain index from history. NOTE: index 0 means latest!
     */
    operator fun get(index: Int): T? =
            if (index >= history.size) null
            else history[index]

    /**
     * Iterate from latest to oldest
     */
    fun iterator() = history.iterator()

    /**
     * Iterate from latest to oldest
     */
    fun forEach(action: Consumer<T?>) = history.forEach(action)

    val latest: T?
        get() = this[0]

    val oldest: T?
        get() = this[history.size - 1]

}