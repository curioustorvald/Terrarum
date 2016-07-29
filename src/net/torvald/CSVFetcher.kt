package net.torvald

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord

import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Files

/**
 * Created by minjaesong on 16-02-16.
 */
object CSVFetcher {

    private var csvString: StringBuffer? = null

    @Throws(IOException::class)
    operator fun invoke(csvFilePath: String): List<CSVRecord> {
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

    @Throws(IOException::class)
    fun readCSVasString(path: String): String {
        csvString = StringBuffer()
        Files.lines(FileSystems.getDefault().getPath(path)).forEach(
                { s -> csvString!!.append("$s\n") }
        )
        return csvString!!.toString()
    }
}
