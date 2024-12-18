package net.torvald.terrarum.worlddrawer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.GdxRuntimeException
import com.jme3.math.FastMath
import net.torvald.gdx.graphics.Cvec
import net.torvald.gdx.graphics.PixmapIO2
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration.SUBTILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.utils.HashArray
import net.torvald.terrarum.worlddrawer.CreateTileAtlas.AtlasSource.*
import net.torvald.terrarum.worlddrawer.CreateTileAtlas.RenderTag.Companion.CONNECT_MUTUAL
import net.torvald.terrarum.worlddrawer.CreateTileAtlas.RenderTag.Companion.CONNECT_SELF
import net.torvald.terrarum.worlddrawer.CreateTileAtlas.RenderTag.Companion.MASK_47
import net.torvald.terrarum.worlddrawer.CreateTileAtlas.RenderTag.Companion.MASK_PLATFORM
import net.torvald.terrarum.worlddrawer.CreateTileAtlas.RenderTag.Companion.MASK_SUBTILE_GENERIC
import net.torvald.terrarum.worlddrawer.CreateTileAtlas.RenderTag.Companion.MASK_SUBTILE_GRASS
import net.torvald.terrarum.worlddrawer.CreateTileAtlas.RenderTag.Companion.TILING_FULL
import kotlin.math.sqrt

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

    companion object {
        val WALL_OVERLAY_COLOUR = Color(.72f, .72f, .72f, 1f)

        val W_SUBTILE_GENERIC = 104
        val W_SUBTILE_GRASS = 168
        val H_SUBTILE = 136

        val tileOffsetsForItemImageFromSubtile = arrayOf(
            intArrayOf(4*2  ,4*5  ,4*8  ,4*11), // TILING_FULL
            intArrayOf(4*2  ,4*5+2,4*8+2,4*11), // TILING_BRICK_SMALL
            intArrayOf(4*2+2,4*5  ,4*8+2,4*11), // TILING_BRICK_LARGE
        )

        val subtileOffsetVectors = arrayOf(
            Point2i(0,0),
            Point2i(SUBTILE_SIZE,0),
            Point2i(SUBTILE_SIZE,SUBTILE_SIZE),
            Point2i(0,SUBTILE_SIZE),
        )
    }

    var MAX_TEX_SIZE = 2048; private set
    var TILES_IN_X = MAX_TEX_SIZE / TILE_SIZE; private set
    var SUBTILES_IN_X = MAX_TEX_SIZE / SUBTILE_SIZE; private set

//    var SHADER_SIZE_KEYS = floatArrayOf(MAX_TEX_SIZE.toFloat(), MAX_TEX_SIZE.toFloat(), TILES_IN_X.toFloat(), TILES_IN_X.toFloat()); private set

    private var TOTAL_TILES = TILES_IN_X * TILES_IN_X

    lateinit var atlasPrevernal: Pixmap
    lateinit var atlasVernal: Pixmap
    lateinit var atlasAestival: Pixmap
    lateinit var atlasSerotinal: Pixmap
    lateinit var atlasAutumnal: Pixmap
    lateinit var atlasHibernal: Pixmap
    lateinit var atlasGlow: Pixmap // glowing won't be affected by the season... for now
    lateinit var atlasEmissive: Pixmap // glowing won't be affected by the season... for now
    lateinit var itemTerrainTexture: Texture
    lateinit var itemTerrainTextureGlow: Texture
    lateinit var itemTerrainTextureEmissive: Texture
    lateinit var itemWallTexture: Texture
    lateinit var itemWallTextureGlow: Texture
    lateinit var itemWallTextureEmissive: Texture
    lateinit var terrainTileColourMap: HashMap<ItemID, Cvec>
    lateinit var tags: HashMap<ItemID, RenderTag> // TileID, RenderTag
        private set
    lateinit var tagsByTileNum: HashArray<RenderTag>; private set
    lateinit var itemSheetNumbers: HashMap<ItemID, Int> // TileID, Int
        private set
    private val defaultRenderTag = RenderTag(3, RenderTag.CONNECT_SELF, RenderTag.MASK_NA, 0, 0) // 'update' block
    var initialised = false
        private set

    /** 0.tga, 1.tga.gz, 3242423.tga, 33.tga.gz */
    private val tileNameRegex = Regex("""(0|[1-9][0-9]*)\.tga(\.gz)?""")

    // 16 tiles are reserved for internal use: solid black, solid white, breakage stages.
    // 0th tile is complete transparent tile and is also a BlockID of zero: air.
    private var atlasCursor = 66 // 66 predefined tiles. The normal blocks (e.g. Air) should start from this number
    private val atlasInit = "./assets/graphics/blocks/init.tga"
    private var itemSheetCursor = 16

    internal lateinit var itemTerrainPixmap: Pixmap
    internal lateinit var itemTerrainPixmapGlow: Pixmap
    internal lateinit var itemTerrainPixmapEmissive: Pixmap
    internal lateinit var itemWallPixmap: Pixmap
    internal lateinit var itemWallPixmapGlow: Pixmap
    internal lateinit var itemWallPixmapEmissive: Pixmap

    val atlas: Pixmap
        get() = atlasVernal

    private fun drawInitPixmap() {
        val initPixmap = Pixmap(Gdx.files.internal(atlasInit))

        val tilesInInitPixmap = (initPixmap.width * initPixmap.height) / (TILE_SIZE * TILE_SIZE)
        val tilesPossibleInCurrentPixmap = (atlas.width * atlas.height) / (TILE_SIZE * TILE_SIZE)

        if (tilesInInitPixmap > tilesPossibleInCurrentPixmap) throw Error("Atlas size too small -- can't even fit the init.tga (MAX_TEX_SIZE must be at least ${FastMath.nextPowerOfTwo((sqrt(tilesInInitPixmap.toFloat()) * TILE_SIZE).ceilToInt())})")

        if (MAX_TEX_SIZE >= initPixmap.width) {
            atlasPrevernal.drawPixmap(initPixmap, 0, 0)
            atlasVernal.drawPixmap(initPixmap, 0, 0)
            atlasAestival.drawPixmap(initPixmap, 0, 0)
            atlasSerotinal.drawPixmap(initPixmap, 0, 0)
            atlasAutumnal.drawPixmap(initPixmap, 0, 0)
            atlasHibernal.drawPixmap(initPixmap, 0, 0)
        }
        else {
        /*
        What's happening:

        src:                dest:
        AAAABBBBCCCCDDDD    AAAA
                            BBBB
                            CCCC
                            DDDD
         */
            val destX = 0
            val srcY = 0
            val scanW = MAX_TEX_SIZE
            val scanH = TILE_SIZE
            for (scantile in 0 until (initPixmap.width.toFloat() / MAX_TEX_SIZE).ceilToInt()) {
                val srcX = scantile * scanW
                val destY = scantile * TILE_SIZE

                atlasPrevernal.drawPixmap(initPixmap, srcX, srcY, scanW, scanH, destX, destY, scanW, scanH)
                atlasVernal.drawPixmap(initPixmap, srcX, srcY, scanW, scanH, destX, destY, scanW, scanH)
                atlasAestival.drawPixmap(initPixmap, srcX, srcY, scanW, scanH, destX, destY, scanW, scanH)
                atlasSerotinal.drawPixmap(initPixmap, srcX, srcY, scanW, scanH, destX, destY, scanW, scanH)
                atlasAutumnal.drawPixmap(initPixmap, srcX, srcY, scanW, scanH, destX, destY, scanW, scanH)
                atlasHibernal.drawPixmap(initPixmap, srcX, srcY, scanW, scanH, destX, destY, scanW, scanH)
            }
        }

        initPixmap.dispose()
    }

    /**
     * Must be called AFTER mods' loading so that all the block props are loaded
     */
    operator fun invoke(updateExisting: Boolean = false) { if (updateExisting || !initialised) {

        tags = HashMap<ItemID, RenderTag>()
        tagsByTileNum = HashArray()
        itemSheetNumbers = HashMap<ItemID, Int>()

        atlasPrevernal = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888).also { it.blending = Pixmap.Blending.None }
        atlasVernal = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888).also { it.blending = Pixmap.Blending.None }
        atlasAestival = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888).also { it.blending = Pixmap.Blending.None }
        atlasSerotinal = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888).also { it.blending = Pixmap.Blending.None }
        atlasAutumnal = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888).also { it.blending = Pixmap.Blending.None }
        atlasHibernal = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888).also { it.blending = Pixmap.Blending.None }
        atlasGlow = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888).also { it.blending = Pixmap.Blending.None }
        atlasEmissive = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888).also { it.blending = Pixmap.Blending.None }

        // populate the atlantes with atlasInit
        // this just directly copies the image to the atlantes :p
        drawInitPixmap()


        // get all the files applicable
        // first, get all the '/blocks' directory, and add all the files, regardless of their extension, to the list
//        val tgaList = ArrayList<Pair<String, FileHandle>>() //Pair of <modname, filehandle>
//        val tgaListOres = ArrayList<Pair<String, FileHandle>>()

        val tgaList = HashMap<String, ArrayList<Pair<String, FileHandle>>>() // Key: directory name, value: pair of <modname, filehandle>
        val prefixes = listOf("blocks", "ores", "fluid")
        prefixes.forEach { prefix ->
            tgaList[prefix] = ArrayList()
            ModMgr.getGdxFilesFromEveryMod(prefix).forEach { (modname, dir) ->
                if (!dir.isDirectory) {
                    throw Error("Path '${dir.path()}' is not a directory")
                }

                if (prefix == "blocks") {
                    // filter files that do not exist on the blockcodex
                    dir.list()
                        .filter { tgaFile -> tgaFile.extension() == "tga" && !tgaFile.isDirectory && (BlockCodex.getOrNull("$modname:${tgaFile.nameWithoutExtension()}") != null) }
                        .sortedBy { it.nameWithoutExtension().toInt() }
                        .forEach { tgaFile: FileHandle -> // toInt() to sort by the number, not lexicographically
                            // tgaFile be like: ./assets/mods/basegame/blocks/32.tga (which is not always .tga)
                            val newFile = ModMgr.GameRetextureLoader.altFilePaths.getOrDefault(tgaFile.path(), tgaFile)
                            tgaList[prefix]!!.add(modname to newFile)
                            // printdbg(this, "modname = $modname, file = $newFile")
                        }
                }
                else if (prefix == "ores") {
                    // filter files that do not exist on the orecodex
                    dir.list()
                        .filter { tgaFile -> tgaFile.extension() == "tga" && !tgaFile.isDirectory && (OreCodex.getOrNull("ores@$modname:${tgaFile.nameWithoutExtension()}") != null) }
                        .sortedBy { it.nameWithoutExtension().toInt() }
                        .forEach { tgaFile: FileHandle -> // toInt() to sort by the number, not lexicographically
                            // tgaFile be like: ./assets/mods/basegame/blocks/32.tga (which is not always .tga)
                            val newFile = ModMgr.GameRetextureLoader.altFilePaths.getOrDefault(tgaFile.path(), tgaFile)
                            tgaList[prefix]!!.add(modname to newFile)
                            // printdbg(this, "modname = $modname, file = $newFile")
                        }
                }
                else if (prefix == "fluid") {
                    // filter files that do not exist on the orecodex
                    dir.list()
                        .filter { tgaFile -> tgaFile.extension() == "tga" && !tgaFile.isDirectory && (FluidCodex.getOrNull("fluid@$modname:${tgaFile.nameWithoutExtension()}") != null) }
                        .sortedBy { it.nameWithoutExtension().toInt() }
                        .forEach { tgaFile: FileHandle -> // toInt() to sort by the number, not lexicographically
                            // tgaFile be like: ./assets/mods/basegame/blocks/32.tga (which is not always .tga)
                            val newFile = ModMgr.GameRetextureLoader.altFilePaths.getOrDefault(tgaFile.path(), tgaFile)
                            tgaList[prefix]!!.add(modname to newFile)
                            // printdbg(this, "modname = $modname, file = $newFile")
                        }
                }
            }
        }



        // Sift through the file list for blocks, but TGA format first
        prefixes.forEach { prefix ->
            tgaList[prefix]!!.forEach { (modname, filehandle) ->
                printdbg(this, "processing $prefix $modname:${filehandle.name()}")

                try {
                    val glowFile = Gdx.files.internal(
                        filehandle.path().dropLast(4) + "_glow.tga"
                    ) // assuming strict ".tga" file for now...
                    val emissiveFile = Gdx.files.internal(
                        filehandle.path().dropLast(4) + "_emsv.tga"
                    ) // assuming strict ".tga" file for now...
                    fileToAtlantes(
                        modname, filehandle,
                        if (glowFile.exists()) glowFile else null,
                        if (emissiveFile.exists()) emissiveFile else null,
                        if (prefix == "blocks") null else prefix
                    )
                }
                catch (e: GdxRuntimeException) {
                    System.err.println("Couldn't load file $filehandle from $modname, skipping...")
                }
            }
        }

        // test print
//        PixmapIO2.writeTGA(Gdx.files.absolute("${App.defaultDir}/atlas.tga"), atlas, false)
//        PixmapIO2.writeTGA(Gdx.files.absolute("${AppLoader.defaultDir}/atlasGlow.tga"), atlasGlow, false)

//        PixmapIO2.writeTGA(Gdx.files.absolute("${App.defaultDir}/atlas_0.tga"), atlasPrevernal, false)
//        PixmapIO2.writeTGA(Gdx.files.absolute("${App.defaultDir}/atlas_1.tga"), atlasVernal, false)
//        PixmapIO2.writeTGA(Gdx.files.absolute("${App.defaultDir}/atlas_2.tga"), atlasAestival, false)
//        PixmapIO2.writeTGA(Gdx.files.absolute("${App.defaultDir}/atlas_3.tga"), atlasSerotinal, false)
//        PixmapIO2.writeTGA(Gdx.files.absolute("${App.defaultDir}/atlas_4.tga"), atlasAutumnal, false)
//        PixmapIO2.writeTGA(Gdx.files.absolute("${App.defaultDir}/atlas_5.tga"), atlasHibernal, false)



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
            MASK_47 -> 17
            MASK_PLATFORM -> 7
            else -> 0
        }

//        val itemTerrainPixmap = Pixmap(16 * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)
//        val itemWallPixmap = Pixmap(16 * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)

        itemTerrainPixmap = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)
        itemTerrainPixmapGlow = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)
        itemTerrainPixmapEmissive = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)
        itemWallPixmap = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)
        itemWallPixmapGlow = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)
        itemWallPixmapEmissive = Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888)

        tags.toMap().forEach { id, tag ->
            val t = tileIDtoItemSheetNumber(id)

            if (tag.maskType >= 16) {
                val tilePosFromAtlas = tileOffsetsForItemImageFromSubtile[tag.tilingMode / 2].map {
                    it + tag.tileNumber
                }
                val srcXYs = (tilePosFromAtlas zip subtileOffsetVectors).map { (pos, off) -> off + Point2i(
                    (pos % TILES_IN_X) * TILE_SIZE,
                    (pos / TILES_IN_X) * TILE_SIZE,
                ) }
                val destXYs = subtileOffsetVectors.map { it + Point2i(
                    (t % TILES_IN_X) * TILE_SIZE,
                    (t / TILES_IN_X) * TILE_SIZE,
                ) }

                (srcXYs zip destXYs).forEach { (src, dest) ->
                    itemTerrainPixmap.drawPixmap(atlas, src.x, src.y, SUBTILE_SIZE, SUBTILE_SIZE, dest.x, dest.y, SUBTILE_SIZE, SUBTILE_SIZE)
                    itemTerrainPixmapGlow.drawPixmap(atlasGlow, src.x, src.y, SUBTILE_SIZE, SUBTILE_SIZE, dest.x, dest.y, SUBTILE_SIZE, SUBTILE_SIZE)
                    itemTerrainPixmapEmissive.drawPixmap(atlasEmissive, src.x, src.y, SUBTILE_SIZE, SUBTILE_SIZE, dest.x, dest.y, SUBTILE_SIZE, SUBTILE_SIZE)
                    itemWallPixmap.drawPixmap(atlas, src.x, src.y, SUBTILE_SIZE, SUBTILE_SIZE, dest.x, dest.y, SUBTILE_SIZE, SUBTILE_SIZE)
                    itemWallPixmapGlow.drawPixmap(atlasGlow, src.x, src.y, SUBTILE_SIZE, SUBTILE_SIZE, dest.x, dest.y, SUBTILE_SIZE, SUBTILE_SIZE)
                    itemWallPixmapEmissive.drawPixmap(atlasEmissive, src.x, src.y, SUBTILE_SIZE, SUBTILE_SIZE, dest.x, dest.y, SUBTILE_SIZE, SUBTILE_SIZE)
                }
            }
            else {
                val tilePosFromAtlas = tag.tileNumber + maskTypetoTileIDForItemImage(tag.maskType)
                val src = Point2i((tilePosFromAtlas % TILES_IN_X) * TILE_SIZE, (tilePosFromAtlas / TILES_IN_X) * TILE_SIZE)
                val dest = Point2i((t % TILES_IN_X) * TILE_SIZE, (t / TILES_IN_X) * TILE_SIZE)

                itemTerrainPixmap.drawPixmap(atlas, src.x, src.y, TILE_SIZE, TILE_SIZE, dest.x, dest.y, TILE_SIZE, TILE_SIZE)
                itemTerrainPixmapGlow.drawPixmap(atlasGlow, src.x, src.y, TILE_SIZE, TILE_SIZE, dest.x, dest.y, TILE_SIZE, TILE_SIZE)
                itemTerrainPixmapEmissive.drawPixmap(atlasEmissive, src.x, src.y, TILE_SIZE, TILE_SIZE, dest.x, dest.y, TILE_SIZE, TILE_SIZE)
                itemWallPixmap.drawPixmap(atlas, src.x, src.y, TILE_SIZE, TILE_SIZE, dest.x, dest.y, TILE_SIZE, TILE_SIZE)
                itemWallPixmapGlow.drawPixmap(atlasGlow, src.x, src.y, TILE_SIZE, TILE_SIZE, dest.x, dest.y, TILE_SIZE, TILE_SIZE)
                itemWallPixmapEmissive.drawPixmap(atlasEmissive, src.x, src.y, TILE_SIZE, TILE_SIZE, dest.x, dest.y, TILE_SIZE, TILE_SIZE)
            }
        }
        // darken things for the wall
        for (y in 0 until itemWallPixmap.height) {
            for (x in 0 until itemWallPixmap.width) {
                val c1 = Color(itemWallPixmap.getPixel(x, y)).mulAndAssign(WALL_OVERLAY_COLOUR).toRGBA()
                itemWallPixmap.drawPixel(x, y, c1)
                val c2 = Color(itemWallPixmapGlow.getPixel(x, y)).mulAndAssign(WALL_OVERLAY_COLOUR).toRGBA()
                itemWallPixmapGlow.drawPixel(x, y, c2)
                val c3 = Color(itemWallPixmapEmissive.getPixel(x, y)).mulAndAssign(WALL_OVERLAY_COLOUR).toRGBA()
                itemWallPixmapEmissive.drawPixel(x, y, c3)
            }
        }


        // create terrain colourmap
        terrainTileColourMap = HashMap<ItemID, Cvec>()
        val pxCount = TILE_SIZE * TILE_SIZE
        for (id in itemSheetNumbers) {
            val tilenum = id.value
            val tx = (tilenum % TILES_IN_X) * TILE_SIZE
            val ty = (tilenum / TILES_IN_X) * TILE_SIZE
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

        // special colour
        terrainTileColourMap[Block.NULL] = Cvec(1f, 0f, 0f, 1f)

        itemTerrainTexture = Texture(itemTerrainPixmap)
        itemTerrainTextureGlow = Texture(itemTerrainPixmapGlow)
        itemTerrainTextureEmissive = Texture(itemTerrainPixmapEmissive)
        itemWallTexture = Texture(itemWallPixmap)
        itemWallTextureGlow = Texture(itemWallPixmapGlow)
        itemWallTextureEmissive = Texture(itemWallPixmapEmissive)
//        itemTerrainPixmap.dispose()
//        itemWallPixmap.dispose()

        initialised = true
    } }

    fun getRenderTag(blockID: ItemID): RenderTag {
        return tags.getOrDefault(blockID, defaultRenderTag)
    }

    fun getRenderTag(tilenum: Int): RenderTag {
        return tagsByTileNum.getOrDefault(tilenum.toLong(), defaultRenderTag)
    }


    val nullTile = Pixmap(TILE_SIZE * 16, TILE_SIZE * 16, Pixmap.Format.RGBA8888)

    private fun fileToAtlantes(modname: String, diffuse: FileHandle, glow: FileHandle?, emissive: FileHandle?, prefix: String?) {
        val tilesPixmap = Pixmap(diffuse)
        val tilesGlowPixmap = if (glow != null) Pixmap(glow) else nullTile
        val tilesEmissivePixmap = if (emissive != null) Pixmap(emissive) else nullTile
        val blockName = diffuse.nameWithoutExtension().split('-').last().toInt() // basically a filename
        val blockID = if (prefix != null) "$prefix@$modname:$blockName" else "$modname:$blockName"


        // determine the type of the block (populate tags list)
        // predefined by the image dimension: 16x16 for (1,0)
        if (tilesPixmap.width == TILE_SIZE && tilesPixmap.height == TILE_SIZE) {
            addTag(blockID, RenderTag.CONNECT_SELF, RenderTag.MASK_NA)
            drawToAtlantes(tilesPixmap, tilesGlowPixmap, tilesEmissivePixmap, RenderTag.MASK_NA)
        }
        // predefined by the image dimension: 64x16 for (2,3)
        else if (tilesPixmap.width == TILE_SIZE * 4 && tilesPixmap.height == TILE_SIZE) {
            addTag(blockID, RenderTag.CONNECT_WALL_STICKER, RenderTag.MASK_TORCH)
            drawToAtlantes(tilesPixmap, tilesGlowPixmap, tilesEmissivePixmap, RenderTag.MASK_TORCH)
        }
        // predefined by the image dimension: 128x16 for (3,4)
        else if (tilesPixmap.width == TILE_SIZE * 8 && tilesPixmap.height == TILE_SIZE) {
            addTag(blockID, RenderTag.CONNECT_WALL_STICKER_CONNECT_SELF, RenderTag.MASK_PLATFORM)
            drawToAtlantes(tilesPixmap, tilesGlowPixmap, tilesEmissivePixmap, RenderTag.MASK_PLATFORM)
        }
        // predefined by the image dimension: 256x16
        else if (tilesPixmap.width == TILE_SIZE * 16 && tilesPixmap.height == TILE_SIZE) {
            addTag(blockID, RenderTag.CONNECT_SELF, RenderTag.MASK_16)
            drawToAtlantes(tilesPixmap, tilesGlowPixmap, tilesEmissivePixmap, RenderTag.MASK_16)
        }
        // predefined by the image dimension: 256x64
        else if (tilesPixmap.width == TILE_SIZE * 16 && tilesPixmap.height == TILE_SIZE * 4) {
            addTag(blockID, RenderTag.CONNECT_SELF, RenderTag.MASK_16X4)
            drawToAtlantes(tilesPixmap, tilesGlowPixmap, tilesEmissivePixmap, RenderTag.MASK_16X4)
        }
        // predefined by the image dimension: 256x128
        else if (tilesPixmap.width == TILE_SIZE * 16 && tilesPixmap.height == TILE_SIZE * 8) {
            addTag(blockID, RenderTag.CONNECT_SELF, RenderTag.MASK_16X8)
            drawToAtlantes(tilesPixmap, tilesGlowPixmap, tilesEmissivePixmap, RenderTag.MASK_16X8)
        }
        // predefined by the image dimension: 256x256
        else if (tilesPixmap.width == TILE_SIZE * 16 && tilesPixmap.height == TILE_SIZE * 16) {
            addTag(blockID, RenderTag.CONNECT_SELF, RenderTag.MASK_16X16)
            drawToAtlantes(tilesPixmap, tilesGlowPixmap, tilesEmissivePixmap, RenderTag.MASK_16X16)
        }
        // predefined by the image dimension: 288x16
        else if (tilesPixmap.width == TILE_SIZE * 18 && tilesPixmap.height == TILE_SIZE * 3) {
            addTag(blockID, RenderTag.CONNECT_SELF, RenderTag.MASK_FLUID)
            drawToAtlantes(tilesPixmap, tilesGlowPixmap, tilesEmissivePixmap, RenderTag.MASK_FLUID)
        }
        // subtitles
        else if (tilesPixmap.width == W_SUBTILE_GENERIC && tilesPixmap.height == H_SUBTILE ||
            tilesPixmap.width == W_SUBTILE_GRASS && tilesPixmap.height == H_SUBTILE ||
            tilesPixmap.width == 3*W_SUBTILE_GENERIC && tilesPixmap.height == 2*H_SUBTILE-SUBTILE_SIZE ||
            tilesPixmap.width == 3*W_SUBTILE_GRASS && tilesPixmap.height == 2*H_SUBTILE-SUBTILE_SIZE) {

            // figure out the tags
            // tags are arranged horizontally, left-to-right, starting from (0,0)
            // Line 0: (reserved for manual subtile allocation)
            // Line 1: Tiling Mode
            //     0000 (0): Full Tiling
            //     1000 (1): Full Tiling with No Fliprot
            //     1100 (3): Brick Tiling Small (4 rows per full tile; indices: 0-7, 8-15, 8-15, 0-7) implies no fliprot
            //     1010 (5): Brick Tiling Large (2 rows per full tile; indices: 0-7, 8-15, 0-7, 8-15) implies no fliprot
            // Line 2: Connection Type
            //     0000 (0): INVALID
            //     1000 (1): connect-mutual
            //     0100 (2): connect-self
            // Line 3: Request for Post-Processing (currently does nothing)
            //     0000 (0): do nothing
            // NOTE: For this system, the "NORANDTILE" tag is ignored
            val maskType = if (tilesPixmap.width >= 3*W_SUBTILE_GENERIC) MASK_SUBTILE_GRASS else MASK_SUBTILE_GENERIC
            var connectionType0 = 0
            var tilingMode = 0
            var postProcessing = 0
            for (x in 0 until 4) {
//                val pixelY0 = (tilesPixmap.getPixel(x, 0).and(255) >= 128).toInt(x)
                val pixelY1 = (tilesPixmap.getPixel(x, 1).and(255) >= 128).toInt(x)
                val pixelY2 = (tilesPixmap.getPixel(x, 2).and(255) >= 128).toInt(x)
                val pixelY3 = (tilesPixmap.getPixel(x, 3).and(255) >= 128).toInt(x)

                tilingMode += pixelY1
                connectionType0 += pixelY2
                postProcessing += pixelY3
            }
            val connectionType = when (connectionType0) {
                1 -> CONNECT_MUTUAL
                2 -> CONNECT_SELF
                else -> throw IllegalArgumentException("$connectionType0")
            }
            addTag(blockID, connectionType, maskType, tilingMode, postProcessing)
//            println("drawToAtlantes tile: $blockID with mode $tilingMode")
            drawToAtlantes(tilesPixmap, tilesGlowPixmap, tilesEmissivePixmap, maskType)
        }
        // 112x112 or 336x224
        else {
            if (tilesPixmap.width != tilesPixmap.height && tilesPixmap.width % (7 * TILE_SIZE) >= 2) {
                throw IllegalArgumentException("Unrecognized image dimension ${tilesPixmap.width}x${tilesPixmap.height} from ${diffuse.path()}")
            }
            // figure out the tags
            // tags are arranged horizontally, right-to-left, starting from (111, 80)
            // Line 0: Connection Type
            //     not marked: connect-mutual
            //     marked: connect-self
            // Line 1: Mask Type
            //     0000 (0): INVALID
            //     0001 (1): 16-tiles tiling [Deprecated]
            //     0010 (2): 47-tiles tiling
            // NOTE: For this system ONLY, the FlipRot can be disabled by adding "NORANDTILE" tag on the blocks.csv
            var connectionType = 0
            var maskType = 0
            for (bit in 0 until TILE_SIZE) {
                val x = (7 * TILE_SIZE - 1) - bit
                val y1 = 5 * TILE_SIZE; val y2 = y1 + 1
                val pixel1 = (tilesPixmap.getPixel(x, y1).and(255) >= 128).toInt(bit)
                val pixel2 = (tilesPixmap.getPixel(x, y2).and(255) >= 128).toInt(bit)

                connectionType += pixel1
                maskType += pixel2
            }

            addTag(blockID, connectionType, maskType)
            drawToAtlantes(tilesPixmap, tilesGlowPixmap, tilesEmissivePixmap, maskType)
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
    private fun addTag(id: ItemID, connectionType: Int, maskType: Int, tilingMode: Int = TILING_FULL, postProcessing: Int = 0) {
        if (tags.containsKey(id)) {
            throw Error("Block $id already exists")
        }

        tags[id] = RenderTag(atlasCursor, connectionType, maskType, tilingMode, postProcessing)
        tagsByTileNum[atlasCursor.toLong()] = RenderTag(atlasCursor, connectionType, maskType, tilingMode, postProcessing)

        printdbg(this, "tileName ${id} ->> tileNumber ${atlasCursor}")
    }

    private fun drawToAtlantes(diffuse: Pixmap, glow: Pixmap, emissive: Pixmap, renderMask: Int) {
        val tilesCount = RenderTag.maskTypeToTileCount(renderMask)
        if (atlasCursor + tilesCount >= TOTAL_TILES) {
//            throw Error("Too much tiles for $MAX_TEX_SIZE texture size: $atlasCursor")
            println("[CreateTileAtlas] Too much tiles for atlas of ${MAX_TEX_SIZE}x$MAX_TEX_SIZE (tiles so far: $atlasCursor/${(MAX_TEX_SIZE*MAX_TEX_SIZE)/(TILE_SIZE* TILE_SIZE)}, tiles to be added: $tilesCount), trying to expand the atlas...")
            expandAtlantes()
        }

        val wSubtileSheet = if (renderMask >= MASK_SUBTILE_GRASS) W_SUBTILE_GRASS else W_SUBTILE_GENERIC
        val hSubtileSheet = H_SUBTILE

        val sixSeasonal = (renderMask >= MASK_SUBTILE_GENERIC && diffuse.width == 3 * wSubtileSheet && diffuse.height == 2 * hSubtileSheet - SUBTILE_SIZE) ||
                (renderMask < MASK_SUBTILE_GENERIC && diffuse.width == 336 && diffuse.height == 224)
        val txOfPixmap = diffuse.width / TILE_SIZE
        val txOfPixmapGlow = glow.width / TILE_SIZE
        val txOfPixmapEmissive = emissive.width / TILE_SIZE

        if (renderMask >= MASK_SUBTILE_GENERIC) {

            for (i in 0 until tilesCount) {
                val srcX = SUBTILE_SIZE * (i / 4)
                val srcY = SUBTILE_SIZE * 4 * (i % 4) + SUBTILE_SIZE

                // snippet straight from the _drawToAtlantesFourSubtiles
//                val atlasX = (atlasCursor % TILES_IN_X) * TILE_SIZE
//                val atlasY = (atlasCursor / TILES_IN_X) * TILE_SIZE
//                println("Drawing tile to the atlas: atlasCursor=${atlasCursor}, atlasXY=($atlasX,$atlasY)")

                if (sixSeasonal) {
                    _drawToAtlantesFourSubtiles(diffuse, atlasCursor, srcX + 0*wSubtileSheet, srcY, PREVERNAL)
                    _drawToAtlantesFourSubtiles(diffuse, atlasCursor, srcX + 1*wSubtileSheet, srcY, VERNAL)
                    _drawToAtlantesFourSubtiles(diffuse, atlasCursor, srcX + 2*wSubtileSheet, srcY, AESTIVAL)

                    _drawToAtlantesFourSubtiles(diffuse, atlasCursor, srcX + 2*wSubtileSheet, srcY + hSubtileSheet - SUBTILE_SIZE, SEROTINAL)
                    _drawToAtlantesFourSubtiles(diffuse, atlasCursor, srcX + 1*wSubtileSheet, srcY + hSubtileSheet - SUBTILE_SIZE, AUTUMNAL)
                    _drawToAtlantesFourSubtiles(diffuse, atlasCursor, srcX + 0*wSubtileSheet, srcY + hSubtileSheet - SUBTILE_SIZE, HIBERNAL)

                    _drawToAtlantesFourSubtiles(glow, atlasCursor, srcX, srcY, GLOW)
                    _drawToAtlantesFourSubtiles(emissive, atlasCursor, srcX, srcY, EMISSIVE)
                }
                else {
                    _drawToAtlantesFourSubtiles(diffuse, atlasCursor, srcX, srcY, SIX_SEASONS)
                    _drawToAtlantesFourSubtiles(glow, atlasCursor, srcX, srcY, GLOW)
                    _drawToAtlantesFourSubtiles(emissive, atlasCursor, srcX, srcY, EMISSIVE)
                }

                atlasCursor += 1
            }
        }
        else {
            for (i in 0 until tilesCount) {
                //printdbg(this, "Rendering to atlas, tile# $atlasCursor, tilesCount = $tilesCount, seasonal = $seasonal")
                val i = if (renderMask == MASK_47) (if (i < 41) i else i + 1) else i // to compensate the discontinuity between 40th and 41st tile

                // different texture for different seasons (336x224)
                if (sixSeasonal) {
                    _drawToAtlantes(diffuse, atlasCursor, i % 7, i / 7, PREVERNAL)
                    _drawToAtlantes(diffuse, atlasCursor, i % 7 + 7, i / 7, VERNAL)
                    _drawToAtlantes(diffuse, atlasCursor, i % 7 + 14, i / 7, AESTIVAL)

                    _drawToAtlantes(diffuse, atlasCursor, i % 7 + 14, i / 7 + 7, SEROTINAL)
                    _drawToAtlantes(diffuse, atlasCursor, i % 7 + 7, i / 7 + 7, AUTUMNAL)
                    _drawToAtlantes(diffuse, atlasCursor, i % 7, i / 7 + 7, HIBERNAL)

                    _drawToAtlantes(glow, atlasCursor, i % 7, i / 7, GLOW)
                    _drawToAtlantes(emissive, atlasCursor, i % 7, i / 7, EMISSIVE)
                }
                else {
                    _drawToAtlantes(diffuse, atlasCursor, i % txOfPixmap, i / txOfPixmap, SIX_SEASONS)
                    _drawToAtlantes(glow, atlasCursor, i % txOfPixmapGlow, i / txOfPixmapGlow, GLOW)
                    _drawToAtlantes(emissive, atlasCursor, i % txOfPixmapEmissive, i / txOfPixmapEmissive, EMISSIVE)
                }

                atlasCursor += 1
            }
        }
    }

    /**
     * This function will draw 8x16 (two subtiles) onto the atlas
     */
    private fun _drawToAtlantesFourSubtiles(pixmap: Pixmap, destTileNum: Int, sourceX: Int, sourceY: Int, source: AtlasSource) {
        if (source == SIX_SEASONS) {
            _drawToAtlantesFourSubtiles(pixmap, destTileNum, sourceX, sourceY, PREVERNAL)
            _drawToAtlantesFourSubtiles(pixmap, destTileNum, sourceX, sourceY, VERNAL)
            _drawToAtlantesFourSubtiles(pixmap, destTileNum, sourceX, sourceY, AESTIVAL)
            _drawToAtlantesFourSubtiles(pixmap, destTileNum, sourceX, sourceY, SEROTINAL)
            _drawToAtlantesFourSubtiles(pixmap, destTileNum, sourceX, sourceY, AUTUMNAL)
            _drawToAtlantesFourSubtiles(pixmap, destTileNum, sourceX, sourceY, HIBERNAL)
        }
        else {
            // destTileNum increments by one, which means FOUR SUBTILES

            val atlasX = (destTileNum % TILES_IN_X) * TILE_SIZE
            val atlasY = (destTileNum / TILES_IN_X) * TILE_SIZE

            val target = when (source) {
                PREVERNAL -> atlasPrevernal
                VERNAL -> atlasVernal
                AESTIVAL -> atlasAestival
                SEROTINAL -> atlasSerotinal
                AUTUMNAL -> atlasAutumnal
                HIBERNAL -> atlasHibernal
                GLOW -> atlasGlow
                EMISSIVE -> atlasEmissive
                else -> throw IllegalArgumentException("Unknown draw source $source")
            }

            target.drawPixmap(pixmap,
                sourceX, sourceY,
                SUBTILE_SIZE, TILE_SIZE,
                atlasX, atlasY,
                SUBTILE_SIZE, TILE_SIZE
            )
            target.drawPixmap(pixmap,
                sourceX, sourceY + TILE_SIZE,
                SUBTILE_SIZE, TILE_SIZE,
                atlasX + SUBTILE_SIZE, atlasY,
                SUBTILE_SIZE, TILE_SIZE
            )
        }
    }

    /**
     * mode: 0 for all the atlantes, 1-4 for summer/autumn/winter/spring atlas
     */
    private fun _drawToAtlantes(pixmap: Pixmap, destTileNum: Int, srcTileX: Int, srcTileY: Int, source: AtlasSource) {
        if (source == SIX_SEASONS) {
            _drawToAtlantes(pixmap, destTileNum, srcTileX, srcTileY, PREVERNAL)
            _drawToAtlantes(pixmap, destTileNum, srcTileX, srcTileY, VERNAL)
            _drawToAtlantes(pixmap, destTileNum, srcTileX, srcTileY, AESTIVAL)
            _drawToAtlantes(pixmap, destTileNum, srcTileX, srcTileY, SEROTINAL)
            _drawToAtlantes(pixmap, destTileNum, srcTileX, srcTileY, AUTUMNAL)
            _drawToAtlantes(pixmap, destTileNum, srcTileX, srcTileY, HIBERNAL)
        }
        else {
            val atlasX = (destTileNum % TILES_IN_X) * TILE_SIZE
            val atlasY = (destTileNum / TILES_IN_X) * TILE_SIZE
            val sourceX = srcTileX * TILE_SIZE
            val sourceY = srcTileY * TILE_SIZE

            //if (mode == 1) printdbg(this, "atlaspos: ($atlasX, $atlasY), srcpos: ($sourceX, $sourceY), srcpixmap = $pixmap")

            val target = when (source) {
                PREVERNAL -> atlasPrevernal
                VERNAL -> atlasVernal
                AESTIVAL -> atlasAestival
                SEROTINAL -> atlasSerotinal
                AUTUMNAL -> atlasAutumnal
                HIBERNAL -> atlasHibernal
                GLOW -> atlasGlow
                EMISSIVE -> atlasEmissive
                else -> throw IllegalArgumentException("Unknown draw source $source")
            }

            target.drawPixmap(pixmap, sourceX, sourceY, TILE_SIZE, TILE_SIZE, atlasX, atlasY, TILE_SIZE, TILE_SIZE)
        }
    }

    /**
     * @param tileNumber ordinal number of a tile in the texture atlas
     */
    data class RenderTag(val tileNumber: Int, val connectionType: Int, val maskType: Int, val tilingMode: Int, val postProcessing: Int) {
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
            const val MASK_16X4 = 5
            const val MASK_16X8 = 6
            const val MASK_16X16 = 7
            const val MASK_FLUID = 8
            const val MASK_SUBTILE_GENERIC = 16
            const val MASK_SUBTILE_GRASS = 32

            const val TILING_FULL = 0
            const val TILING_FULL_NOFLIP = 1
            const val TILING_BRICK_SMALL = 2
            const val TILING_BRICK_SMALL_NOFLIP = 3
            const val TILING_BRICK_LARGE = 4
            const val TILING_BRICK_LARGE_NOFLIP = 5

            const val POSTPROCESS_NONE = 0
            const val POSTPROCESS_DEBLOCKING = 1

            fun maskTypeToTileCount(maskType: Int) = when (maskType) {
                MASK_NA -> 1
                MASK_16 -> 16
                MASK_47 -> 47
                MASK_TORCH -> 4
                MASK_PLATFORM -> 8
                MASK_16X4 -> 64
                MASK_16X8 -> 128
                MASK_16X16 -> 256
                MASK_FLUID -> 18*3
                in 16..31 -> 52
                in 32..47 -> 84
                else -> throw IllegalArgumentException("Unknown maskType: $maskType")
            }
        }
    }

    fun dispose() {
        atlasPrevernal.dispose()
        atlasVernal.dispose()
        atlasAestival.dispose()
        atlasSerotinal.dispose()
        atlasAutumnal.dispose()
        atlasHibernal.dispose()
        atlasGlow.dispose()
        atlasEmissive.dispose()
        //itemTerrainTexture.dispose() //BlocksDrawer will dispose of it as it disposes of 'tileItemTerrain (TextureRegionPack)'
        //itemTerrainTextureGlow.dispose() //BlocksDrawer will dispose of it as it disposes of 'tileItemTerrain (TextureRegionPack)'
        //itemWallTexture.dispose() //BlocksDrawer will dispose of it as it disposes of 'tileItemWall (TextureRegionPack)'
        //itemWallTextureGlow.dispose() //BlocksDrawer will dispose of it as it disposes of 'tileItemWall (TextureRegionPack)'
        itemTerrainPixmap.dispose()
        itemWallPixmap.dispose()

        nullTile.dispose()
    }

    private enum class AtlasSource {
        /*FOUR_SEASONS, SUMMER, AUTUMN, WINTER, SPRING,*/ GLOW, EMISSIVE,
        SIX_SEASONS, PREVERNAL, VERNAL, AESTIVAL, SEROTINAL, AUTUMNAL, HIBERNAL,
    }

    private fun expandAtlantes() {
        if (MAX_TEX_SIZE >= App.glInfo.GL_MAX_TEXTURE_SIZE) {
            throw RuntimeException("Cannot expand atlas: texture size is already at its maximum possible size allowed by the graphics processor (${MAX_TEX_SIZE}x${MAX_TEX_SIZE})")
        }

        val oldTexSize = MAX_TEX_SIZE
        val newTexSize = oldTexSize * 2


        MAX_TEX_SIZE = newTexSize
        TILES_IN_X = MAX_TEX_SIZE / TILE_SIZE
        SUBTILES_IN_X = MAX_TEX_SIZE / SUBTILE_SIZE
//        SHADER_SIZE_KEYS = floatArrayOf(MAX_TEX_SIZE.toFloat(), MAX_TEX_SIZE.toFloat(), TILES_IN_X.toFloat(), TILES_IN_X.toFloat())
        TOTAL_TILES = TILES_IN_X * TILES_IN_X


        val newAtlantes = Array(8) {
            Pixmap(TILES_IN_X * TILE_SIZE, TILES_IN_X * TILE_SIZE, Pixmap.Format.RGBA8888).also {
                it.blending = Pixmap.Blending.None
                it.filter = Pixmap.Filter.NearestNeighbour
            }
        }
        listOf(atlasPrevernal, atlasVernal, atlasAestival, atlasSerotinal, atlasAutumnal, atlasHibernal, atlasGlow, atlasEmissive).forEachIndexed { index, pixmap ->
        /*
        How it works:

        old:        new:
        AAAAAAAA    AAAAAAAABBBBBBBB
        BBBBBBBB    CCCCCCCCDDDDDDDD
        CCCCCCCC    ...
        DDDDDDDD
        ...

         */
            for (scantile in 0 until pixmap.height / TILE_SIZE) {
                val srcX = 0
                val srcY = scantile * TILE_SIZE
                val destX = (scantile % 2) * oldTexSize
                val destY = (scantile / 2) * TILE_SIZE
                val scanW = pixmap.width
                val scanH = TILE_SIZE

                newAtlantes[index].drawPixmap(pixmap, srcX, srcY, scanW, scanH, destX, destY, scanW, scanH)
            }
            pixmap.dispose()
        }

        atlasPrevernal = newAtlantes[0]
        atlasVernal = newAtlantes[1]
        atlasAestival = newAtlantes[2]
        atlasSerotinal = newAtlantes[3]
        atlasAutumnal = newAtlantes[4]
        atlasHibernal = newAtlantes[5]
        atlasGlow = newAtlantes[6]
        atlasEmissive = newAtlantes[7]


        App.setConfig("atlastexsize", newTexSize)
    }
}