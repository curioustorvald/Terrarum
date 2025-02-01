package com.sudoplay.joise.module

import com.sudoplay.joise.ModuleInstanceMap
import com.sudoplay.joise.ModuleMap
import com.sudoplay.joise.ModulePropertyMap
import net.torvald.terrarum.ifNaN
import net.torvald.terrarum.modulebasegame.worldgenerator.Worldgen.YHEIGHT_DIVISOR
import net.torvald.terrarum.modulebasegame.worldgenerator.Worldgen.YHEIGHT_MAGIC

/**
 * Params:
 * - H: World height MINUS 100
 * - L: 620
 *
 * Created by minjaesong on 2024-09-07.
 */
class TerrarumModuleLavaFloorGrad : Module() {
    protected val h = ScalarParameter(5300.0)
    protected val l = ScalarParameter(620.0)

    private fun undoYtransform(yp: Double): Double = yp + (3200 - YHEIGHT_MAGIC) * YHEIGHT_DIVISOR

    private fun Double.op() = Math.sqrt((this - h.value + l.value) / l.value).ifNaN(0.0)

    override fun get(x: Double, y: Double) = undoYtransform(y).op()
    override fun get(x: Double, y: Double, z: Double) = undoYtransform(y).op()/*.also {
        println("$y\t$it")
    }*/
    override fun get(x: Double, y: Double, z: Double, w: Double) = undoYtransform(y).op()
    override fun get(x: Double, y: Double, z: Double, w: Double, u: Double, v: Double) = undoYtransform(y).op()

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