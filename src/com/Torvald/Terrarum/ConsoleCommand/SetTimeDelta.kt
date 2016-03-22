package com.Torvald.Terrarum.ConsoleCommand

import com.Torvald.Terrarum.Terrarum

/**
 * Created by minjaesong on 16-03-20.
 */
class SetTimeDelta : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            Terrarum.game.map.worldTime.setTimeDelta(args[1].toInt())
            if (Terrarum.game.map.worldTime.timeDelta == 0)
                Echo().execute("時間よ止まれ！ザ・ワルド！！")
            else
                Echo().execute("Set time delta to ${Terrarum.game.map.worldTime.timeDelta}")
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo().execute("usage: settimedelta <int>")
    }
}