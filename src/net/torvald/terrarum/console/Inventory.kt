package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.ActorInventory
import net.torvald.terrarum.gameactors.Player
import net.torvald.terrarum.gameactors.Pocketed
import net.torvald.terrarum.itemproperties.ItemPropCodex

/**
 * Created by SKYHi14 on 2016-12-12.
 */
internal object Inventory : ConsoleCommand {

    private var target: ActorInventory = Terrarum.ingame.player.inventory

    override fun execute(args: Array<String>) {
        if (args.size == 1) {
            printUsage()
        } else {
            when (args[1]) {
                "list"   -> listInventory()
                "add"    -> addItem(args[2].toInt(), args[3].toInt())
                "target" -> setTarget(args[2].toInt())
                "assign" -> assignQuickBar(args[2].toInt(), args[3].toInt())
                else     -> printUsage()
            }
        }
    }

    private fun listInventory() {
        if (target.getTotalUniqueCount() == 0) {
            Echo.execute("(inventory empty)")
        } else {
            target.forEach { refId, amount ->
                if (amount == 0) {
                    Error.execute("Unexpected zero-amounted item: ID $refId")
                }
                Echo.execute("ID $refId${if (amount > 1) " ($amount)" else ""}")
            }
        }
    }

    private fun setTarget(actorRefId: Int = Player.PLAYER_REF_ID) {
        val actor = Terrarum.ingame.getActorByID(actorRefId)
        if (actor !is Pocketed) {
            Error.execute("Cannot edit inventory of incompatible actor: $actor")
        } else {
            target = actor.inventory
        }
    }

    private fun addItem(refId: Int, amount: Int = 1) {
        target.add(ItemPropCodex.getProp(refId), amount)
    }

    private fun assignQuickBar(refId: Int, index: Int) {

    }

    override fun printUsage() {
        Echo.execute("Usage: inventory command arguments")
        Echo.execute("Available commands:")
        Echo.execute("list | assign slot | add itemid [amount] | target [actorid]")
    }
}