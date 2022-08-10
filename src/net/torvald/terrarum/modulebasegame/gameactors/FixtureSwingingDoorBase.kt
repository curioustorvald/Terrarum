package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.gdx.graphics.Cvec
import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZED
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.langpack.Lang
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
    var tw = 2 // tilewise width of the door when opened
    var twClosed = 1 // tilewise width of the door when closed
    var th = 3 // tilewise height of the door
    var opacity = BlockCodex[Block.STONE].opacity
    var isOpacityActuallyLuminosity = false
    var moduleName = "basegame"
    var texturePath = "sprites/fixtures/door_test.tga"
    var textureIdentifier = "fixtures-door_test.tga"
    var doorClosedHoldLength: Second = 0.1f
    var doorOpenedHoldLength: Second = 0.25f
    var nameKey = "DOOR_BASE" // goes into the savegame
    var nameKeyReadFromLang = true // goes into the savegame
    /* END OF CUTOMISABLE PARAMETERS */

    private var tilewiseHitboxWidth = tw * 2 - twClosed
    private var tilewiseHitboxHeight = th
    private var pixelwiseHitboxWidth = TILE_SIZE * tilewiseHitboxWidth
    private var pixelwiseHitboxHeight = TILE_SIZE * tilewiseHitboxHeight
    private var tilewiseDistToAxis = tw - twClosed

    @Transient override val lightBoxList: ArrayList<Lightbox> = ArrayList()
    @Transient override val shadeBoxList: ArrayList<Lightbox> = ArrayList()

    protected var doorState = 0 // -1: open toward left, 0: closed, 1: open toward right
    protected var doorStateTimer: Second = 0f

    @Transient private lateinit var customNameFun: () -> String
    @Transient private lateinit var doorHoldLength: HashMap<Int, Second>

    constructor() : super(
            BlockBox(BlockBox.FULL_COLLISION, 1, 1), // temporary value, will be overwritten by spawn()
            nameFun = { "item not loaded properly, alas!" }
    ) {
        _construct(
                2,
                1,
                3,
                BlockCodex[Block.STONE].opacity,
                false,
                "basegame",
                "sprites/fixtures/door_test.tga",
                "fixtures-door_test.tga",
                "DOOR_BASE",
                true
        )
    }

    protected fun _construct(
            tw: Int, // tilewise width of the door when opened
            twClosed: Int, // tilewise width of the door when closed
            th: Int, // tilewise height of the door
            opacity: Cvec,
            isOpacityActuallyLuminosity: Boolean,
            moduleName: String,
            texturePath: String,
            textureIdentifier: String,
            nameKey: String,
            nameKeyReadFromLang: Boolean,
            doorCloseHoldLength: Second = 0.1f,
            doorOpenedHoldLength: Second = 0.25f
    ) {
        this.tw = tw
        this.twClosed = twClosed
        this.th = th
        this.opacity = opacity
        this.isOpacityActuallyLuminosity = isOpacityActuallyLuminosity
        this.moduleName = moduleName
        this.texturePath = texturePath
        this.textureIdentifier = textureIdentifier
        this.nameKey = nameKey
        this.nameKeyReadFromLang = nameKeyReadFromLang
        this.doorClosedHoldLength = doorCloseHoldLength
        this.doorOpenedHoldLength = doorOpenedHoldLength

        this.tilewiseHitboxWidth = tw * 2 - twClosed
        this.tilewiseHitboxHeight = th
        this.pixelwiseHitboxWidth = TILE_SIZE * tilewiseHitboxWidth
        this.pixelwiseHitboxHeight = TILE_SIZE * tilewiseHitboxHeight
        this.tilewiseDistToAxis = tw - twClosed


        this.customNameFun = { if (nameKeyReadFromLang) Lang[nameKey] else nameKey }
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

        doorHoldLength = hashMapOf(
                -1 to doorClosedHoldLength,
                1 to doorClosedHoldLength,
                0 to doorOpenedHoldLength
        )

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
        val ww = INGAME.world.width
        val mouseRelX = Terrarum.mouseX - hitbox.hitboxStart.x
        val mouseRelY = Terrarum.mouseY - hitbox.hitboxStart.y
        return 0.0 <= mouseRelX && mouseRelX < hitbox.width / 2 && mouseRelY in 0.0..hitbox.height
    }

    private fun mouseOnRightSide(): Boolean {
        val ww = INGAME.world.width
        val mouseRelX = Terrarum.mouseX - hitbox.hitboxStart.x
        val mouseRelY = Terrarum.mouseY - hitbox.hitboxStart.y
        return hitbox.width / 2 < mouseRelX && mouseRelX <= hitbox.width && mouseRelY in 0.0..hitbox.height
    }

    private fun ActorWithBody.ontheLeftSideOfDoor(): Boolean {
        val ww = INGAME.world.width * TILE_SIZED
        val pivot = this@FixtureSwingingDoorBase.hitbox.centeredX
        // note: this function uses startX while the dual other function uses endX; the "dist" must be same between two functions
        val dist = pivot - this.hitbox.startX // NOTE: order is different from the other function
        val dist2 = pivot - (this.hitbox.startX - ww)
        val distLim = this@FixtureSwingingDoorBase.hitbox.width / 2 + this.hitbox.width
        return dist in 0.0..distLim || dist2 in 0.0..distLim
    }
    private fun ActorWithBody.ontheRightSideOfDoor(): Boolean {
        val ww = INGAME.world.width * TILE_SIZED
        val pivot = this@FixtureSwingingDoorBase.hitbox.centeredX
        val dist = this.hitbox.endX - pivot // NOTE: order is different from the other function
        val dist2 = (this.hitbox.endX + ww) - pivot
        val distLim = this@FixtureSwingingDoorBase.hitbox.width / 2 + this.hitbox.width
        return dist in 0.0..distLim || dist2 in 0.0..distLim
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
                // TODO auto open from the other side does not work if X-coord is 0 or (world width - 1)
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