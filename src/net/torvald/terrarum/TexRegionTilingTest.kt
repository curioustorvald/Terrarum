package net.torvald.terrarum

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.worlddrawer.BlocksDrawer
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream

/**
 * Created by minjaesong on 2017-07-14.
 */

fun main(args: Array<String>) { // LWJGL 3 won't work? java.lang.VerifyError
    val config = LwjglApplicationConfiguration()
    //config.useGL30 = true
    config.vSyncEnabled = false
    config.resizable = false
    config.width = 1072
    config.height = 742
    config.foregroundFPS = 9999
    LwjglApplication(TexRegionTilingTest, config)
}

object TexRegionTilingTest : ApplicationAdapter() {

    lateinit var batch: SpriteBatch
    lateinit var tilesTerrain: TextureRegionPack



    override fun render() {
        Gdx.gl.glClearColor(.094f, .094f, .094f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)


        val tile = BlocksDrawer.tilesTerrain.get(0, 1)

        tile.texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)


        batch.inUse {
            batch.draw(tile.texture, 10f, 10f, TILE_SIZE * 5f, TILE_SIZE * 5f, 0, 16, TILE_SIZE, TILE_SIZE, false, false)
        }
    }

    private val TILE_SIZE: Int = 16

    override fun create() {
        batch = SpriteBatch()



        // hard-coded as tga.gz
        val gzFileList = listOf("blocks/terrain.tga.gz", "blocks/wire.tga.gz")
        val gzTmpFName = listOf("tmp_terrain.tga", "tmp_wire.tga")
        // unzip GZIP temporarily
        gzFileList.forEachIndexed { index, filename ->
            val terrainTexFile = Gdx.files.internal("assets/modules/basegame/" + filename)
            val gzi = GZIPInputStream(terrainTexFile.read(8192))
            val wholeFile = gzi.readBytes()
            gzi.close()
            val fos = BufferedOutputStream(FileOutputStream(gzTmpFName[index]))
            fos.write(wholeFile)
            fos.flush()
            fos.close()
        }


        val terrainPixMap = Pixmap(Gdx.files.internal(gzTmpFName[0]))
        tilesTerrain = TextureRegionPack(Texture(terrainPixMap), TILE_SIZE, TILE_SIZE)
        tilesTerrain.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    }

    override fun dispose() {
        super.dispose()
    }
}