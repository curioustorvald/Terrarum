package net.torvald.dataclass

import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Function

/**
 * Created by minjaesong on 2018-07-15.
 */
class ArrayListMap<K, V> : MutableMap<K, V> {

    private val keysArray = ArrayList<K>()
    private val valuesArray = ArrayList<V>()

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val size: Int
        get() = keysArray.size

    override val values: MutableCollection<V>
        get() = valuesArray.toMutableSet()

    override val keys: MutableSet<K>
        get() = keysArray.toMutableSet()



    override fun containsKey(key: K): Boolean {
        return keysArray.contains(key)
    }

    override fun containsValue(value: V): Boolean {
        return valuesArray.contains(value)
    }

    override fun forEach(action: BiConsumer<in K, in V>) {
        for (i in 0 until size) {
            action.accept(keysArray[i], valuesArray[i])
        }
    }

    override fun get(key: K): V? {
        val index = keysArray.linearSearch(key)
        index?.let {
            return valuesArray[index]
        }
        return null
    }

    override fun getOrDefault(key: K, defaultValue: V): V {
        return get(key) ?: defaultValue
    }

    override fun isEmpty(): Boolean {
        return size == 0
    }


    private fun ArrayList<K>.linearSearch(element: K): Int? {
        var found = 0
        while (found < keysArray.size) {
            if (keysArray[found] == element)
                return found

            found++
        }

        return null
    }

    override fun put(key: K, value: V): V? {
        val index = keysArray.linearSearch(key)
        if (index != null) {
            val oldValue = valuesArray[index]
            valuesArray[index] = value
            return oldValue
        }
        else {
            keysArray.add(key)
            valuesArray.add(value)
            return null
        }
    }

    override fun clear() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun compute(key: K, remappingFunction: BiFunction<in K, in V?, out V?>): V? {
        return super.compute(key, remappingFunction)
    }

    override fun computeIfAbsent(key: K, mappingFunction: Function<in K, out V>): V {
        return super.computeIfAbsent(key, mappingFunction)
    }

    override fun computeIfPresent(key: K, remappingFunction: BiFunction<in K, in V, out V?>): V? {
        return super.computeIfPresent(key, remappingFunction)
    }

    override fun merge(key: K, value: V, remappingFunction: BiFunction<in V, in V, out V?>): V? {
        return super.merge(key, value, remappingFunction)
    }

    override fun putAll(from: Map<out K, V>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun putIfAbsent(key: K, value: V): V? {
        return super.putIfAbsent(key, value)
    }

    override fun remove(key: K): V? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun remove(key: K, value: V): Boolean {
        return super.remove(key, value)
    }

    override fun replace(key: K, oldValue: V, newValue: V): Boolean {
        return super.replace(key, oldValue, newValue)
    }

    override fun replace(key: K, value: V): V? {
        return super.replace(key, value)
    }

    override fun replaceAll(function: BiFunction<in K, in V, out V>) {
        super.replaceAll(function)
    }

}