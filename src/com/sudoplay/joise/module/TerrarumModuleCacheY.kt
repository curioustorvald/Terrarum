package com.sudoplay.joise.module

import com.sudoplay.joise.ModuleInstanceMap
import com.sudoplay.joise.ModuleMap
import com.sudoplay.joise.ModulePropertyMap

/**
 * Created by minjaesong on 2024-09-08.
 */
class TerrarumModuleCacheY : SourcedModule() {

    class Cache {
        var y: Double = 0.0
        var `val`: Double = 0.0
        var valid: Boolean = false
    }

    protected var c2: Cache = Cache()
    protected var c3: Cache = Cache()
    protected var c4: Cache = Cache()
    protected var c6: Cache = Cache()

    override fun get(x: Double, y: Double): Double {
        if (!c2.valid || c2.y != y) {
            c2.y = y
            c2.valid = true
            c2.`val` = source[x, y]
        }
        return c2.`val`
    }

    override fun get(x: Double, y: Double, z: Double): Double {
        if (!c3.valid || c3.y != y) {
            c3.y = y
            c3.valid = true
            c3.`val` = source[x, y, z]
        }
        return c3.`val`
    }

    override fun get(x: Double, y: Double, z: Double, w: Double): Double {
        if (!c4.valid || c4.y != y ) {
            c4.y = y
            c4.valid = true
            c4.`val` = source[x, y, z, w]
        }
        return c4.`val`
    }

    override fun get(x: Double, y: Double, z: Double, w: Double, u: Double, v: Double): Double {
        if (!c6.valid || c6.y != y ) {
            c6.y = y
            c6.valid = true
            c6.`val` = source[x, y, z, w, u, v]
        }
        return c6.`val`
    }

    override fun _writeToMap(map: ModuleMap) {
        val props = ModulePropertyMap(this)

        writeSource(props, map)

        map[id] = props
    }

    override fun buildFromPropertyMap(
        props: ModulePropertyMap?,
        map: ModuleInstanceMap?
    ): Module {
        readSource(props, map)

        return this
    }

}