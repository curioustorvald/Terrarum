package net.torvald.terrarum.virtualcomputers.worldobject

import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.FixturesBase
import net.torvald.terrarum.virtualcomputers.terminal.SimpleTextTerminal
import net.torvald.terrarum.virtualcomputers.terminal.Terminal
import net.torvald.terrarum.virtualcomputers.worldobject.ui.UITextTerminal
import org.newdawn.slick.Color
import java.util.*

/**
 * Created by minjaesong on 16-09-08.
 */
class FixturesBasicTerminal(phosphor: Color) : FixturesBase() {

    val terminal: Terminal = SimpleTextTerminal(phosphor, 80, 25)
    val ui = UITextTerminal(terminal)

    init {
        collisionFlag = COLLISION_PLATFORM

        actorValue[AVKey.UUID] = UUID.randomUUID().toString()
    }

}