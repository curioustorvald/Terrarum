package com.sudoplay.joise.module

import com.sudoplay.joise.ModuleInstanceMap
import com.sudoplay.joise.ModuleMap
import com.sudoplay.joise.ModulePropertyMap
import net.torvald.terrarum.modulebasegame.worldgenerator.Worldgen.YHEIGHT_DIVISOR
import net.torvald.terrarum.modulebasegame.worldgenerator.Worldgen.YHEIGHT_MAGIC

/**
 * Params:
 * - H: World height MINUS 100
 * - L: 620
 *
 * Created by minjaesong on 2024-09-07.
 */
class TerrarumModuleCaveLayerClosureGrad : Module() {
    protected val h = ScalarParameter(17.2)
    protected val l = ScalarParameter(3.0)


    private fun Double.op() = Math.sqrt((-this + h.value - l.value) / l.value).let { if (it.isNaN()) 0.0 else it.coerceIn(0.0, 1.0) }

    override fun get(x: Double, y: Double) = y.op()
    override fun get(x: Double, y: Double, z: Double) = y.op()/*.also {
        println("$y\t$it")
    }*/
    override fun get(x: Double, y: Double, z: Double, w: Double) = y.op()
    override fun get(x: Double, y: Double, z: Double, w: Double, u: Double, v: Double) = y.op()

    fun setL(source: Double) {
        l.set(source)
    }

    fun setH(source: Double) {
        h.set(source)
    }

    override fun _writeToMap(map: ModuleMap?) {
        val props = ModulePropertyMap(this)

        writeScalar("l", l, props, map)
        writeScalar("h", h, props, map)

        map!![id] = props
    }

    override fun buildFromPropertyMap(props: ModulePropertyMap?, map: ModuleInstanceMap?): Module {
        readScalar("l", "setL", props, map)
        readScalar("h", "setH", props, map)

        return this
    }
}
