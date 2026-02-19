package net.torvald.terrarum

import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.archivers.ClusteredFormatDOM
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.archivers.Clustfile
import java.io.File

/**
 * Build-time tool that creates assets.tevd from assets_release/ directory.
 *
 * Usage: java -cp <classpath> net.torvald.terrarum.AssetArchiveBuilderKt [assets_release_dir] [output_file]
 */
fun main(args: Array<String>) {
    val srcDir = File(if (args.isNotEmpty()) args[0] else "assets_release")
    val outFile = File(if (args.size > 1) args[1] else "out/assets.tevd")

    if (!srcDir.exists() || !srcDir.isDirectory) {
        System.err.println("Error: Source directory '${srcDir.path}' does not exist or is not a directory.")
        System.exit(1)
    }

    outFile.parentFile?.mkdirs()
    if (outFile.exists()) outFile.delete()

    println("Scanning $srcDir...")
    var totalSize = 0L
    var fileCount = 0
    srcDir.walkTopDown().filter { it.isFile }.forEach {
        totalSize += it.length()
        fileCount++
    }

    // Calculate capacity in sectors (4096 bytes per sector/cluster)
    // Add overhead for FAT entries and directory structures
    val clusterSize = ClusteredFormatDOM.CLUSTER_SIZE
    val sectorsForData = (totalSize + clusterSize - 1) / clusterSize
    // Add ~25% overhead for FAT, directory entries, and slack space
    val capacityInSectors = ((sectorsForData * 1.25) + fileCount + 256).toLong()
        .coerceAtMost(ClusteredFormatDOM.MAX_CAPA_IN_SECTORS.toLong())
        .toInt()

    println("  Files: $fileCount")
    println("  Total size: ${totalSize / 1024} KB")
    println("  Allocating $capacityInSectors sectors...")

    println("Creating archive: ${outFile.path}")
    val diskArchive = ClusteredFormatDOM.createNewArchive(outFile, Charsets.UTF_8, "Terrarum Assets", capacityInSectors)
    val dom = ClusteredFormatDOM(diskArchive)

    println("Importing files from ${srcDir.path}...")
    val root = Clustfile(dom, "/")
    root.importFrom(srcDir)

    println("Trimming archive...")
    dom.trimArchive()
    dom.dispose()

    println("Done. Output:")
    println("  ${outFile.path} (${outFile.length() / 1024} KB, $fileCount files)")
}
