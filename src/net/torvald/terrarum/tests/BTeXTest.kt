package net.torvald.terrarum.tests

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import net.torvald.btex.BTeXParser
import net.torvald.terrarum.FlippingSpriteBatch
import net.torvald.terrarum.btex.BTeXDocument
import net.torvald.terrarum.ceilToInt
import net.torvald.terrarum.gdxClearAndEnableBlend
import net.torvald.terrarum.inUse
import net.torvald.unicode.EMDASH
import java.io.File
import kotlin.system.measureTimeMillis


/**
 * Created by minjaesong on 2023-10-28.
 */
class BTeXTest : ApplicationAdapter() {

    val filePath = "btex.xml"
//    val filePath = "test.xml"
//    val filePath = "literature/en/daniel_defoe_robinson_crusoe.xml"
//    val filePath = "literature/ruRU/anton_chekhov_palata_no_6.xml"
//    val filePath = "literature/koKR/yisang_nalgae.xml"


    private lateinit var document: BTeXDocument
    private lateinit var documentHandler: BTeXParser.BTeXHandler
    private lateinit var batch: FlippingSpriteBatch
    private lateinit var camera: OrthographicCamera

    private lateinit var bg: TextureRegion

    private val varMap = hashMapOf(
        "terrarumver" to "Alpha 1.3",
        "bucks" to "121687"
    )

    override fun create() {
        batch = FlippingSpriteBatch(1000)
        camera = OrthographicCamera(1280f, 720f)
        camera.setToOrtho(true) // some elements are pre-flipped, while some are not. The statement itself is absolutely necessary to make edge of the screen as the origin
        camera.update()
        batch.projectionMatrix = camera.combined

        bg = TextureRegion(Texture(Gdx.files.internal("test_assets/real_bg_with_guides.png")))

        val isBookFinalised = filePath.endsWith(".btexbin")

        if (!isBookFinalised) {
            measureTimeMillis {
                val f = BTeXParser.invoke(Gdx.files.internal("./assets/mods/basegame/books/$filePath"), varMap)
                document = f.first
                documentHandler = f.second
            }.also {
                println("Time spent on typesetting [ms]: $it")
            }

            /*measureTimeMillis {
                document.finalise()
                documentHandler.dispose()
            }.also {
                println("Time spent on finalising [ms]: $it")
            }

            measureTimeMillis {
                document.serialise(File("./assets/mods/basegame/books/${filePath.replace(".xml", ".btexbin")}"))
            }.also {
                println("Time spent on serialisation [ms]: $it")
            }*/
        }
        else {
            measureTimeMillis {
                document = BTeXDocument.fromFile(Gdx.files.internal("./assets/mods/basegame/books/$filePath"))
            }.also {
                println("Time spent on loading [ms]: $it")
            }
        }
    }

    private var scroll = 0

    val pageGap = 6

    override fun render() {
        Gdx.graphics.setTitle("BTeXTest $EMDASH F: ${Gdx.graphics.framesPerSecond}")

        gdxClearAndEnableBlend(.063f, .070f, .086f, 1f)

        val drawX = (1280 - (pageGap + document.pageDimensionWidth*2)) / 2
        val drawY = 24

        batch.inUse {
            batch.color = Color.WHITE
            batch.draw(bg, 0f, 0f)

            if (scroll - 1 in document.pageIndices)
                document.render(0f, batch, scroll - 1, drawX, drawY)
            if (scroll in document.pageIndices)
                document.render(0f, batch, scroll, drawX + (6 + document.pageDimensionWidth), drawY)
        }


        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT))
            scroll = (scroll - 2).coerceAtLeast(0)
        else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT))
            scroll = (scroll + 2).coerceAtMost(document.pageIndices.endInclusive.toFloat().div(2f).ceilToInt().times(2))
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