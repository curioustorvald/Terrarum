package net.torvald.terrarum.modulecomputers.virtualcomputer.terminal

import net.torvald.terrarum.modulecomputers.virtualcomputer.computer.TerrarumComputer
import java.io.OutputStream
import java.io.PrintStream

/**
 * Created by minjaesong on 2016-09-10.
 */
class TerminalPrintStream(val host: net.torvald.terrarum.modulecomputers.virtualcomputer.computer.TerrarumComputer) : PrintStream(TerminalOutputStream(host))

class TerminalOutputStream(val host: net.torvald.terrarum.modulecomputers.virtualcomputer.computer.TerrarumComputer) : OutputStream() {
    override fun write(b: Int) = host.term.printChar(b.and(0xFF).toChar())
}