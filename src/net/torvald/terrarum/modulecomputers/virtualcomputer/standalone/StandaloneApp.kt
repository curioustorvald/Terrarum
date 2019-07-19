package net.torvald.terrarum.modulecomputers.virtualcomputer.standalone

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.modulecomputers.virtualcomputer.computer.LuaComputerVM
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

    val display = MDA(64, 40)
    val vm = LuaComputerVM(display)

    override fun create() {
        font = TextureRegionPack(Gdx.files.internal("assets/mods/dwarventech/gui/lcd.tga"), 12, 16)

        background = Texture(Gdx.files.internal("assets/mods/dwarventech/gui/6440_textonly.png"))
        execLed = Texture(Gdx.files.internal("assets/mods/dwarventech/gui/led_green.tga"))
        waitLed = Texture(Gdx.files.internal("assets/mods/dwarventech/gui/led_orange.tga"))

        batch = SpriteBatch()

        Gdx.input.inputProcessor = StandaloneAppInputProcessor(vm)
    }

    private val height: Int; get() = Gdx.graphics.height

    private val lcdOffX = 74f
    private val lcdOffY = 56f

    private val lcdCol = arrayOf(
            Color(0x10101000),
            Color(0x101010AA),
            Color(0x10101055),
            Color(0x101010FF)
    )

    private var textCursorDrawTimer = 0f // 0f..0.5f: not draw

    init {
        vm.runCommand("""
            print("Hello, world!")
            while true do
                local s = io.read()
                print(s)
            end
        """.trimIndent(), "")

        /*vm.runCommand("""
            a = 0
            while true do
                print(a)
                a = a + 1
            end
        """.trimIndent(), "")*/
    }



    override fun render() {
        Gdx.graphics.setTitle("Terrarum Lua Computer Standalone — F: ${Gdx.graphics.framesPerSecond}")

        //display.print(ByteArray(1){ (Math.random() * 255).toByte() })
        //display.print("@")

        batch.inUse {
            batch.color = Color.WHITE
            batch.draw(background, 0f, 0f)


            // draw the screen
            textCursorDrawTimer += Gdx.graphics.rawDeltaTime
            if (textCursorDrawTimer > 1f) textCursorDrawTimer -= 1f

            for (i in 0 until display.width * display.height) {
                val drawX = ((i % display.width) * font.tileW).toFloat()
                val drawY = ((i / display.width) * font.tileH).toFloat()
                val (g, a) = display.rawGet(i)
                val glyph = g.toUint()
                val glyphBack = glyph + 256
                val back = (a.toUint() ushr 4) % lcdCol.size
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

    private class StandaloneAppInputProcessor(private val vm: LuaComputerVM) : InputProcessor {
        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            return false
        }

        override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
            return false
        }

        override fun keyTyped(character: Char): Boolean {
            return false
        }

        override fun scrolled(amount: Int): Boolean {
            return false
        }

        override fun keyUp(keycode: Int): Boolean {
            return false
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            return false
        }

        override fun keyDown(keycode: Int): Boolean {
            vm.keyPressed(keycode)
            return false
        }

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            return false
        }
    }

    private fun Byte.toUint() = java.lang.Byte.toUnsignedInt(this)
}

fun main(args: Array<String>) {
    val config = LwjglApplicationConfiguration()
    config.width = 914
    config.height = 796
    config.foregroundFPS = 100
    config.vSyncEnabled = false
    config.resizable = false

    LwjglApplication(StandaloneApp(), config)
}
