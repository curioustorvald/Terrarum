package net.torvald.terrarum.virtualcomputer.peripheral

import net.torvald.terrarum.virtualcomputer.computer.BaseTerrarumComputer
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue

/**
 * Virtual driver for 4-track squarewave PSG, which has no ability of changing a duty cycle
 * but has a volume control (you'll need some other tracker than MONOTONE)
 *
 * Created by minjaesong on 16-09-27.
 */
internal class PeripheralPSG(val globals: Globals, val host: BaseTerrarumComputer)
: Peripheral(globals, "psg") {

    override fun loadLib() {
        super.loadLib()
    }

}