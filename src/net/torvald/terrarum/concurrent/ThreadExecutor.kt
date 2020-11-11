package net.torvald.terrarum.concurrent

import java.util.concurrent.*
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

typealias RunnableFun = () -> Unit
/** Int: index of the processing core */
typealias ThreadableFun = (Int) -> Unit


object ThreadExecutor {
    val threadCount = Runtime.getRuntime().availableProcessors() // not using (logicalCores + 1) method; it's often better idea to reserve one extra thread for other jobs in the app
    private lateinit var executor: ExecutorService// = Executors.newFixedThreadPool(threadCount)
    val futures = ArrayList<Future<*>>()
    private var isOpen = true

    var allFinished = true
        private set

    private fun checkShutdown() {
        try {
            if (executor.isTerminated)
                throw IllegalStateException("Executor terminated, renew the executor service.")
            if (!isOpen || executor.isShutdown)
                throw IllegalStateException("Pool is closed, come back when all the threads are terminated.")
        }
        catch (e: UninitializedPropertyAccessException) {}
    }

    fun renew() {
        try {
            if (!executor.isTerminated && !executor.isShutdown) throw IllegalStateException("Pool is still running")
        }
        catch (_: UninitializedPropertyAccessException) {}

        executor = Executors.newFixedThreadPool(threadCount)
        futures.clear()
        isOpen = true
        allFinished = false
    }

    /*fun invokeAll(ts: List<Callable<Unit>>) {
        checkShutdown()
        executor.invokeAll(ts)
    }*/

    fun submit(t: Callable<Unit>) {
        checkShutdown()
        futures.add(executor.submit(t))
    }
    fun submitAll(ts: List<Callable<Unit>>) {
        checkShutdown()
        ts.forEach { futures.add(executor.submit(it)) }
    }

    // https://stackoverflow.com/questions/28818494/threads-stopping-prematurely-for-certain-values
    fun join() {
        println("ThreadExecutor.join")
        isOpen = false
        futures.forEach {
            try {
                it.get()
            }
            catch (e: ExecutionException) {
                throw e
            }
        }
        executor.shutdown() // thread status of completed ones will be WAIT instead of TERMINATED without this line...
        executor.awaitTermination(24L, TimeUnit.HOURS)
        allFinished = true
    }

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
                this.size.toFloat().div(slices).times(it).roundToInt(),
                this.size.toFloat().div(slices).times(it + 1).roundToInt()
        )
    }
}

fun <T> Array<T>.sliceEvenly(slices: Int): List<Array<T>> {
    return (0 until slices).map {
        this.sliceArray(
                this.size.toFloat().div(slices).times(it).roundToInt() until
                        this.size.toFloat().div(slices).times(it + 1).roundToInt()
        )
    }
}

fun IntProgression.sliceEvenly(slices: Int): List<IntProgression> {
    if (this.step.absoluteValue != 1) throw UnsupportedOperationException("Sorry, step != +1/-1")
    val size = (this.last - this.first).absoluteValue + (this.step.toFloat()).absoluteValue

    // println(size)

    return if (this.first < this.last) (0 until slices).map {
        this.first + size.div(slices).times(it).roundToInt() ..
                this.first + size.div(slices).times(it + 1).roundToInt() - 1
    }
    else (0 until slices).map {
        this.first - size.div(slices).times(it).roundToInt() downTo
                this.first - size.div(slices).times(it + 1).roundToInt() + 1
    }
}
