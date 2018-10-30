package net.torvald.terrarum.modulebasegame.gameworld


typealias time_t = Long

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
 * And there is no AM/PM concept, 24-hour clock is forced; no leap years.
 * An ingame day should last 22 real-life minutes.
 *
 * // TODO 4-month year? like Stardew Valley
 *
 *  The Yearly Calendar
 *
 *  A calendar tailored to this very game. A year is consisted of 4 seasons (month),
 *  and each season last fixed length of 30 days, leap years does not occur.
 *
 * =========================
 * |Mo|Ty|Mi|To|Fr|La|Su|Ve|
 * |--|--|--|--|--|--|--|--|
 * | 1| 2| 3| 4| 5| 6| 7|  |
 * | 8| 9|10|11|12|13|14|  |
 * |15|16|17|18|19|20|21|  |
 * |22|23|24|25|26|27|28|  |
 * |29|30| 1| 2| 3| 4| 5|  |
 * | 6| 7| 8| 9|10|11|12|  |
 * |13|14|15|16|17|18|19|  |
 * |20|21|22|23|24|25|26|  |
 * |27|28|29|30| 1| 2| 3|  |
 * | 4| 5| 6| 7| 8| 9|10|  |
 * |11|12|13|14|15|16|17|  |
 * |18|19|20|21|22|23|24|  |
 * |25|26|27|28|29|30| 1|  |
 * | 2| 3| 4| 5| 6| 7| 8|  |
 * | 9|10|11|12|13|14|15|  |
 * |16|17|18|19|20|21|22|  |
 * |23|24|25|26|27|28|29|30|
 * =========================
 *
 * Verddag only appears on the last day of the year (30th winter)
 *
 * (Check please:)
 * - Equinox/Solstice always occur on 21st day of the month
 *
 *
 * Created by minjaesong on 2016-01-24.
 */
class WorldTime(initTime: Long = 0L) {
    var TIME_T = 0L // Epoch: Year 125 Spring 1st, 0h00:00 (Mondag) // 125-01-01
        private set

    init {
        TIME_T = initTime
    }

    inline val seconds: Int // 0 - 59
        get() = TIME_T.toPositiveInt() % MINUTE_SEC
    inline val minutes: Int // 0 - 59
        get() = TIME_T.div(MINUTE_SEC).abs().toInt() % HOUR_MIN
    inline val hours: Int // 0 - 21
        get() = TIME_T.div(HOUR_SEC).abs().toInt() % HOURS_PER_DAY

    // The World Calendar implementation
    /*inline val yearlyDays: Int // 0 - 364
        get() = (TIME_T.toPositiveInt().div(DAY_LENGTH) % YEAR_DAYS)

    inline val days: Int // 1 - 31
        get() = quarterlyDays + 1 -
                if (quarterlyMonthOffset == 0)      0
                else if (quarterlyMonthOffset == 1) 31
                else                                61
    inline val months: Int // 1 - 12
        get() = if (yearlyDays == YEAR_DAYS - 1) 12 else
            quarter * 3 + 1 +
            if (quarterlyDays < 31)      0
            else if (quarterlyDays < 61) 1
            else                         2
    inline val years: Int
        get() = TIME_T.div(YEAR_DAYS * DAY_LENGTH).abs().toInt() + EPOCH_YEAR

    inline val quarter: Int // 0 - 3
        get() = if (yearlyDays == YEAR_DAYS - 1) 3 else yearlyDays / QUARTER_LENGTH
    inline val quarterlyDays: Int // 0 - 90(91)
        get() = if (yearlyDays == YEAR_DAYS - 1) 91 else (yearlyDays % QUARTER_LENGTH)
    inline val quarterlyMonthOffset: Int // 0 - 2
        get() = months.minus(1) % 3*/


    // these functions won't need inlining for performance
    val yearlyDays: Int // 0 - 119
        get() = (TIME_T.toPositiveInt().div(DAY_LENGTH) % YEAR_DAYS)
    val days: Int // 1 - 30 fixed
        get() = (yearlyDays % 30) + 1
    val months: Int // 1 - 4
        get() = (yearlyDays / 30) + 1
    val years: Int
        get() = TIME_T.div(YEAR_DAYS * DAY_LENGTH).abs().toInt() + EPOCH_YEAR

    val quarter = months - 1 // 0 - 3


    val dayOfWeek: Int //0: Mondag-The first day of weekday (0 - 7)
        get() = if (yearlyDays == YEAR_DAYS - 1) 7 else yearlyDays % 7

    var timeDelta: Int = 1
        set(value) {
            field = if (value < 0) 0 else value
        }

    inline val moonPhase: Double
        get() = (TIME_T.plus(1700000L) % LUNAR_CYCLE).toDouble() / LUNAR_CYCLE

    @Transient private var realSecAcc: Double = 0.0
    @Transient private val REAL_SEC_TO_GAME_SECS = 1.0 / GAME_MIN_TO_REAL_SEC // how slow is real-life clock (second-wise) relative to the ingame one

    val DAY_NAMES = arrayOf(//daynames are taken from Nynorsk (å -> o)
            "Mondag", "Tysdag", "Midtveke" //middle-week
            , "Torsdag", "Fredag", "Laurdag", "Sundag", "Verddag" //From Norsk word 'verd'
    )
    val DAY_NAMES_SHORT = arrayOf("Mon", "Tys", "Mid", "Tor", "Fre", "Lau", "Sun", "Ver")

    // dwarven calendar of 12 monthes
    /*val MONTH_NAMES = arrayOf(
            "Opal", "Obsidian", "Granite", "Slate", "Felsite", "Hematite",
            "Malachite", "Galena", "Limestone", "Sandstone", "Timber", "Moonstone"
    )
    val MONTH_NAMES_SHORT = arrayOf("Opal", "Obsi", "Gran", "Slat", "Fels", "Hema",
            "Mala", "Gale", "Lime", "Sand", "Timb", "Moon")*/
    val MONTH_NAMES = arrayOf("Spring", "Summer", "Autumn", "Winter")
    val MONTH_NAMES_SHORT = arrayOf("Spri", "Summ", "Autm", "Wint")

    companion object {
        /** Each day is displayed as 24 hours, but in real-life clock it's 22 mins long */
        val DAY_LENGTH = 86400 //must be the multiple of 3600

        val HOUR_SEC: Int = 3600
        val MINUTE_SEC: Int = 60
        val HOUR_MIN: Int = 60
        val GAME_MIN_TO_REAL_SEC: Double = 720.0 / 11.0
        val HOURS_PER_DAY = DAY_LENGTH / HOUR_SEC

        val YEAR_DAYS: Int = 120

        val MONTH_LENGTH = 30 // ingame calendar specific

        val EPOCH_YEAR = 125

        fun parseTime(s: String): Int =
                if (s.length >= 4 && s.contains('h')) {
                    s.toLowerCase().substringBefore('h').toInt() * HOUR_SEC +
                    s.toLowerCase().substringAfter('h').toInt() * MINUTE_SEC
                }
                else if (s.endsWith("h", true)) {
                    s.toLowerCase().substring(0, s.length - 1).toInt() * HOUR_SEC
                }
                else {
                    s.toInt()
                }


        val LUNAR_CYCLE: Int = 29 * DAY_LENGTH + 12 * HOUR_SEC + 44 * MINUTE_SEC + 3 // 29 days, 12 hours, 44 minutes, and 3 seconds in-game calendar
    }

    fun update(delta: Float) {
        //time
        realSecAcc += delta
        if (realSecAcc >= REAL_SEC_TO_GAME_SECS) {
            while (realSecAcc >= REAL_SEC_TO_GAME_SECS) {
                realSecAcc -= REAL_SEC_TO_GAME_SECS
                TIME_T += timeDelta
            }
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

    fun Long.toPositiveInt() = this.and(0x7FFFFFFF).toInt()
    fun Long.abs() = Math.abs(this)

    /** Format: "%A, %Y %B %d %X" */
    fun getFormattedTime() = "${getDayNameShort()}, " +
                             "$years " +
                             "${getMonthNameShort()} " +
                             "$days " +
                             "${String.format("%02d", hours)}:" +
                             "${String.format("%02d", minutes)}:" +
                             "${String.format("%02d", seconds)}"

    fun getDayNameFull() = DAY_NAMES[dayOfWeek]
    fun getDayNameShort() = DAY_NAMES_SHORT[dayOfWeek]
    fun getMonthNameFull() = MONTH_NAMES[months - 1]
    fun getMonthNameShort() = MONTH_NAMES_SHORT[months - 1]

    override fun toString() = getFormattedTime()
}