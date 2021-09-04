package net.torvald.terrarum.gameactors.faction

import java.util.*

/**
 * Created by minjaesong on 2016-05-09.
 */
class FactionCodex {
    val factionContainer = ArrayList<Faction>()

    fun clear() = factionContainer.clear()

    internal constructor()

    fun hasFaction(ID: FactionID): Boolean =
            if (factionContainer.size == 0)
                false
            else
                factionContainer.binarySearch(ID) >= 0

    fun addFaction(faction: Faction) {
        if (hasFaction(faction.referenceID))
            throw RuntimeException("Faction with ID ${faction.referenceID} already exists.")
        factionContainer.add(faction)
        insertionSortLastElem(factionContainer) // we can do this as we are only adding single actor
    }

    fun getFactionByID(ID: FactionID): Faction {
        if (factionContainer.size == 0) throw IllegalArgumentException("Faction with ID $ID does not exist.")

        val index = factionContainer.binarySearch(ID)
        if (index < 0)
            throw IllegalArgumentException("Faction with ID $ID does not exist.")
        else
            return factionContainer[index]
    }

    private fun insertionSortLastElem(arr: ArrayList<Faction>) {
        val x: Faction
        var j: Int
        val index: Int = arr.size - 1
        x = arr[index]
        j = index - 1
        while (j > 0 && arr[j] > x) {
            arr[j + 1] = arr[j]
            j -= 1
        }
        arr[j + 1] = x
    }

    private fun ArrayList<Faction>.binarySearch(ID: FactionID): Int {
        // code from collections/Collections.kt
        var low = 0
        var high = factionContainer.size - 1

        while (low <= high) {
            val mid = (low + high).ushr(1) // safe from overflows
            val midVal = get(mid)

            if (ID > midVal.referenceID)
                low = mid + 1
            else if (ID < midVal.referenceID)
                high = mid - 1
            else
                return mid // key found
        }
        return -(low + 1)  // key not found
    }
}