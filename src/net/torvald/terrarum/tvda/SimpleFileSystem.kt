package net.torvald.terrarum.tvda

/**
 * Created by minjaesong on 2021-10-07.
 */
interface SimpleFileSystem {
    fun getEntry(id: EntryID): DiskEntry?
    fun getFile(id: EntryID): EntryFile?
}