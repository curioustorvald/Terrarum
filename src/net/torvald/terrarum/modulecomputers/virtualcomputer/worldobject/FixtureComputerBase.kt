package net.torvald.terrarum.modulecomputers.virtualcomputer.worldobject

import net.torvald.terrarum.modulebasegame.gameactors.FixtureBase
import net.torvald.terrarum.gameworld.GameWorld

/**
 * Created by minjaesong on 2016-09-08.
 */
open class FixtureComputerBase : FixtureBase() {

    /** Connected terminal */
    var terminal: FixtureBasicTerminal? = null

    var computerInside: net.torvald.terrarum.modulecomputers.virtualcomputer.computer.TerrarumComputer? = null

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
        computerInside = net.torvald.terrarum.modulecomputers.virtualcomputer.computer.TerrarumComputer(8)
        computerInside!!.attachTerminal(fetchedTerminal!!)
        actorValue["computerid"] = computerInside!!.UUID
    }

    fun detatchTerminal() {
        terminal = null
    }

    private fun getTerminalByUUID(uuid: String): net.torvald.terrarum.modulecomputers.virtualcomputer.terminal.Terminal? {
        TODO("get terminal by UUID. Return null if not found")
    }



    ////////////////
    // game codes //
    ////////////////

    override fun update(delta: Float) {
        super.update(delta)
        if (terminal != null) terminal!!.update(delta)
    }

    fun keyPressed(key: Int, c: Char) {
        /*if (terminal != null) {
            terminal!!.vt.keyPressed(key, c)
            computerInside!!.keyPressed(key, c)
        }*/
    }
}