package net.torvald.terrarum.gameactors

import net.torvald.terrarum.gameworld.WorldTime
import org.newdawn.slick.Input

/**
 * An actor (NPC) which has life and death,
 * though death might not exist if it has achieved immortality :)
 *
 * NOTE: all canonical NPCs are must be HistoricalFigure!! (double excl mark, bitch)
 *
 * Created by minjaesong on 16-10-10.
 */
open class HistoricalFigure(val born: GameDate, val dead: GameDate? = null, realAirFriction: Boolean = false) : ActorWithSprite(ActorOrder.MIDDLE, realAirFriction) {

    init {
        this.actorValue["_bornyear"] = born.year
        this.actorValue["_borndays"] = born.yearlyDay

        if (dead != null) {
            this.actorValue["_deadyear"] = dead.year
            this.actorValue["_deaddays"] = dead.yearlyDay
        }
    }

}

data class GameDate(val year: Int, val yearlyDay: Int) {
    operator fun plus(other: GameDate): GameDate {
        var newyd = this.yearlyDay + other.yearlyDay
        var newy = this.year + other.year

        if (newyd > WorldTime.YEAR_DAYS) {
            newyd -= WorldTime.YEAR_DAYS
            newy += 1
        }

        return GameDate(newy, newyd)
    }

    operator fun minus(other: GameDate): GameDate {
        var newyd = this.yearlyDay - other.yearlyDay
        var newy = this.year - other.year

        if (newyd < 0) {
            newyd += WorldTime.YEAR_DAYS
            newy -= 1
        }

        return GameDate(newy, newyd)
    }
}