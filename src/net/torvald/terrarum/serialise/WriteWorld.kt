package net.torvald.terrarum.serialise

import net.torvald.terrarum.modulebasegame.TerrarumIngame

/**
 * Created by minjaesong on 2021-08-23.
 */
class WriteWorld(val ingame: TerrarumIngame) {

    open fun invoke(): String {
        val world = ingame.world
        return "{${world.getJsonFields().joinToString(",\n")}}"
    }


}
