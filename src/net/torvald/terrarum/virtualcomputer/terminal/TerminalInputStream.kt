package net.torvald.terrarum.virtualcomputer.terminal

import org.lwjgl.input.Keyboard
import java.io.InputStream

/**
 * Created by minjaesong on 16-09-10.
 */
class TerminalInputStream(val term: Teletype) : InputStream() {
    override fun read(): Int {
        return -1
    }
}