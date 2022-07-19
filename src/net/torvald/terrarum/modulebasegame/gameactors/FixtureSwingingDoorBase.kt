package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.Hitbox
import net.torvald.terrarum.gameactors.Lightbox
import net.torvald.terrarum.gameactors.Luminous
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * @param width of hitbox, in tiles, when the door is opened. Default to 2. Closed door always have width of 1. (this limits how big and thick the door can be)
 * @param height of hitbox, in ties. Default to 3.
 *
 * Created by minjaesong on 2022-07-15.
 */
open class FixtureSwingingDoorBase : FixtureBase, Luminous {

    /* OVERRIDE THESE TO CUSTOMISE */
    open val tw = 2 // tilewise width of the door when opened
    open val th = 3 // tilewise height of the door
    open val twClosed = 1 // tilewise width of the door when closed
    open val opacity = BlockCodex[Block.STONE].opacity
    open val isOpacityActuallyLuminosity = false
    open val moduleName = "basegame"
    open val texturePath = "sprites/fixtures/door_test.tga"
    open val textureIdentifier = "fixtures-door_test.tga"
    open val customNameFun = { "DOOR_BASE" }
    /* END OF CUTOMISABLE PARAMETERS */

    private val tilewiseHitboxWidth = tw * 2 - twClosed
    private val tilewiseHitboxHeight = th
    private val pixelwiseHitboxWidth = TILE_SIZE * tilewiseHitboxWidth
    private val pixelwiseHitboxHeight = TILE_SIZE * tilewiseHitboxHeight
    private val tilewiseDistToAxis = tw - twClosed

    @Transient override val lightBoxList: ArrayList<Lightbox> = ArrayList()
    @Transient override val shadeBoxList: ArrayList<Lightbox> = ArrayList()

    protected var doorState = 0 // -1: open toward left, 0: closed, 1: open toward right

//    @Transient private var placeActorBlockLatch = false

    constructor() : super(
            BlockBox(BlockBox.FULL_COLLISION, 1, 1), // temporary value, will be overwritten by spawn()
            nameFun = { "item not loaded properly, alas!" }
    ) {
        nameFun = customNameFun

        density = 1200.0
        actorValue[AVKey.BASEMASS] = 10.0

        // loading textures
        CommonResourcePool.addToLoadingList("$moduleName-$textureIdentifier") {
            TextureRegionPack(ModMgr.getGdxFile(moduleName, texturePath), pixelwiseHitboxWidth, pixelwiseHitboxHeight)
        }
        CommonResourcePool.loadAll()
        makeNewSprite(FixtureBase.getSpritesheet(moduleName, texturePath, pixelwiseHitboxWidth, pixelwiseHitboxHeight)).let {
            it.setRowsAndFrames(3,1)
        }

        // define light/shadebox
        // TODO: redefine when opened to left/right
        (if (isOpacityActuallyLuminosity) lightBoxList else shadeBoxList).add(
                Lightbox(Hitbox(TILE_SIZED * tilewiseDistToAxis, 0.0, TILE_SIZED * twClosed, TILE_SIZED * th), opacity))

        // define physical size
        setHitboxDimension(TILE_SIZE * tilewiseHitboxWidth, TILE_SIZE * tilewiseHitboxHeight, 0, 0)
        blockBox = BlockBox(BlockBox.FULL_COLLISION, tilewiseHitboxWidth, tilewiseHitboxHeight)

        reload()
    }

    override fun spawn(posX: Int, posY: Int) = spawn(posX, posY, tilewiseHitboxWidth, tilewiseHitboxHeight)

    override fun reload() {
        super.reload()

        nameFun = customNameFun

        // define light/shadebox
        // TODO: redefine when opened to left/right
        (if (isOpacityActuallyLuminosity) lightBoxList else shadeBoxList).add(
                Lightbox(Hitbox(TILE_SIZED * tilewiseDistToAxis, 0.0, TILE_SIZED * twClosed, TILE_SIZED * th), opacity))

    }

    open protected fun closeDoor() {
        (sprite!! as SheetSpriteAnimation).currentRow = 0
        doorState = 0
    }

    open protected fun openToRight() {
        (sprite!! as SheetSpriteAnimation).currentRow = 1
        doorState = 1
    }

    open protected fun openToLeft() {
        (sprite!! as SheetSpriteAnimation).currentRow = 2
        doorState = -1
    }

    /*override fun forEachBlockbox(action: (Int, Int, Int, Int) -> Unit) {
        val xStart = worldBlockPos!!.x - ((tw * 2 - twClosed - 1) / 2) // worldBlockPos.x is where the mouse was, of when the tilewise width was 1.
        for (y in worldBlockPos!!.y until worldBlockPos!!.y + blockBox.height) {
            for (x in xStart until xStart + blockBox.width) {
                action(x, y, x - xStart, y - worldBlockPos!!.y)
            }
        }
    }*/

    override fun placeActorBlocks() {
        forEachBlockbox { x, y, ox, oy ->
            printdbg(this, "placeActorBlocks xy=$x,$y oxy=$ox,$oy")

            val tile = when (doorState) {
                // CLOSED --
                // fill the actorBlock so that:
                // N..N F N..N (repeated `th` times; N: no collision, F: full collision)
                0/*CLOSED*/ -> {
                    if (ox in tilewiseDistToAxis until tilewiseDistToAxis + twClosed) Block.ACTORBLOCK_FULL_COLLISION
                    else Block.ACTORBLOCK_NO_COLLISION
                }
                else/*OPENED*/ -> Block.ACTORBLOCK_NO_COLLISION
            }

            world!!.setTileTerrain(x, y, tile, false)
        }
    }

    override fun flagDespawn() {
        super.flagDespawn()
        printdbg(this, "flagged to despawn")
        printStackTrace(this)
    }

    override fun updateForTerrainChange(cue: IngameInstance.BlockChangeQueueItem) {

    }

    override fun update(delta: Float) {
        /*if (placeActorBlockLatch) {
            placeActorBlockLatch = false
            placeActorBlocks()
        }*/

        super.update(delta)

        //if (!flagDespawn) placeActorBlocks()

        when (doorState) {
            0/*CLOSED*/ -> {
                if (!flagDespawn && worldBlockPos != null) {

                }
            }
        }
    }
}