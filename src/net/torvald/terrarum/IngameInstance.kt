package net.torvald.terrarum

import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Queue
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.ui.ConsoleWindow
import net.torvald.util.SortedArrayList
import java.util.*
import java.util.concurrent.locks.Lock

/**
 * Although the game (as product) can have infinitely many stages/planets/etc., those stages must be manually managed by YOU;
 * this instance only stores the stage that is currently being used.
 */
open class IngameInstance(val batch: SpriteBatch) : Screen {

    var screenZoom = 1.0f
    val ZOOM_MAXIMUM = 4.0f
    val ZOOM_MINIMUM = 0.5f

    open lateinit var consoleHandler: ConsoleWindow

    open var world: GameWorld = GameWorld.makeNullWorld()
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

    protected val terrainChangeQueue = Queue<BlockChangeQueueItem>()
    protected val wallChangeQueue = Queue<BlockChangeQueueItem>()
    protected val wireChangeQueue = Queue<BlockChangeQueueItem>()

    override fun hide() {
    }

    override fun show() {
        // the very basic show() implementation
        gameInitialised = true
    }

    override fun render(delta: Float) {
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun dispose() {
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
     * Event for triggering held item's `startSecondaryUse(Float)`
     */
    open fun worldSecondaryClickStart(delta: Float) {
    }

    /**
     * Event for triggering held item's `endSecondaryUse(Float)`
     */
    open fun worldSecondaryClickEnd(delta: Float) {
    }

    /**
     * Event for triggering fixture update when something is placed/removed on the world.
     * Normally only called by GameWorld.setTileTerrain
     *
     * Queueing schema is used to make sure things are synchronised.
     */
    open fun queueTerrainChangedEvent(old: Int, new: Int, position: Long) {
        val (x, y) = LandUtil.resolveBlockAddr(world, position)
        terrainChangeQueue.addFirst(BlockChangeQueueItem(old, new, x, y))
    }

    /**
     * Wall version of terrainChanged() event
     */
    open fun queueWallChangedEvent(old: Int, new: Int, position: Long) {
        val (x, y) = LandUtil.resolveBlockAddr(world, position)
        wallChangeQueue.addFirst(BlockChangeQueueItem(old, new, x, y))
    }

    /**
     * Wire version of terrainChanged() event
     */
    open fun queueWireChangedEvent(old: Int, new: Int, position: Long) {
        val (x, y) = LandUtil.resolveBlockAddr(world, position)
        wireChangeQueue.addFirst(BlockChangeQueueItem(old, new, x, y))
    }



    ///////////////////////
    // UTILITY FUNCTIONS //
    ///////////////////////

    fun getActorByID(ID: Int): Actor {
        if (actorContainerActive.size == 0 && actorContainerInactive.size == 0)
            throw IllegalArgumentException("Actor with ID $ID does not exist.")

        var index = actorContainerActive.binarySearch(ID)
        if (index < 0) {
            index = actorContainerInactive.binarySearch(ID)

            if (index < 0) {
                /*JOptionPane.showMessageDialog(
                        null,
                        "Actor with ID $ID does not exist.",
                        null, JOptionPane.ERROR_MESSAGE
                )*/
                throw IllegalArgumentException("Actor with ID $ID does not exist.")
            }
            else
                return actorContainerInactive[index]
        }
        else
            return actorContainerActive[index]
    }

    fun ArrayList<*>.binarySearch(actor: Actor) = this.binarySearch(actor.referenceID!!)

    fun ArrayList<*>.binarySearch(ID: Int): Int {
        // code from collections/Collections.kt
        var low = 0
        var high = this.size - 1

        while (low <= high) {
            val mid = (low + high).ushr(1) // safe from overflows

            val midVal = get(mid)!!

            if (ID > midVal.hashCode())
                low = mid + 1
            else if (ID < midVal.hashCode())
                high = mid - 1
            else
                return mid // key found
        }
        return -(low + 1)  // key not found
    }

    fun SortedArrayList<*>.binarySearch(actor: Actor) = this.toArrayList().binarySearch(actor.referenceID!!)
    fun SortedArrayList<*>.binarySearch(ID: Int) = this.toArrayList().binarySearch(ID)

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

        val indexToDelete = actorContainerActive.binarySearch(actor.referenceID!!)
        if (indexToDelete >= 0) {
            actorContainerActive.removeAt(indexToDelete)
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
                actorContainerActive.binarySearch(ID) >= 0

    fun isInactive(ID: Int): Boolean =
            if (actorContainerInactive.size == 0)
                false
            else
                actorContainerInactive.binarySearch(ID) >= 0

    /**
     * actorContainerActive extensions
     */
    fun theGameHasActor(actor: Actor?) = if (actor == null) false else theGameHasActor(actor.referenceID!!)

    fun theGameHasActor(ID: Int): Boolean =
            isActive(ID) || isInactive(ID)




    data class BlockChangeQueueItem(val old: Int, val new: Int, val posX: Int, val posY: Int)
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
