package net.torvald.terrarum

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2023-06-06.
 */
class ItemSheet(ref: FileHandle, tileW: Int = 48, tileH: Int = 48) : Disposable {

    private val textureRegionPack = TextureRegionPack(ref, tileW, tileH + 1)

    init {
        val pixmap = Pixmap(ref)
        for (y in 0 until textureRegionPack.verticalCount) {
            for (x in 0 until textureRegionPack.horizontalCount) {
                var w = 0
                var h = 0
                for (i in 0..7) {
                    // width
                    w = w or (pixmap.getPixel(x * tileW + i, y * (tileH + 1)).and(255) > 127).toInt(7 - i)
                    // height
                    h = h or (pixmap.getPixel(x * tileW + i + 8, y * (tileH + 1)).and(255) > 127).toInt(7 - i)
                }

                textureRegionPack.get(x, y).apply {
                    this.setRegion(x * tileW, y * (tileH + 1) + 1, w, h)
                }

//                println("[ItemSheet] ${ref.path()} ($x,$y) dim ($w,$h)")
            }
        }
        pixmap.dispose()
    }

    val horizontalCount = textureRegionPack.horizontalCount
    val verticalCount = textureRegionPack.verticalCount

    fun get(x: Int, y: Int) = textureRegionPack.get(x, y)

    fun forEach(action: (TextureRegion) -> Unit) = textureRegionPack.regions.forEach(action)

    override fun dispose() {
        textureRegionPack.dispose()
    }
}