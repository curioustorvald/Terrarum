package net.torvald.terrarum.tests

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import net.torvald.btex.BTeXDocViewer
import net.torvald.btex.BTeXParser
import net.torvald.terrarum.FlippingSpriteBatch
import net.torvald.terrarum.btex.BTeXDocument
import net.torvald.terrarum.gdxClearAndEnableBlend
import net.torvald.terrarum.imagefont.TinyAlphNum
import net.torvald.terrarum.inUse
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.ui.Toolkit
import net.torvald.unicode.EMDASH
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.measureTimeMillis


/**
 * Created by minjaesong on 2023-10-28.
 */
class BTeXTest : ApplicationAdapter() {

    val filePath = "btex.xml"
//    val filePath = "btex_ko.xml"
//    val filePath = "test.xml"
//    val filePath = "literature/en/daniel_defoe_robinson_crusoe.xml"
//    val filePath = "literature/ruRU/anton_chekhov_palata_no_6.xml"
//    val filePath = "literature/koKR/yisang_nalgae.xml"
//    val filePath = "literature/koKR/yisang_geonchukmuhanyukmyeongakche.xml"


    private lateinit var document: BTeXDocument
    private lateinit var documentHandler: BTeXParser.BTeXHandler
    private lateinit var batch: FlippingSpriteBatch
    private lateinit var camera: OrthographicCamera

    private lateinit var bg: TextureRegion

    private val varMap = hashMapOf(
        "terrarumver" to "Alpha 1.3",
        "bucks" to "121687"
    )

    private val errorInfo = AtomicReference<Throwable?>().also {
        it.set(null)
    }

    private val typesetProgress = AtomicInteger(0)
    private val renderProgress = AtomicInteger(0)

    override fun create() {
        Lang
        TinyAlphNum
        Toolkit
        BTeXParser.BTeXHandler.preloadFonts()

        batch = FlippingSpriteBatch(1000)
        camera = OrthographicCamera(1280f, 720f)
        camera.setToOrtho(true) // some elements are pre-flipped, while some are not. The statement itself is absolutely necessary to make edge of the screen as the origin
        camera.update()
        batch.projectionMatrix = camera.combined

        bg = TextureRegion(Texture(Gdx.files.internal("test_assets/real_bg_with_guides.png")))

        val isBookFinalised = filePath.endsWith(".btxbook")

        if (!isBookFinalised) {
            Thread {
                try {
                    measureTimeMillis {
                        val f = BTeXParser.invoke(Gdx.files.internal("./assets/mods/basegame/books/$filePath"), varMap, typesetProgress)
                        document = f.first
                        documentHandler = f.second
                    }.also {
                        println("Time spent on typesetting [ms]: $it")
                    }

                    measureTimeMillis {
                        document.finalise(renderProgress, true)
                    }.also {
                        println("Time spent on finalising [ms]: $it")
                    }

                    /*measureTimeMillis {
                    document.serialise(File("./assets/mods/basegame/books/${filePath.replace(".xml", ".btxbook")}"))
                    }.also {
                        println("Time spent on serialisation [ms]: $it")
                    }*/
                }
                catch (e: Throwable) {
                    errorInfo.set(e)
                    e.printStackTrace()
                }
            }.start()
        }
        else {
            measureTimeMillis {
                document = BTeXDocument.fromFile(Gdx.files.internal("./assets/mods/basegame/books/$filePath"))
            }.also {
                println("Time spent on loading [ms]: $it")
            }
        }


        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if (::viewer.isInitialized)
                    viewer.touchDown(screenX, screenY, pointer, button)
                return true
            }

            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if (::viewer.isInitialized)
                    viewer.touchUp(screenX, screenY, pointer, button)
                return true
            }
        }
    }

    var init = false
    private lateinit var viewer: BTeXDocViewer

    private val drawY = 24

    private fun drawLoadingMsg(batch: SpriteBatch, stage: String) {
        val e = errorInfo.get()
        if (e != null) {
            val st = e.message!!.split('\n').let {
                val idx = it.indexOfFirst { it.startsWith("Caused by: ") }
                it.subList(idx, it.size)
            }
            val th = 14 * st.size
            val tw = st.maxOf { it.length } * 7

            val tx = (1280 - tw) / 2
            val ty = (720 - th) / 2

            batch.color = Color.CORAL
            st.forEachIndexed { i, s ->
                TinyAlphNum.draw(batch, s, tx.toFloat(), ty + 14f*i)
            }
        }
        else {
            batch.color = Color.WHITE
            Toolkit.drawTextCentered(batch, TinyAlphNum, stage, 1280, 0, 354)

            if (stage.lowercase().startsWith("typesetting")) {
                val pgCnt = typesetProgress.get()
                Toolkit.drawTextCentered(batch, TinyAlphNum, "Pages: $pgCnt", 1280, 0, 375)
            }
            else if (stage.lowercase().startsWith("rendering")) {
                val pgCnt = document.pages.size
                val renderCnt = renderProgress.get()
                Toolkit.drawTextCentered(batch, TinyAlphNum, "Pages: $renderCnt/$pgCnt", 1280, 0, 375)
            }
        }
    }

    override fun render() {
        Gdx.graphics.setTitle("BTeXTest $EMDASH F: ${Gdx.graphics.framesPerSecond}")

        gdxClearAndEnableBlend(.063f, .070f, .086f, 1f)


        if (::document.isInitialized) {
            if (!init) {
                init = true
                viewer = BTeXDocViewer(document)
            }
            else {
                if (document.isFinalised || document.fromArchive) {
                    batch.inUse {
                        batch.draw(bg, 0f, 0f)
                        viewer.render(batch, 640f, drawY.toFloat())

                        batch.color = Color.WHITE
                        val pageText = "${viewer.currentPageStr()}/${viewer.pageCount}"
                        Toolkit.drawTextCentered(
                            batch, TinyAlphNum, pageText,
                            1280, 0, drawY + document.pageDimensionHeight + 12
                        )
                    }
                }
                else {
                    batch.inUse {
                        drawLoadingMsg(batch, "Rendering...")
                    }
                }

                // control
                if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT))
                    viewer.prevPage()
                else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT))
                    viewer.nextPage()
                else if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_UP))
                    viewer.gotoFirstPage()
                else if (Gdx.input.isKeyJustPressed(Input.Keys.PAGE_DOWN))
                    viewer.gotoLastPage()
            }
        }
        else {
            batch.inUse {
                drawLoadingMsg(batch, "Typesetting...")
            }
        }
    }


}

fun main() {
    ShaderProgram.pedantic = false

    val appConfig = Lwjgl3ApplicationConfiguration()
    appConfig.useVsync(false)
    appConfig.setResizable(false)
    appConfig.setWindowedMode(1280, 720)
    appConfig.setForegroundFPS(60)
    appConfig.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 2)

    Lwjgl3Application(BTeXTest(), appConfig)
}