package net.torvald.terrarum.concurrent

import net.torvald.terrarum.Terrarum

typealias RunnableFun = () -> Unit
/** Int: index of the processing core */
typealias ThreadableFun = (Int) -> Unit

/**
 * Created by minjaesong on 2016-05-25.
 */
object ThreadParallel {
    val threadCount = Terrarum.THREADS // modify this to your taste

    private val pool: Array<Thread?> = Array(threadCount, { null })

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
    fun map(index: Int, prefix: String, runFunc: ThreadableFun) {
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
        pool.forEach { if (it != null && it.state != Thread.State.TERMINATED) return false }
        return true
    }
}

/**
 * A thread pool that will hold the execution until all the tasks are completed.
 *
 * Tasks are not guaranteed to be done orderly; but the first task in the list will be executed first.
 */
@Deprecated("Experimental.", ReplaceWith("ThreadParallel", "net.torvald.terrarum.concurrent.ThreadParallel"))
object BlockingThreadPool {
    val threadCount = Terrarum.THREADS // modify this to your taste
    private val pool: Array<Thread?> = Array(threadCount, { null })
    private var tasks: List<RunnableFun> = ArrayList<RunnableFun>()
    @Volatile private var dispatchedTasks = 0
    private var threadPrefix = ""

    /** @return false on failure (likely the previous jobs not finished), true on success */
    fun map(prefix: String, tasks: List<RunnableFun>) = setTasks(tasks, prefix)
    /** @return false on failure (likely the previous jobs not finished), true on success */
    fun setTasks(tasks: List<RunnableFun>, prefix: String): Boolean {
        if (!allFinished())
            return false

        this.tasks = tasks
        dispatchedTasks = 0
        threadPrefix = prefix
        return true
    }

    private fun dequeueTask(): RunnableFun {
        dispatchedTasks += 1
        return tasks[dispatchedTasks - 1]
    }


    fun startAllWaitForDie() {
        while (dispatchedTasks <= tasks.lastIndex) {
            // marble rolling down the slanted channel-track of threads, if a channel is empty (a task assigned
            //     to the thread is dead) the marble will roll into the channel, and the marble is a task  #MarbleMachineX
            for (i in 0 until threadCount) {
                // but unlike the marble machine, marble don't actually roll down, we can just pick up any number
                // of marbles and put it into an empty channel whenever we encounter one

                // SO WHAT WE DO is first fill any empty channels:
                if (dispatchedTasks <= tasks.lastIndex && // because cache invalidation damnit
                        (pool[i] == null || pool[i]!!.state == Thread.State.TERMINATED)) {
                    pool[i] = Thread(dequeueTask().makeRunnable(), "$threadPrefix-$dispatchedTasks") // thread name index is one-based
                    pool[i]!!.start()
                }

                // then, sleep this very thread, wake if any of the thread in the pool is terminated,
                // and GOTO loop_start; if we don't sleep, this function will be busy-waiting

            }
        }
    }

    fun allFinished(): Boolean {
        pool.forEach { if (it != null && it.state != Thread.State.TERMINATED) return false }
        return true
    }


    private fun RunnableFun.makeRunnable() = Runnable {
        this.invoke()
    }
}

object ParallelUtils {
    fun <T, R> Iterable<T>.parallelMap(transform: (T) -> R): List<R> {
        val tasks = this.sliceEvenly(ThreadParallel.threadCount)
        val destination = Array(ThreadParallel.threadCount) { ArrayList<R>() }
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

    fun <T> Iterable<T>.sliceEvenly(slices: Int): List<List<T>> = this.toList().sliceEvenly(slices)

    fun <T> List<T>.sliceEvenly(slices: Int): List<List<T>> {
        return (0 until slices).map {
            this.subList(
                    this.size.toFloat().div(slices).times(it).roundInt(),
                    this.size.toFloat().div(slices).times(it + 1).roundInt()
            )
        }
    }

    fun <T> Array<T>.sliceEvenly(slices: Int): List<Array<T>> {
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