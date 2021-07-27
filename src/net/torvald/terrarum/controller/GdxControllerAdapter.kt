package net.torvald.terrarum.controller

import com.badlogic.gdx.controllers.Controller
import net.torvald.terrarum.toInt

/**
 * Created by minjaesong on 2019-02-09.
 */
class GdxControllerAdapter(val c: Controller): TerrarumController {

    override fun getButton(index: Int): Boolean {
        return c.getButton(index)
    }

    override fun getAxisRaw(index: Int): Float {
        return c.getAxis(index)
    }

    override fun getDpad(): Int {
        return (c.getButton(c.mapping.buttonDpadLeft).toInt()) or
                (c.getButton(c.mapping.buttonDpadDown).toInt() shl 1) or
                (c.getButton(c.mapping.buttonDpadRight).toInt() shl 2) or
                (c.getButton(c.mapping.buttonDpadUp).toInt() shl 3)
    }

    override fun getName(): String {
        return "DI:"+c.name
    }

    override fun setRumble(left: Float, right: Float) {
        return
    }
}