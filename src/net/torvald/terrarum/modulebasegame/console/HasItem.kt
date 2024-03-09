package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.*
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo

/**
 * Created by minjaesong on 2024-03-10.
 */
object HasItem : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size != 2)
            printUsage()
        else {
            val it = args[1]
            val hasBlock = BlockCodex.getOrNull(it) != null
            val blockTags = BlockCodex.getOrNull(it)?.tags?.toList()?.sorted()?.joinToString()?.let { "($it)" } ?: ""

            val hasItem = ItemCodex[it] != null
            val itemTags = ItemCodex[it]?.tags?.toList()?.sorted()?.joinToString()?.let { "($it)" } ?: ""

            val hasWire = WireCodex.getOrNull(it) != null
            val wireTags = WireCodex.getOrNull(it)?.tags?.toList()?.sorted()?.joinToString()?.let { "($it)" } ?: ""

            val hasOre = OreCodex.getOrNull(it) != null
            val oreTags = OreCodex.getOrNull(it)?.tags?.toList()?.sorted()?.joinToString()?.let { "($it)" } ?: ""


            Echo("${ccY}hasBlock? $ccG$hasBlock $ccO$blockTags")
            Echo("${ccY}hasWire? $ccG$hasWire $ccO$wireTags")
            Echo("${ccY}hasOre? $ccG$hasOre $ccO$oreTags")
            Echo("${ccM}hasItem? $ccG$hasItem $ccO$itemTags")
        }
    }

    override fun printUsage() {
        Echo("Usage: hasitem itemid")
        Echo("Prints the information if the item exists in the game")
    }
}