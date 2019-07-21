package net.torvald.terrarum

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.Queue
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2019-03-10.
 */
object CommonResourcePool {

    private val loadingList = Queue<Pair<String, () -> Any>>()
    private val pool = HashMap<String, Any>()
    //private val typesMap = HashMap<String, Class<*>>()
    private var loadCounter = -1 // using counters so that the loading can be done on separate thread (gg if the asset requires GL context to be loaded)
    val loaded: Boolean
        get() = loadCounter == 0

    init {
        addToLoadingList("itemplaceholder_16") {
            val t = TextureRegion(Texture("assets/item_kari_16.tga"))
            t.flip(false, true)
            /*return*/t
        }
        addToLoadingList("itemplaceholder_24") {
            val t = TextureRegion(Texture("assets/item_kari_24.tga"))
            t.flip(false, true)
            /*return*/t
        }
        addToLoadingList("itemplaceholder_32") {
            val t = TextureRegion(Texture("assets/item_kari_32.tga"))
            t.flip(false, true)
            /*return*/t
        }
        addToLoadingList("itemplaceholder_48") {
            val t = TextureRegion(Texture("assets/item_kari_48.tga"))
            t.flip(false, true)
            /*return*/t
        }
        addToLoadingList("test_texture") {
            val t = TextureRegion(Texture("assets/test_texture.tga"))
            t.flip(false, true)
            /*return*/t
        }
    }

    fun addToLoadingList(identifier: String, loadFunction: () -> Any) {
        loadingList.addFirst(identifier to loadFunction)

        if (loadCounter == -1)
            loadCounter = 1
        else
            loadCounter += 1
    }

    /**
     * You are supposed to call this function only once.
     */
    fun loadAll() {
        if (loaded) throw IllegalStateException("Assets are already loaded and shipped out :p")

        while (!loadingList.isEmpty) {
            val (name, loadfun) = loadingList.removeFirst()

            if (pool.containsKey(name)) {
                throw IllegalArgumentException("Assets with identifier '$name' already exists.")
            }

            //typesMap[name] = type
            pool[name] = loadfun.invoke()

            loadCounter -= 1
        }
    }

    operator fun get(identifier: String): Any {
        return pool[identifier]!!
    }

    inline fun <reified T> getAs(identifier: String) = get(identifier) as T
    fun getAsTextureRegionPack(identifier: String) = getAs<TextureRegionPack>(identifier)
    fun getAsTextureRegion(identifier: String) = getAs<TextureRegion>(identifier)
    fun getAsTexture(identifier: String) = getAs<Texture>(identifier)

    fun dispose() {
        pool.forEach { _, u ->
            try {
                if (u is Disposable)
                    u.dispose()
                else if (u is Texture)
                    u.dispose()
                else if (u is TextureRegion)
                    u.texture.dispose()
                // TODO
            }
            catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}