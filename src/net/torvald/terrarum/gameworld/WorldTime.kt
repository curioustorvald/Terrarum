package net.torvald.terrarum.gameworld

import net.torvald.terrarum.gameactors.GameDate

/**
 * The World Calendar implementation of Dwarven Calendar, except:
 *      - the year begins with Mondag instead of Sundag (which is ISO standard)
 *      - the first month is Opal instead of Granite    (to reduce confusion)
 *
 *
 * Please also see:
 *      https://en.wikipedia.org/wiki/World_Calendar
 *      http://dwarffortresswiki.org/index.php/DF2014:Calendar
 *
 * And there is no AM/PM concept, 22-hour clock is forced; no leap years.
 * (AM 12 is still 00h in this system, again, to reduce confusion)
 *
 *
 *  Calendar
 *
 * |Mo|Ty|Mi|To|Fr|La|Su|Ve|
 * |--|--|--|--|--|--|--|--|
 * | 1| 2| 3| 4| 5| 6| 7|  |
 * | 8| 9|10|11|12|13|14|  |
 * |15|16|17|18|19|20|21|  |
 * |22|23|24|25|26|27|28|  |
 * |29|30|31| 1| 2| 3| 4|  |
 * | 5| 6| 7| 8| 9|10|11|  |
 * |12|13|14|15|16|17|18|  |
 * |19|20|21|22|23|24|25|  |
 * |26|27|28|29|30| 1| 2|  |
 * | 3| 4| 5| 6| 7| 8| 9|  |
 * |10|11|12|13|14|15|16|  |
 * |17|18|19|20|21|22|23|  |
 * |24|25|26|27|28|29|30|31|
 *
 * Verddag only appears on the last day of the year (31st Moonstone)
 *
 * (Check please:)
 * - Equinox/Solstice always occur on 21st day of the month
 *
 *
 * Created by minjaesong on 16-01-24.
 */
class WorldTime(initTime: Long = 0L) {
    var TIME_T = 0L // Epoch: Year 125, 1st Opal, 0h00:00 (Mondag) // 125-01-01
        private set

    init {
        TIME_T = initTime
    }

    val seconds: Int // 0 - 59
        get() = TIME_T.toPositiveInt() % MINUTE_SEC
    val minutes: Int // 0 - 59
        get() = TIME_T.div(MINUTE_SEC).abs().toInt() % HOUR_MIN
    val hours: Int // 0 - 21
        get() = TIME_T.div(HOUR_SEC).abs().toInt() % HOURS_PER_DAY

    val yearlyDays: Int // 0 - 364
        get() = (TIME_T.toPositiveInt().div(DAY_LENGTH) % YEAR_DAYS)

    val days: Int // 1 - 31
        get() = quarterlyDays + 1 -
                if (quarterlyMonthOffset == 0)      0
                else if (quarterlyMonthOffset == 1) 31
                else                                61
    val months: Int // 1 - 12
        get() = if (yearlyDays == YEAR_DAYS - 1) 12 else
            quarter * 3 + 1 +
            if (quarterlyDays < 31)      0
            else if (quarterlyDays < 61) 1
            else                         2
    val years: Int
        get() = TIME_T.div(YEAR_DAYS * DAY_LENGTH).abs().toInt() + EPOCH_YEAR

    val quarter: Int // 0 - 3
        get() = if (yearlyDays == YEAR_DAYS - 1) 3 else yearlyDays / QUARTER_LENGTH
    val quarterlyDays: Int // 0 - 90(91)
        get() = if (yearlyDays == YEAR_DAYS - 1) 91 else (yearlyDays % QUARTER_LENGTH)
    val quarterlyMonthOffset: Int // 0 - 2
        get() = months.minus(1) % 3

    val dayOfWeek: Int //0: Mondag-The first day of weekday (0 - 7)
        get() = if (yearlyDays == YEAR_DAYS - 1) 7 else yearlyDays % 7

    var timeDelta: Int = 1
        set(value) {
            field = if (value < 0) 0 else value
        }

    val moonPhase: Double
        get() = (TIME_T % LUNAR_CYCLE).toDouble() / LUNAR_CYCLE

    @Transient private var realMillisec: Double = 0.0
    @Transient private val REAL_SEC_TO_GAME_SECS = 60

    val DAY_NAMES = arrayOf(//daynames are taken from Nynorsk (å -> o)
            "Mondag", "Tysdag", "Midtveke" //middle-week
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

    companion object {
        /** Each day is 22-hour long */
        val DAY_LENGTH = 79200 //must be the multiple of 3600

        val HOUR_SEC: Int = 3600
        val MINUTE_SEC: Int = 60
        val HOUR_MIN: Int = 60
        val GAME_MIN_TO_REAL_SEC: Float = 60f
        val HOURS_PER_DAY = DAY_LENGTH / HOUR_SEC

        val YEAR_DAYS: Int = 365
        val QUARTER_LENGTH = 91 // as per The World Calendar

        val EPOCH_YEAR = 125

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


        val LUNAR_CYCLE: Int = 2342643// 29 days, 12 hours, 44 minutes, and 3 seconds in-game calendar
    }

    fun update(delta: Int) {
        //time
        realMillisec += delta
        if (realMillisec >= 1000.0 / REAL_SEC_TO_GAME_SECS) {
            realMillisec -= 1000.0 / REAL_SEC_TO_GAME_SECS
            TIME_T += timeDelta
        }
    }


    val todaySeconds: Int
        get() = TIME_T.toPositiveInt() % DAY_LENGTH

    fun setTimeOfToday(t: Int) {
        TIME_T = TIME_T - todaySeconds + t
    }

    fun addTime(t: Int) {
        TIME_T += t
    }

    val dayName: String
        get() = DAY_NAMES[dayOfWeek]

    private fun Long.toPositiveInt() = this.and(0x7FFFFFFF).toInt()
    private fun Long.abs() = Math.abs(this)

    /** Format: "%A, %d %B %Y %X" */
    fun getFormattedTime() = "${getDayNameShort()}, " +
                             "$days " +
                             "${getMonthNameShort()} " +
                             "$years " +
                             "${String.format("%02d", hours)}:" +
                             "${String.format("%02d", minutes)}:" +
                             "${String.format("%02d", seconds)}"

    fun getDayNameFull() = DAY_NAMES[dayOfWeek]
    fun getDayNameShort() = DAY_NAMES_SHORT[dayOfWeek]
    fun getMonthNameFull() = MONTH_NAMES[months - 1]
    fun getMonthNameShort() = MONTH_NAMES_SHORT[months - 1]

    override fun toString() = getFormattedTime()
}