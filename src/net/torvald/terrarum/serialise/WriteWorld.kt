package net.torvald.terrarum.serialise

import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64GrowableOutputStream
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64OutputStream
import java.io.Writer

/**
 * Created by minjaesong on 2021-08-23.
 */
open class WriteWorld(val ingame: TerrarumIngame) {

    open fun invoke(): String {
        val world = ingame.world
        world.genver = Common.GENVER
        world.comp = Common.COMP_GZIP
        return Common.jsoner.toJson(world)
    }

    fun encodeToByteArray64(): ByteArray64 {
        val world = ingame.world
        world.genver = Common.GENVER
        world.comp = Common.COMP_GZIP

        val baw = ByteArray64Writer(Common.CHARSET)

        Common.jsoner.toJson(world, baw)
        baw.flush(); baw.close()

        return baw.toByteArray64()
    }

}
