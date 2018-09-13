package net.torvald.terrarum.serialise

import net.torvald.terrarum.Terrarum
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream



object SavegameLedger {

    private val SAVE_DIRECTORY = File(Terrarum.defaultSaveDir)

    fun hasSavegameDirectory() = SAVE_DIRECTORY.exists() && SAVE_DIRECTORY.isDirectory

    private fun peekFewBytes(file: File, length: Int): ByteArray {
        val buffer = ByteArray(length)
        val `is` = FileInputStream(file)
        if (`is`.read(buffer) != buffer.size) {
            throw InternalError()
        }
        `is`.close()
        return buffer
    }
    private val MAGIC_TEVD = "TEVd".toByteArray()

    fun getSavefileList(): List<File>? {
        return if (!hasSavegameDirectory()) null
               else SAVE_DIRECTORY.listFiles().filter { it.isFile && peekFewBytes(it, 4) contentEquals MAGIC_TEVD }
    }

    fun getSavefileCount() = getSavefileList()?.count() ?: 0

}