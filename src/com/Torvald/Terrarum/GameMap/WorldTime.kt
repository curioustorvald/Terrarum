package com.Torvald.Terrarum.GameMap

/**
 * Created by minjaesong on 16-01-24.
 */
class WorldTime {
    internal var seconds: Int
    internal var minutes: Int
    internal var hours: Int

    internal var daysCount: Int //NOT a calendar day

    internal var days: Int
    internal var months: Int
    internal var years: Int

    internal var dayOfWeek: Int //0: Mondag-The first day of weekday

    internal var timeDelta = 1

    @Transient private var realMillisec: Int

    val DAYNAMES = arrayOf(//daynames are taken from Nynorsk (å -> o)
            "Mondag", "Tysdag", "Midtedag" //From Islenska Miðvikudagur
            , "Torsdag", "Fredag", "Laurdag", "Sundag", "Verdag" //From Norsk word 'verd'
    )
    val DAYNAMES_SHORT = arrayOf("Mon", "Tys", "Mid", "Tor", "Fre", "Lau", "Sun", "Ver")


    @Transient val REAL_SEC_IN_MILLI = 1000

    init {
        seconds = 0
        minutes = 0
        hours = 0
        daysCount = 0
        days = 1
        months = 1
        years = 1
        dayOfWeek = 0
        realMillisec = 0
    }

    fun update(delta: Int) {
        //time
        realMillisec += delta * timeDelta
        seconds = Math.round(GAME_MIN_TO_REAL_SEC.toFloat() / REAL_SEC_IN_MILLI.toFloat() * realMillisec.toFloat())

        if (realMillisec >= REAL_SEC_IN_MILLI)
            realMillisec -= REAL_SEC_IN_MILLI

        kickVariables()
    }

    /**
     * How much time has passed today, in seconds.
     * 0 == 6 AM
     * @return
     */
    fun elapsedSeconds(): Int {
        return (HOUR_SEC * hours + MINUTE_SEC * minutes + seconds) % DAY_LENGTH
    }

    val isLeapYear: Boolean
        get() = years % 4 == 0 && years % 100 != 0 || years % 400 == 0

    fun setTime(t: Int) {
        days += t / DAY_LENGTH
        hours = t / HOUR_SEC
        minutes = (t - HOUR_SEC * hours) / MINUTE_SEC
        seconds = t - minutes * MINUTE_SEC
    }

    fun addTime(t: Int) {
        setTime(elapsedSeconds() + t)
    }

    fun setTimeDelta(d: Int) {
        timeDelta = if (d < 0) 0 else d
    }

    val dayName: String
        get() = DAYNAMES[dayOfWeek]

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
            daysCount += 1
            dayOfWeek += 1
        }

        //calendar (the world calendar)
        if (dayOfWeek == 7) {
            dayOfWeek = 0
        }
        if ((months == 12 || months == 7 && isLeapYear) && days == 31) {
            dayOfWeek = 7
        }

        if ((months == 12 || months == 7 && isLeapYear) && days == 32) {
            days = 1
            months = 1
            years++
        }
        else if ((months == 1 || months == 4 || months == 7 || months == 10) && days > 31) {
            days = 0
            months++
        }
        else if (days > 30) {
            days = 0
            months++
        }

        if (months > 12) {
            months = 1
            years++
        }
    }

    fun getFormattedTime(): String {
        fun formatMin(min: Int): String {
            return if (min < 10) "0${min.toString()}" else min.toString()
        }

        return "${hours}h${formatMin(minutes)}"
    }

    companion object {
        /**
         * 22h
         */
        @Transient val DAY_LENGTH = 79200 //must be the multiple of 3600

        @Transient val HOUR_SEC: Int = 3600
        @Transient val MINUTE_SEC: Int = 60
        @Transient val HOUR_MIN: Int = 60
        @Transient val GAME_MIN_TO_REAL_SEC: Float = 60f
    }
}