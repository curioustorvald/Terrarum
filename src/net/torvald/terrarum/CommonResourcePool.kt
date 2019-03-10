package net.torvald.terrarum

import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.Queue

/**
 * Created by minjaesong on 2019-03-10.
 */
object CommonResourcePool {

    private val loadingList = Queue<Pair<Pair<String, Class<*>>, () -> Disposable>>()
    private val pool = HashMap<String, Disposable>()
    //private val typesMap = HashMap<String, Class<*>>()
    private var loadCounter = -1 // using counters so that the loading can be done on separate thread (gg if the asset requires GL context to be loaded)
    val loaded: Boolean
        get() = loadCounter == 0

    fun <T> addToLoadingList(identifier: String, type: Class<T>, loadFunction: () -> Disposable) {
        loadingList.addFirst(identifier to type to loadFunction)

        if (loadCounter == -1)
            loadCounter = 1
        else
            loadCounter += 1
    }

    /**
     * You are supposed to call this function only once.
     */
    fun loadAll() {
        if (loaded) throw IllegalStateException("Assets are already loaded and shipped out :p")

        while (!loadingList.isEmpty) {
            val (k, loadfun) = loadingList.removeFirst()
            val (name, type) = k

            if (pool.containsKey(name)) {
                throw IllegalArgumentException("Assets with identifier '$name' already exists.")
            }

            //typesMap[name] = type
            pool[name] = loadfun.invoke()

            loadCounter -= 1
        }
    }

    operator fun get(identifier: String): Disposable {
        val obj = pool[identifier]!!

        return obj
    }

    fun dispose() {
        pool.forEach { _, u ->
            try {
                u.dispose()
            }
            catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}