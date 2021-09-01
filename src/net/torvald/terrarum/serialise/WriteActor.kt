package net.torvald.terrarum.serialise

import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64Writer

/**
 * Created by minjaesong on 2021-08-24.
 */
object WriteActor {

    operator fun invoke(actor: Actor): String {
        val s = Common.jsoner.toJson(actor, actor.javaClass)
        return """{"class":"${actor.javaClass.canonicalName}",${s.substring(1)}"""
    }

    fun encodeToByteArray64(actor: Actor): ByteArray64 {
        val baw = ByteArray64Writer(Common.CHARSET)

        Common.jsoner.toJson(actor, actor.javaClass, baw)
        baw.flush(); baw.close()

        return baw.toByteArray64()
    }

}