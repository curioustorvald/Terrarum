package net.torvald.terrarum.worlddrawer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.utils.GdxRuntimeException
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.toInt
import net.torvald.terrarum.worlddrawer.FeaturesDrawer.TILE_SIZE

/**
 * This class implements work_files/dynamic_shape_2_0.psd
 *
 * Created by minjaesong on 2019-02-28.
 */
object CreateTileAtlas {

    const val TILES_IN_X = 256
    
    lateinit var atlas: Pixmap
    lateinit var atlasAutumn: Pixmap
    lateinit var atlasWinter: Pixmap
    lateinit var atlasSpring: Pixmap
    internal lateinit var tags: HashMap<Int, RenderTag>
        private set
    private val defaultRenderTag = RenderTag(3, RenderTag.CONNECT_SELF, RenderTag.MASK_NA) // 'update' block
    var initialised = false
        private set

    /** 0000.tga, 1.tga.gz, 3242423.tga, 000033.tga.gz */
    // for right now, TGA file only, no gzip
    private val validTerrainTilesFilename = Regex("""[0-9]+\.tga""")//Regex("""[0-9]+\.tga(.gz)?""")

    // 16 tiles are reserved for internal use: solid black, solid white, breakage stages.
    // 0th tile is complete transparent tile and is also a BlockID of zero: air.
    private var atlasCursor = 0
    private val atlasInit = "./assets/graphics/blocks/init.tga"

    /**
     * Must be called AFTER mods' loading
     */
    operator fun invoke(updateExisting: Boolean = false) { if (updateExisting || !initialised) {

        tags = HashMap<Int, RenderTag>()
        tags[0] = RenderTag(0, RenderTag.CONNECT_SELF, RenderTag.MASK_NA)

        atlas = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)
        atlasAutumn = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)
        atlasWinter = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)
        atlasSpring = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)


        val initMap = Pixmap(Gdx.files.internal(atlasInit))
        drawToAtlantes(initMap, 16)
        initMap.dispose()


        // get all the files applicable
        // first, get all the '/blocks' directory
        val tgaList = ArrayList<FileHandle>()
        ModMgr.getGdxFilesFromEveryMod("blocks").forEach {
            if (!it.isDirectory) {
                throw Error("Path '${it.path()}' is not a directory")
            }

            // then, filter the file such that its name matches the regex
            it.list { _, name -> name.matches(validTerrainTilesFilename) }.forEach { tgaFile ->
                tgaList.add(tgaFile)
            }
        }

        // Sift through the file list, but TGA format first
        tgaList.filter { it.extension().toUpperCase() == "TGA" }.forEach {
            try {
                fileToAtlantes(it)
            }
            catch (e: GdxRuntimeException) {
                System.err.println("Couldn't load file $it, skipping...")
            }
        }

        // Sift throuth the file list, second TGA.GZ
        /*tgaList.filter { it.name().toUpperCase().endsWith(".TGA.GZ") }.forEach {
            try {
                fileToAtlantes(it)
            }
            catch (e: GdxRuntimeException) {
                System.err.println("Couldn't load file $it, skipping...")
            }
        }*/


        initialised = true
    } }

    fun getRenderTag(blockID: Int): RenderTag {
        return tags.getOrDefault(blockID, defaultRenderTag)
    }

    private fun fileToAtlantes(it: FileHandle) {
        val tilesPixmap = Pixmap(it)
        val blockID = it.nameWithoutExtension().toInt()

        // determine the type of the block (populate tags list)
        // predefined by the image dimension: 16x16 for (1,0)
        if (tilesPixmap.width == TILE_SIZE && tilesPixmap.height == TILE_SIZE) {
            addTag(blockID, RenderTag.CONNECT_SELF, RenderTag.MASK_NA)
            drawToAtlantes(tilesPixmap, RenderTag.maskTypeToTileCount(RenderTag.MASK_NA))
        }
        // predefined by the image dimension: 64x16 for (2,3)
        else if (tilesPixmap.width == TILE_SIZE * 4 && tilesPixmap.height == TILE_SIZE) {
            addTag(blockID, RenderTag.CONNECT_WALL_STICKER, RenderTag.MASK_TORCH)
            drawToAtlantes(tilesPixmap, RenderTag.maskTypeToTileCount(RenderTag.MASK_TORCH))
        }
        // predefined by the image dimension: 128x16 for (3,4)
        else if (tilesPixmap.width == TILE_SIZE * 8 && tilesPixmap.height == TILE_SIZE) {
            addTag(blockID, RenderTag.CONNECT_WALL_STICKER_CONNECT_SELF, RenderTag.MASK_PLATFORM)
            drawToAtlantes(tilesPixmap, RenderTag.maskTypeToTileCount(RenderTag.MASK_PLATFORM))
        }
        // 112x112 or 224x224
        else {
            if (tilesPixmap.width != tilesPixmap.height && tilesPixmap.width % (7 * TILE_SIZE) >= 2) {
                throw IllegalArgumentException("Unrecognized image dimension: ${tilesPixmap.width}x${tilesPixmap.height}")
            }
            // figure out the tags
            var connectionType = 0
            var maskType = 0
            for (bit in 0 until TILE_SIZE) {
                val x = (7 * TILE_SIZE - 1) - bit
                val y1 = 5 * TILE_SIZE; val y2 = y1 + 1
                val pixel1 = (tilesPixmap.getPixel(x, y1).and(255) >= 128).toInt()
                val pixel2 = (tilesPixmap.getPixel(x, y2).and(255) >= 128).toInt()

                connectionType += pixel1 shl bit
                maskType += pixel2 shl bit
            }

            addTag(blockID, connectionType, maskType)
            drawToAtlantes(tilesPixmap, RenderTag.maskTypeToTileCount(maskType))
        }

        tilesPixmap.dispose()
    }

    /**
     * This function must precede the drawToAtlantes() function, as the marking requires the variable
     * 'atlasCursor' and the draw function modifies it!
     */
    private fun addTag(id: Int, connectionType: Int, maskType: Int) {
        if (tags.containsKey(id)) {
            throw Error("Block $id already exists")
        }

        tags[id] = RenderTag(atlasCursor, connectionType, maskType)
    }

    private fun drawToAtlantes(pixmap: Pixmap, tilesCount: Int) {
        val seasonal = pixmap.width == pixmap.height && pixmap.width == 14 * TILE_SIZE
        val txOfPixmap = pixmap.width / TILE_SIZE
        val tyOfPixmap = pixmap.height / TILE_SIZE
        for (i in 0 until tilesCount) {
            printdbg(this, "Rendering to atlas, tile# $atlasCursor")

            // different texture for different seasons (224x224)
            if (seasonal) {
                val i = if (i < 41) i else i + 1 // to compensate the discontinuity between 40th and 41st tile
                _drawToAtlantes(pixmap, atlasCursor, i % 7, i / 7, 1)
                _drawToAtlantes(pixmap, atlasCursor, i % 7 + 7, i / 7, 2)
                _drawToAtlantes(pixmap, atlasCursor, i % 7 + 7, i / 7 + 7, 3)
                _drawToAtlantes(pixmap, atlasCursor, i % 7, i / 7 + 7, 4)
                atlasCursor += 1
            }
            else {
                val i = if (i < 41) i else i + 1 // to compensate the discontinuity between 40th and 41st tile
                _drawToAtlantes(pixmap, atlasCursor, i % txOfPixmap, i / tyOfPixmap, 0)
                atlasCursor += 1
            }
        }
    }

    /**
     * mode: 0 for all the atlantes, 1-4 for summer/autumn/winter/spring atlas
     */
    private fun _drawToAtlantes(pixmap: Pixmap, destTileNum: Int, srcTileX: Int, srcTileY: Int, mode: Int) {
        if (mode == 0) {
            _drawToAtlantes(pixmap, destTileNum, srcTileX, srcTileY, 1)
            _drawToAtlantes(pixmap, destTileNum, srcTileX, srcTileY, 2)
            _drawToAtlantes(pixmap, destTileNum, srcTileX, srcTileY, 3)
            _drawToAtlantes(pixmap, destTileNum, srcTileX, srcTileY, 4)
        }
        else {
            val atlasX = (destTileNum % TILES_IN_X) * TILE_SIZE
            val atlasY = (destTileNum / TILES_IN_X) * TILE_SIZE
            val sourceX = srcTileX * TILE_SIZE
            val sourceY = srcTileY * TILE_SIZE

            when (mode) {
                1 -> atlas.drawPixmap(pixmap, sourceX, sourceY, TILE_SIZE, TILE_SIZE, atlasX, atlasY, TILE_SIZE, TILE_SIZE)
                2 -> atlasAutumn.drawPixmap(pixmap, sourceX, sourceY, TILE_SIZE, TILE_SIZE, atlasX, atlasY, TILE_SIZE, TILE_SIZE)
                3 -> atlasWinter.drawPixmap(pixmap, sourceX, sourceY, TILE_SIZE, TILE_SIZE, atlasX, atlasY, TILE_SIZE, TILE_SIZE)
                4 -> atlasSpring.drawPixmap(pixmap, sourceX, sourceY, TILE_SIZE, TILE_SIZE, atlasX, atlasY, TILE_SIZE, TILE_SIZE)
            }
        }
    }

    data class RenderTag(val atlasStartingPosition: Int, val connectionType: Int, val maskType: Int) {
        companion object {
            const val CONNECT_MUTUAL = 0
            const val CONNECT_SELF = 1
            const val CONNECT_WALL_STICKER = 2
            const val CONNECT_WALL_STICKER_CONNECT_SELF = 3

            const val MASK_NA = 0
            const val MASK_16 = 1
            const val MASK_47 = 2
            const val MASK_TORCH = 3
            const val MASK_PLATFORM = 4

            fun maskTypeToTileCount(maskType: Int) = when (maskType) {
                MASK_NA -> 1
                MASK_16 -> 16
                MASK_47 -> 47
                MASK_TORCH -> 4
                MASK_PLATFORM -> 8
                else -> throw IllegalArgumentException("Unknown maskType: $maskType")
            }
        }
    }

    fun dispose() {
        atlas.dispose()
        atlasAutumn.dispose()
        atlasWinter.dispose()
        atlasSpring.dispose()
    }

}