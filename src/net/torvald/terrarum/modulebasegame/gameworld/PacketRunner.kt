package net.torvald.terrarum.modulebasegame.gameworld

import net.torvald.terrarum.gameworld.TerrarumSavegameExtrafieldSerialisable
import java.util.TreeMap

/**
 * Manages packet-number-to-actual-packet mapping, and safely puts them into the savegame
 *
 * Created by minjaesong on 2025-02-27.
 */
class PacketRunner : TerrarumSavegameExtrafieldSerialisable {

    private val ledger = TreeMap<Int, IngameNetPacket>()

    operator fun set(id: Int, packet: IngameNetPacket) {
        ledger[id] = packet
    }

    operator fun get(id: Int) = ledger[id]!!

}

