package net.torvald.terrarum.modulebasegame

import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameitems.isDynamic
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.modulebasegame.gameactors.Pocketed
import net.torvald.util.SortedArrayList
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation

/**
 * @param versionAnnotation If the version of the savegame meets the condition, the filter will be run
 * - `all`: applies to all versions
 * - `0.3.1`: applies to any past versions up to 0.3.1
 * - `0.3.1+`: applies to any future versions including 0.3.1
 * - `0.3.1!`: applies to verson 0.3.1 only
 * - `0.3.1-0.3.4`: applies to version 0.3.1 to 0.3.4
 * - `0.3.1-0.3.4;0.4.3-0.4.5`: applies to (0.3.1 to 0.3.4) and (0.4.3-0.4.5)
 * - `0.3.1-0.3.4;0.5.0+`: applies to (0.3.1 to 0.3.4) and (0.5.0 and onward)
 *
 * Following syntaxes are considered as undefined behaviours:
 * - `0.3.1-0.3.4;0.5.0`
 */
annotation class AppliedVersion(val versionAnnotation: String)

/**
 * Created by minjaesong on 2023-09-29.
 */
internal object SavegameMigrator {

    private fun Char.isNotAsciiDigit() = this.code !in 0x30..0x39

    private fun toVersionIndex(a_dot_b_dot_c: String): Long {
        val abc = a_dot_b_dot_c.split('.').map { it.toLong() }
        if (abc.size != 3) throw IllegalArgumentException("Parse error: $a_dot_b_dot_c")

        return abc[0].shl(48) or abc[1].shl(24) or abc[2]
    }

    private fun annotationMatches(fileVersion: Long, annotation: AppliedVersion?): Boolean {
        if (annotation == null) return false
        if (annotation.versionAnnotation.equals("all")) return true

        var matched = false

        annotation.versionAnnotation.split(";").forEach { query ->
            // is range?
            if (query.contains('-')) {
                val from0 = query.substringBefore('-')
                val to0 = query.substringAfter('-')
                if (from0.last().isNotAsciiDigit() || to0.last().isNotAsciiDigit())
                    throw IllegalArgumentException("Illegal query '$query'")

                val from = toVersionIndex(from0)
                val to = toVersionIndex(to0)

                if (from > to) throw IllegalArgumentException("Illegal query '$query'")

                matched = matched or (fileVersion in from..to)
            }
            else if (query.endsWith('+') || query.endsWith('!')) {
                val operator = query.last()
                val specVersion = toVersionIndex(query.substringBefore(operator))

                matched = matched or when (operator) {
                    '+' -> (fileVersion >= specVersion)
                    '!' -> (fileVersion == specVersion)
                    else -> throw IllegalArgumentException("Unknown operator '$operator' for query '$query'")
                }
            }
            else {
                matched = matched or (fileVersion <= toVersionIndex(query))
            }
        }

        return matched
    }

    operator fun invoke(worldVersion: Long, playerVersion: Long, actors0: SortedArrayList<Actor>) {
        if (worldVersion == playerVersion) {
            this::class.declaredFunctions.filter {
                annotationMatches(worldVersion, it.findAnnotation())
            }.forEach { func ->
                printdbg(this, func.toString())
                actors0.forEach { actor -> func.call(this, actor) }
            }
        }
        else {
            val nonPlayers = ArrayList<Actor>()
            val players = ArrayList<IngamePlayer>()
            for (actor in actors0) {
                if (actor is IngamePlayer) players.add(actor)
                else nonPlayers.add(actor)
            }

            this::class.declaredFunctions.filter {
                annotationMatches(worldVersion, it.findAnnotation())
            }.forEach { func ->
                nonPlayers.forEach { actor -> func.call(this, actor) }
            }

            this::class.declaredFunctions.filter {
                annotationMatches(playerVersion, it.findAnnotation())
            }.forEach { func ->
                players.forEach { player -> func.call(this, player) }
            }
        }
    }




    /***********************************/
    /* INSERT MIGRATION FUNCTIONS HERE */
    /***********************************/


    @AppliedVersion("all")
    fun mergeUnlitBlocks(actor: Actor) {
        if (ItemCodex.isEmpty()) throw Error("ItemCodex is empty")

        if (actor is Pocketed) {
            val oldItemEquipped = actor.inventory.itemEquipped.copyOf()
            val oldQuickSlot = actor.inventory.quickSlot.copyOf()
            val oldItems = actor.inventory.clear()

            oldItems.forEach { (itm, qty) ->
                actor.inventory.add(itm, qty)
            }

            oldItemEquipped.forEachIndexed { index, id0 ->
                if (id0?.isDynamic() == true)
                    actor.inventory.itemEquipped[index] = id0
                else {
                    val id = FixtureInventory.filterItem(ItemCodex[id0])
                    actor.inventory.itemEquipped[index] = id?.dynamicID
                }
            }

            oldQuickSlot.forEachIndexed { index, id0 ->
                if (id0?.isDynamic() == true)
                    actor.inventory.quickSlot[index] = id0
                else {
                    val id = FixtureInventory.filterItem(ItemCodex[id0])
                    actor.inventory.quickSlot[index] = id?.dynamicID
                }
            }
        }
    }




}