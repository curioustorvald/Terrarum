package net.torvald.terrarum.virtualcomputer.terminal

import net.torvald.terrarum.virtualcomputer.computer.TerrarumComputer
import java.io.InputStream

/**
 * Created by minjaesong on 16-09-10.
 */
class TerminalInputStream(val host: TerrarumComputer) : InputStream() {

    private val pauseLock = java.lang.Object()

    override fun read(): Int {
        System.err.println("TerminalInputStream.read called")

        //System.err.println(Thread.currentThread().name)
        // would display "LuaJ Separated", which means this InputStream will not block main thread


        host.openStdin()


        synchronized(this) {
            (this as java.lang.Object).wait()
        }

        System.err.println("TerminalInputStream.read exit")



        return host.stdinInput
    }

    override fun read(b: ByteArray?): Int {
        TODO()
    }

    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        TODO()
    }

}