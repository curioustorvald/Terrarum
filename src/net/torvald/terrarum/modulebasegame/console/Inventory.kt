package net.torvald.terrarum.modulebasegame.console

import net.torvald.terrarum.*
import net.torvald.terrarum.Terrarum.PLAYER_REF_ID
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarum.console.EchoError
import net.torvald.terrarum.gameactors.ActorID
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.modulebasegame.gameactors.Pocketed

/**
 * Created by minjaesong on 2016-12-12.
 */
internal object Inventory : ConsoleCommand {

    private var targetID: ActorID = 0

    private fun tryTargetActivePlayer() {
        targetID = INGAME.actorNowPlaying?.referenceID ?: 0
    }

    override fun execute(args: Array<String>) {
        if (args.size == 1) {
            printUsage()
        }
        else if (args[1] == "target") {
            if (args[2].lowercase() == "player") tryTargetActivePlayer() else targetID = args[2].toInt()
        }
        else {
            if (targetID == 0) tryTargetActivePlayer()
            val actor = getActor()
            if (actor != null) {
                when (args[1]) {
                    "list"   -> listInventory(actor)
                    "add"    -> if (args.size > 3) addItem(actor, args[2], args[3].toLong()) else addItem(actor, args[2])
                    "remove" -> if (args.size > 3) removeItem(actor, args[2], args[3].toLong()) else removeItem(actor, args[2])
                    "equip"  -> equipItem(actor, args[2])
                    "unequip"-> unequipItem(actor, args[2])
                    "clear" -> actor.inventory.clear()
                    else     -> printUsage()
                }
            }
            else {
                Echo("Actor $targetID is not Pocketed or does not exist")
            }
        }
    }

    private fun getActor() = Terrarum.ingame?.getActorByID(targetID) as? Pocketed

    private fun listInventory(actor: Pocketed) {
        if (actor.inventory.totalUniqueCount == 0L) {
            Echo("(inventory empty)")
        }
        else {
            actor.inventory.forEach { val (item, amount) = it
                if (amount == 0L) {
                    EchoError("Unexpected zero-amounted item: ID $item")
                }
                Echo("${ccW}ID $ccY$item${if (amount > 1) "$ccW ($ccG$amount$ccW)" else ""}")
            }
        }
    }

    private fun addItem(actor: Pocketed, refId: ItemID, amount: Long = 1L) {
        val item = ItemCodex[refId]
        if (item != null) {
            actor.addItem(item, amount)
            Echo("${ccW}Added$ccG $amount$ccY $refId$ccW to $ccO$targetID")
        }
        else EchoError("No such item: $refId")
    }

    private fun removeItem(actor: Pocketed, refId: ItemID, amount: Long = 1L) {
        val item = ItemCodex[refId]
        if (item != null) {
            actor.removeItem(item, amount)
            Echo("${ccW}Removed$ccG $amount$ccY $refId$ccW from $ccO$targetID")
        }
        else EchoError("No such item: $refId")
    }

    private fun equipItem(actor: Pocketed, refId: ItemID) {
        val item = ItemCodex[refId]
        if (item != null) {
            actor.equipItem(item)
            Echo("${ccW}Equipped$ccY $refId$ccW to $ccO$targetID")
        }
        else EchoError("No such item: $refId")
    }

    private fun unequipItem(actor: Pocketed, refId: ItemID) {
        val item = ItemCodex[refId]
        if (item != null) {
            actor.unequipItem(item)
            Echo("${ccW}Stripped$ccY $refId$ccW out of $ccO$targetID")
        }
        else EchoError("No such item: $refId")
    }

    override fun printUsage() {
        Echo("${ccW}Usage:$ccY inventory$ccG [command]")
        Echo("${ccW}Available commands:")
        Echo("$ccY    target$ccG [actorid|“player”]")
        Echo("$ccY    add$ccG [itemid] [amount]")
        Echo("$ccY    remove$ccG [itemid] [amount]")
        Echo("$ccY    equip$ccG [itemid]")
        Echo("$ccY    unequip$ccG [itemid]")
        Echo("$ccY    list")
        Echo("Currently targeted: $ccO$targetID")
    }
}