package com.sudoplay.joise.module

import com.sudoplay.joise.ModuleInstanceMap
import com.sudoplay.joise.ModuleMap
import com.sudoplay.joise.ModulePropertyMap
import java.util.*

/**
 * Created by minjaesong on 2024-09-22.
 */
class TerrarumModuleCombiner : Module() {

    enum class CombinerType {
        ADD, MULT, MAX, MIN, AVG, VECTORISE
    }

    protected var sources: Array<ScalarParameter?> = arrayOfNulls(MAX_SOURCES)
    protected var type: CombinerType? = null

    fun ModuleCombiner(type: CombinerType?) {
        this.type = type
    }

    fun ModuleCombiner() {
        // serialization
    }

    fun setSource(index: Int, source: Module?) {
        sources[index] = ScalarParameter(source)
    }

    fun setSource(index: Int, source: Double) {
        sources[index] = ScalarParameter(source)
    }

    fun clearAllSources() {
        for (i in 0 until MAX_SOURCES) {
            sources[i] = null
        }
    }

    /**
     * Noise values from empty indices will be substituted with NaN
     */
    fun getVectorise(x: Double, y: Double, z: Double): List<Double> {
        return when (type) {
            CombinerType.VECTORISE -> vectorGet(x, y, z)
            else -> throw UnsupportedOperationException("Use get")
        }
    }

    fun vectorGet(x: Double, y: Double, z: Double): List<Double> {
        return sources.map { it?.get(x, y, z) ?: Double.NaN }
    }

    override fun get(x: Double, y: Double): Double {
        return when (type) {
            CombinerType.ADD -> addGet(x, y)
            CombinerType.AVG -> avgGet(x, y)
            CombinerType.MAX -> maxGet(x, y)
            CombinerType.MIN -> minGet(x, y)
            CombinerType.MULT -> multGet(x, y)
            CombinerType.VECTORISE -> throw UnsupportedOperationException("Use getVector")
            else -> 0.0
        }
    }

    override fun get(x: Double, y: Double, z: Double): Double {
        return when (type) {
            CombinerType.ADD -> addGet(x, y, z)
            CombinerType.AVG -> avgGet(x, y, z)
            CombinerType.MAX -> maxGet(x, y, z)
            CombinerType.MIN -> minGet(x, y, z)
            CombinerType.MULT -> multGet(x, y, z)
            CombinerType.VECTORISE -> throw UnsupportedOperationException("Use getVector")
            else -> 0.0
        }
    }

    override fun get(x: Double, y: Double, z: Double, w: Double): Double {
        return when (type) {
            CombinerType.ADD -> addGet(x, y, z, w)
            CombinerType.AVG -> avgGet(x, y, z, w)
            CombinerType.MAX -> maxGet(x, y, z, w)
            CombinerType.MIN -> minGet(x, y, z, w)
            CombinerType.MULT -> multGet(x, y, z, w)
            CombinerType.VECTORISE -> throw UnsupportedOperationException("Use getVector")
            else -> 0.0
        }
    }

    override fun get(x: Double, y: Double, z: Double, w: Double, u: Double, v: Double): Double {
        return when (type) {
            CombinerType.ADD -> addGet(x, y, z, w, u, v)
            CombinerType.AVG -> avgGet(x, y, z, w, u, v)
            CombinerType.MAX -> maxGet(x, y, z, w, u, v)
            CombinerType.MIN -> minGet(x, y, z, w, u, v)
            CombinerType.MULT -> multGet(x, y, z, w, u, v)
            CombinerType.VECTORISE -> throw UnsupportedOperationException("Use getVector")
            else -> 0.0
        }
    }


    // ==========================================================================
    // = ADD
    // ==========================================================================
    protected fun addGet(x: Double, y: Double): Double {
        var value = 0.0
        for (i in 0 until MAX_SOURCES) {
            if (sources[i] != null) value += sources[i]!![x, y]
        }
        return value
    }

    protected fun addGet(x: Double, y: Double, z: Double): Double {
        var value = 0.0
        for (i in 0 until MAX_SOURCES) {
            if (sources[i] != null) value += sources[i]!![x, y, z]
        }
        return value
    }

    protected fun addGet(x: Double, y: Double, z: Double, w: Double): Double {
        var value = 0.0
        for (i in 0 until MAX_SOURCES) {
            if (sources[i] != null) value += sources[i]!![x, y, z, w]
        }
        return value
    }

    protected fun addGet(
        x: Double, y: Double, z: Double, w: Double, u: Double,
        v: Double
    ): Double {
        var value = 0.0
        for (i in 0 until MAX_SOURCES) {
            if (sources[i] != null) value += sources[i]!![x, y, z, w, u, v]
        }
        return value
    }


    // ==========================================================================
    // = AVG
    // ==========================================================================
    protected fun avgGet(x: Double, y: Double): Double {
        var count = 0
        var value = 0.0
        for (i in 0 until MAX_SOURCES) {
            if (sources[i] != null) {
                value += sources[i]!![x, y]
                count++
            }
        }
        if (count == 0) return 0.0
        return value / count.toDouble()
    }

    protected fun avgGet(x: Double, y: Double, z: Double): Double {
        var count = 0
        var value = 0.0
        for (i in 0 until MAX_SOURCES) {
            if (sources[i] != null) {
                value += sources[i]!![x, y, z]
                count++
            }
        }
        if (count == 0) return 0.0
        return value / count.toDouble()
    }

    protected fun avgGet(x: Double, y: Double, z: Double, w: Double): Double {
        var count = 0
        var value = 0.0
        for (i in 0 until MAX_SOURCES) {
            if (sources[i] != null) {
                value += sources[i]!![x, y, z, w]
                count++
            }
        }
        if (count == 0) return 0.0
        return value / count.toDouble()
    }

    protected fun avgGet(
        x: Double, y: Double, z: Double, w: Double, u: Double,
        v: Double
    ): Double {
        var count = 0
        var value = 0.0
        for (i in 0 until MAX_SOURCES) {
            if (sources[i] != null) {
                value += sources[i]!![x, y, z, w, u, v]
                count++
            }
        }
        if (count == 0) return 0.0
        return value / count.toDouble()
    }


    // ==========================================================================
    // = MAX
    // ==========================================================================
    protected fun maxGet(x: Double, y: Double): Double {
        var mx: Double
        var c = 0
        while (c < MAX_SOURCES && sources[c] == null) {
            c++
        }
        if (c == MAX_SOURCES) return 0.0
        mx = sources[c]!![x, y]

        for (d in c until MAX_SOURCES) {
            if (sources[d] != null) {
                val `val` = sources[d]!![x, y]
                if (`val` > mx) mx = `val`
            }
        }

        return mx
    }

    protected fun maxGet(x: Double, y: Double, z: Double): Double {
        var mx: Double
        var c = 0
        while (c < MAX_SOURCES && sources[c] == null) {
            c++
        }
        if (c == MAX_SOURCES) return 0.0
        mx = sources[c]!![x, y, z]

        for (d in c until MAX_SOURCES) {
            if (sources[d] != null) {
                val `val` = sources[d]!![x, y, z]
                if (`val` > mx) mx = `val`
            }
        }

        return mx
    }

    protected fun maxGet(x: Double, y: Double, z: Double, w: Double): Double {
        var mx: Double
        var c = 0
        while (c < MAX_SOURCES && sources[c] == null) {
            c++
        }
        if (c == MAX_SOURCES) return 0.0
        mx = sources[c]!![x, y, z, w]

        for (d in c until MAX_SOURCES) {
            if (sources[d] != null) {
                val `val` = sources[d]!![x, y, z, w]
                if (`val` > mx) mx = `val`
            }
        }

        return mx
    }

    protected fun maxGet(
        x: Double, y: Double, z: Double, w: Double, u: Double,
        v: Double
    ): Double {
        var mx: Double
        var c = 0
        while (c < MAX_SOURCES && sources[c] == null) {
            c++
        }
        if (c == MAX_SOURCES) return 0.0
        mx = sources[c]!![x, y, z, w, u, v]

        for (d in c until MAX_SOURCES) {
            if (sources[d] != null) {
                val `val` = sources[d]!![x, y, z, w, u, v]
                if (`val` > mx) mx = `val`
            }
        }

        return mx
    }


    // ==========================================================================
    // = MIN
    // ==========================================================================
    protected fun minGet(x: Double, y: Double): Double {
        var mn: Double
        var c = 0
        while (c < MAX_SOURCES && sources[c] == null) {
            c++
        }
        if (c == MAX_SOURCES) return 0.0
        mn = sources[c]!![x, y]

        for (d in c until MAX_SOURCES) {
            if (sources[d] != null) {
                val `val` = sources[d]!![x, y]
                if (`val` < mn) mn = `val`
            }
        }

        return mn
    }

    protected fun minGet(x: Double, y: Double, z: Double): Double {
        var mn: Double
        var c = 0
        while (c < MAX_SOURCES && sources[c] == null) {
            c++
        }
        if (c == MAX_SOURCES) return 0.0
        mn = sources[c]!![x, y, z]

        for (d in c until MAX_SOURCES) {
            if (sources[d] != null) {
                val `val` = sources[d]!![x, y, z]
                if (`val` < mn) mn = `val`
            }
        }

        return mn
    }

    protected fun minGet(x: Double, y: Double, z: Double, w: Double): Double {
        var mn: Double
        var c = 0
        while (c < MAX_SOURCES && sources[c] == null) {
            c++
        }
        if (c == MAX_SOURCES) return 0.0
        mn = sources[c]!![x, y, z, w]

        for (d in c until MAX_SOURCES) {
            if (sources[d] != null) {
                val `val` = sources[d]!![x, y, z, w]
                if (`val` < mn) mn = `val`
            }
        }

        return mn
    }

    protected fun minGet(
        x: Double, y: Double, z: Double, w: Double, u: Double,
        v: Double
    ): Double {
        var mn: Double
        var c = 0
        while (c < MAX_SOURCES && sources[c] == null) {
            c++
        }
        if (c == MAX_SOURCES) return 0.0
        mn = sources[c]!![x, y, z, w, u, v]

        for (d in c until MAX_SOURCES) {
            if (sources[d] != null) {
                val `val` = sources[d]!![x, y, z, w, u, v]
                if (`val` < mn) mn = `val`
            }
        }

        return mn
    }


    // ==========================================================================
    // = MULT
    // ==========================================================================
    protected fun multGet(x: Double, y: Double): Double {
        var value = 1.0
        for (i in 0 until MAX_SOURCES) {
            if (sources[i] != null) value *= sources[i]!![x, y]
        }
        return value
    }

    protected fun multGet(x: Double, y: Double, z: Double): Double {
        var value = 1.0
        for (i in 0 until MAX_SOURCES) {
            if (sources[i] != null) value *= sources[i]!![x, y, z]
        }
        return value
    }

    protected fun multGet(x: Double, y: Double, z: Double, w: Double): Double {
        var value = 1.0
        for (i in 0 until MAX_SOURCES) {
            if (sources[i] != null) value *= sources[i]!![x, y, z, w]
        }
        return value
    }

    protected fun multGet(
        x: Double, y: Double, z: Double, w: Double, u: Double,
        v: Double
    ): Double {
        var value = 1.0
        for (i in 0 until MAX_SOURCES) {
            if (sources[i] != null) value *= sources[i]!![x, y, z, w, u, v]
        }
        return value
    }

    override fun _writeToMap(map: ModuleMap) {
        val props = ModulePropertyMap(this)

        writeEnum("type", type, props)

        for (i in 0 until MAX_SOURCES) {
            writeScalar("source$i", sources[i], props, map)
        }

        map[id] = props
    }

    override fun buildFromPropertyMap(
        props: ModulePropertyMap,
        map: ModuleInstanceMap
    ): Module {
        readEnum("type", "setType", CombinerType::class.java, props)

        var name: String
        var o: Any?
        for (i in 0 until MAX_SOURCES) {
            o = props["source$i"]
            if (o != null) {
                name = o.toString()
                setSource(i, map[name])
            }
        }

        return this
    }


}