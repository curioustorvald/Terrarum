package net.torvald.terrarum

import com.badlogic.gdx.Screen
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent

/**
 * Created by minjaesong on 2021-11-06.
 */
interface TerrarumGamescreen : Screen {
    fun inputStrobed(e: TerrarumKeyboardEvent)
}