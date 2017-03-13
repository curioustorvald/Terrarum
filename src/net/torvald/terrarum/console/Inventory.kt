package net.torvald.terrarum.console

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.ActorInventory
import net.torvald.terrarum.gameactors.Player
import net.torvald.terrarum.gameactors.Pocketed
import net.torvald.terrarum.gameitem.EquipPosition
import net.torvald.terrarum.itemproperties.ItemCodex

/**
 * Created by minjaesong on 2016-12-12.
 */
internal object Inventory : ConsoleCommand {

    private var target: Pocketed = Terrarum.ingame!!.player

    override fun execute(args: Array<String>) {
        if (args.size == 1) {
            printUsage()
        }
        else {
            when (args[1]) {
                "list"   -> listInventory()
                "add"    -> if (args.size > 3) addItem(args[2].toInt(), args[3].toInt())
                            else               addItem(args[2].toInt())
                "target" -> setTarget(args[2].toInt())
                "equip"  -> equipItem(args[2].toInt())
                else     -> printUsage()
            }
        }
    }

    private fun listInventory() {
        if (target.inventory.getTotalUniqueCount() == 0) {
            Echo("(inventory empty)")
        }
        else {
            target.inventory.forEach { refId, amount ->
                if (amount == 0) {
                    EchoError("Unexpected zero-amounted item: ID $refId")
                }
                Echo("ID $refId${if (amount > 1) " ($amount)" else ""}")
            }
        }
    }

    private fun setTarget(actorRefId: Int = Player.PLAYER_REF_ID) {
        val actor = Terrarum.ingame!!.getActorByID(actorRefId)
        if (actor !is Pocketed) {
            EchoError("Cannot edit inventory of incompatible actor: $actor")
        }
        else {
            target = actor
        }
    }

    private fun addItem(refId: Int, amount: Int = 1) {
        target.inventory.add(ItemCodex[refId], amount)
    }

    private fun equipItem(refId: Int) {
        val item = ItemCodex[refId]

        // if the item does not exist, add it first
        if (!target.inventory.contains(item)) {
            target.inventory.add(item)
        }

        target.itemEquipped[item.equipPosition] = item
    }

    override fun printUsage() {
        Echo("Usage: inventory command arguments")
        Echo("Available commands:")
        Echo("list | assign slot | add itemid [amount] | target [actorid] | equip itemid")
    }
}