package net.torvald.terrarum.console

import net.torvald.terrarum.TerrarumGDX

/**
 * Created by minjaesong on 16-03-20.
 */
internal object SetTimeDelta : ConsoleCommand {

    val HARD_LIMIT = 60

    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            TerrarumGDX.ingame!!.world.time.timeDelta = args[1].toInt()
            if (TerrarumGDX.ingame!!.world.time.timeDelta == 0)
                Echo("時間よ止まれ！ザ・ワルド！！")
            else
                Echo("Set time delta to ${TerrarumGDX.ingame!!.world.time.timeDelta}")
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("usage: settimedelta <int>")
    }
}