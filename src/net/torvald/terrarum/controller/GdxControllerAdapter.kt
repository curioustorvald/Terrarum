package net.torvald.terrarum.controller

import com.badlogic.gdx.controllers.Controller

/**
 * Created by minjaesong on 2019-02-09.
 */
class GdxControllerAdapter(val c: Controller): TerrarumController {

    override fun getButton(index: Int): Boolean {
        return c.getButton(index)
    }

    override fun getAxis(index: Int): Float {
        return c.getAxis(index)
    }

    override fun getPov(): Int {
        TODO()
    }

    override fun getName(): String {
        return c.name
    }

    override fun setRumble(left: Float, right: Float) {
        return
    }
}