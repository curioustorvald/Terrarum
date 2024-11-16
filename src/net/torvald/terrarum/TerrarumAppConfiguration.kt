package net.torvald.terrarum

import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.serialise.toUint
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * You directly modify the source code to tune the engine to suit your needs.
 *
 * Created by minjaesong on 2019-08-15.
 */
object TerrarumAppConfiguration {
    //////////////////////////////////////
    // CONFIGURATION FOR THE APP ITSELF //
    //////////////////////////////////////
    const val GAME_NAME = "Terrarum"
    const val COPYRIGHT_DATE_NAME = "© 2013-2024 CuriousToꝛvald (minjaesong)"
    val COPYRIGHT_LICENSE: String; get() = Lang["COPYRIGHT_GNU_GPL_3"]
    const val COPYRIGHT_LICENSE_ENGLISH = "Distributed under GNU GPL 3"
    const val COPYRIGHT_LICENSE_TERMS_SHORT = """
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
"""
    const val DEFAULT_LOADORDER_FILE = """# Load Order
# Modules are loaded from top to bottom.
# Name of the module corresponds with the name of the directory the module is stored in,
# typically under:
#    1. assets/mods of the installation path (the modules comes with the release of the game)
#    2. %APPDATA%/Modules (the modules installed by the user)
#    where %APPDATA% is:
#        Windows -- C:\Users\<username>\AppData\Roaming\Terrarum
#        macOS   -- /Users/<username>/Library/Application Support/Terrarum
#        Linux   -- /home/<username>/.Terrarum
# Please refrain from removing 'basegame' on the load order -- it may render the game unplayable.

basegame
"""

    val FIXED_LATEST_DOWNLOAD_LINK = URL("https://github.com/curioustorvald/Terrarum/releases/latest")

    /**
     *
     *
     * Version numbering that follows Semantic Versioning 2.0.0 (https://semver.org/)
     *
     *
     *
     *
     * 0xAAAA_BBBB_XXXXXX, where:
     *
     *  * AAAA: Major version
     *  * BBBB: Minor version
     *  * XXXXXX: Patch version
     *
     *
     * e.g. 0x02010034 will be translated as 2.1.52
     *
     */
    const val VERSION_RAW: Long = 0x0000_000005_000000
    // Commit counts up to the Release 0.3.0: 2259
    // Commit counts up to the Release 0.3.1: 2278
    // Commit counts up to the Release 0.3.2: 2732
    // Commit counts up to the Release 0.3.3: 3020
    // Commit counts up to the Release 0.4.0: 3631
    // Commit counts up to the Release 0.4.1: 3678
    // Commit counts up to the Release 0.4.2: 3762
    // Commit counts up to the Release 0.5.0: 4090

    val DEV_CYCLE: Map<String, Long> = mapOf(
        "Alpha" to 0x0000_000004_000000,
        "Beta" to 0x0000_FFFFFF_000000,
    )

    val VERSION_NUMBER: String = String.format(
        "%d.%d.%d",
        VERSION_RAW ushr 48,
        (VERSION_RAW and 0xffff000000L) ushr 24,
        VERSION_RAW and 0xffffffL
    )

    private val DEV_CYCLE_LIST_SORTED = DEV_CYCLE.toList().sortedBy { it.second }
    val CURRENT_DEV_CYCLE: String? = DEV_CYCLE_LIST_SORTED.map { it.first to VERSION_RAW - it.second }.firstOrNull { it.second >= 0L }?.first

//    val VERSION_SNAPSHOT = Snapshot(0) // for normal dev
//    val VERSION_SNAPSHOT = ForcedSnapshot("24w07d") // for snapshot release
    val VERSION_SNAPSHOT = null // for the release

    const val VERSION_TAG: String = ""

    val VERSION_STRING: String
        get() {
            val major = if (VERSION_RAW >= 0x0001_000000_000000)
                VERSION_NUMBER
            else if (CURRENT_DEV_CYCLE != null) {
                val delta = VERSION_RAW - DEV_CYCLE[CURRENT_DEV_CYCLE]!!
                CURRENT_DEV_CYCLE + " " + String.format("%d.%d", ((delta and 0xffff000000L) ushr 24) + 1, delta and 0xffffffL)
            }
            else
                VERSION_NUMBER

            val tag = if (VERSION_TAG.isNotBlank()) "-$VERSION_TAG" else ""

            val snapshot = if (VERSION_SNAPSHOT == null) "" else " ($VERSION_SNAPSHOT)"

            return "$major$tag$snapshot"
        }

    val VERSION_STRING_WITHOUT_SNAPSHOT: String
        get() {
            val major = if (VERSION_RAW >= 0x0001_000000_000000)
                VERSION_NUMBER
            else if (CURRENT_DEV_CYCLE != null) {
                val delta = VERSION_RAW - DEV_CYCLE[CURRENT_DEV_CYCLE ?: ""]!!
                CURRENT_DEV_CYCLE + " " + String.format("%d.%d", ((delta and 0xffff000000L) ushr 24) + 1, delta and 0xffffffL)
            }
            else
                VERSION_NUMBER

            val tag = if (VERSION_TAG.isNotBlank()) "-$VERSION_TAG" else ""

            return "$major$tag"
        }

    fun convertVersionNumberToReadable(semverStr: String, snapshotObj: Snapshot? = null): String {
        val numbers = semverStr.split('.')
        val maj = numbers[0].toLong()
        val min = numbers.getOrNull(1)?.toLong() ?: 0L
        val pat = numbers.getOrNull(2)?.toLong() ?: 0L

        val VERSION_RAW = maj.shl(48) or min.shl(24) or pat
        val CURRENT_DEV_CYCLE: String? = DEV_CYCLE_LIST_SORTED.map { it.first to VERSION_RAW - it.second }.firstOrNull { it.second >= 0L }?.first

        val major = if (VERSION_RAW >= 0x0001_000000_000000)
            semverStr
        else if (CURRENT_DEV_CYCLE != null) {
            val delta = VERSION_RAW - DEV_CYCLE[CURRENT_DEV_CYCLE]!!
            CURRENT_DEV_CYCLE + " " + String.format("%d.%d", ((delta and 0xffff000000L) ushr 24) + 1, delta and 0xffffffL)
        }
        else
            semverStr

//        val tag = if (VERSION_TAG.isNotBlank()) "-$VERSION_TAG" else ""

        val snapshot = if (snapshotObj == null) "" else " (${snapshotObj})"

        return "$major$snapshot"
    }

    fun convertVersionNumberToReadableShort(semverStr: String, snapshotObj: Snapshot? = null): String {
        val s = convertVersionNumberToReadable(semverStr, snapshotObj)
        return s.replace("Alpha", "α").replace("Beta", "β")
    }

    //////////////////////////////////////////////////////////
    //             CONFIGURATION FOR TILE MAKER             //
    // MAKE SURE THESE VALUES ARE UNIQUE IN THE SOURCE CODE //
    //////////////////////////////////////////////////////////
    const val SUBTILE_SIZE = 8
    const val TILE_SIZE = SUBTILE_SIZE * 2
    const val TILE_SIZEF = TILE_SIZE.toFloat()
    const val TILE_SIZED = TILE_SIZE.toDouble()


    /**
     * @param string Text representation of the snapshot version, such as "23w50"
     */
    private fun ForcedSnapshot(string: String): Snapshot {
        val s = Snapshot(string.last().code - 0x61)
        s.year = string.substring(0, 2).toInt()
        s.week = string.substring(3, 5).toInt()
        s.update()
        return s
    }
}

data class Snapshot(var revision: Int) {
    private var today = LocalDate.now()
    /** The ISO year, may NOT correspond with the calendar year */
    internal var year: Int
    /** The ISO week number, may NOT correspond with the calendar week */
    internal var week: Int

    init {
        // THE CORRECT WAY of deriving ISO WEEK DATE format (test with LocalDate.parse("1976-12-31"); should return 1977 01)
        // NOTE: Java's implementation seems to think the first day of the week is Sunday, instead of the Monday as in ISO standard. Weird...
        val formatter = DateTimeFormatter.ofPattern("YYYY ww", Locale.getDefault())
        val (y, w) = today.format(formatter).split(' ')
        year = y.toInt() - 2000
        week = w.toInt()
    }

    private var string = ""
    private var bytes = byteArrayOf()

    internal fun update() {
        string = "${year}w${week.toString().padStart(2,'0')}${Char(0x61 + revision)}"
        bytes = byteArrayOf(
            revision.and(4).shl(7).or(year.and(127)).toByte(),
            week.shl(2).or(revision.and(3)).toByte()
        )
    }

    init {
        if (revision !in 0..7) {
            throw IllegalArgumentException("Revision out of range -- expected 0..7 (a..h), got $revision")
        }
        update()
    }

    override fun toString() = string
    fun toBytes() = bytes

    constructor(b: ByteArray) : this(0) {
        year = b[0].toUint() and 127
        week = b[1].toUint() ushr 2
        revision = (b[0].toUint() ushr 7 shl 2) or b[1].toUint().and(3)
        update()
    }

    override fun hashCode(): Int {
        return year.shl(16) or week.shl(8) or revision
    }
}