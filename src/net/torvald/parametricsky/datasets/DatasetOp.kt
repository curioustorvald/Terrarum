package net.torvald.parametricsky.datasets

import net.torvald.terrarum.serialise.toLittleInt64
import java.io.File
import java.io.FileInputStream

object DatasetOp {

    fun readDatasetFromFile(filepath: String): DoubleArray {
        val file = File(filepath)
        val entrysize = file.length().toInt() / 8
        val fis = FileInputStream(file)

        val inputbuf = ByteArray(8)
        val ret = DoubleArray(entrysize) {
            fis.read(inputbuf)
            val rawnum = inputbuf.toLittleInt64()
            Double.fromBits(rawnum)
        }

        fis.close()
        return ret
    }


}