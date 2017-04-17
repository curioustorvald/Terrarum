package net.torvald

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

    fun readFromFile(csvFilePath: String): List<CSVRecord> {
        csvString = StringBuffer() // reset buffer every time it called
        readCSVasString(csvFilePath)

        println("Reading CSV $csvFilePath")

        val csvParser = CSVParser.parse(
                csvString!!.toString(),
                CSVFormat.DEFAULT.withIgnoreSurroundingSpaces()
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

    fun readFromModule(module: String, path: String) = readFromFile(ModMgr.getPath(module, path))

    fun readFromString(csv: String): List<CSVRecord> {
        val csvParser = CSVParser.parse(
                csv,
                CSVFormat.DEFAULT.withIgnoreSurroundingSpaces()
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

    @Throws(IOException::class)
    fun readCSVasString(path: String): String {
        csvString = StringBuffer()
        Files.lines(FileSystems.getDefault().getPath(path)).forEach(
                { s -> csvString!!.append("$s\n") }
        )
        return csvString!!.toString()
    }
}
