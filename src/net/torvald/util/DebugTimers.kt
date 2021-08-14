package net.torvald.util

/**
 * Debugtimers will collect multiple measurements and will return stabilised time reading
 *
 * Created by minjaesong on 2021-08-14
 */
class DebugTimers {

    private val BUFFER_SIZE = 16
    private val bufs = ArrayListMap<String, CircularArray<Long>>()

    fun put(key: String, value: Long) {
        if (bufs[key] == null) bufs[key] = CircularArray(BUFFER_SIZE, true)
        bufs[key]!!.appendHead(value)
    }

    operator fun get(key: String): Long? {
        if (bufs[key] == null) return null
        else {
            var s = 0L; var c = 0
            for (i in 0 until BUFFER_SIZE) {
                bufs[key]!![i]?.let {
                    s += it
                    c += 1
                }
            }
            return if (c == 0) 0L else s / c
        }
    }

    fun forEach(action: (String, Long?) -> Unit) {
        bufs.keys.forEach { key -> action(key, get(key)) }
    }


}