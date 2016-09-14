package net.torvald.terrarum.virtualcomputer.terminal

import java.io.OutputStream
import java.io.PrintStream

/**
 * Created by minjaesong on 16-09-10.
 */
class TerminalPrintStream(val term: Terminal) : PrintStream(TerminalOutputStream(term)) {

}

class TerminalOutputStream(val term: Terminal) : OutputStream() {
    override fun write(b: Int) = term.printChar(b.and(0xFF).toChar())
}