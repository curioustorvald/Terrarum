package net.torvald.terrarum

import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.blockproperties.WireCodex
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.ActorID
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.BlockMarkerActor
import net.torvald.terrarum.gameactors.faction.FactionCodex
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.WorldSimulator
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.itemproperties.MaterialCodex
import net.torvald.terrarum.modulebasegame.IngameRenderer
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.modulebasegame.ui.Notification
import net.torvald.terrarum.modulebasegame.ui.UITooltip
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.VirtualDisk
import net.torvald.terrarum.ui.ConsoleWindow
import net.torvald.terrarum.ui.UICanvas
import net.torvald.util.SortedArrayList
import org.khelekore.prtree.DistanceCalculator
import org.khelekore.prtree.DistanceResult
import org.khelekore.prtree.MBRConverter
import org.khelekore.prtree.PRTree
import org.khelekore.prtree.PointND
import java.util.concurrent.locks.Lock

/**
 * Although the game (as product) can have infinitely many stages/planets/etc., those stages must be manually managed by YOU;
 * this instance only stores the stage that is currently being used.
 */
open class IngameInstance(val batch: SpriteBatch) : Screen {

    open protected val actorMBRConverter = object : MBRConverter<ActorWithBody> {
        override fun getDimensions(): Int = 2
        override fun getMin(axis: Int, t: ActorWithBody): Double =
                when (axis) {
                    0 -> t.hitbox.startX
                    1 -> t.hitbox.startY
                    else -> throw IllegalArgumentException("nonexistent axis $axis for ${dimensions}-dimensional object")
                }

        override fun getMax(axis: Int, t: ActorWithBody): Double =
                when (axis) {
                    0 -> t.hitbox.endX
                    1 -> t.hitbox.endY
                    else -> throw IllegalArgumentException("nonexistent axis $axis for ${dimensions}-dimensional object")
                }
    }

    lateinit var savegameArchive: VirtualDisk
        internal set

    var screenZoom = 1.0f
    val ZOOM_MAXIMUM = 4.0f
    val ZOOM_MINIMUM = 1.0f

    open var consoleHandler: ConsoleWindow = ConsoleWindow()

    var paused: Boolean = false
    val consoleOpened: Boolean
        get() = consoleHandler.isOpened || consoleHandler.isOpening

    var newWorldLoadedLatch = false

    /** For in-world text overlays? e.g. cursor on the ore block and tooltip will say "Malachite" or something */
    open var uiTooltip: UITooltip = UITooltip()
    open var notifier: Notification = Notification()


    init {
        consoleHandler.setPosition(0, 0)
        notifier.setPosition(
                (AppLoader.screenSize.screenW - notifier.width) / 2,
                AppLoader.screenSize.screenH - notifier.height - AppLoader.screenSize.tvSafeGraphicsHeight
        )

        printdbg(this, "New ingame instance ${this.hashCode()}, called from")
        printStackTrace(this)
    }

    open var world: GameWorld = GameWorld.makeNullWorld()
        set(value) {
            val oldWorld = field
            newWorldLoadedLatch = true
            printdbg(this, "Ingame instance ${this.hashCode()}, accepting new world ${value.layerTerrain}; called from")
            printStackTrace(this)
            field = value
            IngameRenderer.setRenderedWorld(value)
            oldWorld.dispose()
        }
    /** how many different planets/stages/etc. are thenre. Whole stages must be manually managed by YOU. */
    var gameworldIndices = ArrayList<Int>()

    /** The actor the game is currently allowing you to control.
     *
     *  Most of the time it'd be the "player", but think about the case where you have possessed
     *  some random actor of the game. Now that actor is now actorNowPlaying, the actual gamer's avatar
     *  (reference ID of 0x91A7E2) (must) stay in the actorContainerActive, but it's not a actorNowPlaying.
     *
     *  Nullability of this property is believed to be unavoidable (trust me!). I'm sorry for the inconvenience.
     */
    open var actorNowPlaying: ActorHumanoid? = null
    /**
     * The actual gamer
     */
    val actorGamer: ActorHumanoid
        get() = getActorByID(Terrarum.PLAYER_REF_ID) as ActorHumanoid

    open var gameInitialised = false
        internal set
    open var gameFullyLoaded = false
        internal set

    val ACTORCONTAINER_INITIAL_SIZE = 64
    val actorContainerActive = SortedArrayList<Actor>(ACTORCONTAINER_INITIAL_SIZE)
    val actorContainerInactive = SortedArrayList<Actor>(ACTORCONTAINER_INITIAL_SIZE)

    var actorsRTree: PRTree<ActorWithBody> = PRTree(actorMBRConverter, 24) // no lateinit!
        protected set

    val terrainChangeQueue = ArrayList<BlockChangeQueueItem>()
    val wallChangeQueue = ArrayList<BlockChangeQueueItem>()
    val wireChangeQueue = ArrayList<BlockChangeQueueItem>() // if 'old' is set and 'new' is blank, it's a wire cutter

    var loadedTime_t = AppLoader.getTIME_T()
        protected set

    override fun hide() {
    }

    override fun show() {
        // the very basic show() implementation

        // add blockmarking_actor into the actorlist
        (CommonResourcePool.get("blockmarking_actor") as BlockMarkerActor).let {
            it.isVisible = false // make sure the actor is invisible on new instance
            try { addNewActor(it) } catch (e: ReferencedActorAlreadyExistsException) {}
        }


        gameInitialised = true
    }

    override fun render(updateRate: Float) {
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun resize(width: Int, height: Int) {
    }

    /**
     * You ABSOLUTELY must call this in your child classes (```super.dispose()```) and the AppLoader to properly
     * dispose of the world, which uses unsafe memory allocation.
     * Failing to do this will result to a memory leak!
     */
    override fun dispose() {
        printdbg(this, "Thank you for properly disposing the world!")
        printdbg(this, "dispose called by")
        printStackTrace(this)

        actorContainerActive.forEach { it.dispose() }
        actorContainerInactive.forEach { it.dispose() }
        world.dispose()
    }

    ////////////
    // EVENTS //
    ////////////

    /**
     * Event for triggering held item's `startPrimaryUse(Float)`
     */
    open fun worldPrimaryClickStart(delta: Float) {
    }

    /**
     * Event for triggering held item's `endPrimaryUse(Float)`
     */
    open fun worldPrimaryClickEnd(delta: Float) {
    }

    /**
     * I have decided that left and right clicks must do the same thing, so no secondary use from now on. --Torvald on 2019-05-26
     *
     * Event for triggering held item's `startSecondaryUse(Float)`
     */
    //open fun worldSecondaryClickStart(delta: Float) { }

    /**
     * I have decided that left and right clicks must do the same thing, so no secondary use from now on. --Torvald on 2019-05-26
     *
     * Event for triggering held item's `endSecondaryUse(Float)`
     */
    //open fun worldSecondaryClickEnd(delta: Float) { }

    /**
     * Event for triggering fixture update when something is placed/removed on the world.
     * Normally only called by GameWorld.setTileTerrain
     *
     * Queueing schema is used to make sure things are synchronised.
     */
    open fun queueTerrainChangedEvent(old: ItemID, new: ItemID, x: Int, y: Int) {
        terrainChangeQueue.add(BlockChangeQueueItem(old, new, x, y))
        //printdbg(this, terrainChangeQueue)
    }

    /**
     * Wall version of terrainChanged() event
     */
    open fun queueWallChangedEvent(old: ItemID, new: ItemID, x: Int, y: Int) {
        wallChangeQueue.add(BlockChangeQueueItem(old, new, x, y))
    }

    /**
     * Wire version of terrainChanged() event
     *
     * @param old previous settings of conduits in bit set format.
     * @param new current settings of conduits in bit set format.
     */
    open fun queueWireChangedEvent(wire: ItemID, isRemoval: Boolean, x: Int, y: Int) {
        wireChangeQueue.add(BlockChangeQueueItem(if (isRemoval) wire else "", if (isRemoval) "" else wire, x, y))
        //printdbg(this, wireChangeQueue)
    }



    ///////////////////////
    // UTILITY FUNCTIONS //
    ///////////////////////

    fun getActorByID(ID: Int): Actor {
        if (actorContainerActive.size == 0 && actorContainerInactive.size == 0)
            throw NoSuchActorWithIDException(ID)

        var actor = actorContainerActive.searchFor(ID) { it.referenceID }
        if (actor == null) {
            actor = actorContainerInactive.searchFor(ID) { it.referenceID }

            if (actor == null) {
                /*JOptionPane.showMessageDialog(
                        null,
                        "Actor with ID $ID does not exist.",
                        null, JOptionPane.ERROR_MESSAGE
                )*/
                throw NoSuchActorWithIDException(ID)
            }
            else
                return actor
        }
        else
            return actor
    }

    //fun SortedArrayList<*>.binarySearch(actor: Actor) = this.toArrayList().binarySearch(actor.referenceID)
    //fun SortedArrayList<*>.binarySearch(ID: Int) = this.toArrayList().binarySearch(ID)

    open fun removeActor(ID: Int) = removeActor(getActorByID(ID))
    /**
     * get index of the actor and delete by the index.
     * we can do this as the list is guaranteed to be sorted
     * and only contains unique values.
     *
     * Any values behind the index will be automatically pushed to front.
     * This is how remove function of [java.util.ArrayList] is defined.
     */
    open fun removeActor(actor: Actor?) {
        if (actor == null) return

        forceRemoveActor(actor)
    }

    open fun forceRemoveActor(actor: Actor) {
        arrayOf(actorContainerActive, actorContainerInactive).forEach { actorContainer ->
            val indexToDelete = actorContainer.searchFor(actor.referenceID) { it.referenceID }
            if (indexToDelete != null) {
                actor.dispose()
                actorContainer.remove(indexToDelete)
            }
        }
    }

    /**
     * Check for duplicates, append actor and sort the list
     */
    open fun addNewActor(actor: Actor?) {
        if (actor == null) return

        if (theGameHasActor(actor.referenceID)) {
            throw ReferencedActorAlreadyExistsException(actor)
        }
        else {
            actorContainerActive.add(actor)
        }
    }

    fun isActive(ID: Int): Boolean =
            if (actorContainerActive.size == 0)
                false
            else
                actorContainerActive.searchFor(ID) { it.referenceID } != null

    fun isInactive(ID: Int): Boolean =
            if (actorContainerInactive.size == 0)
                false
            else
                actorContainerInactive.searchFor(ID) { it.referenceID } != null

    /**
     * actorContainerActive extensions
     */
    fun theGameHasActor(actor: Actor?) = if (actor == null) false else theGameHasActor(actor.referenceID)

    fun theGameHasActor(ID: Int): Boolean =
            isActive(ID) || isInactive(ID)



    data class BlockChangeQueueItem(val old: ItemID, val new: ItemID, val posX: Int, val posY: Int)

    open fun sendNotification(messages: Array<String>) {
        notifier.sendNotification(messages.toList())
    }

    open fun sendNotification(messages: List<String>) {
        notifier.sendNotification(messages)
    }

    open fun sendNotification(singleMessage: String) = sendNotification(listOf(singleMessage))


    open fun setTooltipMessage(message: String?) {
        if (message == null) {
            uiTooltip.setAsClose()
        }
        else {
            if (uiTooltip.isClosed || uiTooltip.isClosing) {
                uiTooltip.setAsOpen()
            }
            uiTooltip.message = message
        }
    }


    // simple euclidean norm, squared
    private val actorDistanceCalculator = DistanceCalculator<ActorWithBody> { t: ActorWithBody, p: PointND ->
        val dist1 = (p.getOrd(0) - t.hitbox.centeredX).sqr() + (p.getOrd(1) - t.hitbox.centeredY).sqr()
        // ROUNDWORLD implementation
        val dist2 = (p.getOrd(0) - (t.hitbox.centeredX - world.width * TerrarumAppConfiguration.TILE_SIZE)).sqr() + (p.getOrd(1) - t.hitbox.centeredY).sqr()
        val dist3 = (p.getOrd(0) - (t.hitbox.centeredX + world.width * TerrarumAppConfiguration.TILE_SIZE)).sqr() + (p.getOrd(1) - t.hitbox.centeredY).sqr()

        minOf(dist1, minOf(dist2, dist3))
    }

    /**
     * @return list of actors under the bounding box given, list may be empty if no actor is under the point.
     */
    fun getActorsAt(startPoint: Point2d, endPoint: Point2d): List<ActorWithBody> {
        val outList = ArrayList<ActorWithBody>()
        try {
            actorsRTree.find(startPoint.x, startPoint.y, endPoint.x, endPoint.y, outList)
        }
        catch (e: NullPointerException) {}
        return outList
    }

    fun getActorsAt(worldX: Double, worldY: Double): List<ActorWithBody> {
        val outList = ArrayList<ActorWithBody>()
        try {
            actorsRTree.find(worldX, worldY, worldX + 1.0, worldY + 1.0, outList)
        }
        catch (e: NullPointerException) {}
        return outList
    }

    /** Will use centre point of the actors
     * @return List of DistanceResult, list may be empty */
    fun findKNearestActors(from: ActorWithBody, maxHits: Int): List<DistanceResult<ActorWithBody>> {
        return actorsRTree.nearestNeighbour(actorDistanceCalculator, null, maxHits, object : PointND {
            override fun getDimensions(): Int = 2
            override fun getOrd(axis: Int): Double = when(axis) {
                0 -> from.hitbox.centeredX
                1 -> from.hitbox.centeredY
                else -> throw IllegalArgumentException("nonexistent axis $axis for ${dimensions}-dimensional object")
            }
        })
    }
    /** Will use centre point of the actors
     * @return Pair of: the actor, distance from the actor; null if none found */
    fun findNearestActors(from: ActorWithBody): DistanceResult<ActorWithBody>? {
        val t = findKNearestActors(from, 1)
        return if (t.isNotEmpty())
            t[0]
        else
            null
    }
}

inline fun Lock.lock(body: () -> Unit) {
    this.lock()
    try {
        body()
    }
    finally {
        this.unlock()
    }
}

class NoSuchActorWithIDException(id: ActorID) : Exception("Actor with ID $id does not exist.")
class NoSuchActorWithRefException(actor: Actor) : Exception("No such actor in the game: $actor")
class ReferencedActorAlreadyExistsException(actor: Actor) : Exception("The actor $actor already exists in the game")
class ProtectedActorRemovalException(whatisit: String) : Exception("Attempted to removed protected actor '$whatisit'")

val INGAME: IngameInstance
    get() = Terrarum.ingame!!

