package net.torvald.terrarum.modulebasegame.icongen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture

/**
 * Created by minjaesong on 2020-02-11.
 */
object Rasteriser {

    operator fun invoke(size: Int, accessories: IcongenOverlays, colour: Color,
                        mesh: IconGenMesh): Texture {

        val retPixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)








        val ret = Texture(retPixmap)
        retPixmap.dispose()
        return ret

    }

}

// dummy class plz del
class IcongenOverlays {

}

