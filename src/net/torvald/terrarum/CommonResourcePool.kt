package net.torvald.terrarum

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.Queue
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.unsafe.UnsafePtr
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by minjaesong on 2019-03-10.
 */
object CommonResourcePool {

    private val loadingList = Queue<ResourceLoadingDescriptor>()
    private val pool = ConcurrentHashMap<String, Any>()
    private val poolKillFun = HashMap<String, ((Any) -> Unit)?>()
    private var loadCounter = -1 // using counters so that the loading can be done on separate thread (gg if the asset requires GL context to be loaded)
    val loaded: Boolean
        get() = loadCounter <= 0 && slowLoadingRemaining.get() == 0

    @Volatile private var glThread: Thread? = null

    private val glDispatchQueue = ConcurrentLinkedQueue<Pair<List<ResourceLoadingDescriptor>, CountDownLatch>>()
    private val glRunnableQueue = ConcurrentLinkedQueue<Pair<() -> Unit, CountDownLatch>>()

    private val slowLoadingQueue = ConcurrentLinkedQueue<ResourceLoadingDescriptor>()
    private val slowLoadingRemaining = AtomicInteger(0)
    private val slowLoadingTotal = AtomicInteger(0)

    /** 0.0 = not started yet, 1.0 = all done. Only meaningful during / after [loadAllSlowly]. */
    val loadingProgress: Float
        get() {
            val total = slowLoadingTotal.get()
            if (total == 0) return 0f
            val remaining = slowLoadingRemaining.get()
            return (total - remaining).toFloat() / total.toFloat()
        }

    fun setGLThread(thread: Thread) {
        glThread = thread
    }

    fun isOnGLThread(): Boolean {
        return glThread == null || Thread.currentThread() == glThread
    }

    /**
     * Runs [block] on the GL thread, blocking the calling thread until it completes.
     * If already on the GL thread, runs [block] directly.
     */
    fun <T> runOnGLThread(block: () -> T): T {
        if (isOnGLThread()) return block()
        var result: Any? = null
        val latch = CountDownLatch(1)
        glRunnableQueue.add(Pair({ result = block() }, latch))
        latch.await()
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    init {
        addToLoadingList("itemplaceholder_16") {
            TextureRegion(Texture(AssetCache.getFileHandle("item_kari_16.tga"))).also { it.flip(false, false) }
        }
        addToLoadingList("itemplaceholder_24") {
            TextureRegion(Texture(AssetCache.getFileHandle("item_kari_24.tga"))).also { it.flip(false, false) }
        }
        addToLoadingList("itemplaceholder_32") {
            TextureRegion(Texture(AssetCache.getFileHandle("item_kari_32.tga"))).also { it.flip(false, false) }
        }
        addToLoadingList("itemplaceholder_48") {
            TextureRegion(Texture(AssetCache.getFileHandle("item_kari_48.tga"))).also { it.flip(false, false) }
        }
        /*addToLoadingList("test_texture") {
            TextureRegion(Texture("assets/test_texture.tga")).also { it.flip(false, false) }
        }*/
        loadAll()
    }

    fun resourceExists(name: String): Boolean {
        loadingList.forEach {
            if (it.name == name) return true
        }
        pool.forEach {
            if (it.key == name) return true
        }
        return false
    }

    /**
     * Following objects doesn't need destroy function:
     * - com.badlogic.gdx.utils.Disposable
     * - com.badlogic.gdx.graphics.Texture
     * - com.badlogic.gdx.graphics.g2d.TextureRegion
     * - net.torvald.UnsafePtr
     */
    fun addToLoadingList(identifier: String, loadFunction: () -> Any) {
        CommonResourcePool.addToLoadingList(identifier, loadFunction, null)
    }

    /**
     * Following objects doesn't need destroy function:
     * - com.badlogic.gdx.utils.Disposable
     * - com.badlogic.gdx.graphics.Texture
     * - com.badlogic.gdx.graphics.g2d.TextureRegion
     * - net.torvald.UnsafePtr
     */
    fun addToLoadingList(identifier: String, loadFunction: () -> Any, destroyFunction: ((Any) -> Unit)?) {
        // check if resource is already there
        if (!resourceExists(identifier)) {
            loadingList.addFirst(ResourceLoadingDescriptor(identifier, loadFunction, destroyFunction))

            if (loadCounter == -1)
                loadCounter = 1
            else
                loadCounter += 1
        }
    }

    /**
     * Consumes the loading list. After the load, the list will be empty.
     * When called from a non-GL thread, dispatches the actual loading to the GL thread and blocks until complete.
     */
    fun loadAll() {
        if (loaded) return
        if (loadingList.isEmpty) return

        // Drain the loadingList into a local list
        val batch = mutableListOf<ResourceLoadingDescriptor>()
        while (!loadingList.isEmpty) {
            batch.add(loadingList.removeFirst())
        }

        if (isOnGLThread()) {
            // Load directly on GL thread
            for ((name, loadfun, killfun) in batch) {
                pool[name] = loadfun.invoke()
                poolKillFun[name] = killfun
                loadCounter -= 1
            }
        }
        else {
            // Dispatch to GL thread and block until done
            val latch = CountDownLatch(1)
            glDispatchQueue.add(batch to latch)
            latch.await()
        }
    }

    /**
     * Moves all pending items in the loading list to the slow loading queue,
     * then blocks until the GL thread has processed all of them (one per frame via [update]).
     */
    fun loadAllSlowly() {
        while (!loadingList.isEmpty) {
            val desc = loadingList.removeFirst()
            slowLoadingQueue.add(desc)
            slowLoadingRemaining.incrementAndGet()
            slowLoadingTotal.incrementAndGet()
        }
        // Block until the GL thread has processed all slow items
        while (slowLoadingRemaining.get() > 0) {
            Thread.sleep(16)
        }
    }

    /**
     * Called every frame from App.render() on the GL thread.
     * Processes dispatched loadAll() requests and one slow-loading item per frame.
     */
    fun update() {
//        printdbg(this, "CommonResPool update!")
        // 1. Process all immediate dispatch requests (from loadAll() on background thread)
        while (true) {
            val request = glDispatchQueue.poll() ?: break
            val (batch, latch) = request
            for ((name, loadfun, killfun) in batch) {
                pool[name] = loadfun.invoke()
                poolKillFun[name] = killfun
                loadCounter -= 1
            }
            latch.countDown()
        }

        // 2. Process all generic GL runnables (from runOnGLThread() on background thread)
        while (true) {
            val (runnable, latch) = glRunnableQueue.poll() ?: break
            runnable()
            latch.countDown()
        }

        // 3. Process one item from the slow loading queue (timesliced)
        val desc = slowLoadingQueue.poll()
        if (desc != null) {
            val (name, loadfun, killfun) = desc
            pool[name] = loadfun.invoke()
            poolKillFun[name] = killfun
            loadCounter -= 1
            slowLoadingRemaining.decrementAndGet()
        }
    }

    operator fun get(identifier: String): Any {
        return pool[identifier]!!
    }

    fun getOrNull(name: String) = pool[name]
    fun getOrPut(name: String, loadfun: () -> Any) = CommonResourcePool.getOrPut(name, loadfun, null)
        fun getOrPut(name: String, loadfun: () -> Any, killfun: ((Any) -> Unit)?): Any {
        if (pool.containsKey(name)) return pool[name]!!
        if (isOnGLThread()) {
            pool[name] = loadfun.invoke()
            poolKillFun[name] = killfun
        }
        else {
            val latch = CountDownLatch(1)
            glDispatchQueue.add(listOf(ResourceLoadingDescriptor(name, loadfun, killfun)) to latch)
            latch.await()
        }
        return pool[name]!!
    }

    inline fun <reified T> getAs(identifier: String) = get(identifier) as T
    fun getAsTextureRegionPack(identifier: String) = getAs<TextureRegionPack>(identifier)
    fun getAsTextureRegion(identifier: String) = getAs<TextureRegion>(identifier)
    fun getAsTexture(identifier: String) = getAs<Texture>(identifier)
    fun getAsItemSheet(identifier: String) = getAs<ItemSheet>(identifier)

    fun dispose() {
        pool.forEach { (name, u) ->
            try {
                when {
                    u is Disposable -> u.dispose()
                    u is Texture -> u.dispose()
                    u is TextureRegion -> u.texture.dispose()
                    u is UnsafePtr     -> u.destroy()
                    else               -> poolKillFun[name]?.invoke(u)
                }
            }
            catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    private data class ResourceLoadingDescriptor(
            val name: String,
            val loadfun: () -> Any,
            val killfun: ((Any) -> Unit)? = null
    )
}
