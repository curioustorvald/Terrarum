package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum

/**
 * Created by minjaesong on 2016-03-20.
 */
internal object SetTimeDelta : ConsoleCommand {

    val HARD_LIMIT = 60

    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            Terrarum.ingame!!.world.time.timeDelta = args[1].toInt()
            if (Terrarum.ingame!!.world.time.timeDelta == 0)
                Echo("時間よ止まれ！ザ・ワルド！！")
            else
                Echo("Set time delta to ${Terrarum.ingame!!.world.time.timeDelta}")
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("usage: settimedelta <int>")
    }
}