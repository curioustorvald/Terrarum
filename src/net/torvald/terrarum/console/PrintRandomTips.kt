package net.torvald.terrarum.console

import net.torvald.terrarum.langpack.Lang
import java.util.*

/**
 * Created by minjaesong on 16-07-04.
 */
internal object PrintRandomTips : ConsoleCommand {
    override fun execute(args: Array<String>) {
        Echo(Lang["GAME_TIPS_${Random().nextInt(Lang.TIPS_COUNT) + 1}"])
    }

    override fun printUsage() {
        Echo("Prints random tips for game.")
    }
}