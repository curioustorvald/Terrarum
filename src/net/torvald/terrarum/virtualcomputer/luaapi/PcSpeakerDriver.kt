package net.torvald.terrarum.virtualcomputer.luaapi

import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import net.torvald.terrarum.virtualcomputer.computer.BaseTerrarumComputer
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.lib.OneArgFunction

/**
 * PC Speaker driver and arpeggiator (MONOTONE-style 4 channels)
 *
 * Notes are tuned to A440, equal temperament. This is an ISO standard.
 *
 * Created by minjaesong on 16-09-27.
 */
class PcSpeakerDriver(val globals: Globals, host: BaseTerrarumComputer) {

    init {
        globals["speaker"] = LuaTable()
        globals["speaker"]["enqueue"] = EnqueueTone(host)
        globals["speaker"]["clear"] = ClearQueue(host)
        globals["speaker"]["retune"] = Retune(globals)
        globals["speaker"]["resetTune"] = ResetTune(globals)
        globals["speaker"]["toFreq"] = StringToFrequency(globals)
        globals["speaker"]["__basefreq__"] = LuaValue.valueOf(BASE_FREQ) // every other PSGs should use this very variable

        // constants
        //      e.g. speaker.A0 returns number 1
        fun Int.toNote(): String = NOTE_NAMES[this % 12] + this.plus(8).div(12).toString()
        fun Int.toNoteAlt(): String = NOTE_NAMES_ALT[this % 12] + this.plus(8).div(12).toString()

        for (i in 1..126) {
            globals["speaker"][i.toNote()] = i // sharps
            globals["speaker"][i.toNoteAlt()] = i // flats
        }
    }

    companion object {
        val BASE_FREQ = 27.5 // frequency of A0
        val NOTE_NAMES = arrayOf("GS", "A", "AS", "B", "C", "CS",
                "D", "DS", "E", "F", "FS", "G")
        val NOTE_NAMES_ALT = arrayOf("Ab", "A", "Bb", "B", "C", "Db",
                "D", "Eb", "E", "F", "Gb", "G")

        /** @param basefreq: Frequency of A-0 */
        fun Int.toFreq(basefreq: Double): Double = basefreq * Math.pow(2.0, (this - 1.0) / 12.0)

        /** @param "A-5", "B4", "C#5", ... */
        fun String.toNoteIndex(): Int {
            var notestr = this.replace("-", "")
            notestr = notestr.replace("#", "S")

            val baseNote = if (notestr.contains("S") || notestr.contains("b"))
                notestr.substring(0, 2)
            else
                notestr.substring(0, 1)

            var note: Int = NOTE_NAMES.indexOf(baseNote) // [0-11]
            if (note < 0) note = NOTE_NAMES_ALT.indexOf(baseNote) // search again
            if (note < 0) throw IllegalArgumentException("Unknown note: $this") // failed to search

            val octave: Int = notestr.replace(Regex("""[^0-9]"""), "").toInt()
            return octave.minus(if (note >= 4) 1 else 0) * 12 + note
        }
    }

    class EnqueueTone(val host: BaseTerrarumComputer) : TwoArgFunction() {
        /**
         * @param freq: number (hertz) or string (A-4, A4, B#2, ...)
         */
        override fun call(millisec: LuaValue, freq: LuaValue): LuaValue {
            if (freq.isnumber())
                host.enqueueBeep(millisec.checkint(), freq.checkdouble())
            else {
                host.enqueueBeep(millisec.checkint(),
                        freq.checkjstring().toNoteIndex()
                                .toFreq(host.luaJ_globals["speaker"]["__basefreq__"].checkdouble())
                )
            }
            return LuaValue.NONE
        }
    }

    class ClearQueue(val host: BaseTerrarumComputer) : ZeroArgFunction() {
        override fun call(): LuaValue {
            host.clearBeepQueue()
            return LuaValue.NONE
        }
    }

    class Retune(val globals: Globals) : LuaFunction() {
        /**
         * Examples: C256, A440, A#440, ...
         */
        override fun call(arg: LuaValue): LuaValue {
            val tuneName = arg.checkjstring()

            val baseNote = if (tuneName.contains("#") || tuneName.contains("b")) tuneName.substring(0, 2) else tuneName.substring(0, 1)
            val freq = tuneName.replace(Regex("""[^0-9]"""), "").toInt()

            // we're assuming the input to be C4, C#4, ... A4, A#4, B4
            // diffPivot corsp. to G#4, A4, ...
            val diffPivot = arrayOf(-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10) // 2^(12 / n)
            var diff = diffPivot[NOTE_NAMES.indexOf(baseNote)]
            if (diff < 0) diff = diffPivot[NOTE_NAMES_ALT.indexOf(baseNote)] // search again
            if (diff < 0) throw IllegalArgumentException("Unknown note: $baseNote") // failed to search

            val exp = -diff / 12.0
            val basefreq = freq * Math.pow(2.0, exp) / if (diff >= 3) 8.0 else 16.0 // converts whatever baseNote to A0

            globals["speaker"]["__basefreq__"] = basefreq
            return LuaValue.NONE
        }

        override fun call(): LuaValue {
            globals["speaker"]["__basefreq__"] = LuaValue.valueOf(BASE_FREQ)
            return LuaValue.NONE
        }
    }

    class ResetTune(val globals: Globals) : ZeroArgFunction() {
        override fun call(): LuaValue {
            globals["speaker"]["__basefreq__"] = LuaValue.valueOf(BASE_FREQ)
            return LuaValue.NONE
        }
    }

    /**
     * usage = speaker.toFreq(speaker.AS5) --'S' is a substitution for '#'
     */
    class StringToFrequency(val globals: Globals) : OneArgFunction() {
        /**
         * @param arg: number (note index) or string (A-4, A4, B#2, ...)
         */
        override fun call(arg: LuaValue): LuaValue {
            val note = if (arg.isint()) arg.checkint()
            else {
                arg.checkjstring().toNoteIndex()
            }
            val basefreq = globals["speaker"]["__basefreq__"].checkdouble()

            return LuaValue.valueOf(note.toFreq(basefreq))
        }
    }

}