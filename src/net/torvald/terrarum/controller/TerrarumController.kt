package net.torvald.terrarum.controller

/**
 * Created by minjaesong on 2019-02-09.
 */
interface TerrarumController {

    /**
     * 0, 1, 2, 3 : A B X Y
     *
     * 4, 5 : L/R Shoulder
     *
     * 6, 7 : Back Start
     *
     * 8, 9 : L/R Trigger
     *
     * 10, 11 : L/R Stick
     *
     * DirectInput devices may need external Index-to-button mapping (just a config file)
     *
     */
    fun getButton(index: Int): Boolean

    /**
     * 0: Left Y
     *
     * 1: Left X
     *
     * 2: Right Y
     *
     * 3: Right X
     *
     * 4: Left Trigger
     *
     * 5: Right Trigger
     *
     * DirectInput devices may need external Index-to-button mapping (just a config file).
     *
     * Warning: Under DirectInput, Xbox controllers' LT and RT shares the same axis #4, and thus it's impossible to read LT+RT input.
     *
     * @return 0f..1f for the axis
     */
    fun getAxis(index: Int): Float

    /**
     * ```
     * 12 8 9
     *  4 0 1
     *  6 2 3
     * ```
     */
    fun getPov(): Int
    fun getName(): String

    /**
     * @param left left rumble motor, 0f..1f
     * @param right right rumble moter, 0f..1f
     */
    fun setRumble(left: Float, right: Float)

    companion object {
        const val POV_E = 1
        const val POV_S = 2
        const val POV_W = 4
        const val POV_N = 8

        const val POV_SE = POV_E or POV_S
        const val POV_SW = POV_W or POV_S
        const val POV_NW = POV_W or POV_N
        const val POV_NE = POV_E or POV_N
    }
}