package net.torvald.terrarum.worlddrawer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.GdxRuntimeException
import net.torvald.terrarum.GdxColorMap
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.blockproperties.Fluid
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.mulAndAssign
import net.torvald.terrarum.toInt
import kotlin.math.roundToInt

/**
 * This class implements work_files/dynamic_shape_2_0.psd
 *
 * Tile at (0,0) AND (5,0) must be transparent. Former is because block 0 is considered as an air, and the latter
 * is because it's breakage of 0, and 0 means no breakage. Breakage part is hard-coded in the tiling shader.
 *
 * Any real tiles must begin from (0,16), the first 256x16 section is reserved for special purpose (terrain: breakage, fluid: empty)
 *
 * Created by minjaesong on 2019-02-28.
 */
object CreateTileAtlas {

    const val MAX_TEX_SIZE = 4096
    const val TILE_SIZE = 16
    const val TILES_IN_X = MAX_TEX_SIZE / TILE_SIZE

    private val TOTAL_TILES = TILES_IN_X * TILES_IN_X

    lateinit var atlas: Pixmap
    lateinit var atlasAutumn: Pixmap
    lateinit var atlasWinter: Pixmap
    lateinit var atlasSpring: Pixmap
    lateinit var atlasFluid: Pixmap
    lateinit var itemTerrainTexture: Texture
    lateinit var itemWallTexture: Texture
    lateinit var terrainTileColourMap: GdxColorMap
    internal lateinit var tags: HashMap<Int, RenderTag>
        private set
    private val defaultRenderTag = RenderTag(3, RenderTag.CONNECT_SELF, RenderTag.MASK_NA) // 'update' block
    var initialised = false
        private set

    /** 0000.tga, 1.tga.gz, 3242423.tga, 000033.tga.gz */
    // for right now, TGA file only, no gzip
    private val validTerrainTilesFilename = Regex("""[0-9]+\.tga""")//Regex("""[0-9]+\.tga(.gz)?""")
    private val validFluidTilesFilename = Regex("""fluid_[0-9]+\.tga""")

    // 16 tiles are reserved for internal use: solid black, solid white, breakage stages.
    // 0th tile is complete transparent tile and is also a BlockID of zero: air.
    private var atlasCursor = 0
    private val atlasInit = "./assets/graphics/blocks/init.tga"

    /**
     * Must be called AFTER mods' loading so that all the block props are loaded
     */
    operator fun invoke(updateExisting: Boolean = false) { if (updateExisting || !initialised) {

        tags = HashMap<Int, RenderTag>()
        tags[0] = RenderTag(0, RenderTag.CONNECT_SELF, RenderTag.MASK_NA)

        atlas = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)
        atlasAutumn = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)
        atlasWinter = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)
        atlasSpring = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)
        atlasFluid = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)

        atlas.blending = Pixmap.Blending.None
        atlasAutumn.blending = Pixmap.Blending.None
        atlasWinter.blending = Pixmap.Blending.None
        atlasSpring.blending = Pixmap.Blending.None
        atlasFluid.blending = Pixmap.Blending.None

        val initMap = Pixmap(Gdx.files.internal(atlasInit))
        drawToAtlantes(initMap, 16)
        initMap.dispose()


        // get all the files applicable
        // first, get all the '/blocks' directory, and add all the files, regardless of their extension, to the list
        val tgaList = ArrayList<FileHandle>()
        ModMgr.getGdxFilesFromEveryMod("blocks").forEach {
            if (!it.isDirectory) {
                throw Error("Path '${it.path()}' is not a directory")
            }

            it.list().forEach { tgaFile ->
                if (!tgaFile.isDirectory) tgaList.add(tgaFile)
            }
        }

        // Sift through the file list for blocks, but TGA format first
        tgaList.filter { it.name().matches(validTerrainTilesFilename) && it.extension().toUpperCase() == "TGA" }.forEach {
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


        // Sift through the file list for fluids, but TGA format first
        val fluidMasterPixmap = Pixmap(TILE_SIZE * 47, TILE_SIZE * 8, Pixmap.Format.RGBA8888)
        tgaList.filter { it.name().matches(validFluidTilesFilename) && it.extension().toUpperCase() == "TGA" }.forEachIndexed { fluidLevel, it ->
            val pixmap = Pixmap(it)
            // dirty manual copy
            repeat(5) {
                fluidMasterPixmap.drawPixmap(pixmap,
                        it * TILE_SIZE * 7, fluidLevel * TILE_SIZE,
                        0, TILE_SIZE * it,
                        TILE_SIZE * 7, TILE_SIZE
                )
            }
            repeat(2) {
                fluidMasterPixmap.drawPixmap(pixmap,
                        (35 + it * 6) * TILE_SIZE, fluidLevel * TILE_SIZE,
                        0, TILE_SIZE * (5 + it),
                        TILE_SIZE * 6, TILE_SIZE
                )
            }

            pixmap.dispose()
        }
        // test print
        //PixmapIO2.writeTGA(Gdx.files.absolute("${AppLoader.defaultDir}/fluidpixmapmaster.tga"), fluidMasterPixmap, false)

        // occupy the fluid pixmap with software rendering
        for (i in BlockCodex.MAX_TERRAIN_TILES..BlockCodex.highestNumber) {
            val fluid = Color(BlockCodex[i].colour)

            // pixmap <- (color SCREEN fluidMasterPixmap)
            // then occupy the atlasFluid
            val pixmap = Pixmap(fluidMasterPixmap.width, fluidMasterPixmap.height, Pixmap.Format.RGBA8888)
            pixmap.blending = Pixmap.Blending.None

            for (y in 0 until pixmap.height) {
                for (x in 0 until pixmap.width) {
                    val inColour = Color(fluidMasterPixmap.getPixel(x, y))
                    // SCREEN for RGB, MUL for A.
                    inColour.r = 1f - (1f - fluid.r) * (1f - inColour.r)
                    inColour.g = 1f - (1f - fluid.g) * (1f - inColour.g)
                    inColour.b = 1f - (1f - fluid.b) * (1f - inColour.b)
                    inColour.a = fluid.a * inColour.a

                    pixmap.drawPixel(x, y, inColour.toRGBA())
                }
            }

            // test print
            //PixmapIO2.writeTGA(Gdx.files.absolute("${AppLoader.defaultDir}/$i.tga"), pixmap, false)

            // to the atlas
            val atlasTargetPos = 16 + 47 * 8 * (i - BlockCodex.MAX_TERRAIN_TILES)
            for (k in 0 until 47 * 8) {
                val srcX = (k % 47) * TILE_SIZE
                val srcY = (k / 47) * TILE_SIZE
                val destX = ((atlasTargetPos + k) % TILES_IN_X) * TILE_SIZE
                val destY = ((atlasTargetPos + k) / TILES_IN_X) * TILE_SIZE
                atlasFluid.drawPixmap(pixmap, srcX, srcY, TILE_SIZE, TILE_SIZE, destX, destY, TILE_SIZE, TILE_SIZE)
            }

            pixmap.dispose()
        }


        fluidMasterPixmap.dispose()

        // create item_wall images

        fun maskTypetoTileIDForItemImage(maskType: Int) = when(maskType) {
            CreateTileAtlas.RenderTag.MASK_47 -> 17
            CreateTileAtlas.RenderTag.MASK_PLATFORM -> 7
            else -> 0
        }

        val itemTerrainPixmap = Pixmap(16 * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)
        val itemWallPixmap = Pixmap(16 * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)
        val terrainColormapPixmap = Pixmap(16, TILES_IN_X, Pixmap.Format.RGBA8888)

        CreateTileAtlas.tags.toMap().forEach { t, u ->
            val tilePosFromAtlas = u.tileNumber + maskTypetoTileIDForItemImage(u.maskType)
            val srcX = (tilePosFromAtlas % TILES_IN_X) * TILE_SIZE
            val srcY = (tilePosFromAtlas / TILES_IN_X) * TILE_SIZE
            val destX = (t % 16) * TILE_SIZE
            val destY = (t / 16) * TILE_SIZE
            itemTerrainPixmap.drawPixmap(CreateTileAtlas.atlas, srcX, srcY, TILE_SIZE, TILE_SIZE, destX, destY, TILE_SIZE, TILE_SIZE)
            itemWallPixmap.drawPixmap(CreateTileAtlas.atlas, srcX, srcY, TILE_SIZE, TILE_SIZE, destX, destY, TILE_SIZE, TILE_SIZE)
        }
        // darken things for the wall
        for (y in 0 until itemWallPixmap.height) {
            for (x in 0 until itemWallPixmap.width) {
                val c = Color(itemWallPixmap.getPixel(x, y)).mulAndAssign(BlocksDrawer.wallOverlayColour).toRGBA()
                itemWallPixmap.drawPixel(x, y, c)
            }
        }
        // create terrain colourmap
        val pxCount = TILE_SIZE * TILE_SIZE
        for (id in 0 until BlockCodex.MAX_TERRAIN_TILES) {
            val tx = (id % 16) * TILE_SIZE
            val ty = (id / 16) * TILE_SIZE
            var r = 0; var g = 0; var b = 0; var a = 0
            // average out the whole block
            for (y in ty until ty + TILE_SIZE) {
                for (x in tx until tx + TILE_SIZE) {
                    val data = itemTerrainPixmap.getPixel(x, y)
                    r += (data ushr 24) and 255
                    g += (data ushr 16) and 255
                    b += (data ushr  8) and 255
                    a += data and 255
                }
            }

            terrainColormapPixmap.drawPixel(tx / TILE_SIZE, ty / TILE_SIZE,
                    (r / pxCount).shl(24) or
                            (g / pxCount).shl(16) or
                            (b / pxCount).shl(8) or
                            (a / pxCount)
            )
        }

        //PixmapIO2.writeTGA(Gdx.files.absolute("${AppLoader.defaultDir}/terrain_colormap.tga"), terrainColormapPixmap, false)
        //PixmapIO2.writeTGA(Gdx.files.absolute("${AppLoader.defaultDir}/terrainitem.tga"), itemTerrainPixmap, false)

        terrainTileColourMap = GdxColorMap(terrainColormapPixmap)
        itemTerrainTexture = Texture(itemTerrainPixmap)
        itemWallTexture = Texture(itemWallPixmap)
        itemTerrainPixmap.dispose()
        itemWallPixmap.dispose()

        initialised = true
    } }

    fun getRenderTag(blockID: Int): RenderTag {
        return tags.getOrDefault(blockID, defaultRenderTag)
    }

    fun fluidFillToTileLevel(fill: Float) = fill.times(8).roundToInt().coerceIn(0, 8)

    fun fluidToTileNumber(fluid: GameWorld.FluidInfo): Int {
        val fluidLevel = fluidFillToTileLevel(fluid.amount)
        return if (fluid.type == Fluid.NULL || fluidLevel == 0) 0 else
            16 + (376 * (fluid.type.abs() - 1)) + (47 * (fluidLevel - 1))
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
        if (atlasCursor >= TOTAL_TILES) {
            throw Error("Too much tiles for $MAX_TEX_SIZE texture size: $atlasCursor")
        }

        val seasonal = pixmap.width == pixmap.height && pixmap.width == 14 * TILE_SIZE
        val txOfPixmap = pixmap.width / TILE_SIZE
        for (i in 0 until tilesCount) {
            //printdbg(this, "Rendering to atlas, tile# $atlasCursor, tilesCount = $tilesCount, seasonal = $seasonal")

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
                _drawToAtlantes(pixmap, atlasCursor, i % txOfPixmap, i / txOfPixmap, 0)
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

            //if (mode == 1) printdbg(this, "atlaspos: ($atlasX, $atlasY), srcpos: ($sourceX, $sourceY), srcpixmap = $pixmap")

            when (mode) {
                1 -> atlas.drawPixmap(pixmap, sourceX, sourceY, TILE_SIZE, TILE_SIZE, atlasX, atlasY, TILE_SIZE, TILE_SIZE)
                2 -> atlasAutumn.drawPixmap(pixmap, sourceX, sourceY, TILE_SIZE, TILE_SIZE, atlasX, atlasY, TILE_SIZE, TILE_SIZE)
                3 -> atlasWinter.drawPixmap(pixmap, sourceX, sourceY, TILE_SIZE, TILE_SIZE, atlasX, atlasY, TILE_SIZE, TILE_SIZE)
                4 -> atlasSpring.drawPixmap(pixmap, sourceX, sourceY, TILE_SIZE, TILE_SIZE, atlasX, atlasY, TILE_SIZE, TILE_SIZE)
                5 -> atlasFluid.drawPixmap(pixmap, sourceX, sourceY, TILE_SIZE, TILE_SIZE, atlasX, atlasY, TILE_SIZE, TILE_SIZE)
            }
        }
    }

    data class RenderTag(val tileNumber: Int, val connectionType: Int, val maskType: Int) {
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
        atlasFluid.dispose()
    }
}