package net.torvald.terrarum

import com.badlogic.gdx.files.FileHandle
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.archivers.Clustfile
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.archivers.ClustfileInputStream
import java.io.File
import java.io.InputStream

/**
 * A GDX FileHandle backed by a Clustfile from TerranVirtualDisk.
 * Allows transparent asset loading from .tevd archives using all standard GDX APIs.
 *
 * Created by minjaesong on 2026-02-19.
 */
class ClustfileHandle(private val clustfile: Clustfile) : FileHandle() {

    override fun read(): InputStream = ClustfileInputStream(clustfile)

    override fun readBytes(): ByteArray = clustfile.readBytes()

    override fun readString(charset: String?): String = String(readBytes(), charset(charset ?: "UTF-8"))

    override fun exists(): Boolean = clustfile.exists()

    override fun length(): Long = clustfile.length()

    override fun isDirectory(): Boolean = clustfile.isDirectory

    override fun name(): String = clustfile.name

    override fun path(): String = clustfile.path

    override fun extension(): String {
        val n = name()
        val dotIndex = n.lastIndexOf('.')
        return if (dotIndex == -1) "" else n.substring(dotIndex + 1)
    }

    override fun nameWithoutExtension(): String {
        val n = name()
        val dotIndex = n.lastIndexOf('.')
        return if (dotIndex == -1) n else n.substring(0, dotIndex)
    }

    override fun list(): Array<FileHandle> {
        return clustfile.listFiles()?.map { ClustfileHandle(it) }?.toTypedArray() ?: arrayOf()
    }

    override fun child(name: String): FileHandle {
        val childPath = if (clustfile.path.endsWith("/")) "${clustfile.path}$name" else "${clustfile.path}/$name"
        return ClustfileHandle(Clustfile(clustfile.DOM, childPath))
    }

    override fun parent(): FileHandle {
        val parentFile = clustfile.parentFile
        return if (parentFile != null) ClustfileHandle(parentFile)
        else ClustfileHandle(Clustfile(clustfile.DOM, "/"))
    }

    override fun file(): File {
        // Return a dummy File for logging/toString purposes only
        return File(clustfile.path)
    }

    override fun toString(): String = clustfile.path
}
