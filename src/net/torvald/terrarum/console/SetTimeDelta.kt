package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 16-03-20.
 */
class SetTimeDelta : ConsoleCommand {

    val HARD_LIMIT = 60

    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            if (args[1].toInt() > HARD_LIMIT)
                Error().execute("Delta too large -- acceptable delta is 0-60.")

            Terrarum.ingame.world.time.setTimeDelta(args[1].toInt())
            if (Terrarum.ingame.world.time.timeDelta == 0)
                Echo().execute("時間よ止まれ！ザ・ワルド！！")
            else
                Echo().execute("Set time delta to ${Terrarum.ingame.world.time.timeDelta}")
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo().execute("usage: settimedelta <int>")
    }
}