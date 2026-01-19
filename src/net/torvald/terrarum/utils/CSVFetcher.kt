package net.torvald.terrarum.utils

import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.ModMgr
import org.apache.commons.csv.CSVFormat
import java.util.ArrayDeque

/**
 * Created by minjaesong on 2016-02-16.
 */
object CSVFetcher {

    private const val DEFAULT_PACKAGE = "net.torvald.terrarum"
    private val resolvedVariables = mutableMapOf<String, Boolean>()

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
        val preprocessed = net.torvald.terrarum.utils.CSVFetcher.readCSVasString(csvFilePath)

        printdbg(this, "Reading CSV $csvFilePath")

        val csvParser = org.apache.commons.csv.CSVParser.parse(
                preprocessed,
                terrarumCSVFormat
        )

        val csvRecordList = csvParser.records
        csvParser.close()

        return csvRecordList
    }

    fun readFromModule(module: String, path: String) = net.torvald.terrarum.utils.CSVFetcher.readFromFile(ModMgr.getGdxFile(module, path).path())

    fun readFromString(csv: String): List<org.apache.commons.csv.CSVRecord> {
        val preprocessed = preprocessCSV(csv)
        val csvParser = org.apache.commons.csv.CSVParser.parse(
                preprocessed,
                terrarumCSVFormat
        )

        val csvRecordList = csvParser.records
        csvParser.close()

        return csvRecordList
    }

    @Throws(java.io.IOException::class)
    fun readCSVasString(path: String): String {
        net.torvald.terrarum.utils.CSVFetcher.csvString = StringBuffer()
        java.nio.file.Files.lines(java.nio.file.FileSystems.getDefault().getPath(path)).forEach {
            s -> net.torvald.terrarum.utils.CSVFetcher.csvString!!.append("$s\n")
        }

        return preprocessCSV(net.torvald.terrarum.utils.CSVFetcher.csvString!!.toString(), path)
    }

    /**
     * Preprocesses CSV content, handling #ifdef, #ifndef, #else, and #endif directives.
     *
     * Supported syntax:
     * - #ifdef VARIABLE_NAME ... #endif
     * - #ifdef VARIABLE_NAME ... #else ... #endif
     * - #ifndef VARIABLE_NAME ... #endif
     *
     * Variables can be specified as:
     * - Short form: App.IS_DEVELOPMENT_BUILD (resolved with net.torvald.terrarum prefix)
     * - Fully qualified: net.torvald.terrarum.App.IS_DEVELOPMENT_BUILD
     *
     * @param content The raw CSV content to preprocess
     * @param sourcePath Optional source path for error messages
     * @return The preprocessed CSV content
     */
    fun preprocessCSV(content: String, sourcePath: String = "<unknown>"): String {
        val result = StringBuilder()
        // Stack of pairs: (shouldIncludeInThisBlock, hasSeenElse)
        val stateStack = ArrayDeque<Pair<Boolean, Boolean>>()
        var lineNumber = 0

        fun shouldInclude(): Boolean = stateStack.isEmpty() || stateStack.all { it.first }

        for (line in content.lineSequence()) {
            lineNumber++
            val trimmed = line.trim()

            when {
                trimmed.startsWith("#ifdef ") -> {
                    val varName = trimmed.substring(7).trim()
                    if (varName.isEmpty()) {
                        printdbg(this, "Warning: Empty #ifdef at $sourcePath:$lineNumber")
                        stateStack.addLast(Pair(false, false))
                    } else {
                        val parentIncluding = shouldInclude()
                        val value = if (parentIncluding) resolveVariable(varName, sourcePath, lineNumber) else false
                        stateStack.addLast(Pair(parentIncluding && value, false))
                    }
                }

                trimmed.startsWith("#ifndef ") -> {
                    val varName = trimmed.substring(8).trim()
                    if (varName.isEmpty()) {
                        printdbg(this, "Warning: Empty #ifndef at $sourcePath:$lineNumber")
                        stateStack.addLast(Pair(true, false))
                    } else {
                        val parentIncluding = shouldInclude()
                        val value = if (parentIncluding) resolveVariable(varName, sourcePath, lineNumber) else true
                        stateStack.addLast(Pair(parentIncluding && !value, false))
                    }
                }

                trimmed == "#else" -> {
                    if (stateStack.isEmpty()) {
                        printdbg(this, "Warning: Unmatched #else at $sourcePath:$lineNumber")
                    } else {
                        val (wasIncluding, hasSeenElse) = stateStack.removeLast()
                        if (hasSeenElse) {
                            printdbg(this, "Warning: Duplicate #else at $sourcePath:$lineNumber")
                            stateStack.addLast(Pair(false, true))
                        } else {
                            // Check if parent blocks are including
                            val parentIncluding = stateStack.isEmpty() || stateStack.all { it.first }
                            // Invert the condition, but only if parent is including and we weren't including before
                            stateStack.addLast(Pair(parentIncluding && !wasIncluding, true))
                        }
                    }
                }

                trimmed == "#endif" -> {
                    if (stateStack.isEmpty()) {
                        printdbg(this, "Warning: Unmatched #endif at $sourcePath:$lineNumber")
                    } else {
                        stateStack.removeLast()
                    }
                }

                // Regular line (including plain # comments)
                else -> {
                    if (shouldInclude()) {
                        result.append(line).append('\n')
                    }
                }
            }
        }

        // Check for unclosed directives
        if (stateStack.isNotEmpty()) {
            printdbg(this, "Warning: ${stateStack.size} unclosed #ifdef/#ifndef directive(s) in $sourcePath")
        }

        return result.toString()
    }

    private fun resolveVariable(varSpec: String, sourcePath: String, lineNumber: Int): Boolean {
        // Check cache first
        resolvedVariables[varSpec]?.let { return it }

        val result = try {
            val (className, fieldName) = parseVariableSpec(varSpec)
            val clazz = Class.forName(className)
            val field = clazz.getDeclaredField(fieldName)
            field.isAccessible = true

            if (field.type != Boolean::class.javaPrimitiveType && field.type != Boolean::class.java) {
                printdbg(this, "Warning: Variable '$varSpec' is not a boolean at $sourcePath:$lineNumber")
                false
            } else {
                field.getBoolean(null) // null for static fields
            }
        } catch (e: ClassNotFoundException) {
            printdbg(this, "Warning: Class not found for '$varSpec' at $sourcePath:$lineNumber")
            false
        } catch (e: NoSuchFieldException) {
            printdbg(this, "Warning: Field not found for '$varSpec' at $sourcePath:$lineNumber")
            false
        } catch (e: IllegalAccessException) {
            printdbg(this, "Warning: Cannot access '$varSpec' at $sourcePath:$lineNumber")
            false
        } catch (e: Exception) {
            printdbg(this, "Warning: Error resolving '$varSpec' at $sourcePath:$lineNumber: ${e.message}")
            false
        }

        // Cache the result
        resolvedVariables[varSpec] = result
        return result
    }

    private fun parseVariableSpec(varSpec: String): Pair<String, String> {
        val lastDot = varSpec.lastIndexOf('.')
        if (lastDot == -1) {
            throw IllegalArgumentException("Invalid variable spec: $varSpec (expected ClassName.fieldName)")
        }

        val classPath = varSpec.substring(0, lastDot)
        val fieldName = varSpec.substring(lastDot + 1)

        // Apply default package if class path doesn't contain a dot (simple class name)
        val fullClassName = if (classPath.contains('.')) {
            classPath
        } else {
            "$DEFAULT_PACKAGE.$classPath"
        }

        return fullClassName to fieldName
    }
}
