package net.torvald.terrarum.virtualcomputer.terminal

import net.torvald.terrarum.virtualcomputer.computer.TerrarumComputer
import java.io.InputStream

/**
 * Created by minjaesong on 2016-09-10.
 */
class TerminalInputStream(val host: TerrarumComputer) : InputStream() {

    override fun read(): Int {
        //System.err.println(Thread.currentThread().name)
        // would display "LuaJ Separated", which means this InputStream will not block main thread

        host.openStdin()
        synchronized(this) {
            (this as java.lang.Object).wait()
        }

        return host.stdinInput
    }

}