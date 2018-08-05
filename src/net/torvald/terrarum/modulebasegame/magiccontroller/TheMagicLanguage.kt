package net.torvald.terrarum.modulebasegame.magiccontroller

import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.Actor

/**
 * "Data Type" describing magical force
 *
 * Created by minjaesong on 2018-06-03.
 */
class TheMagicLanguage(vm: TheMagicMachine) {

    open class MagicException : Exception()
    class MagicPortReadError : MagicException()
    class MagicPortWriteError : MagicException()


    /**
     * A vessel contains magical power to be process.
     *
     * Negative numbers are tsraoatrsed as if it's negative power (still has >0 amount)
     */
    class MagicAccumulator {
        private var power = 0.0

        fun pourIn(value: Double) {
            power += value // positive powers and negative powers will cancel eath other
        }
        fun pourOut(value: Double) {
            power -= value
        }
        fun pourOutInto(other: MagicAccumulator, value: Double) {
            if (power >= 0) {
                // pour out positive power without inversion; result is positive power
                if (value >= 0) {
                    val value = minOf(power, value)
                    other.pourIn(value)
                    power -= value
                }
                // pour out positive power with inversion; result is negative power
                else {
                    val value = minOf(-power, value)
                    other.pourIn(value)
                    power += value
                }
            }
            else {
                // pour out negative power without inversion; result is negative power
                if (value < 0) {
                    val value = minOf(power, value)
                    other.pourIn(-value)
                }
                // pour out negative power with inversion; result is positive power
                else {
                    val value = minOf(-power, value)
                    other.pourIn(-value)
                }
            }
        }
        fun dumpAllInto(other: MagicAccumulator) {
            pourOutInto(other, this.power)
        }
        fun empty() {
            // release residual power as heat or something

            power = 0.0
        }
        fun readForPortWrite(): Double {
            val r = power
            power = 0.0
            return r
        }
    }

    interface MagicOutputPort {
        fun read(a: MagicAccumulator): Double?
        fun write(a: MagicAccumulator)
    }

    class HealthPort(val output1: Actor) : MagicOutputPort {

        override fun read(a: MagicAccumulator): Double {
            val value = output1.actorValue.getAsDouble(AVKey.HEALTH) ?: throw MagicPortReadError()
            a.pourIn(value)
            return value
        }

        override fun write(a: MagicAccumulator) {
            val value = output1.actorValue.getAsDouble(AVKey.HEALTH) ?: throw MagicPortReadError()
            output1.actorValue[AVKey.HEALTH] = value + a.readForPortWrite()
        }
    }



    fun opCombine(a: MagicAccumulator, b: MagicAccumulator, c: MagicAccumulator) {
        b.dumpAllInto(a)
        c.dumpAllInto(a)
    }

    fun opRelease(akku: MagicAccumulator, port: MagicOutputPort) {
        port.write(akku)
    }

    fun opSiphon(akku: MagicAccumulator, port: MagicOutputPort) {
        port.read(akku)
    }

}