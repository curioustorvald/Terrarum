package net.torvald.terrarum.virtualcomputer.terminal

import org.lwjgl.input.Keyboard
import java.io.InputStream

/**
 * Created by minjaesong on 16-09-10.
 */
class TerminalInputStream(val term: Terminal) : InputStream() {
    override fun read(): Int {
        val ret = term.lastInputByte
        term.lastInputByte = -1
        return ret
    }
}