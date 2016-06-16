package net.torvald.terrarum.concurrent

import net.torvald.terrarum.Terrarum
import java.util.*

/**
 * Created by minjaesong on 16-05-25.
 */
object ThreadPool {
    val POOL_SIZE = Terrarum.CORES + 1

    private val pool: Array<Thread?> = Array(POOL_SIZE, { null })

    /**
     * Map array of Runnable objects to thread pool.
     * @param prefix : will name each thread as "Foo-1"
     * @param runnables : vararg
     */
    fun mapAll(prefix: String, runnables: Array<Runnable>) {
        if (runnables.size != POOL_SIZE)
            throw RuntimeException("Thread pool argument size mismatch. If you have four cores, you must use four runnables.")

        for (i in 0..runnables.size)
            pool[i] = Thread(runnables[i], "$prefix-$i")
    }

    /**
     * Map Runnable object to certain index of the thread pool.
     * @param index of the runnable
     * @param runnable
     * @param prefix Will name each thread like "Foo-1", "Foo-2", etc.
     */
    fun map(index: Int, runnable: Runnable, prefix: String) {
        pool[index] = Thread(runnable, "$prefix-$index")
    }

    /**
     * Fill the thread pool with NULL value.
     */
    fun purge() {
        for (i in 0..POOL_SIZE)
            pool[i] = null
    }

    /**
     * Start all thread in the pool. If the thread in the pool is NULL, it will simply ignored.
     */
    fun startAll() {
        pool.forEach { it?.start() }
    }
}