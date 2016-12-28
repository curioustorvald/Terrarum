package net.torvald.terrarum.gameactors.ai.scripts

/**
 * Created by SKYHi14 on 2016-12-28.
 */
abstract class EncapsulatedString {
    abstract fun getString(): String
    override fun toString() = getString()
}