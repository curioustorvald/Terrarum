package net.torvald.terrarum.utils

import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.ModMgr
import org.apache.commons.csv.CSVFormat

/**
 * Created by minjaesong on 2016-02-16.
 */
object CSVFetcher {

    val terrarumCSVFormat: CSVFormat = org.apache.commons.csv.CSVFormat.DEFAULT.withIgnoreSurroundingSpaces()
                .withHeader()
                .withIgnoreEmptyLines()
                .withDelimiter(';')
                .withCommentMarker('#')
                .withNullString("N/A")
                .withRecordSeparator('\n')

    private var csvString: StringBuffer? = null

    fun readFromFile(csvFilePath: String): List<org.apache.commons.csv.CSVRecord> {
        net.torvald.terrarum.utils.CSVFetcher.csvString = StringBuffer() // reset buffer every time it called
        net.torvald.terrarum.utils.CSVFetcher.readCSVasString(csvFilePath)

        printdbg(this, "Reading CSV $csvFilePath")

        val csvParser = org.apache.commons.csv.CSVParser.parse(
                net.torvald.terrarum.utils.CSVFetcher.csvString!!.toString(),
                terrarumCSVFormat
        )

        val csvRecordList = csvParser.records
        csvParser.close()

        return csvRecordList
    }

    fun readFromModule(module: String, path: String) = net.torvald.terrarum.utils.CSVFetcher.readFromFile(ModMgr.getPath(module, path))

    fun readFromString(csv: String): List<org.apache.commons.csv.CSVRecord> {
        val csvParser = org.apache.commons.csv.CSVParser.parse(
                csv,
                terrarumCSVFormat
        )

        val csvRecordList = csvParser.records
        csvParser.close()

        return csvRecordList
    }

    @Throws(java.io.IOException::class)
    fun readCSVasString(path: String): String {
        net.torvald.terrarum.utils.CSVFetcher.csvString = StringBuffer()
        java.nio.file.Files.lines(java.nio.file.FileSystems.getDefault().getPath(path)).forEach(
                { s -> net.torvald.terrarum.utils.CSVFetcher.csvString!!.append("$s\n") }
        )
        return net.torvald.terrarum.utils.CSVFetcher.csvString!!.toString()
    }
}
