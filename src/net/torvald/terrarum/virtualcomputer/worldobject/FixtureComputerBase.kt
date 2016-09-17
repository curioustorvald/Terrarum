package net.torvald.terrarum.virtualcomputer.worldobject

import net.torvald.terrarum.gameactors.FixtureBase
import net.torvald.terrarum.virtualcomputer.computer.BaseTerrarumComputer
import net.torvald.terrarum.virtualcomputer.terminal.SimpleTextTerminal
import net.torvald.terrarum.virtualcomputer.terminal.Terminal
import org.newdawn.slick.GameContainer
import java.io.PrintStream
import java.security.SecureRandom
import java.util.*

/**
 * Created by minjaesong on 16-09-08.
 */
open class FixtureComputerBase() : FixtureBase() {

    val processorCycle: Int // number of Lua statement to process per tick (1/100 s)
        get() = ComputerPartsCodex.getProcessorCycles(computerInside!!.computerValue.getAsInt("processor") ?: -1)
    val memSize: Int // max: 8 GB
        get() {
            var size = 0
            for (i in 0..3)
                size += ComputerPartsCodex.getRamSize(computerInside!!.computerValue.getAsInt("memSlot$i") ?: -1)

            return size
        }

    /** Connected terminal */
    var terminal: FixtureBasicTerminal? = null

    var computerInside: BaseTerrarumComputer? = null

    init {
        // UUID of the "brain"
        actorValue["computerid"] = "none"


        collisionFlag = COLLISION_PLATFORM
    }

    ////////////////////////////////////
    // get the computer actually work //
    ////////////////////////////////////

    fun attachTerminal(uuid: String) {
        val fetchedTerminal = getTerminalByUUID(uuid)
        computerInside = BaseTerrarumComputer(fetchedTerminal)
        actorValue["computerid"] = computerInside!!.UUID
    }

    fun detatchTerminal() {
        terminal = null
    }

    private fun getTerminalByUUID(uuid: String): Terminal? {
        TODO("get terminal by UUID. Return null if not found")
    }



    ////////////////
    // game codes //
    ////////////////

    override fun update(gc: GameContainer, delta: Int) {
        super.update(gc, delta)
        if (terminal != null) terminal!!.update(gc, delta)
    }

    fun keyPressed(key: Int, c: Char) {
        if (terminal != null) {
            terminal!!.vt.keyPressed(key, c)
        }
    }
}