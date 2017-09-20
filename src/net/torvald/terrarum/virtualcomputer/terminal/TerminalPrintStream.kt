package net.torvald.terrarum.virtualcomputer.terminal

import net.torvald.terrarum.virtualcomputer.computer.TerrarumComputer
import java.io.OutputStream
import java.io.PrintStream

/**
 * Created by minjaesong on 2016-09-10.
 */
class TerminalPrintStream(val host: TerrarumComputer) : PrintStream(TerminalOutputStream(host))

class TerminalOutputStream(val host: TerrarumComputer) : OutputStream() {
    override fun write(b: Int) = host.term.printChar(b.and(0xFF).toChar())
}