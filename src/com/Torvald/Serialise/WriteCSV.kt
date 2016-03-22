package com.Torvald.Serialise

import com.Torvald.CSVFetcher
import com.Torvald.Terrarum.ItemProperties.ItemPropCodex
import com.Torvald.Terrarum.ItemProperties.MaterialPropCodex
import com.Torvald.Terrarum.Terrarum
import com.Torvald.Terrarum.TileProperties.TilePropCodex
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * Created by minjaesong on 16-03-18.
 */
object WriteCSV {
    val META_FILENAME_TILE = "worldinfo2"
    val META_FILENAME_ITEM = "worldinfo3"
    val META_FILENAME_MAT = "worldinfo4"

    fun write(saveDirectoryName: String): Boolean {
        val tileCSV = CSVFetcher.readCSVasString(TilePropCodex.CSV_PATH)
        val itemCSV = CSVFetcher.readCSVasString(ItemPropCodex.CSV_PATH)
        val matCSV = CSVFetcher.readCSVasString(MaterialPropCodex.CSV_PATH)

        val pathTile = Paths.get("${Terrarum.defaultSaveDir}" +
                                       "/$saveDirectoryName/${META_FILENAME_TILE}")
        val pathItem = Paths.get("${Terrarum.defaultSaveDir}" +
                                       "/$saveDirectoryName/${META_FILENAME_ITEM}")
        val pathMat = Paths.get("${Terrarum.defaultSaveDir}" +
                                       "/$saveDirectoryName/${META_FILENAME_MAT}")
        val tempPathTile = Files.createTempFile(pathTile.toString(), "_temp")
        val tempPathItem = Files.createTempFile(pathItem.toString(), "_temp")
        val tempPathMat = Files.createTempFile(pathMat.toString(), "_temp")

        // TODO gzip

        // write CSV to path
        Files.write(tempPathTile, tileCSV.toByteArray(Charsets.UTF_8))
        Files.write(tempPathItem, itemCSV.toByteArray(Charsets.UTF_8))
        Files.write(tempPathMat, matCSV.toByteArray(Charsets.UTF_8))

        // replace savemeta with tempfile
        try {
            Files.copy(tempPathTile, pathTile, StandardCopyOption.REPLACE_EXISTING)
            Files.deleteIfExists(tempPathTile)

            Files.copy(tempPathItem, pathItem, StandardCopyOption.REPLACE_EXISTING)
            Files.deleteIfExists(tempPathItem)

            Files.copy(tempPathMat, pathMat, StandardCopyOption.REPLACE_EXISTING)
            Files.deleteIfExists(tempPathMat)

            println("Saved map data '${WriteGameMapData.META_FILENAME}' to $saveDirectoryName.")

            return true
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

}