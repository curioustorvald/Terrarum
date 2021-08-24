package net.torvald.terrarum.serialise

import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64

/**
 * Created by minjaesong on 2021-08-23.
 */
class WriteWorld(val ingame: TerrarumIngame) {

    open fun invoke(): String {
        val world = ingame.world
        return "{${world.getJsonFields().joinToString(",\n")}}"
    }

    fun encodeToByteArray64(): ByteArray64 {
        val world = ingame.world
        val ba = ByteArray64()
        ba.add('{'.code.toByte())
        world.getJsonFields().forEachIndexed { index, str ->
            if (index > 0) {
                ba.add(','.code.toByte())
                ba.add('\n'.code.toByte())
            }
            str.toByteArray().forEach { ba.add(it) }
        }
        ba.add('}'.code.toByte())
        return ba
    }
}
