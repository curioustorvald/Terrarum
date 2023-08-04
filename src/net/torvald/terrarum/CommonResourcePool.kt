package net.torvald.terrarum

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.Queue
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.unsafe.UnsafePtr

/**
 * Created by minjaesong on 2019-03-10.
 */
object CommonResourcePool {

    private val loadingList = Queue<ResourceLoadingDescriptor>()
    private val pool = HashMap<String, Any>()
    private val poolKillFun = HashMap<String, ((Any) -> Unit)?>()
    //private val typesMap = HashMap<String, Class<*>>()
    private var loadCounter = -1 // using counters so that the loading can be done on separate thread (gg if the asset requires GL context to be loaded)
    val loaded: Boolean // see if there's a thing to load
        get() = loadCounter == 0

    init {
        addToLoadingList("itemplaceholder_16") {
            TextureRegion(Texture("assets/item_kari_16.tga")).also { it.flip(false, false) }
        }
        addToLoadingList("itemplaceholder_24") {
            TextureRegion(Texture("assets/item_kari_24.tga")).also { it.flip(false, false) }
        }
        addToLoadingList("itemplaceholder_32") {
            TextureRegion(Texture("assets/item_kari_32.tga")).also { it.flip(false, false) }
        }
        addToLoadingList("itemplaceholder_48") {
            TextureRegion(Texture("assets/item_kari_48.tga")).also { it.flip(false, false) }
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
     * Consumes the loading list. After the load, the list will be empty
     */
    fun loadAll() {
        if (loaded) return

        while (!loadingList.isEmpty) {
            val (name, loadfun, killfun) = loadingList.removeFirst()

            // no need for the collision checking; quarantine is done when the loading list is being appended
            /*if (pool.containsKey(name)) {
                throw IllegalArgumentException("Assets with identifier '$name' already exists.")
            }*/

            //typesMap[name] = type
            pool[name] = loadfun.invoke()
            poolKillFun[name] = killfun

            loadCounter -= 1
        }
    }

    operator fun get(identifier: String): Any {
        return pool[identifier]!!
    }

    fun getOrNull(name: String) = pool[name]
    fun getOrPut(name: String, loadfun: () -> Any) = CommonResourcePool.getOrPut(name, loadfun, null)
        fun getOrPut(name: String, loadfun: () -> Any, killfun: ((Any) -> Unit)?): Any {
        if (pool.containsKey(name)) return pool[name]!!
        pool[name] = loadfun.invoke()
        poolKillFun[name] = killfun
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