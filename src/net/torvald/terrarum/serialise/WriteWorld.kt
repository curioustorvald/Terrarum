package net.torvald.terrarum.serialise

import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64
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
        /*val world = ingame.world
        world.genver = Common.GENVER
        world.comp = Common.COMP_GZIP

        val ba = ByteArray64()
        val bao = ByteArray64OutputStream(ba)
        val wr = object : Writer() {
            override fun close() {
            }

            override fun flush() {
            }

            override fun write(cbuf: CharArray, off: Int, len: Int) {
                bao.write(cbuf.copyOfRange(off, off + len).toString().toByteArray())
            }
        }
        Common.jsoner.toJson(world, wr)
        wr.flush(); wr.close()

        return ba*/

        val ba = ByteArray64()
        this.invoke().toByteArray().forEach { ba.add(it) }
        return ba
    }

}
