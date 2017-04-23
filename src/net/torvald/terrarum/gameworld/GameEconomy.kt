package net.torvald.terrarum.gameworld

import net.torvald.terrarum.gameactors.ActorID

/**
 * This world is economically isolated system. Economy will be important to make player keep playing,
 * when all the necessary contents are set and implemented to the production.
 *
 * Created by SKYHi14 on 2017-04-23.
 */
class GameEconomy {

    val transactionHistory = TransanctionHistory()

}

class TransanctionHistory {

    private val entries = ArrayList<TransanctionHistory>()

    /**
     * @param to set 0 to indicate the money was lost to void
     */
    data class TransactionEntry(val from: ActorID, val to: ActorID, val amount: Long) {
        override fun toString() = "$from -> $to; $amount"
    }

}

