package net.torvald.terrarum.modulecomputers.virtualcomputer.peripheral

/**
 * Abstraction of the communication line. (e.g. serial cable)
 *
 * A cable may have multiple of CommLines (e.g. serial cable need two for Tx and Rx)
 *
 * Created by minjaesong on 2019-07-10.
 */
open class CommLine(val bandwidth: Int) {

    open val postbox = StringBuilder()

    /**
     * Returns how many bytes are actually posted, e.g. 0 when band limit is exceeded.
     */
    open fun post(msg: String): Int {
        if (bandwidth >= msg.length) {
            postbox.append(msg)
            return msg.length
        }
        else if (postbox.length >= bandwidth) {
            return 0
        }
        else {
            postbox.append(msg.substring(0 until (bandwidth - postbox.length)))
            return bandwidth - postbox.length
        }
    }

    open fun get(): String {
        val s = postbox.toString()
        postbox.clear()
        return s
    }

}