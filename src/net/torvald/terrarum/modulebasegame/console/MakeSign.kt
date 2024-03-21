package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZEF
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.itemproperties.Item
import net.torvald.terrarum.modulebasegame.gameitems.ItemTextSignCopper
import net.torvald.unicode.TIMES

/**
 * Created by minjaesong on 2024-03-21.
 */
class MakeSign : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size !in 2..3) {
            printUsage(); return
        }

        val text = args[1]
        val textLen = App.fontGame.getWidth(text)
        val panelCount = (args.getOrNull(2)?.toInt() ?: (textLen / TILE_SIZEF).ceilToInt()).coerceAtLeast(2)

        val actorInventory = INGAME.actorNowPlaying!!.inventory

        val item = ItemTextSignCopper(Item.COPPER_SIGN).makeDynamic(actorInventory).also {
            it.extra["signContent"] = text
            it.extra["signPanelCount"] = panelCount
            it.nameSecondary = "[$panelCount${TIMES}2] $text"
        }

        actorInventory.add(item)
        Echo("Sign added: ${item.nameSecondary}")
    }

    override fun printUsage() {
        Echo("Usage: makesign <text>")
        Echo("Usage: makesign <text> <panelcount>")
        Echo("If panel count is not given, smallest possible panel count will be used.")
    }
}