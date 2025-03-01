package net.torvald.terrarum.modulebasegame.gameworld

import net.torvald.random.HQRNG
import net.torvald.terrarum.gameworld.TerrarumSavegameExtrafieldSerialisable
import java.util.TreeMap

/**
 * Manages packet-number-to-actual-packet mapping, and safely puts them into the savegame
 *
 * Created by minjaesong on 2025-02-27.
 */
class PacketRunner : TerrarumSavegameExtrafieldSerialisable {

    @Transient private val rng = HQRNG()

    private val ledger = TreeMap<Int, IngameNetPacket>()

    operator fun set(id: Int, packet: IngameNetPacket) {
        ledger[id] = packet
    }

    operator fun get(id: Int) = ledger[id]!!

    fun addPacket(packet: IngameNetPacket): Int {
        var i = rng.nextInt()
        while (ledger.containsKey(i)) {
            i = rng.nextInt()
        }

        ledger[i] = packet

        return i
    }
}

