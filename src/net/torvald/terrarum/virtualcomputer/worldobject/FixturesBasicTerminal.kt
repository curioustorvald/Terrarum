package net.torvald.terrarum.virtualcomputer.worldobject

import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.FixturesBase
import net.torvald.terrarum.virtualcomputer.terminal.SimpleTextTerminal
import net.torvald.terrarum.virtualcomputer.terminal.Terminal
import net.torvald.terrarum.virtualcomputer.worldobject.ui.UITextTerminal
import org.newdawn.slick.Color
import java.util.*

/**
 * Created by minjaesong on 16-09-08.
 */
class FixturesBasicTerminal(phosphor: Color) : FixturesBase() {

    val vt: Terminal = SimpleTextTerminal(phosphor, 80, 25)
    val ui = UITextTerminal(vt)

    init {
        collisionFlag = COLLISION_PLATFORM

        actorValue[AVKey.UUID] = UUID.randomUUID().toString()
    }

}