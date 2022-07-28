package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2
import java.util.*
import kotlin.math.absoluteValue

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
    open val doorClosedHoldLength: Second = 0.1f
    open val doorOpenedHoldLength: Second = 0.25f
    /* END OF CUTOMISABLE PARAMETERS */

    private val tilewiseHitboxWidth = tw * 2 - twClosed
    private val tilewiseHitboxHeight = th
    private val pixelwiseHitboxWidth = TILE_SIZE * tilewiseHitboxWidth
    private val pixelwiseHitboxHeight = TILE_SIZE * tilewiseHitboxHeight
    private val tilewiseDistToAxis = tw - twClosed

    @Transient override val lightBoxList: ArrayList<Lightbox> = ArrayList()
    @Transient override val shadeBoxList: ArrayList<Lightbox> = ArrayList()

    protected var doorState = 0 // -1: open toward left, 0: closed, 1: open toward right
    protected var doorStateTimer: Second = 0f

    @Transient private val doorHoldLength: HashMap<Int, Second> = hashMapOf(
            -1 to doorClosedHoldLength,
            1 to doorClosedHoldLength,
            0 to doorOpenedHoldLength
    )

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

    override fun spawn(posX: Int, posY: Int, installersUUID: UUID?): Boolean = spawn(posX, posY, installersUUID, tilewiseHitboxWidth, tilewiseHitboxHeight)

    override fun reload() {
        super.reload()

        nameFun = customNameFun

        // define light/shadebox
        // TODO: redefine when opened to left/right
        (if (isOpacityActuallyLuminosity) lightBoxList else shadeBoxList).add(
                Lightbox(Hitbox(TILE_SIZED * tilewiseDistToAxis, 0.0, TILE_SIZED * twClosed, TILE_SIZED * th), opacity))

    }

    open protected fun closeDoor() {
        if (doorState != 0) {
            (sprite!! as SheetSpriteAnimation).currentRow = 0
            doorState = 0
            placeActorBlocks()
        }
        doorCloseQueued = false
    }

    open protected fun openToRight() {
        if (doorState != 1) {
            (sprite!! as SheetSpriteAnimation).currentRow = 1
            doorState = 1
            placeActorBlocks()
        }
        doorCloseQueued = false
    }

    open protected fun openToLeft() {
        if (doorState != -1) {
            (sprite!! as SheetSpriteAnimation).currentRow = 2
            doorState = -1
            placeActorBlocks()
        }
        doorCloseQueued = false
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

    private fun mouseOnLeftSide(): Boolean {
        val mouseRelX = Terrarum.mouseX - hitbox.hitboxStart.x
        val mouseRelY = Terrarum.mouseY - hitbox.hitboxStart.y
        return 0.0 <= mouseRelX && mouseRelX < hitbox.width / 2 && mouseRelY in 0.0..hitbox.height
    }

    private fun mouseOnRightSide(): Boolean {
        val mouseRelX = Terrarum.mouseX - hitbox.hitboxStart.x
        val mouseRelY = Terrarum.mouseY - hitbox.hitboxStart.y
        return hitbox.width / 2 < mouseRelX && mouseRelX <= hitbox.width && mouseRelY in 0.0..hitbox.height
    }

    private fun ActorWithBody.ontheLeftSideOfDoor(): Boolean {
        return this.hitbox.startX < this@FixtureSwingingDoorBase.hitbox.centeredX
    }
    private fun ActorWithBody.ontheRightSideOfDoor(): Boolean {
        return this.hitbox.endX > this@FixtureSwingingDoorBase.hitbox.centeredX
    }

    private fun ActorWithBody.movingTowardsRight(): Boolean {
        return ((this.controllerV ?: Vector2()) + this.externalV).x >= PHYS_EPSILON_VELO
    }
    private fun ActorWithBody.movingTowardsLeft(): Boolean {
        return ((this.controllerV ?: Vector2()) + this.externalV).x <= -PHYS_EPSILON_VELO
    }
    private fun ActorWithBody.notMoving(): Boolean {
        return ((this.controllerV ?: Vector2()) + this.externalV).x.absoluteValue < PHYS_EPSILON_VELO
    }

    private var doorCloseQueueTimer = 0f
    private var doorCloseQueued = false

    private fun queueDoorClosing() {
        doorCloseQueueTimer = 0f
        doorCloseQueued = true
    }

    override fun update(delta: Float) {
        super.update(delta)


        // debug colouring
//        this.sprite?.colourFilter =
//                if (doorCloseQueued) Color.YELLOW
//                else if (doorStateTimer > doorHoldLength[doorState]!!) Color.LIME
//                else Color.CORAL


        if (!flagDespawn && worldBlockPos != null) {
            // delayed auto closing
            if (doorCloseQueued && doorCloseQueueTimer >= doorOpenedHoldLength) {
                closeDoor()
            }
            else if (doorCloseQueued) {
                doorCloseQueueTimer += delta
            }

            // automatic opening/closing
            if (doorStateTimer > doorHoldLength[doorState]!!) {
                val actors = INGAME.actorContainerActive.filterIsInstance<ActorWithBody>()

                // auto opening and closing
                // TODO make this work with "player_alies" faction, not just a player
                val installer: IngamePlayer? = if (actorThatInstalledThisFixture == null) null
                else INGAME.actorContainerActive.filterIsInstance<IngamePlayer>().filter { it.uuid == actorThatInstalledThisFixture }.ifEmpty {
                    INGAME.actorContainerInactive.filterIsInstance<IngamePlayer>().filter { it.uuid == actorThatInstalledThisFixture }
                }.getOrNull(0)

                // if the door is "owned" by someone, restrict access to its "amicable" (defined using Faction subsystem) actors
                // if the door is owned by null, restrict access to ActorHumanoid and actors with "intelligent" actor value set up
                if (actorThatInstalledThisFixture == null || installer != null) {
                    val amicableActors: List<ActorWithBody> = ArrayList(
                            if (actorThatInstalledThisFixture == null)
                                actors.filterIsInstance<ActorHumanoid>() union actors.filter { it.actorValue.getAsBoolean("intelligent") == true }
                            else {
                                val goodFactions = installer?.faction?.flatMap { it.factionAmicable }?.toHashSet()
                                if (goodFactions != null)
                                    actors.filterIsInstance<Factionable>().filter {
                                        (it.faction.map { it.factionName } intersect goodFactions).isNotEmpty()
                                    } as List<ActorWithBody>
                                else
                                    listOf()
                            }
                    ).also {
                        // add the installer of the door to the amicableActors if for some reason it was not added
                        if (installer != null && !it.contains(installer)) it.add(0, installer)
                    }.filter {
                        // filter amicableActors so that ones standing near the door remain
                        this.hitbox.containsHitbox(INGAME.world.width * TILE_SIZED, it.hitbox)
                    }

                    var nobodyIsThere = true
                    val oldState = doorState

                    for (actor in amicableActors) {
                        if (doorState != 0) {
                            if (this.hitbox.containsHitbox(INGAME.world.width * TILE_SIZED, actor.hitbox)) {
                                nobodyIsThere = false
                                break
                            }
                        }
                        else {
                            if (actor.ontheLeftSideOfDoor() && actor.movingTowardsRight()) {
                                openToRight()
                                nobodyIsThere = false
                                break
                            }
                            else if (actor.ontheRightSideOfDoor() && actor.movingTowardsLeft()) {
                                openToLeft()
                                nobodyIsThere = false
                                break
                            }
                        }
                    }
                    if (nobodyIsThere && !doorCloseQueued && doorState != 0) {
                        queueDoorClosing()
                    }

                    if (oldState != doorState) doorStateTimer = 0f
                }
            }
            else {
                doorStateTimer += delta
            }

        }

    }
}