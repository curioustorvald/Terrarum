package net.torvald.terrarum

import com.badlogic.gdx.Input
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.ActorID
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.ActorWithBody.Companion.PHYS_EPSILON_DIST
import net.torvald.terrarum.gameactors.BlockMarkerActor
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.IngameRenderer
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.modulebasegame.ui.Noticelet
import net.torvald.terrarum.modulebasegame.ui.Notification
import net.torvald.terrarum.modulebasegame.ui.UITooltip
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.savegame.VirtualDisk
import net.torvald.terrarum.ui.ConsoleWindow
import net.torvald.terrarum.ui.Toolkit
import net.torvald.util.CircularArray
import net.torvald.util.SortedArrayList
import org.khelekore.prtree.*
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.concurrent.locks.Lock
import kotlin.math.min

/**
 * Although the game (as product) can have infinitely many stages/planets/etc., those stages must be manually managed by YOU;
 * this instance only stores the stage that is currently being used.
 */
open class IngameInstance(val batch: FlippingSpriteBatch, val isMultiplayer: Boolean = false) : TerrarumGamescreen {

    var WORLD_UPDATE_TIMER = Random().nextInt(1020) + 1L; protected set

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
                    0 -> t.hitbox.endX - PHYS_EPSILON_DIST
                    1 -> t.hitbox.endY - PHYS_EPSILON_DIST
                    else -> throw IllegalArgumentException("nonexistent axis $axis for ${dimensions}-dimensional object")
                }
    }

    /** things to be disposed of when the current instance of the game disposed of */
//    val disposables = HashSet<Disposable>()

    lateinit var worldDisk: VirtualDisk; internal set
    lateinit var playerDisk: VirtualDisk; internal set
    lateinit var worldSavefileName: String; internal set
    lateinit var playerSavefileName: String; internal set
    var worldName: String = "SplinesReticulated"; internal set // worldName is stored as a name of the disk

    var screenZoom = 1.0f
    val ZOOM_MAXIMUM = 4.0f
    val ZOOM_MINIMUM = 1.0f

    open var consoleHandler: ConsoleWindow = ConsoleWindow()

    var paused: Boolean = false; protected set
    var playerControlDisabled = false; protected set
    val consoleOpened: Boolean
        get() = consoleHandler.isOpened || consoleHandler.isOpening

    var newWorldLoadedLatch = false

    /** For in-world text overlays? e.g. cursor on the ore block and tooltip will say "Malachite" or something */
    open var uiTooltip: UITooltip = UITooltip()
    open var notifier: Notification = Notification()
    open var noticelet: Noticelet = Noticelet()

    val deltaTeeBenchmarks = CircularArray<Float>(App.getConfigInt("debug_deltat_benchmark_sample_sizes"), true)

    val uiContainer = UIContainer()

    init {
        consoleHandler.setPosition(0, 0)
        notifier.setPosition(
                (Toolkit.drawWidth - notifier.width) / 2,
                App.scr.height - notifier.height - App.scr.tvSafeGraphicsHeight
        )
        noticelet.setPosition(0, 0)

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
    //var gameworldIndices = ArrayList<Int>()

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
    open lateinit var actorGamer: IngamePlayer

    open var gameInitialised = false
        internal set
    open var gameFullyLoaded = false
        internal set

    val ACTORCONTAINER_INITIAL_SIZE = 64
    val actorContainerActive = SortedArrayList<Actor>(ACTORCONTAINER_INITIAL_SIZE)
    val actorContainerInactive = SortedArrayList<Actor>(ACTORCONTAINER_INITIAL_SIZE)

    val actorAdditionQueue = ArrayList<Triple<Actor, Throwable, (Actor) -> Unit>>() // actor, stacktrace object, onSpawn
    val actorRemovalQueue = ArrayList<Triple<Actor, Throwable, (Actor) -> Unit>>() // actor, stacktrace object, onDespawn

    /**
     * ## BIG NOTE: Calculated actor distance is the **Euclidean distance SQUARED**
     *
     * But when a function does not take the distance (e.g. `actorsRTree.find()`) you must not square the numbers
     */
    var actorsRTree: PRTree<ActorWithBody> = PRTree(actorMBRConverter, 24) // no lateinit!
        protected set

    val terrainChangeQueue = ArrayList<BlockChangeQueueItem?>() // keep it nullable to deal with the concurrentmodification better
    val wallChangeQueue = ArrayList<BlockChangeQueueItem?>()
    val wireChangeQueue = ArrayList<BlockChangeQueueItem?>() // if 'old' is set and 'new' is blank, it's a wire cutter

    val modifiedChunks = Array(16) { TreeSet<Int>() }

    var loadedTime_t = App.getTIME_T()
        protected set

    val blockMarkingActor: BlockMarkerActor
        get() = CommonResourcePool.get("blockmarking_actor") as BlockMarkerActor

    protected lateinit var gameUpdateGovernor: GameUpdateGovernor

    override fun hide() {
    }

    override fun inputStrobed(e: TerrarumKeyboardEvent) {
    }

    override fun show() {
        // the very basic show() implementation
        for (k in Input.Keys.F1..Input.Keys.F12) {
            KeyToggler.forceSet(k, false)
        }

        // add blockmarking_actor into the actorlist
        (CommonResourcePool.get("blockmarking_actor") as BlockMarkerActor).let {
            forceRemoveActor(it)
            forceAddActor(it)
        }

        blockMarkingActor.let {
            it.unsetGhost()
        }

        gameInitialised = true
    }

    private var prerenderCalled = false

    open fun preRender() {

    }

    final override fun render(updateRate: Float) {
        if (!prerenderCalled) {
            preRender()
            prerenderCalled = true
        }

        renderImpl(updateRate)
    }

    open fun renderImpl(updateRate: Float) {

    }

    override fun pause() {
        paused = true
    }

    override fun resume() {
        paused = false
    }

    open fun disablePlayerControl() {
        playerControlDisabled = true
    }

    open fun resumePlayerControl() {
        playerControlDisabled = false
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

        blockMarkingActor.isVisible = false

        actorContainerActive.forEach { it.dispose() }
        actorContainerInactive.forEach { it.dispose() }
        world.dispose()

//        disposables.forEach { it.tryDispose() }
    }

    ////////////
    // EVENTS //
    ////////////

    /**
     * Event for triggering held item's `startPrimaryUse(Float)`
     */
    open fun worldPrimaryClickStart(actor: ActorWithBody, delta: Float) {
    }

    /**
     * Event for triggering held item's `endPrimaryUse(Float)`
     */
    open fun worldPrimaryClickEnd(actor: ActorWithBody, delta: Float) {
    }

    // I have decided that left and right clicks must do the same thing, so no secondary use from now on. --Torvald on 2019-05-26
    // Nevermind: we need to distinguish picking up and using the fixture. --Torvald on 2022-08-26
    /**
     * Event for triggering held item's `startSecondaryUse(Float)`
     */
    open fun worldSecondaryClickStart(actor: ActorWithBody, delta: Float) { }

    // I have decided that left and right clicks must do the same thing, so no secondary use from now on. --Torvald on 2019-05-26
    // Nevermind: we need to distinguish picking up and using the fixture. --Torvald on 2022-08-26
    /***
     * Event for triggering held item's `endSecondaryUse(Float)`
     */
    open fun worldSecondaryClickEnd(actor: ActorWithBody, delta: Float) { }

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


    open fun modified(layer: Int, x: Int, y: Int) {
//        printdbg(this, "Chunk modified: layer $layer ($x, $y)")
        modifiedChunks[layer].add(LandUtil.toChunkNum(world, x, y))
    }

    open fun clearModifiedChunks() {
        modifiedChunks.forEach { it.clear() }
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

    open fun queueActorRemoval(ID: Int) = queueActorRemoval(getActorByID(ID))
    /**
     * get index of the actor and delete by the index.
     * we can do this as the list is guaranteed to be sorted
     * and only contains unique values.
     *
     * Any values behind the index will be automatically pushed to front.
     * This is how remove function of [java.util.ArrayList] is defined.
     */
    open fun queueActorRemoval(actor: Actor?) {
        if (actor == null) return
        actorRemovalQueue.add(Triple(actor, StackTraceRecorder(), {}))
    }

    open fun queueActorRemoval(actor: Actor?, onDespawn: (Actor) -> Unit) {
        if (actor == null) return
        actorRemovalQueue.add(Triple(actor, StackTraceRecorder(), onDespawn))
    }

    protected open fun forceRemoveActor(actor: Actor, caller: Throwable = StackTraceRecorder()) {
        arrayOf(actorContainerActive, actorContainerInactive).forEach { actorContainer ->
            val indexToDelete = actorContainer.searchForIndex(actor.referenceID) { it.referenceID }
            if (indexToDelete != null) {
                actor.dispose()
                actorContainer.removeAt(indexToDelete)
            }
        }
    }

    protected open fun forceAddActor(actor: Actor?, caller: Throwable = StackTraceRecorder()) {
        if (actor == null) return

        if (theGameHasActor(actor.referenceID)) {
            throw ReferencedActorAlreadyExistsException(actor, caller)
        }
        else {
            actorContainerActive.add(actor)
        }
    }

    /**
     * Queue an actor to be added into the world. The actors will be added on the next update-frame and then the queue will be cleared.
     * If the actor is null, this function will do nothing.
     */
    open fun queueActorAddition(actor: Actor?) {
        if (actor == null) return
        actorAdditionQueue.add(Triple(actor, StackTraceRecorder(), {}))
    }

    open fun queueActorAddition(actor: Actor?, onSpawn: (Actor) -> Unit) {
        if (actor == null) return
        actorAdditionQueue.add(Triple(actor, StackTraceRecorder(), onSpawn))
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
//            printdbg(this, "Tooltip close!")
        }
        else {
            if (uiTooltip.isClosed || uiTooltip.isClosing) {
                uiTooltip.setAsOpen()
//                printdbg(this, "Tooltip open!")
            }
            uiTooltip.message = message

//            printStackTrace(this)
        }
    }

    open fun getTooltipMessage(): String {
        return uiTooltip.message
    }

    open fun requestForceSave(callback: () -> Unit) {

    }

    open fun saveTheGame(onSuccessful: () -> Unit, onError: (Throwable) -> Unit) {
        loadedTime_t = App.getTIME_T()
    }

    /**
     * Copies most recent `save` to `save.1`, leaving `save` for overwriting, previous `save.1` will be copied to `save.2`
     */
    fun makeSavegameBackupCopy(file: File) {
        if (!file.exists()) {
            return
        }

        val file1 = File("${file.absolutePath}.1")
        val file2 = File("${file.absolutePath}.2")
        val file3 = File("${file.absolutePath}.3")

        try {
            // do not overwrite clean .2 with dirty .1
            val flags3 = FileInputStream(file3).let { it.skip(49L); val r = it.read(); it.close(); r }
            val flags2 = FileInputStream(file2).let { it.skip(49L); val r = it.read(); it.close(); r }
            if (!(flags3 == 0 && flags2 != 0) || !file3.exists()) Files.move(file2.toPath(), file3.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: NoSuchFileException) {} catch (e: FileNotFoundException) {}
        try {
            // do not overwrite clean .2 with dirty .1
            val flags2 = FileInputStream(file2).let { it.skip(49L); val r = it.read(); it.close(); r }
            val flags1 = FileInputStream(file1).let { it.skip(49L); val r = it.read(); it.close(); r }
            if (!(flags2 == 0 && flags1 != 0) || !file2.exists()) Files.move(file1.toPath(), file2.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: NoSuchFileException) {} catch (e: FileNotFoundException) {}
        try {
            if (file2.exists() && !file3.exists())
                Files.move(file2.toPath(), file3.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            if (file1.exists() && !file2.exists())
                Files.move(file1.toPath(), file2.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)

            file.copyTo(file1, true)
        } catch (e: IOException) {}
    }


    fun makeSavegameBackupCopyAuto(file0: File): File {
        val file1 = File("${file0.absolutePath}.a")
        val file2 = File("${file0.absolutePath}.b")
        val file3 = File("${file0.absolutePath}.c")

        try {
            // do not overwrite clean .2 with dirty .1
            val flags3 = FileInputStream(file3).let { it.skip(49L); val r = it.read(); it.close(); r }
            val flags2 = FileInputStream(file2).let { it.skip(49L); val r = it.read(); it.close(); r }
            if (!(flags3 == 0 && flags2 != 0) || !file3.exists()) Files.move(file2.toPath(), file3.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: NoSuchFileException) {} catch (e: FileNotFoundException) {}
        try {
            // do not overwrite clean .2 with dirty .1
            val flags2 = FileInputStream(file2).let { it.skip(49L); val r = it.read(); it.close(); r }
            val flags1 = FileInputStream(file1).let { it.skip(49L); val r = it.read(); it.close(); r }
            if (!(flags2 == 0 && flags1 != 0) || !file2.exists()) Files.move(file1.toPath(), file2.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: NoSuchFileException) {} catch (e: FileNotFoundException) {}
        try {
            if (file2.exists() && !file3.exists())
                Files.move(file2.toPath(), file3.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            if (file1.exists() && !file2.exists())
                Files.move(file1.toPath(), file2.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)

            file0.copyTo(file1, true)
        } catch (e: IOException) {}

        return file1
    }



    // simple euclidean norm, squared
    private val actorDistanceCalculator = DistanceCalculator<ActorWithBody> { t: ActorWithBody, p: PointND ->
        val dist1 = (p.getOrd(0) - t.hitbox.centeredX).sqr() + (p.getOrd(1) - t.hitbox.centeredY).sqr()
        // ROUNDWORLD implementation
        val dist2 = (p.getOrd(0) - (t.hitbox.centeredX - world.width * TILE_SIZE)).sqr() + (p.getOrd(1) - t.hitbox.centeredY).sqr()
        val dist3 = (p.getOrd(0) - (t.hitbox.centeredX + world.width * TILE_SIZE)).sqr() + (p.getOrd(1) - t.hitbox.centeredY).sqr()

        min(min(dist1, dist2), dist3)
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
            actorsRTree.find(worldX - 0.5, worldY - 0.5, worldX + 0.5, worldY + 0.5, outList)
        }
        catch (e: NullPointerException) {}
        return outList
    }

    /** Will use centre point of the actors
     * HOPEFULLY sorted by the distance...?
     * @return List of DistanceResult (the actor and the distance SQUARED from the actor), list may be empty */
    fun findKNearestActors(from: ActorWithBody, maxHits: Int, nodeFilter: (ActorWithBody) -> Boolean): List<DistanceResult<ActorWithBody>> {
        return actorsRTree.nearestNeighbour(actorDistanceCalculator, nodeFilter, maxHits, object : PointND {
            override fun getDimensions(): Int = 2
            override fun getOrd(axis: Int): Double = when(axis) {
                0 -> from.hitbox.centeredX
                1 -> from.hitbox.centeredY
                else -> throw IllegalArgumentException("nonexistent axis $axis for ${dimensions}-dimensional object")
            }
        })
    }
    /** Will use centre point of the actors
     * @return Pair of: the actor, distance SQUARED from the actor; null if none found */
    fun findNearestActor(from: ActorWithBody, nodeFilter: (ActorWithBody) -> Boolean): DistanceResult<ActorWithBody>? {
        val t = findKNearestActors(from, 1, nodeFilter)
        return if (t.isNotEmpty())
            t[0]
        else
            null
    }

    fun onConfigChange() {
    }

    fun sendItemPickupNoticelet(itemID: ItemID, itemCount: Long) {
        noticelet.sendNotification(itemID, itemCount)
    }

    open val musicStreamer: MusicStreamer = MusicStreamer()
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

class StackTraceRecorder() : Exception("(I'm here to just record the stack trace, move along)")
class NoSuchActorWithIDException(id: ActorID) : Exception("Actor with ID $id does not exist.")
class NoSuchActorWithRefException(actor: Actor) : Exception("No such actor in the game: $actor")
class ReferencedActorAlreadyExistsException(actor: Actor, caller: Throwable) : Exception("The actor $actor already exists in the game", caller)
class ProtectedActorRemovalException(whatisit: String, caller: Throwable) : Exception("Attempted to removed protected actor '$whatisit'", caller)

val INGAME: IngameInstance
    get() = Terrarum.ingame!!

