package net.torvald.terrarum.controller

import com.github.strikerx3.jxinput.XInputAxes
import com.github.strikerx3.jxinput.XInputDevice
import net.torvald.terrarum.AppLoader
import kotlin.math.roundToInt

/**
 * Note: XInput is a basically a XBOX 360 pad. XBOX One pad and any other compatible pads can be used,
 * but any additional features (e.g. Impulse Trigger on XBOne) cannot be supported.
 *
 * Created by minjaesong on 2019-02-09.
 */
class XinputControllerAdapter(val c: XInputDevice): TerrarumController {

    override fun getButton(index: Int): Boolean {
        if (c.poll()) {
            val button = c.components.buttons

            return when (index) {
                0 -> button.a
                1 -> button.b
                2 -> button.x
                3 -> button.y
                4 -> button.lShoulder
                5 -> button.rShoulder
                6 -> button.back
                7 -> button.start
                8 -> getAxis(4) >= AppLoader.gamepadDeadzone
                9 -> getAxis(5) >= AppLoader.gamepadDeadzone
                10 -> button.lThumb
                11 -> button.rThumb
                else -> throw UnsupportedOperationException("Unknown button: $index")
            }
        }
        return false
    }

    override fun getAxisRaw(index: Int): Float {
        if (c.poll()) {
            val axes = c.components.axes

            return when (index) {
                0 -> axes.ly
                1 -> axes.lx
                2 -> axes.ry
                3 -> axes.rx
                4 -> axes.lt
                5 -> axes.rt
                else -> throw UnsupportedOperationException("Unknown axis: $index")
            }
        }
        return -1f
    }

    override fun getPov(): Int {
        if (c.poll()) {
            val axes = c.components.axes

            return when (axes.dpad) {
                XInputAxes.DPAD_CENTER -> 0
                XInputAxes.DPAD_UP_LEFT -> TerrarumController.POV_NW
                XInputAxes.DPAD_UP -> TerrarumController.POV_N
                XInputAxes.DPAD_UP_RIGHT -> TerrarumController.POV_NE
                XInputAxes.DPAD_RIGHT -> TerrarumController.POV_E
                XInputAxes.DPAD_DOWN_RIGHT -> TerrarumController.POV_SE
                XInputAxes.DPAD_DOWN -> TerrarumController.POV_S
                XInputAxes.DPAD_DOWN_LEFT -> TerrarumController.POV_SW
                XInputAxes.DPAD_LEFT -> TerrarumController.POV_W
                else -> 0//throw UnsupportedOperationException("Unknown pov: ${axes.dpad}")
            }
        }
        return -1
    }

    override fun getName(): String {
        return "(XINPUT Compatible)"
    }

    override fun setRumble(left: Float, right: Float) {
        c.setVibration((left * 65535f).roundToInt(), (right * 65535f).roundToInt())
    }
}
