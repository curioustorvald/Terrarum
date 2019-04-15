package net.torvald.terrarum.controller

import net.torvald.util.CircularArray

/**
 * Created by minjaesong on 2019-04-10.
 */
abstract class VirtualKeyboard(val BUFFER_SIZE: Int = DEFAULT_BUFFER_SIZE) {

    val inputBuffer = CircularArray<Char>(BUFFER_SIZE)

    abstract fun takeFromInputBuffer()

    fun addToBuffer(char: Char) {
        inputBuffer.add(char)
    }

    companion object {
        const val DEFAULT_BUFFER_SIZE = 20
    }
}