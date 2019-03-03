package net.torvald.terrarum.blockstats

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.gameworld.GameWorld

object MinimapComposer {

    // strategy: mosaic the textures, maximum texture size is 4 096.


    private var world: GameWorld = GameWorld.makeNullWorld()

    fun setWorld(world: GameWorld) {
        try {
            if (this.world != world) {
                AppLoader.printdbg(this, "World change detected -- old world: ${this.world.hashCode()}, new world: ${world.hashCode()}")

                // TODO, also set totalWidth/Height
            }
        }
        catch (e: UninitializedPropertyAccessException) {
            // new init, do nothing
        }
        finally {
            this.world = world
        }
    }

    val tempTex = Array(4) { Texture(1,1,Pixmap.Format.RGBA8888) }
    // total size of the minimap. Remember: textures can be mosaic-ed to display full map.
    var totalWidth = 0
    var totalHeight = 0

    init {
        repeat(4) {
            tempTex[it] = Texture(Gdx.files.internal("./assets/testimage.png"))
        }
        totalWidth = tempTex[0].width * 2
        totalHeight = tempTex[0].height * 2
    }

    fun dispose() {
        // tempTex.forEach { it.dispose }
        // minimapPixmaps.forEach { it.dispose }
    }

}