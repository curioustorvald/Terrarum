package net.torvald.terrarum.modulebasegame.gameworld

import net.torvald.random.HQRNG
import net.torvald.terrarum.gameworld.TerrarumSavegameExtrafieldSerialisable
import java.util.TreeMap

/**
 * Manages packet-number-to-actual-packet mapping, and safely puts them into the savegame
 *
 * Created by minjaesong on 2025-02-27.
 */
class NetRunner : TerrarumSavegameExtrafieldSerialisable {

    @Transient private val rng = HQRNG()

    private val ledger = TreeMap<Int, NetFrame>()

    operator fun set(id: Int, packet: NetFrame) {
        ledger[id] = packet
    }

    operator fun get(id: Int) = ledger[id]!!

    fun addFrame(frame: NetFrame): Int {
        var i = rng.nextInt()
        while (ledger.containsKey(i)) {
            i = rng.nextInt()
        }

        ledger[i] = frame

        return i
    }

    fun purgeDeadFrames() {
        ledger.filter { it.value.getFrameType() == "invalid" }.map { it.key }.forEach {
            ledger.remove(it)
        }
    }
}

