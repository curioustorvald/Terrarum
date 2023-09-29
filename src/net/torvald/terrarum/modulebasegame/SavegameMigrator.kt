package net.torvald.terrarum.modulebasegame

import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
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

                matched = matched or (from <= fileVersion && fileVersion <= to)
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
                actors0.forEach { actor -> func.call(actor) }
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
                nonPlayers.forEach { actor -> func.call(actor) }
            }

            this::class.declaredFunctions.filter {
                annotationMatches(playerVersion, it.findAnnotation())
            }.forEach { func ->
                players.forEach { player -> func.call(player) }
            }
        }
    }

    @AppliedVersion("all")
    fun mergeUnlitBlocks(actor: Actor) {
        TODO()
    }
}