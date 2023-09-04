package net.torvald.terrarum

/**
 * Created by minjaesong on 2023-09-05.
 */
object TerrarumGlobalState {

    var HAS_KEYBOARD_INPUT_FOCUS = CountedBool()

}

class CountedBool {
    private var counter = 0L

    fun set() {
        counter += 1
    }
    fun unset() {
        if (counter >= 1) counter -= 1
    }
    val value: Boolean
        get() = counter > 0
    fun isOn() = value
    fun isOff() = !value
}