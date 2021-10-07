package net.torvald.terrarum.tvda

/**
 * Created by minjaesong on 2021-10-07.
 */
interface SimpleFileSystem {
    fun getFile(id: EntryID): EntryFile?
}