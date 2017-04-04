package net.torvald.terrarum.gameworld

import net.torvald.terrarum.gameactors.GameDate

/**
 * The World Calendar implementation of Dwarven Calendar (we're talking about DF!)
 *
 * Please see:
 *      https://en.wikipedia.org/wiki/World_Calendar
 *      http://dwarffortresswiki.org/index.php/DF2014:Calendar
 *
 * Normal format for day is
 *      Tysdag 12th Granite
 *
 * And there is no AM/PM concept, 22-hour clock is forced.
 *
 * Created by minjaesong on 16-01-24.
 */
class YeOldeWorldTime {
    internal var TIME_T = 0L // TODO use it! Epoch: Year 125, 1st Granite, 0h00:00

    internal var seconds: Int // 0 - 59
    internal var minutes: Int // 0 - 59
    internal var hours: Int // 0 - 21

    // days on the year
    internal var yearlyDays: Int //NOT a calendar day

    internal var days: Int // 1 - 31
    internal var months: Int // 1 - 12
    internal var years: Int // 1+

    internal var dayOfWeek: Int //0: Mondag-The first day of weekday (0 - 7)

    internal var timeDelta = 1

    @Transient private var realMillisec: Int

    val DAY_NAMES = arrayOf(//daynames are taken from Nynorsk (å -> o)
            "Mondag", "Tysdag", "Midvikdag" //From Islenska Miðvikudagur
            , "Torsdag", "Fredag", "Laurdag", "Sundag", "Verddag" //From Norsk word 'verd'
    )
    val DAY_NAMES_SHORT = arrayOf("Mon", "Tys", "Mid", "Tor", "Fre", "Lau", "Sun", "Ver")

    val MONTH_NAMES = arrayOf(
            "Opal", "Obsidian", "Granite", "Slate", "Felsite", "Hematite",
            "Malachite", "Galena", "Limestone", "Sandstone", "Timber", "Moonstone"
    )
    val MONTH_NAMES_SHORT = arrayOf("Opal", "Obsi", "Gran", "Slat", "Fels", "Hema",
            "Mala", "Gale", "Lime", "Sand", "Timb", "Moon")

    val currentTimeAsGameDate: GameDate
        get() = GameDate(years, yearlyDays)

    @Transient val REAL_SEC_IN_MILLI = 1000

    companion object {
        /** Each day is 22-hour long */
        val DAY_LENGTH = 79200 //must be the multiple of 3600

        val HOUR_SEC: Int = 3600
        val MINUTE_SEC: Int = 60
        val HOUR_MIN: Int = 60
        val GAME_MIN_TO_REAL_SEC: Float = 60f

        val YEAR_DAYS: Int = 365

        fun parseTime(s: String): Int =
                if (s.length >= 4 && s.contains('h')) {
                    s.toLowerCase().substringBefore('h').toInt() * WorldTime.HOUR_SEC +
                    s.toLowerCase().substringAfter('h').toInt() * WorldTime.MINUTE_SEC
                }
                else if (s.endsWith("h", true)) {
                    s.toLowerCase().substring(0, s.length - 1).toInt() * WorldTime.HOUR_SEC
                }
                else {
                    s.toInt()
                }
    }

    init {
        // The day when the new world ever is being made.
        // If we use Multiverse system (which replaces Terraria's "hack"
        // as a reward rather than a cheat), time of current world's time is
        // copied to the new world's. (it's Multi-nation rather than Multiverse)
        seconds = 0
        minutes = 30
        hours = 8
        yearlyDays = 73
        days = 12
        months = 3
        years = 125
        dayOfWeek = 1 // Tysdag
        realMillisec = 0
    }

    fun update(delta: Int) {
        val oldsec = seconds

        //time
        realMillisec += delta * timeDelta
        val newsec = Math.round(GAME_MIN_TO_REAL_SEC / REAL_SEC_IN_MILLI.toFloat() * realMillisec.toFloat())
        seconds = newsec

        if (realMillisec >= REAL_SEC_IN_MILLI)
            realMillisec -= REAL_SEC_IN_MILLI

        kickVariables()
    }

    /**
     * How much time has passed today, in seconds.
     * 0 == 6 AM
     * @return
     */
    val elapsedSeconds: Int
        get() = (HOUR_SEC * hours + MINUTE_SEC * minutes + seconds) % DAY_LENGTH

    /** Sets time of this day. */
    fun setTime(t: Int) {
        days += t / DAY_LENGTH
        hours = t / HOUR_SEC
        minutes = (t - HOUR_SEC * hours) / MINUTE_SEC
        seconds = t - minutes * MINUTE_SEC
        yearlyDays += t / DAY_LENGTH
    }

    fun addTime(t: Int) {
        setTime(elapsedSeconds + t)
    }

    fun setTimeDelta(d: Int) {
        timeDelta = if (d < 0) 0 else d
    }

    val dayName: String
        get() = DAY_NAMES[dayOfWeek]

    private fun kickVariables() {
        if (seconds >= MINUTE_SEC) {
            seconds = 0
            minutes += 1
        }

        if (minutes >= HOUR_MIN) {
            minutes = 0
            hours += 1
        }

        if (hours >= DAY_LENGTH / HOUR_SEC) {
            hours = 0
            days += 1
            yearlyDays += 1
            dayOfWeek += 1
        }

        //calendar (the world calendar)
        if (dayOfWeek == 7) {
            dayOfWeek = 0
        }
        if (months == 12 && days == 31) {
            dayOfWeek = 7
        }

        if (months == 12 && days == 32) {
            days = 1
            months = 1
            years++
        }
        else if ((months == 1 || months == 4 || months == 7 || months == 10) && days > 31) {
            days = 1
            months++
        }
        else if (days > 30) {
            days = 1
            months++
        }

        if (months > 12) {
            months = 1
            years++
            yearlyDays = 1
        }
    }

    /** Format: "%A %d %B %Y %X" */
    fun getFormattedTime() = "${getDayNameFull()} " +
                             "$days " +
                             "${getMonthNameFull()} " +
                             "$years " +
                             "${String.format("%02d", hours)}:" +
                             "${String.format("%02d", minutes)}:" +
                             "${String.format("%02d", seconds)}"

    fun getDayNameFull() = DAY_NAMES[dayOfWeek]
    fun getDayNameShort() = DAY_NAMES_SHORT[dayOfWeek]
    fun getMonthNameFull() = MONTH_NAMES[months - 1]
    fun getMonthNameShort() = MONTH_NAMES_SHORT[months - 1]
}