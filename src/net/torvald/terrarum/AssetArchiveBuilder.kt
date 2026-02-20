package net.torvald.terrarum

import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.archivers.ClusteredFormatDOM
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.archivers.Clustfile
import java.io.File

/**
 * Build-time tool that creates assets.tevd from assets_release/ directory.
 *
 * Usage: java -cp <classpath> net.torvald.terrarum.AssetArchiveBuilderKt [assets_release_dir] [output_file]
 *
 * Created by minjaesong on 2026-02-19.
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
    var dirCount = 0
    srcDir.walkTopDown().forEach {
        if (it.isFile) {
            totalSize += it.length()
            fileCount++
        } else if (it.isDirectory && it != srcDir) {
            dirCount++
        }
    }

    // Calculate capacity in sectors (4096 bytes per cluster)
    // Each file uses at least 1 cluster regardless of size
    // Each directory also needs a FAT entry and cluster(s) for its directory listing
    // FAT entries are 256 bytes each; each cluster holds 16 FAT entries
    val clusterSize = ClusteredFormatDOM.CLUSTER_SIZE
    val totalEntries = fileCount + dirCount
    // Sectors for actual file data (each file rounds up to whole clusters)
    val sectorsForData = (totalSize + clusterSize - 1) / clusterSize
    // Each entry (file or dir) needs at least 1 sector, plus FAT overhead
    // FAT entries: 256 bytes each, so totalEntries * 256 / 4096 sectors for FAT
    val sectorsForFAT = (totalEntries.toLong() * ClusteredFormatDOM.FAT_ENTRY_SIZE + clusterSize - 1) / clusterSize
    // Directory listings: each dir needs cluster(s) to store its child entries
    val sectorsForDirs = dirCount.toLong()
    // Total with generous margin (50% on data + fixed overhead)
    val capacityInSectors = ((sectorsForData * 1.5) + sectorsForFAT + sectorsForDirs + totalEntries + 512).toLong()
        .coerceAtMost(ClusteredFormatDOM.MAX_CAPA_IN_SECTORS.toLong())
        .toInt()

    println("  Files: $fileCount, Directories: $dirCount")
    println("  Total size: ${totalSize / 1024} KB")
    println("  Allocating $capacityInSectors sectors...")

    println("Creating archive: ${outFile.path}")
    val diskArchive = ClusteredFormatDOM.createNewArchive(outFile, Charsets.UTF_8, "Terrarum Assets", capacityInSectors)
    val dom = ClusteredFormatDOM(diskArchive)

    // Pre-grow the FAT area to avoid growFAT/renum being triggered mid-import.
    // Initial FAT is 2 clusters (32 entries). Each growFAT roughly doubles it.
    // Each file/dir needs 1 FAT entry PLUS extended entries for inline data, long filenames, etc.
    // Inline files need ~ceil(fileSize/248) extended entries; inline dirs need extended entries for listings.
    // Conservative estimate: each entry needs ~3 FAT slots on average (1 head + 2 extended).
    val neededFatClusters = (totalEntries * 3 + 20) / 16 + 4
    var estimatedFatClusters = 2
    var growthCount = 0
    while (estimatedFatClusters < neededFatClusters) {
        estimatedFatClusters = 2 * estimatedFatClusters + 2
        growthCount++
    }
    if (growthCount > 0) {
        println("  Pre-growing FAT: $growthCount growth(s) -> ~$estimatedFatClusters FAT clusters (${estimatedFatClusters * 16} entries)...")
        repeat(growthCount) { dom.growFAT() }
    }

    println("Importing files from ${srcDir.path}...")
    val children = srcDir.listFiles() ?: run {
        System.err.println("Error: Could not list files in ${srcDir.path}")
        System.exit(1)
        return
    }
    var importedCount = 0
    for (child in children.sortedBy { it.name }) {
        println("  Importing: ${child.name} (${if (child.isDirectory) "dir" else "${child.length()} bytes"})...")
        val dest = Clustfile(dom, "/${child.name}")
        val success = dest.importFrom(child)
        if (!success) {
            System.err.println("Warning: Failed to import ${child.name}")
        } else {
            importedCount++
            println("  Imported: ${child.name}")
        }
    }

    println("Trimming archive...")
    dom.trimArchive()
    dom.dispose()

    println("Done. Imported $importedCount top-level entries. Output:")
    println("  ${outFile.path} (${outFile.length() / 1024} KB, $fileCount files)")
}
