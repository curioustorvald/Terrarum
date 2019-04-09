package net.torvald.terrarum.controller

import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.PovDirection

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

    override fun getPov(): Int {
        return when(c.getPov(0)) {
            PovDirection.north -> TerrarumController.POV_N
            PovDirection.northEast -> TerrarumController.POV_NE
            PovDirection.northWest -> TerrarumController.POV_NW
            PovDirection.east -> TerrarumController.POV_E
            PovDirection.west -> TerrarumController.POV_W
            PovDirection.south -> TerrarumController.POV_S
            PovDirection.southEast -> TerrarumController.POV_SE
            PovDirection.southWest -> TerrarumController.POV_SW
            else -> 0
        }
    }

    override fun getName(): String {
        return "DI:"+c.name
    }

    override fun setRumble(left: Float, right: Float) {
        return
    }
}