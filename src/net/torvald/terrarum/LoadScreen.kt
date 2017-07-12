package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import net.torvald.dataclass.HistoryArray
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2017-07-13.
 */
object LoadScreen : ScreenAdapter() {

    private lateinit var actualSceneToBeLoaded: Screen
    private lateinit var sceneLoadingThread: Thread


    private val messages = HistoryArray<String>(20)

    fun setMessage(msg: String) {
        messages.add(msg)
    }



    private var arrowObjPos = 0f // 0 means at starting position, regardless of screen position
    private var arrowObjGlideOffsetX = 0f
    private var arrowObjGlideSize = 0f
    private var arrowGlideSpeed = Terrarum.WIDTH * 1.2f // pixels per sec
    private lateinit var arrowObjTex: TextureRegionPack
    private var glideTimer = 0f
    private var glideDispY = 0f
    private var arrowColours = arrayOf(
            Color(0xff847fff.toInt()),
            Color(0xffc87fff.toInt()),
            Color(0xbffff2ff.toInt()),
            Color(0x7fcaffff)
    )


    var camera = OrthographicCamera(Terrarum.WIDTH.toFloat(), Terrarum.HEIGHT.toFloat())

    fun initViewPort(width: Int, height: Int) {
        //val width = if (width % 1 == 1) width + 1 else width
        //val height = if (height % 1 == 1) height + 1 else width

        // Set Y to point downwards
        camera.setToOrtho(true, width.toFloat(), height.toFloat())

        // Update camera matrix
        camera.update()

        // Set viewport to restrict drawing
        Gdx.gl20.glViewport(0, 0, width, height)
    }



    override fun show() {
        initViewPort(Terrarum.WIDTH, Terrarum.HEIGHT)

        arrowObjTex = TextureRegionPack(Gdx.files.internal("assets/graphics/test_loading_arrow_atlas.tga"), 22, 17)
        arrowObjGlideOffsetX = -arrowObjTex.texture.width.toFloat()
        arrowObjGlideSize = arrowObjTex.texture.width + 1.5f * Terrarum.WIDTH
        glideDispY = Terrarum.HEIGHT - 140f
    }



    private val textColour = Color(0xeeeeeeff.toInt())

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(.157f, .157f, .157f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)


        glideTimer += delta
        if (glideTimer >= arrowObjGlideSize / arrowGlideSpeed) {
            glideTimer -= arrowObjGlideSize / arrowGlideSpeed
        }
        arrowObjPos = glideTimer * arrowGlideSpeed


        Terrarum.batch.inUse {
            it.projectionMatrix = camera.combined


            it.color = textColour
            val textWidth = Terrarum.fontGame.getWidth(Lang["MENU_IO_LOADING"])
            Terrarum.fontGame.draw(it, Lang["MENU_IO_LOADING"], (Terrarum.WIDTH - 2.5f * textWidth).round(), glideDispY - 2f)


            arrowColours.forEachIndexed { index, color ->
                it.color = color
                it.draw(arrowObjTex.get(index, 0), arrowObjPos + arrowObjGlideOffsetX + 22 * index, glideDispY)
            }
        }

    }

    override fun dispose() {
        arrowObjTex.dispose()
    }

    override fun hide() {
        dispose()
    }

    override fun resize(width: Int, height: Int) {
        initViewPort(Terrarum.WIDTH, Terrarum.HEIGHT)
    }
}