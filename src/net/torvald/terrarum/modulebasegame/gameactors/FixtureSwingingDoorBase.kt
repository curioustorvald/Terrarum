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

    @Transient private var placeActorBlockLatch = false

    constructor() : super(
            BlockBox(BlockBox.FULL_COLLISION, 1, 3), // temporary value, will be overwritten by reload()
            nameFun = { "item not loaded properly, alas!" }
    ) {
        reload()
        placeActorBlockLatch = true
    }

    override fun reload() {
        super.reload()

        nameFun = customNameFun

        val hbw = TILE_SIZE * (tw * 2 - twClosed)
        val hbh = TILE_SIZE * th

        blockBox = BlockBox(BlockBox.FULL_COLLISION, tw * 2 - twClosed, th)

        // loading textures
        CommonResourcePool.addToLoadingList("$moduleName-$textureIdentifier") {
            TextureRegionPack(ModMgr.getGdxFile(moduleName, texturePath), hbw, hbh)
        }
        CommonResourcePool.loadAll()

        density = 1200.0
        actorValue[AVKey.BASEMASS] = 10.0

//        setHitboxDimension(hbw, hbh, TILE_SIZE * (tw * 2 - twClosed), 0)
        setHitboxDimension(hbw, hbh, TILE_SIZE * ((tw * 2 - twClosed - 1) / 2), 0)

        (if (isOpacityActuallyLuminosity) lightBoxList else shadeBoxList).add(
                Lightbox(Hitbox(0.0, 0.0, TILE_SIZED, th * TILE_SIZED), opacity))

        makeNewSprite(FixtureBase.getSpritesheet(moduleName, texturePath, hbw, hbh)).let {
            it.setRowsAndFrames(3,1)
        }

        placeActorBlockLatch = false
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

    override fun forEachBlockbox(action: (Int, Int, Int, Int) -> Unit) {
        val xStart = worldBlockPos!!.x - ((tw * 2 - twClosed - 1) / 2) // worldBlockPos.x is where the mouse was, of when the tilewise width was 1.
        for (y in worldBlockPos!!.y until worldBlockPos!!.y + blockBox.height) {
            for (x in xStart until xStart + blockBox.width) {
                action(x, y, x - xStart, y - worldBlockPos!!.y)
            }
        }
    }

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
        if (placeActorBlockLatch) {
            placeActorBlockLatch = false
            placeActorBlocks()
        }

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