package net.torvald.terrarum.serialise

/**
 * Created by minjaesong on 2021-08-23.
 */
object WriteMeta {


    private fun modnameToOrnamentalHeader(s: String) =
            "\n\n${"#".repeat(16 + s.length)}\n" +
            "##  module: $s  ##\n" +
            "${"#".repeat(16 + s.length)}\n\n"
}


/**
 * Created by minjaesong on 2021-09-03.
 */
object ReadMeta {


}