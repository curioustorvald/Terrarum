package net.torvald.terrarum.utils

import net.torvald.terrarum.ModMgr
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord

import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.FileSystems
import java.nio.file.Files

/**
 * Created by minjaesong on 16-02-16.
 */
object CSVFetcher {

    private var csvString: StringBuffer? = null

    fun readFromFile(csvFilePath: String): List<org.apache.commons.csv.CSVRecord> {
        net.torvald.terrarum.utils.CSVFetcher.csvString = StringBuffer() // reset buffer every time it called
        net.torvald.terrarum.utils.CSVFetcher.readCSVasString(csvFilePath)

        println("[CSVFetcher] Reading CSV $csvFilePath")

        val csvParser = org.apache.commons.csv.CSVParser.parse(
                net.torvald.terrarum.utils.CSVFetcher.csvString!!.toString(),
                org.apache.commons.csv.CSVFormat.DEFAULT.withIgnoreSurroundingSpaces()
                        .withHeader()
                        .withIgnoreEmptyLines()
                        .withDelimiter(';')
                        .withCommentMarker('#')
                        .withNullString("N/A")
                        .withRecordSeparator('\n')
        )

        val csvRecordList = csvParser.records
        csvParser.close()

        return csvRecordList
    }

    fun readFromModule(module: String, path: String) = net.torvald.terrarum.utils.CSVFetcher.readFromFile(ModMgr.getPath(module, path))

    fun readFromString(csv: String): List<org.apache.commons.csv.CSVRecord> {
        val csvParser = org.apache.commons.csv.CSVParser.parse(
                csv,
                org.apache.commons.csv.CSVFormat.DEFAULT.withIgnoreSurroundingSpaces()
                        .withHeader()
                        .withIgnoreEmptyLines()
                        .withDelimiter(';')
                        .withCommentMarker('#')
                        .withNullString("N/A")
                        .withRecordSeparator('\n')
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
