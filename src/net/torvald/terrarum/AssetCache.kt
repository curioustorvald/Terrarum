package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.archivers.ClusteredFormatDOM
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.archivers.Clustfile
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile

/**
 * Central accessor for the game assets.
 * In distribution mode, assets are read directly from assets.tevd.
 * In development mode, assets are read from the local ./assets/ directory.
 *
 * Never call `gdx.files.internal` directly!
 *
 * Created by minjaesong on 2026-02-19.
 */
object AssetCache {

    private val defaultArchivePath = File("./assets.tevd")

    /** Whether we're running from a distribution archive */
    val isDistribution: Boolean get() = dom != null

    private var dom: ClusteredFormatDOM? = null

    /**
     * Open the archive on startup. Call early, after defaultDir is set.
     *
     * @param overridePath When non-null, load this specific .tevd archive exclusively,
     *                     ignoring both the default ./assets.tevd and the ./assets/ directory.
     */
    @JvmOverloads
    fun init(overridePath: String? = null) {
        if (overridePath != null) {
            val f = File(overridePath)
            if (!f.exists()) throw FileNotFoundException("Specified assets archive not found: $overridePath")
            println("[AssetCache] Override archive: opening ${f.path}")
            dom = ClusteredFormatDOM(RandomAccessFile(f, "r"))
            println("[AssetCache] Override archive opened successfully")
        } else if (defaultArchivePath.exists()) {
            println("[AssetCache] Distribution mode: opening ${defaultArchivePath.path}")
            dom = ClusteredFormatDOM(RandomAccessFile(defaultArchivePath, "r"))
            println("[AssetCache] Archive opened successfully")
        } else {
            println("[AssetCache] No archive found, using loose assets (development mode)")
        }
    }

    /**
     * Get a Clustfile for a path relative to the assets root.
     *
     * Note: nothing guarantees the file's actual existence!
     */
    fun getClustfile(relativePath: String): Clustfile {
        val path = if (relativePath.startsWith("/")) relativePath else "/$relativePath"
        return Clustfile(dom!!, path)
    }

    /**
     * Get a GDX FileHandle — returns ClustfileHandle in distribution, Gdx.files.internal in dev.
     */
    fun getFileHandle(relativePath: String): FileHandle {
        return if (isDistribution) ClustfileHandle(getClustfile(relativePath))
        else Gdx.files.internal("./assets/$relativePath")
    }

    /**
     * Resolve a path string. In dev mode returns local path; in distribution mode throws
     * as callers should use getFileHandle() instead.
     */
    fun resolve(relativePath: String): String {
        return if (isDistribution) throw UnsupportedOperationException(
            "Use AssetCache.getFileHandle(\"$relativePath\") in distribution mode"
        )
        else "./assets/$relativePath"
    }

    fun dispose() {
        dom?.dispose()
        dom = null
    }
}
