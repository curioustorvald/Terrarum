package net.torvald.terrarum.worlddrawer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.GdxRuntimeException
import net.torvald.gdx.graphics.Cvec
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.blockproperties.Fluid
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameworld.GameWorld
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
class CreateTileAtlas {

    // min size 1024 = tile_size 16 * atlasCursor 64
    val MAX_TEX_SIZE = App.getConfigInt("atlastexsize").coerceIn(1024, App.glInfo.GL_MAX_TEXTURE_SIZE)
    val TILES_IN_X = MAX_TEX_SIZE / TILE_SIZE

    val SHADER_SIZE_KEYS = floatArrayOf(MAX_TEX_SIZE.toFloat(), MAX_TEX_SIZE.toFloat(), TILES_IN_X.toFloat(), TILES_IN_X.toFloat())

    val ITEM_ATLAS_TILES_X = 16

    private val TOTAL_TILES = TILES_IN_X * TILES_IN_X

    val wallOverlayColour = Color(.65f, .65f, .65f, 1f)

    lateinit var atlas: Pixmap
    lateinit var atlasAutumn: Pixmap
    lateinit var atlasWinter: Pixmap
    lateinit var atlasSpring: Pixmap
    lateinit var atlasFluid: Pixmap
    lateinit var atlasGlow: Pixmap // glowing won't be affected by the season... for now
    lateinit var itemTerrainTexture: Texture
    lateinit var itemWallTexture: Texture
    lateinit var terrainTileColourMap: HashMap<ItemID, Cvec>
    lateinit var tags: HashMap<ItemID, RenderTag> // TileID, RenderTag
        private set
    lateinit var itemSheetNumbers: HashMap<ItemID, Int> // TileID, Int
        private set
    private val defaultRenderTag = RenderTag(3, RenderTag.CONNECT_SELF, RenderTag.MASK_NA) // 'update' block
    var initialised = false
        private set

    /** 0.tga, 1.tga.gz, 3242423.tga, 33.tga.gz */
    private val tileNameRegex = Regex("""(0|[1-9][0-9]*)\.tga(\.gz)?""")

    // 16 tiles are reserved for internal use: solid black, solid white, breakage stages.
    // 0th tile is complete transparent tile and is also a BlockID of zero: air.
    private var atlasCursor = 64 // 64 predefined tiles. The normal blocks (e.g. Air) should start from this number
    private val atlasInit = "./assets/graphics/blocks/init.tga"
    private var itemSheetCursor = 16

    /**
     * Must be called AFTER mods' loading so that all the block props are loaded
     */
    operator fun invoke(updateExisting: Boolean = false) { if (updateExisting || !initialised) {

        tags = HashMap<ItemID, RenderTag>()
        itemSheetNumbers = HashMap<ItemID, Int>()

        atlas = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)
        atlasAutumn = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)
        atlasWinter = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)
        atlasSpring = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)
        atlasFluid = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)
        atlasGlow = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)

        atlas.blending = Pixmap.Blending.None
        atlasAutumn.blending = Pixmap.Blending.None
        atlasWinter.blending = Pixmap.Blending.None
        atlasSpring.blending = Pixmap.Blending.None
        atlasFluid.blending = Pixmap.Blending.None
        atlasGlow.blending = Pixmap.Blending.None

        // populate the atlantes with atlasInit
        // this just directly copies the image to the atlantes :p
        val initPixmap = Pixmap(Gdx.files.internal(atlasInit))
        atlas.drawPixmap(initPixmap, 0, 0)
        atlasAutumn.drawPixmap(initPixmap, 0, 0)
        atlasWinter.drawPixmap(initPixmap, 0, 0)
        atlasSpring.drawPixmap(initPixmap, 0, 0)

        // get all the files applicable
        // first, get all the '/blocks' directory, and add all the files, regardless of their extension, to the list
        val tgaList = ArrayList<Pair<String, FileHandle>>() //Pair of <modname, filehandle>
        ModMgr.getGdxFilesFromEveryMod("blocks").forEach { (modname, dir) ->
            if (!dir.isDirectory) {
                throw Error("Path '${dir.path()}' is not a directory")
            }

            // filter files that do not exist on the blockcodex
            dir.list().filter { tgaFile -> !tgaFile.isDirectory && (BlockCodex.getOrNull("$modname:${tgaFile.nameWithoutExtension()}") != null) }
                    .sortedBy { it.nameWithoutExtension().toInt() }.forEach { tgaFile -> // toInt() to sort by the number, not lexicographically
                        tgaList.add(modname to tgaFile)
                    }
        }


        // Sift through the file list for blocks, but TGA format first
        tgaList.forEach { (modname, filehandle) ->
            printdbg(this, "processing $modname:${filehandle.name()}")

            try {
                val glowFile = Gdx.files.internal(filehandle.path().dropLast(4) + "_glow.tga") // assuming strict ".tga" file for now...
                fileToAtlantes(modname, filehandle, if (glowFile.exists()) glowFile else null)
            }
            catch (e: GdxRuntimeException) {
                System.err.println("Couldn't load file $filehandle from $modname, skipping...")
            }
        }

        // test print
        //PixmapIO2.writeTGA(Gdx.files.absolute("${AppLoader.defaultDir}/atlas.tga"), atlas, false)
        //PixmapIO2.writeTGA(Gdx.files.absolute("${AppLoader.defaultDir}/atlasGlow.tga"), atlasGlow, false)



        // Sift throuth the file list, second TGA.GZ
        /*tgaList.filter { it.name().toUpperCase().endsWith(".TGA.GZ") }.forEach {
            try {
                fileToAtlantes(it)
            }
            catch (e: GdxRuntimeException) {
                System.err.println("Couldn't load file $it, skipping...")
            }
        }*/

        // create item_wall images

        fun maskTypetoTileIDForItemImage(maskType: Int) = when(maskType) {
            CreateTileAtlas.RenderTag.MASK_47 -> 17
            CreateTileAtlas.RenderTag.MASK_PLATFORM -> 7
            else -> 0
        }

        val itemTerrainPixmap = Pixmap(16 * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)
        val itemWallPixmap = Pixmap(16 * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)

        tags.toMap().forEach { id, tag ->
            val tilePosFromAtlas = tag.tileNumber + maskTypetoTileIDForItemImage(tag.maskType)
            val srcX = (tilePosFromAtlas % TILES_IN_X) * TILE_SIZE
            val srcY = (tilePosFromAtlas / TILES_IN_X) * TILE_SIZE
            val t = tileIDtoItemSheetNumber(id)
            val destX = (t % ITEM_ATLAS_TILES_X) * TILE_SIZE
            val destY = (t / ITEM_ATLAS_TILES_X) * TILE_SIZE
            itemTerrainPixmap.drawPixmap(atlas, srcX, srcY, TILE_SIZE, TILE_SIZE, destX, destY, TILE_SIZE, TILE_SIZE)
            itemWallPixmap.drawPixmap(atlas, srcX, srcY, TILE_SIZE, TILE_SIZE, destX, destY, TILE_SIZE, TILE_SIZE)
        }
        // darken things for the wall
        for (y in 0 until itemWallPixmap.height) {
            for (x in 0 until itemWallPixmap.width) {
                val c = Color(itemWallPixmap.getPixel(x, y)).mulAndAssign(wallOverlayColour).toRGBA()
                itemWallPixmap.drawPixel(x, y, c)
            }
        }


        // create terrain colourmap
        terrainTileColourMap = HashMap<ItemID, Cvec>()
        val pxCount = TILE_SIZE * TILE_SIZE
        for (id in itemSheetNumbers) {
            val tilenum = id.value
            val tx = (tilenum % ITEM_ATLAS_TILES_X) * TILE_SIZE
            val ty = (tilenum / ITEM_ATLAS_TILES_X) * TILE_SIZE
            var r = 0f; var g = 0f; var b = 0f; var a = 0f
            // average out the whole block
            for (y in ty until ty + TILE_SIZE) {
                for (x in tx until tx + TILE_SIZE) {
                    val data = itemTerrainPixmap.getPixel(x, y)
                    r += ((data ushr 24) and 255).div(255f)
                    g += ((data ushr 16) and 255).div(255f)
                    b += ((data ushr  8) and 255).div(255f)
                    a += (data and 255).div(255f)
                }
            }

            terrainTileColourMap[id.key] = Cvec(
                    (r / pxCount),
                    (g / pxCount),
                    (b / pxCount),
                    (a / pxCount)
            )
        }

        itemTerrainTexture = Texture(itemTerrainPixmap)
        itemWallTexture = Texture(itemWallPixmap)
        itemTerrainPixmap.dispose()
        itemWallPixmap.dispose()
        initPixmap.dispose()

        initialised = true
    } }

    fun getRenderTag(blockID: ItemID): RenderTag {
        return tags.getOrDefault(blockID, defaultRenderTag)
    }

    fun fluidFillToTileLevel(fill: Float) = fill.times(8).roundToInt().coerceIn(0, 8)

    fun fluidToTileNumber(fluid: GameWorld.FluidInfo): Int {
        val fluidLevel = fluidFillToTileLevel(fluid.amount)
        return if (fluid.type == Fluid.NULL || fluidLevel == 0) 0 else
            16 + (376 * (fluid.type.abs() - 1)) + (47 * (fluidLevel - 1))
    }

    val nullTile = Pixmap(TILE_SIZE * 16, TILE_SIZE * 16, Pixmap.Format.RGBA8888)

    private fun fileToAtlantes(modname: String, matte: FileHandle, glow: FileHandle?) {
        val tilesPixmap = Pixmap(matte)
        val tilesGlowPixmap = if (glow != null) Pixmap(glow) else nullTile
        val blockName = matte.nameWithoutExtension().toInt() // basically a filename
        val blockID = "$modname:$blockName"

        // determine the type of the block (populate tags list)
        // predefined by the image dimension: 16x16 for (1,0)
        if (tilesPixmap.width == TILE_SIZE && tilesPixmap.height == TILE_SIZE) {
            addTag(blockID, RenderTag.CONNECT_SELF, RenderTag.MASK_NA)
            drawToAtlantes(tilesPixmap, tilesGlowPixmap, RenderTag.maskTypeToTileCount(RenderTag.MASK_NA))
        }
        // predefined by the image dimension: 64x16 for (2,3)
        else if (tilesPixmap.width == TILE_SIZE * 4 && tilesPixmap.height == TILE_SIZE) {
            addTag(blockID, RenderTag.CONNECT_WALL_STICKER, RenderTag.MASK_TORCH)
            drawToAtlantes(tilesPixmap, tilesGlowPixmap, RenderTag.maskTypeToTileCount(RenderTag.MASK_TORCH))
        }
        // predefined by the image dimension: 128x16 for (3,4)
        else if (tilesPixmap.width == TILE_SIZE * 8 && tilesPixmap.height == TILE_SIZE) {
            addTag(blockID, RenderTag.CONNECT_WALL_STICKER_CONNECT_SELF, RenderTag.MASK_PLATFORM)
            drawToAtlantes(tilesPixmap, tilesGlowPixmap, RenderTag.maskTypeToTileCount(RenderTag.MASK_PLATFORM))
        }
        // 112x112 or 224x224
        else {
            if (tilesPixmap.width != tilesPixmap.height && tilesPixmap.width % (7 * TILE_SIZE) >= 2) {
                throw IllegalArgumentException("Unrecognized image dimension ${tilesPixmap.width}x${tilesPixmap.height} from $modname:${matte.name()}")
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
            val tileCount = RenderTag.maskTypeToTileCount(maskType)
            drawToAtlantes(tilesPixmap, tilesGlowPixmap, tileCount)
        }

        itemSheetNumbers[blockID] = itemSheetCursor
        itemSheetCursor += 1

        tilesPixmap.dispose()
    }

    fun tileIDtoAtlasNumber(tileID: ItemID) = tags[tileID]?.tileNumber
                                              ?: throw NullPointerException("AtlasNumbers mapping from $tileID does not exist")
    fun tileIDtoItemSheetNumber(tileID: ItemID) = itemSheetNumbers[tileID]
                                                  ?: throw NullPointerException("ItemSheetNumber mapping from $tileID does not exist")

    /**
     * This function must precede the drawToAtlantes() function, as the marking requires the variable
     * 'atlasCursor' and the draw function modifies it!
     */
    private fun addTag(id: ItemID, connectionType: Int, maskType: Int) {
        if (tags.containsKey(id)) {
            throw Error("Block $id already exists")
        }

        tags[id] = RenderTag(atlasCursor, connectionType, maskType)

        printdbg(this, "tileName ${id} ->> tileNumber ${atlasCursor}")
    }

    private fun drawToAtlantes(pixmap: Pixmap, glow: Pixmap, tilesCount: Int) {
        if (atlasCursor >= TOTAL_TILES) {
            throw Error("Too much tiles for $MAX_TEX_SIZE texture size: $atlasCursor")
        }

        val seasonal = pixmap.width == pixmap.height && pixmap.width == 14 * TILE_SIZE
        val txOfPixmap = pixmap.width / TILE_SIZE
        val txOfPixmapGlow = glow.width / TILE_SIZE
        for (i in 0 until tilesCount) {
            //printdbg(this, "Rendering to atlas, tile# $atlasCursor, tilesCount = $tilesCount, seasonal = $seasonal")

            // different texture for different seasons (224x224)
            if (seasonal) {
                val i = if (i < 41) i else i + 1 // to compensate the discontinuity between 40th and 41st tile
                _drawToAtlantes(pixmap, atlasCursor, i % 7, i / 7, 1)
                _drawToAtlantes(pixmap, atlasCursor, i % 7 + 7, i / 7, 2)
                _drawToAtlantes(pixmap, atlasCursor, i % 7 + 7, i / 7 + 7, 3)
                _drawToAtlantes(pixmap, atlasCursor, i % 7, i / 7 + 7, 4)
                _drawToAtlantes(glow, atlasCursor, i % 7, i / 7, 6)
                atlasCursor += 1
            }
            else {
                val i = if (i < 41) i else i + 1 // to compensate the discontinuity between 40th and 41st tile
                _drawToAtlantes(pixmap, atlasCursor, i % txOfPixmap, i / txOfPixmap, 0)
                _drawToAtlantes(glow, atlasCursor, i % txOfPixmapGlow, i / txOfPixmapGlow, 6)
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
                6 -> atlasGlow.drawPixmap(pixmap, sourceX, sourceY, TILE_SIZE, TILE_SIZE, atlasX, atlasY, TILE_SIZE, TILE_SIZE)
            }
        }
    }

    /**
     * @param tileNumber ordinal number of a tile in the texture atlas
     */
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
        atlasGlow.dispose()
        //itemTerrainTexture.dispose() //BlocksDrawer will dispose of it as it disposes of 'tileItemTerrain (TextureRegionPack)'
        //itemWallTexture.dispose() //BlocksDrawer will dispose of it as it disposes of 'tileItemWall (TextureRegionPack)'

        nullTile.dispose()
    }
}