package net.torvald.terrarum.modulecomputers.virtualcomputer.standalone

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.modulecomputers.virtualcomputer.computer.MDA
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2019-07-09.
 */
class StandaloneApp : Game() {

    lateinit var font: TextureRegionPack

    lateinit var background: Texture
    lateinit var execLed: Texture
    lateinit var waitLed: Texture

    lateinit var batch: SpriteBatch

    lateinit var vmThread: Thread

    val display = MDA(80, 25)

    override fun create() {
        font = TextureRegionPack(Gdx.files.internal("assets/mods/dwarventech/gui/lcd.tga"), 12, 16)

        background = Texture(Gdx.files.internal("assets/mods/dwarventech/gui/8025_textonly.png"))
        execLed = Texture(Gdx.files.internal("assets/mods/dwarventech/gui/led_green.tga"))
        waitLed = Texture(Gdx.files.internal("assets/mods/dwarventech/gui/led_orange.tga"))

        batch = SpriteBatch()

        //Gdx.input.inputProcessor = TVMInputProcessor()


        //vmThread = Thread(vm)
        //vmThread.start()

    }

    private val height: Int; get() = Gdx.graphics.height

    private val lcdOffX = 74f
    private val lcdOffY = 56f

    private val lcdCol = arrayOf(
            Color(0x14141400),
            Color(0x141414AA),
            Color(0x14141455),
            Color(0x141414FF)
    )

    private var textCursorDrawTimer = 0f // 0f..0.5f: not draw

    override fun render() {
        Gdx.graphics.setTitle("Terrarum Lua Computer Standalone — F: ${Gdx.graphics.framesPerSecond}")

        batch.inUse {
            batch.color = Color.WHITE
            batch.draw(background, 0f, 0f)


            // draw the screen
            textCursorDrawTimer += Gdx.graphics.rawDeltaTime
            if (textCursorDrawTimer > 1f) textCursorDrawTimer -= 1f
            val cursorX = display.cursor % display.width
            val cursorY = display.cursor / display.height

            for (i in 0 until display.width * display.height) {
                val drawX = ((i % display.width) * font.tileW).toFloat()
                val drawY = ((i / display.width) * font.tileH).toFloat()
                val (g, a) = display.rawGet(i)
                val glyph = g.toUint()
                val glyphBack = glyph + 256
                val back = (a.toUint() ushr 0x3) % lcdCol.size
                val fore = a.toUint() % lcdCol.size

                if (display.blink && i == display.cursor && textCursorDrawTimer >= 0.5f) {
                    batch.color = lcdCol[1]
                    batch.draw(
                            font.get(0, 8),
                            lcdOffX + drawX,
                            (this.height - font.tileH) - (lcdOffY + drawY)
                    )
                }
                else {
                    // print background
                    batch.color = lcdCol[back]
                    batch.draw(
                            font.get(glyphBack % font.horizontalCount, glyphBack / font.horizontalCount),
                            lcdOffX + drawX,
                            (this.height - font.tileH) - (lcdOffY + drawY)
                    )
                    // print foreground
                    batch.color = lcdCol[fore]
                    batch.draw(
                            font.get(glyph % font.horizontalCount, glyph / font.horizontalCount),
                            lcdOffX + drawX,
                            (this.height - font.tileH) - (lcdOffY + drawY)
                    )
                }

            }
            // end of draw the screen

        }


        //vm.resumeExec()
    }

    override fun dispose() {
        background.dispose()
        display.dispose()
        //vm.destroy()
    }

    private inline fun SpriteBatch.inUse(action: () -> Unit) {
        this.begin()
        action.invoke()
        this.end()
    }


    /*class TVMInputProcessor(val vm: TerranVM) : InputProcessor {
        override fun touchUp(p0: Int, p1: Int, p2: Int, p3: Int): Boolean {
            return false
        }

        override fun mouseMoved(p0: Int, p1: Int): Boolean {
            return false
        }

        override fun keyTyped(p0: Char): Boolean {


            return true
        }

        override fun scrolled(p0: Int): Boolean {
            return false
        }

        override fun keyUp(p0: Int): Boolean {
            return false
        }

        override fun touchDragged(p0: Int, p1: Int, p2: Int): Boolean {
            return false
        }

        override fun keyDown(p0: Int): Boolean {
            return false
        }

        override fun touchDown(p0: Int, p1: Int, p2: Int, p3: Int): Boolean {
            return false
        }
    }*/


    private fun Byte.toUint() = java.lang.Byte.toUnsignedInt(this)
}

fun main(args: Array<String>) {
    val config = LwjglApplicationConfiguration()
    config.width = 1106
    config.height = 556
    config.foregroundFPS = 100
    config.vSyncEnabled = false
    config.resizable = false

    LwjglApplication(StandaloneApp(), config)
}
