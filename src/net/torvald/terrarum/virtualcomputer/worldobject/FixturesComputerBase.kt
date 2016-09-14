package net.torvald.terrarum.virtualcomputer.worldobject

import net.torvald.terrarum.gameactors.FixturesBase
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
open class FixturesComputerBase() : FixturesBase() {

    val processorCycle: Int // number of Lua statement to process per tick (1/100 s)
        get() = ComputerPartsCodex.getProcessorCycles(actorValue.getAsInt("processor") ?: -1)
    val memSize: Int // max: 8 GB
        get() {
            var size = 0
            for (i in 0..3)
                size += ComputerPartsCodex.getRamSize(actorValue.getAsInt("memSlot$i") ?: -1)

            return size
        }

    /** Connected terminal */
    var terminal: FixturesBasicTerminal? = null

    var computerInside: BaseTerrarumComputer? = null

    init {
        actorValue["memslot0"] = -1 // -1 indicates mem slot is empty
        actorValue["memslot1"] = -1 // put index of item here
        actorValue["memslot2"] = -1 // ditto.
        actorValue["memslot3"] = -1 // do.

        actorValue["processor"] = -1 // do.

        // as in "dev/hda"; refers hard disk drive (and no partitioning)
        actorValue["hda"] = "none" // 'UUID rendered as String' or "none"
        actorValue["hdb"] = "none"
        actorValue["hdc"] = "none"
        actorValue["hdd"] = "none"
        // as in "dev/fd1"; refers floppy disk drive
        actorValue["fd1"] = "none"
        actorValue["fd2"] = "none"
        actorValue["fd3"] = "none"
        actorValue["fd4"] = "none"
        // SCSI connected optical drive
        actorValue["sda"] = "none"

        // UUID of this device
        actorValue["uuid"] = UUID.randomUUID().toString()

        collisionFlag = COLLISION_PLATFORM


    }

    ////////////////////////////////////
    // get the computer actually work //
    ////////////////////////////////////

    fun attachTerminal(uuid: String) {
        val fetchedTerminal = getTerminalByUUID(uuid)
        computerInside = BaseTerrarumComputer(fetchedTerminal)
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