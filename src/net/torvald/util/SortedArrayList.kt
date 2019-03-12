package net.torvald.util

import net.torvald.terrarum.lock
import java.util.concurrent.locks.ReentrantLock

/**
 * The modification of the arraylist that its element is always sorted.
 *
 * Created by minjaesong on 2019-03-12.
 */
class SortedArrayList<T: Comparable<T>>(initialSize: Int = 10) {

    private val arrayList = ArrayList<T>(initialSize)

    /**
     */
    fun add(elem: T) {
        // don't append-at-tail-and-sort; just insert at right index
        ReentrantLock().lock {
            var low = 0
            var high = arrayList.size

            while (low < high) {
                val mid = (low + high).ushr(1)

                if (arrayList[mid] > elem)
                    high = mid
                else
                    low = mid + 1
            }

            arrayList.add(low, elem)
        }
    }

    val size: Int
        get() = arrayList.size

    fun removeAt(index: Int) = arrayList.removeAt(index)
    fun remove(element: T) = arrayList.remove(element)
    fun removeLast() = arrayList.removeAt(arrayList.size)

    operator fun get(index: Int) = arrayList[index]

    fun iterator() = arrayList.iterator()
    fun forEach(action: (T) -> Unit) = arrayList.forEach(action)
    fun forEachIndexed(action: (Int, T) -> Unit) = arrayList.forEachIndexed(action)
    //fun <R> map(transformation: (T) -> R) = arrayList.map(transformation)

    /**
     * Select one unsorted element from the array and put it onto the sorted spot.
     *
     * The list must be fully sorted except for that one "renegade", otherwise the operation is undefined behaviour.
     */
    private fun sortThisRenegade(index: Int) {
        if (
                (index == arrayList.lastIndex && arrayList[index - 1] <= arrayList[index]) ||
                (index == 0 && arrayList[index] <= arrayList[index + 1]) ||
                (arrayList[index - 1] <= arrayList[index] && arrayList[index] <= arrayList[index + 1])
        ) return

        // modified binary search
        ReentrantLock().lock {
            val renegade = arrayList.removeAt(index)

            var low = 0
            var high = arrayList.size

            while (low < high) {
                val mid = (low + high).ushr(1)

                if (arrayList[mid] > renegade)
                    high = mid
                else
                    low = mid + 1
            }

            arrayList.add(low, renegade)
        }
    }

    /**
     * Does NOT create copies!
     */
    fun toArrayList() = arrayList
}