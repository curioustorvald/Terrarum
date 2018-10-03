package net.torvald.terrarum

import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.ui.ConsoleWindow
import java.util.ArrayList
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import javax.swing.JOptionPane

/**
 * Although the game (as product) can have infinitely many stages/planets/etc., those stages must be manually managed by YOU;
 * this instance only stores the stage that is currently being used.
 */
open class IngameInstance(val batch: SpriteBatch) : Screen {

    var screenZoom = 1.0f
    val ZOOM_MAXIMUM = 4.0f
    val ZOOM_MINIMUM = 0.5f

    open lateinit var consoleHandler: ConsoleWindow

    open lateinit var world: GameWorld
    /** how many different planets/stages/etc. are thenre. Whole stages must be manually managed by YOU. */
    var gameworldCount = 0
    /** The actor the game is currently allowing you to control.
     *
     *  Most of the time it'd be the "player", but think about the case where you have possessed
     *  some random actor of the game. Now that actor is now actorNowPlaying, the actual gamer's avatar
     *  (reference ID of 0x91A7E2) (must) stay in the actorContainer, but it's not a actorNowPlaying.
     *
     *  Nullability of this property is believed to be unavoidable (trust me!). I'm sorry for the inconvenience.
     */
    open var actorNowPlaying: ActorHumanoid? = null

    val ACTORCONTAINER_INITIAL_SIZE = 64
    val actorContainer = ArrayList<Actor>(ACTORCONTAINER_INITIAL_SIZE)
    val actorContainerInactive = ArrayList<Actor>(ACTORCONTAINER_INITIAL_SIZE)

    override fun hide() {
    }

    override fun show() {
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



    ///////////////////////
    // UTILITY FUNCTIONS //
    ///////////////////////

    fun getActorByID(ID: Int): Actor {
        if (actorContainer.size == 0 && actorContainerInactive.size == 0)
            throw IllegalArgumentException("Actor with ID $ID does not exist.")

        var index = actorContainer.binarySearch(ID)
        if (index < 0) {
            index = actorContainerInactive.binarySearch(ID)

            if (index < 0) {
                JOptionPane.showMessageDialog(
                        null,
                        "Actor with ID $ID does not exist.",
                        null, JOptionPane.ERROR_MESSAGE
                )
                throw IllegalArgumentException("Actor with ID $ID does not exist.")
            }
            else
                return actorContainerInactive[index]
        }
        else
            return actorContainer[index]
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

        val indexToDelete = actorContainer.binarySearch(actor.referenceID!!)
        if (indexToDelete >= 0) {
            actorContainer.removeAt(indexToDelete)
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
            actorContainer.add(actor)
            insertionSortLastElem(actorContainer) // we can do this as we are only adding single actor
        }
    }

    fun isActive(ID: Int): Boolean =
            if (actorContainer.size == 0)
                false
            else
                actorContainer.binarySearch(ID) >= 0

    fun isInactive(ID: Int): Boolean =
            if (actorContainerInactive.size == 0)
                false
            else
                actorContainerInactive.binarySearch(ID) >= 0

    /**
     * actorContainer extensions
     */
    fun theGameHasActor(actor: Actor?) = if (actor == null) false else theGameHasActor(actor.referenceID!!)

    fun theGameHasActor(ID: Int): Boolean =
            isActive(ID) || isInactive(ID)




    fun insertionSortLastElem(arr: ArrayList<Actor>) {
        lock(ReentrantLock()) {
            var j = arr.lastIndex - 1
            val x = arr.last()
            while (j >= 0 && arr[j] > x) {
                arr[j + 1] = arr[j]
                j -= 1
            }
            arr[j + 1] = x
        }
    }

    inline fun lock(lock: Lock, body: () -> Unit) {
        lock.lock()
        try {
            body()
        }
        finally {
            lock.unlock()
        }
    }
}