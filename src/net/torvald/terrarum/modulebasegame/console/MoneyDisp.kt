package net.torvald.terrarum.modulebasegame.console

import net.torvald.EMDASH
import net.torvald.random.HQRNG
import net.torvald.terrarum.console.ConsoleAlias
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo

@ConsoleAlias("money")
object MoneyDisp : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            Echo("¤${0x3000.toChar()}${args[1]}")
        }
        else {
            Echo("¤${0x3000.toChar()}${HQRNG().nextInt(100000)}")
        }
    }

    override fun printUsage() {
        Echo("Usage: money [amount] $EMDASH Prints given or random amount of money")
    }
}