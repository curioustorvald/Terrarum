package com.Torvald;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.List;

/**
 * Created by minjaesong on 16-02-16.
 */
public class CSVFetcher {

    private static StringBuffer csvString;

    public static List<CSVRecord> readCSV(String csvFilePath) throws IOException {
        csvString = new StringBuffer();
        readCsvFileAsString(csvFilePath);

        CSVParser csvParser = CSVParser.parse(csvString.toString()
                , CSVFormat.DEFAULT.withIgnoreSurroundingSpaces().withHeader()
                                   .withIgnoreEmptyLines().withDelimiter(';')
                                   .withCommentMarker('#').withNullString("N/A")
                                   .withRecordSeparator('\n')
        );

        List<CSVRecord> csvRecordList = csvParser.getRecords();
        csvParser.close();

        return csvRecordList;
    }

    private static void readCsvFileAsString(String path) throws IOException {
        Files.lines(
                FileSystems.getDefault().getPath(path)
        ).forEach(s -> csvString.append(s += "\n"));
    }
}
