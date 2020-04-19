package net.torvald.parametricsky.datasets

import net.torvald.terrarum.serialise.toLittleLong
import java.io.File
import java.io.FileInputStream

object DatasetOp {

    fun readDatasetFromFile(filepath: String): DoubleArray {
        val file = File(filepath)
        val entrysize = file.length().toInt() / 8
        val fis = FileInputStream(file)

        val ret = DoubleArray(entrysize) {
            val inputbuf = ByteArray(8)
            fis.read(inputbuf)
            val rawnum = inputbuf.toLittleLong()
            Double.fromBits(rawnum)
        }

        fis.close()
        return ret
    }
}