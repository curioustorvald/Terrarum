package com.sudoplay.joise.module

import com.sudoplay.joise.ModuleInstanceMap
import com.sudoplay.joise.ModuleMap
import com.sudoplay.joise.ModulePropertyMap

/**
 * Created by minjaesong on 2024-09-08.
 */
class ModuleConstant : Module() {
    private val constant = ScalarParameter(1.0)

    override fun _writeToMap(map: ModuleMap?) {
        val props = ModulePropertyMap(this)

        writeScalar("constant", constant, props, map)

        map!![id] = props
    }

    override fun get(x: Double, y: Double) = constant.value
    override fun get(x: Double, y: Double, z: Double) = constant.value
    override fun get(x: Double, y: Double, z: Double, w: Double) = constant.value
    override fun get(x: Double, y: Double, z: Double, w: Double, u: Double, v: Double) = constant.value

    fun setConstant(c: Double) {
        constant.set(c)
    }

    override fun buildFromPropertyMap(props: ModulePropertyMap?, map: ModuleInstanceMap?): Module {
        readScalar("constant", "setConstant", props, map)

        return this
    }
}