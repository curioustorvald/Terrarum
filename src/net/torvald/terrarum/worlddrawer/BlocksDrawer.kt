package net.torvald.terrarum.worlddrawer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.glutils.Float16FrameBuffer
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.math.Matrix4
import com.jme3.math.FastMath
import net.torvald.gdx.graphics.Cvec
import net.torvald.random.XXHash64
import net.torvald.terrarum.*
import net.torvald.terrarum.App.measureDebugTime
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZEF
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameworld.FLUID_MIN_MASS
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.modulebasegame.worldgenerator.shake
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.serialise.toBig64
import net.torvald.terrarum.worlddrawer.CreateTileAtlas.Companion.WALL_OVERLAY_COLOUR
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.unsafe.UnsafeLong2D
import kotlin.math.roundToInt


/**
 * Sub-portion of IngameRenderer. You are not supposed to directly deal with this.
 *
 * The terrain texture atlas is HARD CODED as "4096x4096, on which 256x256 tiles are contained"
 * in the shader (tiling.frag). This will not be a problem in the base game, but if you are modifying
 * this engine for your project, you must edit the shader program accordingly.
 *
 * To render and draw images, modify the ```selectedWireBitToDraw``` (bitset) property from the IngameRenderer.
 *
 * Created by minjaesong on 2016-01-19.
 */
internal object BlocksDrawer {

    /** World change is managed by IngameRenderer.setWorld() */
    internal var world: GameWorld = GameWorld.makeNullWorld()


    /**
     * Widths of the tile atlantes must have exactly the same width (height doesn't matter)
     * If not, the engine will choose wrong tile for a number you provided.
     */

    /** Index zero: spring */
    val seasonalTerrains: Array<TextureRegionPack>
    lateinit var tilesTerrain: TextureRegionPack; private set
    lateinit var tilesTerrainNext: TextureRegionPack; private set
    private var tilesTerrainBlendDegree = 0f
    //val tilesWire: TextureRegionPack
    val tileItemTerrain: TextureRegionPack
    val tileItemTerrainGlow: TextureRegionPack
    val tileItemTerrainEmissive: TextureRegionPack
    val tileItemWall: TextureRegionPack
    val tileItemWallGlow: TextureRegionPack
    val tileItemWallEmissive: TextureRegionPack
    val tilesGlow: TextureRegionPack
    val tilesEmissive: TextureRegionPack
    val nullTex = Texture(1, 1, Pixmap.Format.RGBA8888);

    //val tileItemWall = Image(TILE_SIZE * 16, TILE_SIZE * GameWorld.TILES_SUPPORTED / 16) // 4 MB

    const val BREAKAGE_STEPS = 10

    val WALL = GameWorld.WALL
    val TERRAIN = GameWorld.TERRAIN
    val ORES = GameWorld.ORES
    val FLUID = -2
    val OCCLUSION = 31337
    val BLURMAP_BASE = 31338
    val BLURMAP_TERR = BLURMAP_BASE + TERRAIN
    val BLURMAP_WALL = BLURMAP_BASE + WALL

    private const val OCCLUSION_TILE_NUM_BASE = 16

    private const val NEARBY_TILE_KEY_UP = 0
    private const val NEARBY_TILE_KEY_RIGHT = 1
    private const val NEARBY_TILE_KEY_DOWN = 2
    private const val NEARBY_TILE_KEY_LEFT = 3

    private lateinit var terrainTilesBuffer: UnsafeLong2D // stores subtiles (dimension is doubled)
    private lateinit var wallTilesBuffer: UnsafeLong2D // stores subtiles (dimension is doubled)
    private lateinit var oreTilesBuffer: UnsafeLong2D // stores subtiles (dimension is doubled)
    private lateinit var fluidTilesBuffer: UnsafeLong2D // stores subtiles (dimension is doubled)
    private lateinit var occlusionBuffer: UnsafeLong2D // stores subtiles (dimension is doubled)
    private lateinit var blurMapTerr: UnsafeLong2D // stores subtiles (dimension is doubled)
    private lateinit var blurMapWall: UnsafeLong2D // stores subtiles (dimension is doubled)
    private lateinit var tempRenderTypeBuffer: UnsafeLong2D // this one is NOT dimension doubled; 0x tttt 00 ii where t=rawTileNum, i=nearbyTilesInfo
    private var terrainDrawBuffer1: Pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
    private var terrainDrawBuffer2: Pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
    private var wallDrawBuffer1: Pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
    private var wallDrawBuffer2: Pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
    private var oresDrawBuffer: Pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
    private var fluidDrawBuffer: Pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
    private var occlusionDrawBuffer: Pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
    private var blurTilesBuffer: Pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
    private var nullBuffer: Pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)

    private lateinit var tilesQuad: Mesh
    private val shaderTiling = App.loadShaderFromClasspath("shaders/default.vert", "shaders/tiling.frag")
    private val shaderDeblock = App.loadShaderFromClasspath("shaders/default.vert", "shaders/deblocking.frag")

    private lateinit var deblockingFBO: Float16FrameBuffer
    private lateinit var blurmapFBO: Float16FrameBuffer

    lateinit var batch: FlippingSpriteBatch
    private lateinit var camera: OrthographicCamera

    init {

        // PNG still doesn't work right.
        // The thing is, pixel with alpha 0 must have RGB of also 0, which PNG does not guarantee it.
        // (pixels of RGB = 255, A = 0 -- white transparent -- causes 'glow')
        // with TGA, you have a complete control over this, with the expense of added hassle on your side.
        // -- Torvald, 2018-12-19

        // CreateTileAtlas.invoke() has been moved to the AppLoader.create() //

        // create terrain texture from pixmaps
        seasonalTerrains = arrayOf(
            TextureRegionPack(Texture(App.tileMaker.atlasPrevernal), TILE_SIZE, TILE_SIZE),
            TextureRegionPack(Texture(App.tileMaker.atlasVernal), TILE_SIZE, TILE_SIZE),
            TextureRegionPack(Texture(App.tileMaker.atlasAestival), TILE_SIZE, TILE_SIZE),
            TextureRegionPack(Texture(App.tileMaker.atlasSerotinal), TILE_SIZE, TILE_SIZE),
            TextureRegionPack(Texture(App.tileMaker.atlasAutumnal), TILE_SIZE, TILE_SIZE),
            TextureRegionPack(Texture(App.tileMaker.atlasHibernal), TILE_SIZE, TILE_SIZE),
        )

        tilesGlow = TextureRegionPack(Texture(App.tileMaker.atlasGlow), TILE_SIZE, TILE_SIZE)
        tilesEmissive = TextureRegionPack(Texture(App.tileMaker.atlasEmissive), TILE_SIZE, TILE_SIZE)


        printdbg(this, "Making terrain and wall item textures...")



        // test print

        tileItemTerrain = TextureRegionPack(App.tileMaker.itemTerrainTexture, TILE_SIZE, TILE_SIZE)
        tileItemTerrainGlow = TextureRegionPack(App.tileMaker.itemTerrainTextureGlow, TILE_SIZE, TILE_SIZE)
        tileItemTerrainEmissive = TextureRegionPack(App.tileMaker.itemTerrainTextureEmissive, TILE_SIZE, TILE_SIZE)
        tileItemWall = TextureRegionPack(App.tileMaker.itemWallTexture, TILE_SIZE, TILE_SIZE)
        tileItemWallGlow = TextureRegionPack(App.tileMaker.itemWallTextureGlow, TILE_SIZE, TILE_SIZE)
        tileItemWallEmissive = TextureRegionPack(App.tileMaker.itemWallTextureEmissive, TILE_SIZE, TILE_SIZE)


//        val texdata = tileItemTerrain.texture.textureData
//        val textureBackedByPixmap = texdata.isPrepared
//        if (!textureBackedByPixmap) texdata.prepare()
//        val imageSheet = texdata.consumePixmap()
//        PixmapIO2.writeTGA(Gdx.files.absolute("${App.defaultDir}/terrainitem.tga"), imageSheet, false)
//        if (!textureBackedByPixmap) imageSheet.dispose()



        // finally
        tilesTerrain = seasonalTerrains[1]


        batch = FlippingSpriteBatch()
        camera = OrthographicCamera(App.scr.width.toFloat(), App.scr.height.toFloat())


        printdbg(this, "init() exit")
    }

    /**
     * See work_files/dynamic_shape_2_0.psd
     *
     * bits position: (0 being LSB)
     *
     * 5 6 7
     * 4 @ 0
     * 3 2 1
     *
     * size of this LUT must be equal to 256.
     */
    val connectLut47 = intArrayOf(17,1,17,1,2,3,2,14,17,1,17,1,2,3,2,14,9,7,9,7,4,5,4,35,9,7,9,7,16,37,16,15,17,1,17,1,2,3,2,14,17,1,17,1,2,3,2,14,9,7,9,7,4,5,4,35,9,7,9,7,16,37,16,15,8,10,8,10,0,12,0,43,8,10,8,10,0,12,0,43,11,13,11,13,6,20,6,34,11,13,11,13,36,33,36,46,8,10,8,10,0,12,0,43,8,10,8,10,0,12,0,43,30,42,30,42,38,26,38,18,30,42,30,42,23,45,23,31,17,1,17,1,2,3,2,14,17,1,17,1,2,3,2,14,9,7,9,7,4,5,4,35,9,7,9,7,16,37,16,15,17,1,17,1,2,3,2,14,17,1,17,1,2,3,2,14,9,7,9,7,4,5,4,35,9,7,9,7,16,37,16,15,8,28,8,28,0,41,0,21,8,28,8,28,0,41,0,21,11,44,11,44,6,27,6,40,11,44,11,44,36,19,36,32,8,28,8,28,0,41,0,21,8,28,8,28,0,41,0,21,30,29,30,29,38,39,38,25,30,29,30,29,23,24,23,22)
    val connectLut16 = intArrayOf(0,2,0,2,4,6,4,6,0,2,0,2,4,6,4,6,8,10,8,10,12,14,12,14,8,10,8,10,12,14,12,14,0,2,0,2,4,6,4,6,0,2,0,2,4,6,4,6,8,10,8,10,12,14,12,14,8,10,8,10,12,14,12,14,1,3,1,3,5,7,5,7,1,3,1,3,5,7,5,7,9,11,9,11,13,15,13,15,9,11,9,11,13,15,13,15,1,3,1,3,5,7,5,7,1,3,1,3,5,7,5,7,9,11,9,11,13,15,13,15,9,11,9,11,13,15,13,15,0,2,0,2,4,6,4,6,0,2,0,2,4,6,4,6,8,10,8,10,12,14,12,14,8,10,8,10,12,14,12,14,0,2,0,2,4,6,4,6,0,2,0,2,4,6,4,6,8,10,8,10,12,14,12,14,8,10,8,10,12,14,12,14,1,3,1,3,5,7,5,7,1,3,1,3,5,7,5,7,9,11,9,11,13,15,13,15,9,11,9,11,13,15,13,15,1,3,1,3,5,7,5,7,1,3,1,3,5,7,5,7,9,11,9,11,13,15,13,15,9,11,9,11,13,15,13,15)

    // order: TL, TR, BR, BL
    val subtileVarBaseLuts = arrayOf(
        intArrayOf(10,2,2,2,1,1,3,1,10,1,10,3,10,3,2,1,1,2,0,3,3,10,0,0,0,0,0,3,10,0,0,0,3,3,3,1,3,1,0,0,3,10,0,10,3,0,3),
        intArrayOf(4,1,5,1,5,1,4,1,4,5,6,4,6,6,1,1,5,5,6,0,6,0,0,4,0,0,6,0,0,0,4,6,0,6,6,1,4,1,4,0,0,0,6,6,0,6,6),
        intArrayOf(4,7,4,9,4,9,4,7,8,8,7,8,9,7,0,0,4,8,0,9,9,0,0,4,9,0,9,9,7,7,8,0,0,9,0,0,4,9,4,9,0,9,7,0,7,9,0),
        intArrayOf(10,11,10,10,12,12,12,7,11,7,11,7,10,7,10,0,0,11,12,0,12,10,0,0,0,12,12,12,11,7,7,0,0,0,12,12,0,0,12,12,12,10,7,10,7,0,0),
    )
    // order: TL, TR, BR, BL
    private val subtileReorientLUT = arrayOf(
        intArrayOf(0 ,1 ,2 ,3 ,4 ,5 ,6 ,7 ,8 ,9 ,10,11,12,13,14,15,16,17,18,19,20), /* normal */
        intArrayOf(0 ,1 ,5 ,6 ,10,2 ,3 ,7 ,11,12,4 ,8 ,9 ,14,13,20,19,18,17,16,15), /* horz flip */
        intArrayOf(0 ,10,11,12,1 ,2 ,3 ,4 ,5 ,6 ,7 ,8 ,9 ,19,20,13,14,15,16,17,18), /* CW 90 */
        intArrayOf(0 ,10,2 ,3 ,7 ,11,12,4 ,8 ,9 ,1 ,5 ,6 ,20,19,18,17,16,15,14,13), /* hfCW 90 */
        intArrayOf(0 ,7 ,8 ,9 ,10,11,12,1 ,2 ,3 ,4 ,5 ,6 ,17,18,19,20,13,14,15,16), /* CW 180 */
        intArrayOf(0 ,7 ,11,12,4 ,8 ,9 ,1 ,5 ,6 ,10,2 ,3 ,18,17,16,15,14,13,20,19), /* hfCW 180 */
        intArrayOf(0 ,4 ,5 ,6 ,7 ,8 ,9 ,10,11,12,1 ,2 ,3 ,15,16,17,18,19,20,13,14), /* CW 270 */
        intArrayOf(0 ,4 ,8 ,9 ,1 ,5 ,6 ,10,2 ,3 ,7 ,11,12,16,15,14,13,20,19,18,17), /* hfCW 270 */
    )
    // order: TL, TR, BR, BL
    private val variantOpsLUT = arrayOf(
        // newIndex = (oldIndex % A) + B for (a to b)
        arrayOf(16 to 0,16 to 0,16 to 0,16 to 0), // TILING_FULL
        arrayOf(16 to 0,16 to 0,16 to 0,16 to 0), // TILING_FULL_NOFLIP
        arrayOf(8 to 0,8 to 8,8 to 8,8 to 0), // TILING_BRICK_SMALL
        arrayOf(8 to 0,8 to 8,8 to 8,8 to 0), // TILING_BRICK_SMALL_NOFLIP
        arrayOf(8 to 8,8 to 0,8 to 8,8 to 0), // TILING_BRICK_LARGE
        arrayOf(8 to 8,8 to 0,8 to 8,8 to 0), // TILING_BRICK_LARGE_NOFLIP
    )

    init {
        assert(256 == connectLut47.size)
        assert(256 == connectLut16.size)
    }

    /**
     * Tiles that half-transparent and has hue
     * will blend colour using colour multiplication
     * i.e. red hues get lost if you dive into the water
     */
    private val TILES_BLEND_MUL = hashSetOf(
            Block.WATER,
            Block.LAVA
    )

    /**
     * To interact with external modules
     */
    @JvmStatic fun addBlendMul(blockID: ItemID): Boolean {
        return TILES_BLEND_MUL.add(blockID)
    }

    ///////////////////////////////////////////
    // NO draw lightmap using colour filter, actors must also be hidden behind the darkness
    ///////////////////////////////////////////

    /**
     * Which wires should be drawn. Normally this value is set by the wiring item (e.g. wire pieces, wirecutters)
     */
//    var selectedWireRenderClass = ""

    internal fun renderData() {
        try {
            val seasonalMonth = world.worldTime.ecologicalSeason

            tilesTerrain = seasonalTerrains[seasonalMonth.floorToInt()]
            tilesTerrainNext = seasonalTerrains[(seasonalMonth + 1).floorToInt() fmod seasonalTerrains.size]
            tilesTerrainBlendDegree = seasonalMonth % 1f
        }
        catch (e: ClassCastException) { }




        if (doTilemapUpdate) {
            wrapCamera()

            camTransX = WorldCamera.x fmod TILE_SIZE
            camTransY = WorldCamera.y fmod TILE_SIZE
        }
        else {
            camTransX += WorldCamera.deltaX
            camTransY += WorldCamera.deltaY
        }

        if (doTilemapUpdate) {
            // rendering tilemap only updates every three frame
            measureDebugTime("Renderer.Tiling*") {
                fillInTileBuffer(WALL)
                fillInTileBuffer(TERRAIN) // regular tiles
                fillInTileBuffer(ORES)
                fillInTileBuffer(FLUID)
                fillInTileBuffer(OCCLUSION)
                prepareDrawBuffers()
            }
        }
    }

    internal fun drawWall(projectionMatrix: Matrix4, drawGlow: Boolean, drawEmissive: Boolean = false) {
        gdxBlendNormalStraightAlpha()
        renderUsingBuffer(WALL, projectionMatrix, drawGlow, drawEmissive)

        gdxBlendMul()
        renderUsingBuffer(OCCLUSION, projectionMatrix, false, drawEmissive)
    }


    private fun clearBuffer() {
        gdxClearAndEnableBlend(0f,0f,0f,0f)
    }

    private fun setCameraPosition(newX: Float, newY: Float) {
        camera.position.set((-newX + App.scr.halfw).roundToFloat(), (-newY + App.scr.halfh).roundToFloat(), 0f)
        camera.update()
        batch.projectionMatrix = camera.combined
    }

    internal fun drawTerrain(projectionMatrix: Matrix4, drawGlow: Boolean, drawEmissive: Boolean = false) {
        gdxBlendNormalStraightAlpha()

        renderUsingBuffer(TERRAIN, projectionMatrix, drawGlow, drawEmissive)
        renderUsingBuffer(ORES, projectionMatrix, drawGlow, drawEmissive)
        renderUsingBuffer(FLUID, projectionMatrix, drawGlow, drawEmissive)
    }


    internal fun drawFront(projectionMatrix: Matrix4, drawEmissive: Boolean = false) {
        gdxBlendMul()

        // let's just not MUL on terrain, make it FLUID only...
        renderUsingBuffer(FLUID, projectionMatrix, false, drawEmissive)


        gdxBlendNormalStraightAlpha()
    }


    private fun deblockAndWriteTexture(projectionMatrix: Matrix4, dest: FrameBuffer, blurmap: FrameBuffer) {
        blurmap.colorBufferTexture.bind(1)
        dest.colorBufferTexture.bind(0)



        // basically this part is correct, test with any test texture
        shaderDeblock.bind()
        shaderDeblock.setUniformMatrix("u_projTrans", projectionMatrix)
        shaderDeblock.setUniformf("resolution", oldScreenW.toFloat(), oldScreenH.toFloat())
        shaderDeblock.setUniformi("u_blurmap", 1)
        shaderDeblock.setUniformi("u_texture", 0)
        tilesQuad.render(shaderDeblock, GL20.GL_TRIANGLE_FAN)

        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0)
    }

    /**
     * Turns bitmask-with-single-bit-set into its bit index. The LSB is counted as 1, and thus the index starts at one.
     * @return 0 -> -1, 1 -> 0, 2 -> 1, 4 -> 2, 8 -> 3, 16 -> 4, ...
     */
    private fun Int.toBitOrd(): Int {
        val k = FastMath.intLog2(this, -1)
        return k
    }

    private val occlusionRenderTag = CreateTileAtlas.RenderTag(
        OCCLUSION_TILE_NUM_BASE, CreateTileAtlas.RenderTag.CONNECT_SELF, CreateTileAtlas.RenderTag.MASK_47, 0, 0
    )

    private lateinit var renderOnF3Only: Array<Int>
    private lateinit var platformTiles: Array<Int>
    private lateinit var wallStickerTiles: Array<Int>
    private lateinit var connectMutualTiles: Array<Int>
    private lateinit var connectSelfTiles: Array<Int>
    private lateinit var treeLeavesTiles: Array<Int>
    private lateinit var treeTrunkTiles: Array<Int>

    internal fun rebuildInternalPrecalculations() {
        if (App.IS_DEVELOPMENT_BUILD) {
            printdbg(this, "Current TileName to Number map:")
            world.tileNameToNumberMap.forEach { id, num ->
                println("$id -> $num")
            }
            println("================================")
        }


        renderOnF3Only = BlockCodex.blockProps.filter { (id, prop) ->
            prop.isActorBlock && !prop.hasTag("DORENDER") && !id.startsWith("virt:") && id != Block.NULL
        }.map { world.tileNameToNumberMap[it.key] ?: throw NullPointerException("No tilenumber for ${it.key} exists") }.sorted().toTypedArray()

        platformTiles = BlockCodex.blockProps.filter { (id, prop) ->
            isPlatform(id) && !id.startsWith("virt:") && id != Block.NULL
        }.map { world.tileNameToNumberMap[it.key] ?: throw NullPointerException("No tilenumber for ${it.key} exists") }.sorted().toTypedArray()

        wallStickerTiles = BlockCodex.blockProps.filter { (id, prop) ->
            isWallSticker(id) && !id.startsWith("virt:") && id != Block.NULL
        }.map { world.tileNameToNumberMap[it.key] ?: throw NullPointerException("No tilenumber for ${it.key} exists") }.sorted().toTypedArray()

        connectMutualTiles = BlockCodex.blockProps.filter { (id, prop) ->
            isConnectMutual(id) && !id.startsWith("virt:") && id != Block.NULL
        }.map { world.tileNameToNumberMap[it.key] ?: throw NullPointerException("No tilenumber for ${it.key} exists") }.sorted().toTypedArray()

        connectSelfTiles = BlockCodex.blockProps.filter { (id, prop) ->
            isConnectSelf(id) && !id.startsWith("virt:") && id != Block.NULL
        }.map { world.tileNameToNumberMap[it.key] ?: throw NullPointerException("No tilenumber for ${it.key} exists") }.sorted().toTypedArray()

        treeLeavesTiles = BlockCodex.blockProps.filter { (id, prop) ->
            isTreeFoliage(id) && !id.startsWith("virt:") && id != Block.NULL
        }.map { world.tileNameToNumberMap[it.key] ?: throw NullPointerException("No tilenumber for ${it.key} exists") }.sorted().toTypedArray()

        treeTrunkTiles = BlockCodex.blockProps.filter { (id, prop) ->
            isTreeTrunk(id) && !id.startsWith("virt:") && id != Block.NULL
        }.map { world.tileNameToNumberMap[it.key] ?: throw NullPointerException("No tilenumber for ${it.key} exists") }.sorted().toTypedArray()

    }


    private fun getHashCoord(x: Int, y: Int, mod: Long, layer: Int, tileNumber: Int): Int {
        val (x, y) = world.coerceXY(x, y)
        return (XXHash64.hash(
            LandUtil.getBlockAddr(world, x, y).toBig64(),
            world.generatorSeed shake tileNumber.toLong() shake layer.toLong()
        ) fmod mod).toInt()
    }

    private var for_y_start = 0
    private var for_y_end = 0
    private var for_x_start = 0
    private var for_x_end = 0
    private var camX = 0
    private var camY = 0

    private fun wrapCamera() {
        camX = WorldCamera.x
        camY = WorldCamera.y

        // can't be "WorldCamera.y / TILE_SIZE":
        //      ( 3 / 16) == 0
        //      (-3 / 16) == -1  <-- We want it to be '-1', not zero
        // using cast and floor instead of IF on ints: the other way causes jitter artefact, which I don't fucking know why

        for_y_start = (camY.toFloat() / TILE_SIZE).floorToInt()
        for_y_end = for_y_start + hTilesInVertical - 1
        for_x_start = (camX.toFloat() / TILE_SIZE).floorToInt()
        for_x_end = for_x_start + hTilesInHorizontal - 1
    }

    /**
     * Autotiling; writes to buffer. Actual draw code must be called after this operation.
     *
     * @param drawModeTilesBlendMul If current drawing mode is MULTIPLY. Doesn't matter if mode is FLUID.
     * @param wire coduitTypes bit that is selected to be drawn. Must be the power of two.
     */
    private fun fillInTileBuffer(mode: Int) {

        // TODO the real fluid rendering must use separate function, but its code should be similar to this.
        //      shader's tileAtlas will be fluid.tga, pixels written to the buffer is in accordance with the new
        //      atlas. IngameRenderer must be modified so that fluid-draw call is separated from drawing tiles.
        //      The MUL draw mode can be removed from this (it turns out drawing tinted glass is tricky because of
        //      the window frame which should NOT be MUL'd)

        // loop
        for (y in for_y_start..for_y_end) {
            for (x in for_x_start..for_x_end) {

                val bufferBaseX = x - for_x_start
                val bufferBaseY = y - for_y_start

                val (wx, wy) = world.coerceXY(x, y)

                var rawTileNum: Int = when (mode) {
                    WALL -> world.layerWall.unsafeGetTile(wx, wy)
                    TERRAIN -> world.layerTerrain.unsafeGetTile(wx, wy)
                    ORES -> world.layerOres.unsafeGetTile(wx, wy)//.also { println(it) }
                    FLUID -> world.layerFluids.unsafeGetTile1(wx, wy).let { (number, fill) ->
                        if (number == 65535 || fill < 1f/30f) 0
                        else number
                    }
                    OCCLUSION -> occlusionRenderTag.tileNumber
                    else -> throw IllegalArgumentException()
                }

                val renderTag = if (mode == OCCLUSION) occlusionRenderTag else App.tileMaker.getRenderTag(rawTileNum)

                var hash = if ((mode == WALL || mode == TERRAIN) && !BlockCodex[world.tileNumberToNameMap[rawTileNum.toLong()]].hasTag("NORANDTILE"))
                    getHashCoord(x, y, 8, mode, renderTag.tileNumber)
                else 0 // this zero is completely ignored if the block uses Subtiling

                // draw a tile
                val nearbyTilesInfo = if (mode == OCCLUSION) {
                    getNearbyTilesInfoFakeOcc(x, y)
                }
                else if (mode == ORES) {
                    0
                }
                else if (mode == FLUID) {
                    val solids = getNearbyTilesInfoTileCnx(x, y)
                    val notSolid = 15 - solids
                    val fluids = getNearbyFluidsInfo(x, y)
                    val fluidU = fluids[3].amount

                    val nearbyFluidType = fluids.asSequence().filter { it.amount >= 0.5f / 16f }.map { it.type }.filter { it.startsWith("fluid@") }.sorted().firstOrNull()

                    val fillThis =
                        world.layerFluids.unsafeGetTile1(wx, wy).second.let { if (it.isNaN()) 0f else it.coerceAtMost(1f) }

                    val tile = world.getTileFromTerrain(wx, wy)

                    if (BlockCodex[tile].isSolidForTileCnx && nearbyFluidType != null) {
                        val fmask = getFluidMaskStatus(fluids)
                        var tileToUse = fluidCornerLut[notSolid and fmask] and fluidCornerLut[solids]

                        rawTileNum = world.tileNameToNumberMap[nearbyFluidType]!!

                        // upper points and lower points use different maths
                        // in either case, LR fluids are to be checked

                        val fluidR = fluids[0].amount
                        val fluidL = fluids[2].amount

                        // check LR for down points
                        if (tileToUse and 0b0110 != 0) {
                            if (fluidR < 0.5f / 16f)
                                tileToUse = tileToUse and 0b1101
                            if (fluidL < 0.5f / 16f)
                                tileToUse = tileToUse and 0b1011
                        }
                        // check R
                        else if (tileToUse and 0b0010 != 0) {
                            if (fluidR < 0.5f / 16f )
                                tileToUse = 0
                        }
                        // check L
                        else if (tileToUse and 0b0100 != 0) {
                            if (fluidL < 0.5f / 16f)
                                tileToUse = 0
                        }


                        18 + tileToUse
                    }
                    else if (rawTileNum == 0)
                        0
                    else if (bufferBaseY > 0 && tempRenderTypeBuffer[bufferBaseY - 1, bufferBaseX].let {
                        it.ushr(16).toInt() == rawTileNum && it.and(255) == 0L
                    })
                        16
                    else if (bufferBaseY > 0 && tempRenderTypeBuffer[bufferBaseY - 1, bufferBaseX].let {
                        it.ushr(16).toInt() == rawTileNum && (it.and(255) < 18 || it.and(255) >= 36)
                    })
                        17
                    else if (fillThis < 0.5f / 16f)
                        0
                    else if (fillThis >= 15.5f / 16f) {
                        // wy > 0 and tileUp is solid
                        if (wy > 0 && bufferBaseY > 0 && solids and 0b1000 != 0) {
                            val tileUpTag = tempRenderTypeBuffer[bufferBaseY - 1, bufferBaseX]
                            val tileNum = tileUpTag ushr 16
                            val tileTag = tileUpTag and 255

                            if (tileNum.toInt() == rawTileNum && tileTag in 18..33) {
                                if ((tileTag - 18 and 0b0110).toInt() == 0b0110)
                                    38
                                else if ((tileTag - 18 and 0b0100).toInt() == 0b0100)
                                    37
                                else if ((tileTag - 18 and 0b0010).toInt() == 0b0010)
                                    36
                                else
                                    15
                            }
                            else
                                15
                        }
                        else
                            15
                    }
                    else
                        (fillThis * 16f - 0.5f).floorToInt().coerceIn(0, 15)
                }
                else if (treeLeavesTiles.binarySearch(rawTileNum) >= 0) {
                    getNearbyTilesInfoTreeLeaves(x, y, mode).swizzle8(renderTag.maskType, hash)
                }
                else if (treeTrunkTiles.binarySearch(rawTileNum) >= 0) {
                    getNearbyTilesInfoTreeTrunks(x, y, mode)
                }
                else if (platformTiles.binarySearch(rawTileNum) >= 0) {
                    hash %= 2
                    getNearbyTilesInfoPlatform(x, y).swizzleH2(renderTag.maskType, hash)
                }
                else if (wallStickerTiles.binarySearch(rawTileNum) >= 0) {
                    hash = 0
                    getNearbyTilesInfoWallSticker(x, y)
                }
                else if (connectMutualTiles.binarySearch(rawTileNum) >= 0) {
                    getNearbyTilesInfoConMutual(x, y, mode).swizzle8(renderTag.maskType, hash)
                }
                else if (connectSelfTiles.binarySearch(rawTileNum) >= 0) {
                    getNearbyTilesInfoConSelf(x, y, mode, rawTileNum).swizzle8(renderTag.maskType, hash)
                }
                else {
                    0
                }

                val breakage = if (mode == TERRAIN || mode == ORES)
                    world.getTerrainDamage(x, y)
                else if (mode == WALL)
                    world.getWallDamage(x, y)
                else 0f

                if (breakage.isNaN()) throw IllegalStateException("Block breakage at ($x, $y) is NaN (mode=$mode)")

                val maxHealth = if (mode == TERRAIN || mode == ORES)
                    BlockCodex[world.getTileFromTerrain(x, y)].strength
                else if (mode == WALL)
                    BlockCodex[world.getTileFromWall(x, y)].strength
                else 1

                val breakingStage =
                    if (mode == TERRAIN || mode == WALL || mode == ORES)
                            (breakage / maxHealth).coerceIn(0f, 1f).times(BREAKAGE_STEPS).roundToInt()
                    else 0

                if (mode == TERRAIN || mode == WALL) {
                    // translate nearbyTilesInfo into proper subtile number
                    val nearbyTilesInfo = getNearbyTilesInfoDeblocking(mode, x, y)
                    val subtiles = if (nearbyTilesInfo == null)
                        listOf(
                            Point2i(130, 1), Point2i(130, 1), Point2i(130, 1), Point2i(130, 1)
                        )
                    else
                        (0..3).map {
                            Point2i(126, 0) + deblockerNearbyTilesToSubtile[it][nearbyTilesInfo]
                        }

                    /*TL*/writeToBufferSubtile(BLURMAP_BASE + mode, bufferBaseX * 2 + 0, bufferBaseY * 2 + 0, subtiles[0].x, subtiles[0].y, 0, 0)
                    /*TR*/writeToBufferSubtile(BLURMAP_BASE + mode, bufferBaseX * 2 + 1, bufferBaseY * 2 + 0, subtiles[1].x, subtiles[1].y, 0, 0)
                    /*BR*/writeToBufferSubtile(BLURMAP_BASE + mode, bufferBaseX * 2 + 1, bufferBaseY * 2 + 1, subtiles[2].x, subtiles[2].y, 0, 0)
                    /*BL*/writeToBufferSubtile(BLURMAP_BASE + mode, bufferBaseX * 2 + 0, bufferBaseY * 2 + 1, subtiles[3].x, subtiles[3].y, 0, 0)
                }

                if (renderTag.maskType >= CreateTileAtlas.RenderTag.MASK_SUBTILE_GENERIC) {
                    hash = getHashCoord(x, y, 268435456, mode, renderTag.tileNumber)

                    val subtileSwizzlers = if (renderTag.tilingMode and 1 == 1)
                        intArrayOf(0,0,0,0)
                    else
                        intArrayOf(
                            (hash ushr 16) and 7,
                            (hash ushr 19) and 7,
                            (hash ushr 22) and 7,
                            (hash ushr 25) and 7,
                        )
                    val variantOps = variantOpsLUT[renderTag.tilingMode]
                    val isGrass = (renderTag.maskType == CreateTileAtlas.RenderTag.MASK_SUBTILE_GRASS)
                    val nearbyGrasses = if (isGrass) {
                        // indices: R D L U
                        getNearbyTilesPos4(x, y).mapIndexed { i, it ->
                            BlockCodex[world.getTileFromTerrain(it.x, it.y)].hasTag("GRASS").toInt(i)
                        }.fold(0) { acc, it -> acc or it }
                    }
                    else null
                    val subtiles = getSubtileIndexOf(rawTileNum, nearbyTilesInfo, hash, subtileSwizzlers, variantOps, nearbyGrasses)

                    /*TL*/writeToBufferSubtile(mode, bufferBaseX * 2 + 0, bufferBaseY * 2 + 0, subtiles[0].x, subtiles[0].y, breakingStage, subtileSwizzlers[0])
                    /*TR*/writeToBufferSubtile(mode, bufferBaseX * 2 + 1, bufferBaseY * 2 + 0, subtiles[1].x, subtiles[1].y, breakingStage, subtileSwizzlers[1])
                    /*BR*/writeToBufferSubtile(mode, bufferBaseX * 2 + 1, bufferBaseY * 2 + 1, subtiles[2].x, subtiles[2].y, breakingStage, subtileSwizzlers[2])
                    /*BL*/writeToBufferSubtile(mode, bufferBaseX * 2 + 0, bufferBaseY * 2 + 1, subtiles[3].x, subtiles[3].y, breakingStage, subtileSwizzlers[3])
                }
                else {
                    var tileNumber = if (rawTileNum == 0 && mode != OCCLUSION) 0
                    // special case: actorblocks and F3 key
                    else if (renderOnF3Only.binarySearch(rawTileNum) >= 0 && !KeyToggler.isOn(Keys.F3))
                        0
                    // special case: fluids
                    else if (mode == FLUID)
                        rawTileNum + nearbyTilesInfo
                    // special case: ores
                    else if (mode == ORES)
                        rawTileNum + world.layerOres.unsafeGetTile1(wx, wy).second
                    // rest of the cases: terrain and walls
                    else rawTileNum + when (renderTag.maskType) {
                        CreateTileAtlas.RenderTag.MASK_NA -> 0
                        CreateTileAtlas.RenderTag.MASK_16 -> connectLut16[nearbyTilesInfo]
                        CreateTileAtlas.RenderTag.MASK_47 -> connectLut47[nearbyTilesInfo]
                        CreateTileAtlas.RenderTag.MASK_TORCH, CreateTileAtlas.RenderTag.MASK_PLATFORM -> nearbyTilesInfo
                        else -> throw IllegalArgumentException("Unknown mask type: ${renderTag.maskType}")
                    }

                    // hide tiles with super low lights, kinda like Minecraft's Orebfuscator
                    val lightAtXY = LightmapRenderer.getLight(x, y) ?: Cvec(0)
                    if (mode != FLUID && mode != OCCLUSION && maxOf(lightAtXY.fastLum(), lightAtXY.a) <= 1.5f / 255f) {
                        tileNumber = 2 // black solid
                    }

                    val subtileNum = tileNumber.tileToSubtile()

                    val thisTileX = subtileNum % App.tileMaker.TILES_IN_X
                    val thisTileY = subtileNum / App.tileMaker.TILES_IN_X

                    // draw a tile
                    val offsets = subtileOffsetsBySwizzleIndex[hash]
                    /*TL*/writeToBuffer(mode, bufferBaseX * 2 + offsets[0].x, bufferBaseY * 2 + offsets[0].y, thisTileX + 0, thisTileY + 0, breakingStage, hash)
                    /*TR*/writeToBuffer(mode, bufferBaseX * 2 + offsets[1].x, bufferBaseY * 2 + offsets[1].y, thisTileX + 1, thisTileY + 0, breakingStage, hash)
                    /*BR*/writeToBuffer(mode, bufferBaseX * 2 + offsets[2].x, bufferBaseY * 2 + offsets[2].y, thisTileX + 1, thisTileY + 2, breakingStage, hash)
                    /*BL*/writeToBuffer(mode, bufferBaseX * 2 + offsets[3].x, bufferBaseY * 2 + offsets[3].y, thisTileX + 0, thisTileY + 2, breakingStage, hash)
                }

                tempRenderTypeBuffer[bufferBaseY, bufferBaseX] = (nearbyTilesInfo or rawTileNum.shl(16)).toLong()
            }
        }
//        println("App.tileMaker.TILES_IN_X = ${App.tileMaker.TILES_IN_X}\tApp.tileMaker.atlas.width = ${App.tileMaker.atlas.width}")
//        println("tilesTerrain.horizontalCount = ${tilesTerrain.horizontalCount}")
    }

    private fun Int.tileToSubtile() = (this / App.tileMaker.TILES_IN_X) * 4*App.tileMaker.TILES_IN_X +
            (this % App.tileMaker.TILES_IN_X) * 2

    private val swizzleMap8 = arrayOf(
        arrayOf(0,1,2,3,4,5,6,7), /* normal */
        arrayOf(4,3,2,1,0,7,6,5), /* horz flip */
        arrayOf(2,3,4,5,6,7,0,1), /* CW 90 */
        arrayOf(2,1,0,7,6,5,4,3), /* hfCW 90 */
        arrayOf(4,5,6,7,0,1,2,3), /* CW 180 */
        arrayOf(0,7,6,5,4,3,2,1), /* hfCW 180 */
        arrayOf(6,7,0,1,2,3,4,5), /* CW 270 */
        arrayOf(6,5,4,3,2,1,0,7), /* hfCW 270 */
    )

    private val subtileOffsetsBySwizzleIndex = arrayOf(
        // index: TL->TR->BR->BL
        arrayOf(Point2i(0,0),Point2i(1,0),Point2i(1,1),Point2i(0,1)), /* normal */
        arrayOf(Point2i(1,0),Point2i(0,0),Point2i(0,1),Point2i(1,1)), /* horz flip */
        arrayOf(Point2i(1,0),Point2i(1,1),Point2i(0,1),Point2i(0,0)), /* CW 90 */
        arrayOf(Point2i(0,0),Point2i(0,1),Point2i(1,1),Point2i(1,0)), /* hfCW 90 */
        arrayOf(Point2i(1,1),Point2i(0,1),Point2i(0,0),Point2i(1,0)), /* CW 180 */
        arrayOf(Point2i(0,1),Point2i(1,1),Point2i(1,0),Point2i(0,0)), /* hfCW 180 */
        arrayOf(Point2i(0,1),Point2i(0,0),Point2i(1,0),Point2i(1,1)), /* CW 270 */
        arrayOf(Point2i(1,1),Point2i(1,0),Point2i(0,0),Point2i(0,1)), /* hfCW 270 */
    )

    private fun Int.swizzle8(maskType: Int, hash: Int): Int {
        if (maskType >= CreateTileAtlas.RenderTag.MASK_SUBTILE_GENERIC) return this
        var ret = 0
        swizzleMap8[hash].forEachIndexed { index, ord ->
            ret = ret or this.ushr(ord).and(1).shl(index)
        }
        return ret
    }


    private val h2lut = arrayOf(
        arrayOf(0,1,2,3,4,5,6,7),
        arrayOf(0,2,1,5,6,3,4,7),
    )

    private val fluidCornerLut = arrayOf(15,12,9,8,3,0,1,0,6,4,0,0,2,0,0,0)

    private fun Int.swizzleH2(maskType: Int, hash: Int): Int {
        if (maskType >= CreateTileAtlas.RenderTag.MASK_SUBTILE_GENERIC) return this
        return h2lut[hash][this]
    }

    private fun getNearbyTilesPos(x: Int, y: Int): Array<Point2i> {
        return arrayOf(
            Point2i(x + 1, y),
            Point2i(x + 1, y + 1),
            Point2i(x, y + 1),
            Point2i(x - 1, y + 1),
            Point2i(x - 1, y),
            Point2i(x - 1, y - 1),
            Point2i(x, y - 1),
            Point2i(x + 1, y - 1)
        )
    }

    private fun getNearbyTilesPos4(x: Int, y: Int): Array<Point2i> {
        return arrayOf(
            Point2i(x + 1, y),
            Point2i(x, y + 1),
            Point2i(x - 1, y),
            Point2i(x, y - 1),
        )
    }

    private fun getNearbyTilesInfoConSelf(x: Int, y: Int, mode: Int, mark: ItemID?): Int {
        val nearbyTiles = getNearbyTilesPos(x, y).map { world.getTileFrom(mode, it.x, it.y) }

        var ret = 0
        for (i in nearbyTiles.indices) {
            if (nearbyTiles[i] == mark) {
                ret += (1 shl i) // add 1, 2, 4, 8 for i = 0, 1, 2, 3
            }
        }

        return ret
    }

    private fun getNearbyTilesInfoConSelf(x: Int, y: Int, mode: Int, mark: Int): Int {
        val layer = when (mode) {
            WALL -> world.layerWall
            TERRAIN -> world.layerTerrain
            ORES -> world.layerOres
            FLUID -> world.layerFluids
            else -> throw IllegalArgumentException("Unknown mode $mode")
        }
        val nearbyTiles = getNearbyTilesPos(x, y).map {
            val (wx, wy) = world.coerceXY(it.x, it.y)
            layer.unsafeGetTile(wx, wy)
        }

        var ret = 0
        for (i in nearbyTiles.indices) {
            if (nearbyTiles[i] == mark) {
                ret += (1 shl i) // add 1, 2, 4, 8 for i = 0, 1, 2, 3
            }
        }

        return ret
    }

    private fun getNearbyTilesInfoFakeOcc(x: Int, y: Int): Int {
        val eligible = BlockCodex[world.getTileFromWall(x, y)].isSolidForTileCnx && !BlockCodex[world.getTileFromTerrain(x, y)].isSolidForTileCnx
        val nearbyTiles = getNearbyTilesPos(x, y).map {
            !BlockCodex[world.getTileFromTerrain(it.x, it.y)].isSolidForTileCnx
        }

        if (!eligible) return 255

        var ret = 0
        for (i in nearbyTiles.indices) {
            if (nearbyTiles[i] == true) {
                ret += (1 shl i) // add 1, 2, 4, 8 for i = 0, 1, 2, 3
            }
        }

        return ret
    }

    private fun getNearbyTilesInfoDeblocking(mode: Int, x: Int, y: Int): Int? {
        val tileThis = world.getTileFrom(mode, x, y)
        val nearbyTiles = getNearbyTilesPos4(x, y).map { world.getTileFrom(mode, it.x, it.y) }

        val renderTagThis = App.tileMaker.getRenderTag(tileThis)

        if (renderTagThis.postProcessing != CreateTileAtlas.RenderTag.POSTPROCESS_DEBLOCKING) return null

        when (renderTagThis.connectionType) {
            CreateTileAtlas.RenderTag.CONNECT_SELF -> {
                var ret = 0
                for (i in nearbyTiles.indices) {
                    if (nearbyTiles[i] == tileThis) {
                        ret += (1 shl i) // add 1, 2, 4, 8 for i = 0, 1, 2, 3
                    }
                }
                return ret
            }
            CreateTileAtlas.RenderTag.CONNECT_MUTUAL -> {
                // make sure to not connect to tiles with no deblocking
                var ret = 0
                for (i in nearbyTiles.indices) {
                    if (BlockCodex[nearbyTiles[i]].isSolidForTileCnx && isConnectMutual(nearbyTiles[i]) &&
                        App.tileMaker.getRenderTag(nearbyTiles[i]).postProcessing == CreateTileAtlas.RenderTag.POSTPROCESS_DEBLOCKING
                    ) {
                        ret += (1 shl i) // add 1, 2, 4, 8 for i = 0, 1, 2, 3
                    }
                }

                return ret
            }
            else -> return null
        }
    }

    private val deblockerNearbyTilesToSubtile: Array<Array<Point2i>> = arrayOf(
        arrayOf(Point2i(0,0),Point2i(0,0),Point2i(0,0),Point2i(0,0),Point2i(3,0),Point2i(3,0),Point2i(3,0),Point2i(3,0),Point2i(2,0),Point2i(2,0),Point2i(2,0),Point2i(2,0),Point2i(4,0),Point2i(4,0),Point2i(4,0),Point2i(4,0)), /*TL*/
        arrayOf(Point2i(1,0),Point2i(3,0),Point2i(1,0),Point2i(3,0),Point2i(1,0),Point2i(3,0),Point2i(1,0),Point2i(3,0),Point2i(3,1),Point2i(4,0),Point2i(3,1),Point2i(4,0),Point2i(3,1),Point2i(4,0),Point2i(3,1),Point2i(4,0)), /*TR*/
        arrayOf(Point2i(1,1),Point2i(2,1),Point2i(3,1),Point2i(4,0),Point2i(1,1),Point2i(2,1),Point2i(3,1),Point2i(4,0),Point2i(1,1),Point2i(2,1),Point2i(3,1),Point2i(4,0),Point2i(1,1),Point2i(2,1),Point2i(3,1),Point2i(4,0)), /*BR*/
        arrayOf(Point2i(0,1),Point2i(0,1),Point2i(2,0),Point2i(2,0),Point2i(2,1),Point2i(2,1),Point2i(4,0),Point2i(4,0),Point2i(0,1),Point2i(0,1),Point2i(2,0),Point2i(2,0),Point2i(2,1),Point2i(2,1),Point2i(4,0),Point2i(4,0)), /*BL*/
    )

    private fun getNearbyTilesInfoConMutual(x: Int, y: Int, mode: Int): Int {
        val nearbyTiles: List<ItemID> = getNearbyTilesPos(x, y).map { world.getTileFrom(mode, it.x, it.y) }

        var ret = 0
        for (i in nearbyTiles.indices) {
            if (BlockCodex[nearbyTiles[i]].isSolidForTileCnx && isConnectMutual(nearbyTiles[i])) {
                ret += (1 shl i) // add 1, 2, 4, 8 for i = 0, 1, 2, 3
            }
        }

        return ret
    }

    private fun getNearbyTilesInfoTileCnx(x: Int, y: Int): Int {
        val nearbyTiles: List<ItemID> = getNearbyTilesPos4(x, y).map { world.getTileFromTerrain(it.x, it.y) }

        var ret = 0
        for (i in nearbyTiles.indices) {
            if (BlockCodex[nearbyTiles[i]].isSolidForTileCnx) {
                ret += (1 shl i) // add 1, 2, 4, 8 for i = 0, 1, 2, 3
            }
        }

        return ret
    }

    private fun getNearbyFluidsInfo(x: Int, y: Int): List<GameWorld.FluidInfo> {
        val nearbyTiles: List<GameWorld.FluidInfo> = getNearbyTilesPos4(x, y).map { world.getFluid(it.x, it.y) }
        return nearbyTiles
    }

    private val fluidSolidMaskLut = arrayOf(0b1010, 0b1000, 0b0010, 0b0000)
    private fun getFluidMaskStatus(nearbyFluids: List<GameWorld.FluidInfo>): Int {
        // TODO reverse gravity

        val D = (nearbyFluids[1].amount >= 15.5f / 16f)
        val U = (nearbyFluids[3].amount >= 0.5f / 16f)

        val i = D.toInt(0) or U.toInt(1)

        return fluidSolidMaskLut[i]
    }

    private fun getNearbyTilesInfoTreeLeaves(x: Int, y: Int, mode: Int): Int {
        val nearbyTiles: List<ItemID> = getNearbyTilesPos(x, y).map { world.getTileFrom(mode, it.x, it.y) }

        var ret = 0
        for (i in nearbyTiles.indices) {
            if (isTreeFoliage(nearbyTiles[i])) { // foliage "shadow" should not connect to the tree trunk
                ret += (1 shl i) // add 1, 2, 4, 8 for i = 0, 1, 2, 3
            }
        }

        return ret
    }

    private fun getNearbyTilesInfoTreeTrunks(x: Int, y: Int, mode: Int): Int {
        val nearbyTiles: List<ItemID> = getNearbyTilesPos(x, y).map { world.getTileFrom(mode, it.x, it.y) }

        var ret = 0
        for (i in nearbyTiles.indices) {
            if (isTreeTrunk(nearbyTiles[i]) ||
                i == 6 && isTreeFoliage(nearbyTiles[i]) ||
                i == 2 && isCultivable(nearbyTiles[i])) { // if tile above is leaves or tile below is cultivable, connect to it
                ret += (1 shl i) // add 1, 2, 4, 8 for i = 0, 1, 2, 3
            }
        }

        return ret
    }

    private fun Int.popcnt() = Integer.bitCount(this)

    /**
     * Basically getNearbyTilesInfoConMutual() but connects mutually with all the fluids
     */
    private fun getNearbyTilesInfoFluids(x: Int, y: Int): Int {
        val nearbyPos = getNearbyTilesPos(x, y)
        val nearbyTiles: List<ItemID> = nearbyPos.map { world.getTileFromTerrain(it.x, it.y) }

        var ret = 0
        for (i in nearbyTiles.indices) {
            val fluid = world.getFluid(nearbyPos[i].x, nearbyPos[i].y)
            if (BlockCodex[nearbyTiles[i]].isSolidForTileCnx || (fluid.isFluid() && fluid.amount > FLUID_MIN_MASS)) {
                ret += (1 shl i) // add 1, 2, 4, 8 for i = 0, 1, 2, 3
            }
        }

        return ret
    }

    private fun getNearbyTilesInfoWallSticker(x: Int, y: Int): Int {
        val nearbyTiles = arrayOf(Block.NULL, Block.NULL, Block.NULL, Block.NULL)
        val NEARBY_TILE_KEY_BACK = NEARBY_TILE_KEY_UP
        nearbyTiles[NEARBY_TILE_KEY_LEFT] =  world.getTileFrom(TERRAIN, x - 1, y)
        nearbyTiles[NEARBY_TILE_KEY_RIGHT] = world.getTileFrom(TERRAIN, x + 1, y)
        nearbyTiles[NEARBY_TILE_KEY_DOWN] =  world.getTileFrom(TERRAIN, x    , y + 1)
        nearbyTiles[NEARBY_TILE_KEY_BACK] =  world.getTileFrom(WALL,    x    , y)

        try {
            if (BlockCodex[nearbyTiles[NEARBY_TILE_KEY_DOWN]].isSolidForTileCnx)
            // has tile on the bottom
                return 3
            else if (BlockCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolidForTileCnx
                     && BlockCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolidForTileCnx)
            // has tile on both sides
                return 0
            else if (BlockCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolidForTileCnx)
            // has tile on the right
                return 2
            else if (BlockCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolidForTileCnx)
            // has tile on the left
                return 1
            else if (BlockCodex[nearbyTiles[NEARBY_TILE_KEY_BACK]].isSolidForTileCnx)
            // has tile on the back
                return 0
            else
                return 3
        } catch (e: ArrayIndexOutOfBoundsException) {
            return if (BlockCodex[nearbyTiles[NEARBY_TILE_KEY_DOWN]].isSolidForTileCnx)
            // has tile on the bottom
                3 else 0
        }
    }

    private fun getNearbyTilesInfoPlatform(x: Int, y: Int): Int {
        val nearbyTiles = arrayOf(Block.NULL, Block.NULL, Block.NULL, Block.NULL)
        //val NEARBY_TILE_KEY_BACK = NEARBY_TILE_KEY_UP
        nearbyTiles[NEARBY_TILE_KEY_LEFT] =  world.getTileFrom(TERRAIN, x - 1, y)
        nearbyTiles[NEARBY_TILE_KEY_LEFT] = world.getTileFrom(TERRAIN, x - 1, y)
        nearbyTiles[NEARBY_TILE_KEY_RIGHT] = world.getTileFrom(TERRAIN, x + 1, y)

        if ((BlockCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolidForTileCnx &&
             BlockCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolidForTileCnx) ||
            isPlatform(nearbyTiles[NEARBY_TILE_KEY_LEFT]) &&
            isPlatform(nearbyTiles[NEARBY_TILE_KEY_RIGHT])) // LR solid || LR platform
            return 0
        else if (BlockCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolidForTileCnx &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_LEFT]) &&
                 !BlockCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolidForTileCnx &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_RIGHT])) // L solid and not platform && R not solid and not platform
            return 4
        else if (BlockCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolidForTileCnx &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_RIGHT]) &&
                 !BlockCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolidForTileCnx &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_LEFT])) // R solid and not platform && L not solid and nto platform
            return 6
        else if (BlockCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolidForTileCnx &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_LEFT])) // L solid && L not platform
            return 3
        else if (BlockCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolidForTileCnx &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_RIGHT])) // R solid && R not platform
            return 5
        else if ((BlockCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolidForTileCnx ||
                  isPlatform(nearbyTiles[NEARBY_TILE_KEY_LEFT])) &&
                 !BlockCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolidForTileCnx &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_RIGHT])) // L solid or platform && R not solid and not platform
            return 1
        else if ((BlockCodex[nearbyTiles[NEARBY_TILE_KEY_RIGHT]].isSolidForTileCnx ||
                  isPlatform(nearbyTiles[NEARBY_TILE_KEY_RIGHT])) &&
                 !BlockCodex[nearbyTiles[NEARBY_TILE_KEY_LEFT]].isSolidForTileCnx &&
                 !isPlatform(nearbyTiles[NEARBY_TILE_KEY_LEFT])) // R solid or platform && L not solid and not platform
            return 2
        else
            return 7
    }

    /**
     * @param base base full tile number in atlas
     * @param nearbyTilesInfo nearbyTilesInfo (0-255)
     * @param variants 0-65535
     * @param subtileSwizzlers flip-rotation indices (4-element array of 0-7)
     * @param variantOps operations for variant selection
     * @param nearbyGrasses bitmask of nearby grasses. Marked = is grass. Indices: (RIGHT-DOWN-LEFT-UP)
     *
     * @return subtile indices on the atlas, in the following order: TL, TR, BR, BL
     */
    private fun getSubtileIndexOf(base: Int, nearbyTilesInfo: Int, variants: Int, subtileSwizzlers: IntArray, variantOps: Array<Pair<Int, Int>>, nearbyGrasses: Int? = null): List<Point2i> {
        val variants = (0..3).map { quadrant ->
            (variants.ushr(quadrant * 4) and 15).let { oldIdx ->
                variantOps[quadrant].let { (mod, add) -> (oldIdx % mod) + add }
            }
        }
        val tilenumInAtlas = (0..3).map {
            val subtile = subtileVarBaseLuts[it][connectLut47[nearbyTilesInfo]].alterUsingGrassMap(it, nearbyGrasses)
            base.tileToSubtile() + 8 * subtile.reorientUsingFliprotIdx(subtileSwizzlers[it])
        }
        val baseXY = tilenumInAtlas.map { Point2i(
            it % App.tileMaker.SUBTILES_IN_X,
            (it / App.tileMaker.SUBTILES_IN_X).let {
                if (it % 2 == 1) it + 1 else it
            },
        ) }

        // apply variants
        return (baseXY zip variants).map { (base, va) -> Point2i(
            base.x + va / 2,
            base.y + va % 2,
        ).wrapAroundAtlasBySubtile() }
    }

    private fun Int.alterUsingGrassMap(quadrant: Int, nearbyGrasses: Int?): Int {
        return if (nearbyGrasses == null) this
        else when (this) {
            1 ->
                if (quadrant == 0)
                    if (nearbyGrasses and 0b0100 != 0) this else 13
                else if (quadrant == 1)
                    if (nearbyGrasses and 0b0001 != 0) this else 14
                else this
            7 ->
                if (quadrant == 3)
                    if (nearbyGrasses and 0b0100 != 0) this else 18
                else if (quadrant == 2)
                    if (nearbyGrasses and 0b0001 != 0) this else 17
                else this
            4 ->
                if (quadrant == 1)
                    if (nearbyGrasses and 0b1000 != 0) this else 15
                else if (quadrant == 2)
                    if (nearbyGrasses and 0b0010 != 0) this else 16
                else this
            10 ->
                if (quadrant == 0)
                    if (nearbyGrasses and 0b1000 != 0) this else 20
                else if (quadrant == 3)
                    if (nearbyGrasses and 0b0010 != 0) this else 19
                else this
            else -> this
        }
    }

    private fun Int.reorientUsingFliprotIdx(fliprotIndex: Int): Int {
        return subtileReorientLUT[fliprotIndex][this]
    }

    private fun Point2i.wrapAroundAtlasBySubtile(): Point2i {
        return Point2i(
            this.x % App.tileMaker.SUBTILES_IN_X,
            this.y + 2 * (this.x / App.tileMaker.SUBTILES_IN_X)
        )
    }


    /**
     * @param sheetX x-coord of the FULL TILE in an atlas
     * @param sheetY y-coord of the FULL TILE in an atlas
     *
     * @return Raw colour bits in RGBA8888 format
     */
    private fun sheetXYToTilemapColour1(mode: Int, sheetX: Int, sheetY: Int, breakage: Int, hash: Int): Int =
        (tilesTerrain.horizontalCount * sheetY + sheetX).shl(8) or // the actual tile bits
                255 // does it premultiply the alpha?!?!!!?!?!


    /**
     * @param sheetX x-coord of the SUBTILE in an atlas
     * @param sheetY y-coord of the SUBTILE in an atlas
     *
     * @return Raw colour bits in RGBA8888 format
     */
    private fun sheetXYToTilemapColour1Subtile(mode: Int, sheetX: Int, sheetY: Int, breakage: Int, hash: Int): Int =
        (2 * tilesTerrain.horizontalCount * sheetY + sheetX).shl(8) or // the actual tile bits
                255 // does it premultiply the alpha?!?!!!?!?!


    private fun sheetXYToTilemapColour2(mode: Int, sheetX: Int, sheetY: Int, breakage: Int, hash: Int): Int =
        breakage.and(15).shl(8) or // breakage bits on B
            hash.and(15).shl(16) or // fliprot on G
                255 // does it premultiply the alpha?!?!!!?!?!


    private fun writeToBuffer(mode: Int, bufferPosX: Int, bufferPosY: Int, sheetX: Int, sheetY: Int, breakage: Int, hash: Int) {
        val sourceBuffer = when(mode) {
            TERRAIN -> terrainTilesBuffer
            WALL -> wallTilesBuffer
            ORES -> oreTilesBuffer
            FLUID -> fluidTilesBuffer
            OCCLUSION -> occlusionBuffer
            else -> throw IllegalArgumentException()
        }


        sourceBuffer[bufferPosY, bufferPosX] =
            sheetXYToTilemapColour1(mode, sheetX, sheetY, breakage, hash).toLong().and(0xFFFFFFFFL) or
            sheetXYToTilemapColour2(mode, sheetX, sheetY, breakage, hash).toLong().and(0xFFFFFFFFL).shl(32)
    }

    private fun writeToBufferSubtile(mode: Int, bufferPosX: Int, bufferPosY: Int, sheetX: Int, sheetY: Int, breakage: Int, hash: Int) {
        val sourceBuffer = when(mode) {
            TERRAIN -> terrainTilesBuffer
            WALL -> wallTilesBuffer
            ORES -> oreTilesBuffer
            FLUID -> fluidTilesBuffer
            OCCLUSION -> occlusionBuffer
            BLURMAP_TERR -> blurMapTerr
            BLURMAP_WALL -> blurMapWall
            else -> throw IllegalArgumentException()
        }


        sourceBuffer[bufferPosY, bufferPosX] =
            sheetXYToTilemapColour1Subtile(mode, sheetX, sheetY, breakage, hash).toLong().and(0xFFFFFFFFL) or
            sheetXYToTilemapColour2(mode, sheetX, sheetY, breakage, hash).toLong().and(0xFFFFFFFFL).shl(32)
    }

    private var _tilesBufferAsTex: Texture = Texture(1, 1, Pixmap.Format.RGBA8888)
    private var _tilesBufferAsTex2: Texture = Texture(1, 1, Pixmap.Format.RGBA8888)
    private var _blurTilesBuffer: Texture = Texture(1, 1, Pixmap.Format.RGBA8888)
    private val occlusionIntensity = 0.25f // too low value and dark-coloured walls won't darken enough

    private val doTilemapUpdate: Boolean
        get() {
            val rate = (((Gdx.graphics.framesPerSecond / 50f) * TILE_SIZEF) / maxOf(WorldCamera.deltaX.abs(), WorldCamera.deltaY.abs()).coerceAtLeast(1)).roundToInt().coerceIn(1, 4)
            App.debugTimers.put("Renderer.tilemapUpdateDivider", rate.toLong())
            return (!world.layerTerrain.ptrDestroyed && App.GLOBAL_RENDER_TIMER % rate == 0L)
        }

    private var camTransX = 0
    private var camTransY = 0

    private fun prepareDrawBuffers() {
        listOf(TERRAIN, WALL, ORES, FLUID, OCCLUSION).forEach { mode ->
            val sourceBuffer = when(mode) {
                TERRAIN -> terrainTilesBuffer
                WALL -> wallTilesBuffer
                ORES -> oreTilesBuffer
                FLUID -> fluidTilesBuffer
                OCCLUSION -> occlusionBuffer
                else -> throw IllegalArgumentException()
            }

            val drawBuffer1 = when(mode) {
                TERRAIN -> terrainDrawBuffer1
                WALL -> wallDrawBuffer1
                ORES -> oresDrawBuffer
                FLUID -> fluidDrawBuffer
                OCCLUSION -> occlusionDrawBuffer
                else -> throw IllegalArgumentException()
            }

            val drawBuffer2 = when(mode) {
                TERRAIN -> terrainDrawBuffer2
                WALL -> wallDrawBuffer2
                else -> null
            }

            for (y in 0 until drawBuffer1.height) {
                for (x in 0 until drawBuffer1.width) {
                    val colRaw = sourceBuffer[y, x]
                    val colMain = colRaw.toInt()
                    val colSub = colRaw.ushr(32).toInt()

                    drawBuffer1.setColor(colMain)
                    drawBuffer1.drawPixel(x, y)

                    drawBuffer2?.setColor(colSub)
                    drawBuffer2?.drawPixel(x, y)

                    // write blurmap to its own buffer for TERRAIN and WALL
                    if (mode == TERRAIN || mode == WALL) {
                        val colRaw = (if (mode == TERRAIN) blurMapTerr else blurMapWall)[y, x]
                        val colMain = colRaw.toInt()

                        blurTilesBuffer.setColor(colMain)
                        blurTilesBuffer.drawPixel(x, y)
                    }
                }
            }
        }
    }

    private fun renderUsingBuffer(mode: Int, projectionMatrix: Matrix4, drawGlow: Boolean, drawEmissive: Boolean) {
        //Gdx.gl.glClearColor(.094f, .094f, .094f, 0f)
        //Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)


        //val tilesInHorizontal = tilesBuffer.width
        //val tilesInVertical =   tilesBuffer.height


        val tileAtlas = when (mode) {
            TERRAIN, ORES, WALL, OCCLUSION, FLUID, BLURMAP_TERR, BLURMAP_WALL -> tilesTerrain
            else -> throw IllegalArgumentException()
        }

        val vertexColour = when (mode) {
            WALL -> WALL_OVERLAY_COLOUR
            else -> Color.WHITE
        }

        val drawBuffer1 = when(mode) {
            TERRAIN -> terrainDrawBuffer1
            WALL -> wallDrawBuffer1
            ORES -> oresDrawBuffer
            FLUID -> fluidDrawBuffer
            OCCLUSION -> occlusionDrawBuffer
            else -> throw IllegalArgumentException()
        }

        val drawBuffer2 = when(mode) {
            TERRAIN -> terrainDrawBuffer2
            WALL -> wallDrawBuffer2
            else -> nullBuffer
        }

        _tilesBufferAsTex.dispose()
        _tilesBufferAsTex = Texture(drawBuffer1)
        _tilesBufferAsTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

        _tilesBufferAsTex2.dispose()
        _tilesBufferAsTex2 = Texture(drawBuffer2)
        _tilesBufferAsTex2.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

        _blurTilesBuffer.dispose()
        _blurTilesBuffer = Texture(blurTilesBuffer)
        _blurTilesBuffer.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

        if (drawEmissive) {
            nullTex.bind(4)
            _tilesBufferAsTex2.bind(3)
            _tilesBufferAsTex.bind(2)
            tilesEmissive.texture.bind(1)
            tilesEmissive.texture.bind(0) // for some fuck reason, it must be bound as last
        }
        else if (drawGlow) {
            nullTex.bind(4)
            _tilesBufferAsTex2.bind(3)
            _tilesBufferAsTex.bind(2)
            tilesGlow.texture.bind(1)
            tilesGlow.texture.bind(0) // for some fuck reason, it must be bound as last
        }
        else {
            _blurTilesBuffer.bind(4)
            _tilesBufferAsTex2.bind(3)
            _tilesBufferAsTex.bind(2)
            tilesTerrainNext.texture.bind(1)
            tileAtlas.texture.bind(0) // for some fuck reason, it must be bound as last
        }

        shaderTiling.bind()
        shaderTiling.setUniformMatrix("u_projTrans", projectionMatrix)//camera.combined)
        shaderTiling.setUniformf("colourFilter", vertexColour)
        shaderTiling.setUniformi("tilesAtlas", 0)
        shaderTiling.setUniformi("tilesBlendAtlas", 1)
        shaderTiling.setUniformi("tilemap", 2)
        shaderTiling.setUniformi("tilemap2", 3)
        shaderTiling.setUniformi("deblockingMap", 4)
        shaderTiling.setUniformi("tilemapDimension", drawBuffer1.width, drawBuffer1.height)
        shaderTiling.setUniformf("tilesInAxes", tilesInHorizontal.toFloat(), tilesInVertical.toFloat())
        shaderTiling.setUniformi("cameraTranslation", camTransX, camTransY) // usage of 'fmod' and '%' were depend on the for_x_start, which I can't just do naive int div
        shaderTiling.setUniformf("tilesInAtlas", tileAtlas.horizontalCount * 2f, tileAtlas.verticalCount * 2f) //depends on the tile atlas
        shaderTiling.setUniformf("atlasTexSize", tileAtlas.texture.width.toFloat(), tileAtlas.texture.height.toFloat()) //depends on the tile atlas
        // set the blend value as world's time progresses, in linear fashion
        shaderTiling.setUniformf("tilesBlend", if (mode == TERRAIN || mode == WALL)
            tilesTerrainBlendDegree
        else
            0f
        )
        shaderTiling.setUniformf("mulBlendIntensity", if (mode == OCCLUSION) occlusionIntensity else 1f)
        tilesQuad.render(shaderTiling, GL20.GL_TRIANGLE_FAN)

        //tilesBufferAsTex.dispose()
    }

    private var oldScreenW = 0
    private var oldScreenH = 0

    var tilesInHorizontal = -1; private set
    var tilesInVertical = -1; private set
    var hTilesInHorizontal = -1; private set
    var hTilesInVertical = -1; private set

    fun resize(screenW: Int, screenH: Int) {
        hTilesInHorizontal = (App.scr.wf / TILE_SIZE).ceilToInt() + 1
        hTilesInVertical = (App.scr.hf / TILE_SIZE).ceilToInt() + 1

        tilesInHorizontal = hTilesInHorizontal * 2
        tilesInVertical = hTilesInVertical * 2

        val oldTH = (oldScreenW.toFloat() / TILE_SIZE).ceilToInt().times(2) + 2
        val oldTV = (oldScreenH.toFloat() / TILE_SIZE).ceilToInt().times(2) + 2

        // only update if it's really necessary
        if (oldTH != tilesInHorizontal || oldTV != tilesInVertical) {
            if (::terrainTilesBuffer.isInitialized) terrainTilesBuffer.destroy()
            if (::wallTilesBuffer.isInitialized) wallTilesBuffer.destroy()
            if (::oreTilesBuffer.isInitialized) oreTilesBuffer.destroy()
            if (::fluidTilesBuffer.isInitialized) fluidTilesBuffer.destroy()
            if (::occlusionBuffer.isInitialized) occlusionBuffer.destroy()
            if (::tempRenderTypeBuffer.isInitialized) tempRenderTypeBuffer.destroy()

            terrainTilesBuffer = UnsafeLong2D(tilesInHorizontal, tilesInVertical)
            wallTilesBuffer = UnsafeLong2D(tilesInHorizontal, tilesInVertical)
            oreTilesBuffer = UnsafeLong2D(tilesInHorizontal, tilesInVertical)
            fluidTilesBuffer = UnsafeLong2D(tilesInHorizontal, tilesInVertical)
            occlusionBuffer = UnsafeLong2D(tilesInHorizontal, tilesInVertical)
            blurMapTerr = UnsafeLong2D(tilesInHorizontal, tilesInVertical)
            blurMapWall = UnsafeLong2D(tilesInHorizontal, tilesInVertical)
            tempRenderTypeBuffer = UnsafeLong2D(hTilesInHorizontal, hTilesInVertical)

            terrainDrawBuffer1.dispose()
            terrainDrawBuffer1 = Pixmap(tilesInHorizontal, tilesInVertical, Pixmap.Format.RGBA8888)
            terrainDrawBuffer2.dispose()
            terrainDrawBuffer2 = Pixmap(tilesInHorizontal, tilesInVertical, Pixmap.Format.RGBA8888)
            wallDrawBuffer1.dispose()
            wallDrawBuffer1 = Pixmap(tilesInHorizontal, tilesInVertical, Pixmap.Format.RGBA8888)
            wallDrawBuffer2.dispose()
            wallDrawBuffer2 = Pixmap(tilesInHorizontal, tilesInVertical, Pixmap.Format.RGBA8888)
            oresDrawBuffer.dispose()
            oresDrawBuffer = Pixmap(tilesInHorizontal, tilesInVertical, Pixmap.Format.RGBA8888)
            fluidDrawBuffer.dispose()
            fluidDrawBuffer = Pixmap(tilesInHorizontal, tilesInVertical, Pixmap.Format.RGBA8888)
            occlusionDrawBuffer.dispose()
            occlusionDrawBuffer = Pixmap(tilesInHorizontal, tilesInVertical, Pixmap.Format.RGBA8888)
            blurTilesBuffer.dispose()
            blurTilesBuffer = Pixmap(tilesInHorizontal, tilesInVertical, Pixmap.Format.RGBA8888)
        }

        if (oldScreenW != screenW || oldScreenH != screenH) {
            tilesQuad = Mesh(
                    true, 4, 4,
                    VertexAttribute.Position(),
                    VertexAttribute.ColorUnpacked(),
                    VertexAttribute.TexCoords(0)
            )

            tilesQuad.setVertices(floatArrayOf( // WARNING! not ususal quads; TexCoords of Y is flipped
                    0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 0f,
                    App.scr.wf, 0f, 0f, 1f, 1f, 1f, 1f, 1f, 0f,
                    App.scr.wf, App.scr.hf, 0f, 1f, 1f, 1f, 1f, 1f, 1f,
                    0f, App.scr.hf, 0f, 1f, 1f, 1f, 1f, 0f, 1f
            ))
            tilesQuad.setIndices(shortArrayOf(0, 1, 2, 3))
        }

        if (::deblockingFBO.isInitialized) deblockingFBO.dispose()
        deblockingFBO = Float16FrameBuffer(screenW, screenH, false)
        if (::blurmapFBO.isInitialized) deblockingFBO.dispose()
        blurmapFBO = Float16FrameBuffer(screenW, screenH, false)

        oldScreenW = screenW
        oldScreenH = screenH


        printdbg(this, "Resize event")

    }

    fun clampWTile(x: Int): Int {
        if (x < 0) {
            return 0
        } else if (x > world.width) {
            return world.width
        } else {
            return x
        }
    }

    fun clampHTile(x: Int): Int {
        if (x < 0) {
            return 0
        } else if (x > world.height) {
            return world.height
        } else {
            return x
        }
    }

    fun dispose() {
        printdbg(this, "dispose called by")
        printStackTrace(this)

        seasonalTerrains.forEach { it.dispose() }
        tilesGlow.dispose()
        tilesEmissive.dispose()
        tileItemTerrain.dispose()
        tileItemTerrainGlow.dispose()
        tileItemTerrainEmissive.dispose()
        tileItemWall.dispose()
        tileItemWallGlow.dispose()
        tileItemWallEmissive.dispose()
        terrainDrawBuffer1.dispose()
        terrainDrawBuffer2.dispose()
        wallDrawBuffer1.dispose()
        wallDrawBuffer2.dispose()
        oresDrawBuffer.dispose()
        fluidDrawBuffer.dispose()
        occlusionDrawBuffer.dispose()
        blurTilesBuffer.dispose()
        _tilesBufferAsTex.dispose()
        _tilesBufferAsTex2.dispose()
        _blurTilesBuffer.dispose()
        tilesQuad.tryDispose()
        shaderTiling.dispose()
        shaderDeblock.dispose()
        nullTex.dispose()
        nullBuffer.dispose()

        if (::terrainTilesBuffer.isInitialized) terrainTilesBuffer.destroy()
        if (::wallTilesBuffer.isInitialized) wallTilesBuffer.destroy()
        if (::oreTilesBuffer.isInitialized) oreTilesBuffer.destroy()
        if (::fluidTilesBuffer.isInitialized) fluidTilesBuffer.destroy()
        if (::occlusionBuffer.isInitialized) occlusionBuffer.destroy()
        if (::blurMapTerr.isInitialized) blurMapTerr.destroy()
        if (::blurMapWall.isInitialized) blurMapWall.destroy()
        if (::tempRenderTypeBuffer.isInitialized) tempRenderTypeBuffer.destroy()

        if (::batch.isInitialized) batch.tryDispose()

        App.tileMaker.dispose()
    }

    fun isConnectSelf(b: ItemID) = App.tileMaker.getRenderTag(b).connectionType == CreateTileAtlas.RenderTag.CONNECT_SELF
    fun isConnectMutual(b: ItemID) = App.tileMaker.getRenderTag(b).connectionType == CreateTileAtlas.RenderTag.CONNECT_MUTUAL
    fun isWallSticker(b: ItemID) = App.tileMaker.getRenderTag(b).connectionType == CreateTileAtlas.RenderTag.CONNECT_WALL_STICKER
    fun isPlatform(b: ItemID) = App.tileMaker.getRenderTag(b).connectionType == CreateTileAtlas.RenderTag.CONNECT_WALL_STICKER_CONNECT_SELF
    //fun isBlendMul(b: Int) = TILES_BLEND_MUL.contains(b)
    fun isTreeFoliage(b: ItemID) = BlockCodex[b].hasAllTagsOf("TREE", "LEAVES")
    fun isTreeTrunk(b: ItemID) = BlockCodex[b].hasAllTagsOf("TREE", "TREETRUNK")
    fun isCultivable(b: ItemID) = BlockCodex[b].hasAllTagsOf("NATURAL", "CULTIVABLE")

}
