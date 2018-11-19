package net.torvald.terrarum.concurrent

import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 2016-05-25.
 */
object ThreadParallel {
    val threads = Terrarum.THREADS // modify this to your taste

    private val pool: Array<Thread?> = Array(threads, { null })

    /**
     * Map Runnable object to certain index of the thread pool.
     * @param index of the runnable
     * @param runnable
     * @param prefix Will name each thread like "Foo-1", "Foo-2", etc.
     */
    fun map(index: Int, prefix: String, runnable: Runnable) {
        pool[index] = Thread(runnable, "$prefix-$index")
    }

    /**
     * @param runFunc A function that takes an int input (the index), and returns nothing
     */
    fun map(index: Int, prefix: String, runFunc: (Int) -> Unit) {
        val runnable = object : Runnable {
            override fun run() {
                runFunc(index)
            }
        }

        map(index, prefix, runnable)
    }

    /**
     * Start all thread in the pool. If the thread in the pool is NULL, it will simply ignored.
     */
    fun startAll() {
        pool.forEach { it?.start() }
    }

    /**
     * Start all thread in the pool and wait for them to all die. If the thread in the pool is NULL, it will simply ignored.
     */
    fun startAllWaitForDie() {
        pool.forEach { it?.start() }
        pool.forEach { it?.join() }
    }

    /**
     * Primitive locking
     */
    fun allFinished(): Boolean {
        pool.forEach { if (it?.state != Thread.State.TERMINATED) return false }
        return true
    }
}

object ParallelUtils {
    fun <T, R> Iterable<T>.parallelMap(transform: (T) -> R): List<R> {
        val tasks = this.sliceEvenly(ThreadParallel.threads)
        val destination = Array(ThreadParallel.threads) { ArrayList<R>() }
        tasks.forEachIndexed { index, list ->
            ThreadParallel.map(index, "ParallelUtils.parallelMap@${this.javaClass.canonicalName}") {
                for (item in list)
                    destination[index].add(transform(item as T))
            }
        }

        ThreadParallel.startAllWaitForDie()

        return destination.flatten()
    }

    /**
     * Shallow flat of the array
     */
    fun <T> Array<out Iterable<T>>.flatten(): List<T> {
        val al = ArrayList<T>()
        this.forEach { it.forEach { al.add(it) } }
        return al
    }

    /**
     * Shallow flat of the iterable
     */
    fun <T> Iterable<out Iterable<T>>.flatten(): List<T> {
        val al = ArrayList<T>()
        this.forEach { it.forEach { al.add(it) } }
        return al
    }

    /**
     * Shallow flat of the array
     */
    fun <T> Array<out Array<T>>.flatten(): List<T> {
        val al = ArrayList<T>()
        this.forEach { it.forEach { al.add(it) } }
        return al
    }

    fun Iterable<*>.sliceEvenly(slices: Int): List<List<*>> = this.toList().sliceEvenly(slices)

    fun List<*>.sliceEvenly(slices: Int): List<List<*>> {
        return (0 until slices).map {
            this.subList(
                    this.size.toFloat().div(slices).times(it).roundInt(),
                    this.size.toFloat().div(slices).times(it + 1).roundInt()
            )
        }
    }

    fun Array<*>.sliceEvenly(slices: Int): List<Array<*>> {
        return (0 until slices).map {
            this.sliceArray(
                    this.size.toFloat().div(slices).times(it).roundInt() until
                            this.size.toFloat().div(slices).times(it + 1).roundInt()
            )
        }
    }

    fun IntRange.sliceEvenly(slices: Int): List<IntRange> {
        if (this.step != 1) throw UnsupportedOperationException("Sorry, step != 1")
        val size = this.last - this.first + 1f

        return (0 until slices).map {
            size.div(slices).times(it).roundInt() until
                    size.div(slices).times(it + 1).roundInt()
        }
    }


    private inline fun Float.roundInt(): Int = Math.round(this)
}