package net.torvald.terrarum.tvda

import java.nio.charset.Charset

/**
 * Created by minjaesong on 2021-10-07.
 */
interface SimpleFileSystem {
    fun getEntry(id: EntryID): DiskEntry?
    fun getFile(id: EntryID): EntryFile?
    fun getDiskName(charset: Charset): String
}