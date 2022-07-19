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
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * @param width of hitbox, in tiles, when the door is opened. Default to 2. Closed door always have width of 1. (this limits how big and thick the door can be)
 * @param height of hitbox, in ties. Default to 3.
 *
 * Created by minjaesong on 2022-07-15.
 */
class FixtureSwingingDoorBase : FixtureBase, Luminous {

    /* OVERRIDE THESE TO CUSTOMISE */
    open val tw = 2
    open val th = 3
    open val twClosed = 1
    open val opacity = BlockCodex[Block.STONE].opacity
    open val isOpacityActuallyLuminosity = false
    open val moduleName = "basegame"
    open val texturePath = "sprites/fixtures/door_test.tga"
    open val textureIdentifier = "fixtures-door_test.tga"
    open val customNameFun = { "DOOR_BASE" }
    /* END OF CUTOMISABLE PARAMETERS */

    @Transient override val lightBoxList: ArrayList<Lightbox> = ArrayList()
    @Transient override val shadeBoxList: ArrayList<Lightbox> = ArrayList()

    protected var doorState = 0 // -1: open toward left, 0: closed, 1: open toward right

//    @Transient private var placeActorBlockLatch = false

    constructor() : super(
            BlockBox(BlockBox.FULL_COLLISION, 1, 3), // temporary value, will be overwritten by reload()
            nameFun = { "item not loaded properly, alas!" }
    ) {
        val hbw = TILE_SIZE * (tw * 2 - twClosed)
        val hbh = TILE_SIZE * th

        nameFun = customNameFun

        density = 1200.0
        actorValue[AVKey.BASEMASS] = 10.0

        // loading textures
        CommonResourcePool.addToLoadingList("$moduleName-$textureIdentifier") {
            TextureRegionPack(ModMgr.getGdxFile(moduleName, texturePath), hbw, hbh)
        }
        CommonResourcePool.loadAll()
        makeNewSprite(FixtureBase.getSpritesheet(moduleName, texturePath, hbw, hbh)).let {
            it.setRowsAndFrames(3,1)
        }

        // define light/shadebox
        (if (isOpacityActuallyLuminosity) lightBoxList else shadeBoxList).add(
                Lightbox(Hitbox(0.0, 0.0, TILE_SIZED, th * TILE_SIZED), opacity))

        reload()
    }

    override fun spawn(posX: Int, posY: Int) = spawn(posX, posY, TILE_SIZE * (tw * 2 - twClosed), TILE_SIZE * th)

    // TODO move this function over FixtureBase once it's done and perfected
    protected fun spawn(posX: Int, posY: Int, hbw: Int, hbh: Int): Boolean {
        // wrap x-position
        val posX = posX fmod world!!.width

        // define physical size
        setHitboxDimension(hbw, hbh, 0, 0)
        /*this.hitbox.setFromWidthHeight(
                posX * TILE_SIZED,
                posY * TILE_SIZED,
                blockBox.width * TILE_SIZED,
                blockBox.height * TILE_SIZED
        )*/
        blockBox = BlockBox(BlockBox.FULL_COLLISION, tw * 2 - twClosed, th)

        // check for existing blocks (and fixtures)
        var hasCollision = false
        checkForCollision@
        for (y in posY until posY + blockBox.height) {
            for (x in posX until posX + blockBox.width) {
                val tile = world!!.getTileFromTerrain(x, y)
                if (BlockCodex[tile].isSolid || BlockCodex[tile].isActorBlock) {
                    hasCollision = true
                    break@checkForCollision
                }
            }
        }

        if (hasCollision) {
            printdbg(this, "cannot spawn fixture ${nameFun()} at F${INGAME.WORLD_UPDATE_TIMER}, has tile collision; tilewise dim: (${blockBox.width}, ${blockBox.height}) ")
            return false
        }

        printdbg(this, "spawn fixture ${nameFun()} at F${INGAME.WORLD_UPDATE_TIMER}, tilewise dim: (${blockBox.width}, ${blockBox.height})")

        // set the position of this actor
        worldBlockPos = Point2i(posX - (hbw - 1) / 2, posY)

        // fill the area with the filler blocks
        placeActorBlocks()


        this.isVisible = true


        // actually add this actor into the world
        INGAME.queueActorAddition(this)
        spawnRequestedTime = System.nanoTime()


        return true
    }

    override fun reload() {
        super.reload()

        nameFun = customNameFun

        val hbw = TILE_SIZE * (tw * 2 - twClosed)
        val hbh = TILE_SIZE * th

        // redefined things that are affected by sprite size
//        blockBox = BlockBox(BlockBox.FULL_COLLISION, tw * 2 - twClosed, th)

        // loading textures

//        setHitboxDimension(hbw, hbh, TILE_SIZE * (tw * 2 - twClosed), 0)
//        setHitboxDimension(hbw, hbh, TILE_SIZE * ((tw * 2 - twClosed - 1) / 2), 0)


//        placeActorBlockLatch = false
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
            val tile = when (doorState) {
                // CLOSED --
                // fill the actorBlock so that:
                // N..N F N..N (repeated `th` times; N: no collision, F: full collision)
                0/*CLOSED*/ -> {
                    if (ox in tw-1 until tw-1+twClosed) Block.ACTORBLOCK_FULL_COLLISION
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