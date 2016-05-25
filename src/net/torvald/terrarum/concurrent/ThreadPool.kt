package net.torvald.terrarum.concurrent

import net.torvald.terrarum.Terrarum
import java.util.*

/**
 * Created by minjaesong on 16-05-25.
 */
object ThreadPool {
    private val pool = Array<Thread>(Terrarum.CORES, { Thread() })
    val POOL_SIZE = Terrarum.CORES

    /**
     * @param prefix : will name each thread as "Foo-1"
     * @param runnables : vararg
     */
    fun mapAll(prefix: String, vararg runnables: Runnable) {
        if (runnables.size != Terrarum.CORES)
            throw RuntimeException("Thread pool argument size mismatch. If you have four cores, you must use four runnables.")

        for (i in 0..runnables.size)
            pool[i] = Thread(runnables[i], "$prefix-$i")
    }

    /**
     * @param index of the runnable
     * @param runnable
     * @param prefix Will name each thread like "Foo-1", "Foo-2", etc.
     */
    fun map(index: Int, runnable: Runnable, prefix: String) {
        pool[index] = Thread(runnable, "$prefix-$index")
    }

    fun startAll() {
        pool.forEach { it.start() }
    }
}