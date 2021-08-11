package net.torvald.terrarum

import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Queue
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.BlockMarkerActor
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.ui.ConsoleWindow
import net.torvald.util.SortedArrayList
import java.util.concurrent.locks.Lock

/**
 * Although the game (as product) can have infinitely many stages/planets/etc., those stages must be manually managed by YOU;
 * this instance only stores the stage that is currently being used.
 */
open class IngameInstance(val batch: SpriteBatch) : Screen {

    var screenZoom = 1.0f
    val ZOOM_MAXIMUM = 4.0f
    val ZOOM_MINIMUM = 1.0f

    open var consoleHandler: ConsoleWindow = ConsoleWindow()

    var paused: Boolean = false
    val consoleOpened: Boolean
        get() = consoleHandler.isOpened || consoleHandler.isOpening

    init {
        consoleHandler.setPosition(0, 0)

        printdbg(this, "New ingame instance ${this.hashCode()}, called from")
        printStackTrace(this)
    }

    open var world: GameWorld = GameWorld.makeNullWorld()
        set(value) {
            printdbg(this, "Ingame instance ${this.hashCode()}, accepting new world ${value.layerTerrain}; called from")
            printStackTrace(this)

            field = value
        }
    /** how many different planets/stages/etc. are thenre. Whole stages must be manually managed by YOU. */
    var gameworldCount = 0
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

    // FIXME queues will not work; input processing (blocks will queue) and queue consuming cannot be synchronised
    protected val terrainChangeQueue = ArrayList<BlockChangeQueueItem>()
    protected val wallChangeQueue = ArrayList<BlockChangeQueueItem>()
    protected val wireChangeQueue = ArrayList<BlockChangeQueueItem>() // if 'old' is set and 'new' is blank, it's a wire cutter

    override fun hide() {
    }

    override fun show() {
        // the very basic show() implementation

        // add blockmarking_actor into the actorlist
        (CommonResourcePool.get("blockmarking_actor") as BlockMarkerActor).let {
            it.isVisible = false // make sure the actor is invisible on new instance
            try { addNewActor(it) } catch (e: Error) {}
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
        printdbg(this, "Terrain change enqueued: ${BlockChangeQueueItem(old, new, x, y)}")
        printdbg(this, terrainChangeQueue)
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
    }



    ///////////////////////
    // UTILITY FUNCTIONS //
    ///////////////////////

    fun getActorByID(ID: Int): Actor {
        if (actorContainerActive.size == 0 && actorContainerInactive.size == 0)
            throw IllegalArgumentException("Actor with ID $ID does not exist.")

        var actor = actorContainerActive.searchFor(ID) { it.referenceID }
        if (actor == null) {
            actor = actorContainerInactive.searchFor(ID) { it.referenceID }

            if (actor == null) {
                /*JOptionPane.showMessageDialog(
                        null,
                        "Actor with ID $ID does not exist.",
                        null, JOptionPane.ERROR_MESSAGE
                )*/
                throw IllegalArgumentException("Actor with ID $ID does not exist.")
            }
            else
                return actor
        }
        else
            return actor
    }

    //fun SortedArrayList<*>.binarySearch(actor: Actor) = this.toArrayList().binarySearch(actor.referenceID!!)
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

        val indexToDelete = actorContainerActive.searchFor(actor.referenceID!!) { it.referenceID!! }
        if (indexToDelete != null) {
            actorContainerActive.remove(indexToDelete)
        }
    }

    /**
     * Check for duplicates, append actor and sort the list
     */
    open fun addNewActor(actor: Actor?) {
        if (actor == null) return

        if (theGameHasActor(actor.referenceID!!)) {
            throw Error("The actor $actor already exists in the game")
        }
        else {
            actorContainerActive.add(actor)
        }
    }

    fun isActive(ID: Int): Boolean =
            if (actorContainerActive.size == 0)
                false
            else
                actorContainerActive.searchFor(ID) { it.referenceID!! } != null

    fun isInactive(ID: Int): Boolean =
            if (actorContainerInactive.size == 0)
                false
            else
                actorContainerInactive.searchFor(ID) { it.referenceID!! } != null

    /**
     * actorContainerActive extensions
     */
    fun theGameHasActor(actor: Actor?) = if (actor == null) false else theGameHasActor(actor.referenceID!!)

    fun theGameHasActor(ID: Int): Boolean =
            isActive(ID) || isInactive(ID)




    data class BlockChangeQueueItem(val old: ItemID, val new: ItemID, val posX: Int, val posY: Int)

    open fun sendNotification(messages: Array<String>) {}
    open fun sendNotification(messages: List<String>) {}
    open fun sendNotification(singleMessage: String) {}
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
