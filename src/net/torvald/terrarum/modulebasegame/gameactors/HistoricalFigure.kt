package net.torvald.terrarum.modulebasegame.gameactors


/**
 * An actor (NPC) which has life and death,
 * though death might not exist if it has achieved immortality :)
 *
 * NOTE: all canonical NPCs are must be HistoricalFigure!! (double excl mark, bitch)
 *
 * This interface is just a marker. Actual historical information must be contained as the Actor Value with:
 *
 * "__borntime" // time_t
 * "__deadtime" // time_t
 *
 * Created by minjaesong on 2016-10-10.
 */
interface HistoricalFigure {



}

