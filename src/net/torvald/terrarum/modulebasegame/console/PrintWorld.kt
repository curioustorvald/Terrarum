package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.gameworld.BlockAddress
import kotlin.reflect.full.memberProperties

/**
 * Created by minjaesong on 2021-08-26.
 */
object PrintWorld : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {
            val w = Terrarum.ingame!!.world

            val field = w::class.java.getDeclaredField(args[1])
            val fieldAccessibility = field.isAccessible

            field.isAccessible = true
            Echo(field.get(w).toString())
            Echo(field.get(w).javaClass.simpleName)
            w.wirings.forEach { i, node ->
                Echo(i.toString())
            }

            field.isAccessible = fieldAccessibility
        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {
        Echo("Usage: Exportworld <field>")
    }
}