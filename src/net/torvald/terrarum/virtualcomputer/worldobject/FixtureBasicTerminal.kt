package net.torvald.terrarum.virtualcomputer.worldobject

import com.badlogic.gdx.graphics.Color
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.FixtureBase
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.virtualcomputer.computer.TerrarumComputer
import net.torvald.terrarum.virtualcomputer.terminal.Terminal
import net.torvald.terrarum.virtualcomputer.worldobject.ui.UITextTerminal
import java.util.*

/**
 * Created by minjaesong on 2016-09-08.
 */
class FixtureBasicTerminal(world: GameWorld, phosphor: Color) : FixtureBase(world) {

    /*val computer = TerrarumComputer(8)
    val vt: Terminal = SimpleTextTerminal(phosphor, 80, 25, computer)
    val ui = UITextTerminal(vt)

    init {
        computer.attachTerminal(vt)

        collisionFlag = COLLISION_PLATFORM

        actorValue[AVKey.UUID] = UUID.randomUUID().toString()
    }*/

}