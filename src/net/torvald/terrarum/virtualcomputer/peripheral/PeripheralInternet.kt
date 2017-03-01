package net.torvald.terrarum.virtualcomputer.peripheral

import org.luaj.vm2.Globals
import net.torvald.terrarum.virtualcomputer.computer.TerrarumComputer
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

/**
 * Provides internet access.
 *
 * Created by minjaesong on 16-09-24.
 */
internal class PeripheralInternet(val host: TerrarumComputer)
: Peripheral("internet"){

    override fun loadLib(globals: Globals) {
        globals["internet"] = LuaTable()
        globals["internet"]["fetch"] = FetchWebPage()
    }

    class FetchWebPage() : OneArgFunction() {
        override fun call(urlstr: LuaValue): LuaValue {
            val url = URL(urlstr.checkjstring())
            val inputstream = BufferedReader(InputStreamReader(url.openStream()))

            var inline = ""
            var readline = inputstream.readLine()
            while (readline != null) {
                inline += readline
                readline = inputstream.readLine()
            }
            inputstream.close()

            return LuaValue.valueOf(inline)
        }
    }

}