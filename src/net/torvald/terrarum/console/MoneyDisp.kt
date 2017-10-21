package net.torvald.terrarum.console

import net.torvald.random.HQRNG

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
        Echo("Usage: money [amount] — Prints given or random amount of money")
    }
}