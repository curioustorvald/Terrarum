package net.torvald.terrarum.modulecomputers.virtualcomputer.peripheral

import net.torvald.terrarum.modulecomputers.virtualcomputer.computer.TerrarumComputer
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue

/**
 * Virtual driver for 4-track squarewave PSG, which has no ability of changing a duty cycle
 * but has a volume control (you'll need some other tracker than MONOTONE)
 *
 * Created by minjaesong on 2016-09-27.
 */
internal class PeripheralPSG(val host: net.torvald.terrarum.modulecomputers.virtualcomputer.computer.TerrarumComputer)
: net.torvald.terrarum.modulecomputers.virtualcomputer.peripheral.Peripheral("psg") {

    override val memSize = 1024

    override fun loadLib(globals: Globals) {
        globals["psg"] = LuaTable()
    }

}