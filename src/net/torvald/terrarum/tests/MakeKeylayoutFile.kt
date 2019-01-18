
import com.badlogic.gdx.*
import com.badlogic.gdx.Input.Keys.*
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.gdxClearAndSetBlend
import net.torvald.terrarum.inUse

/**
 * Not meant to implement IME, just to be used with Options > Config
 * where it shows a "summary" diagram of a keyboard and icon for functions on its keycap
 *
 * If unknown key was chosen (e.g. caret used on french AZERTY), config will simply won't display it
 * on the "summary".
 */
object MakeKeylayoutFile {

    fun invoke() {
        val qwerty = arrayOf( // QWERTY
                intArrayOf(TAB,Q,W,E,R,T,Y,U,I,O,P,LEFT_BRACKET,RIGHT_BRACKET,BACKSLASH),
                intArrayOf(UNKNOWN,A,S,D,F,G,H,J,K,L,SEMICOLON,APOSTROPHE),
                intArrayOf(SHIFT_LEFT,Z,X,C,V,B,N,M,COMMA,PERIOD,SLASH,SHIFT_RIGHT),
                intArrayOf(SPACE)
        )
        val qwerty_hhk = arrayOf( // QWERTY HHK
                intArrayOf(TAB,Q,W,E,R,T,Y,U,I,O,P,LEFT_BRACKET,RIGHT_BRACKET,BACKSLASH),
                intArrayOf(CONTROL_LEFT,A,S,D,F,G,H,J,K,L,SEMICOLON,APOSTROPHE),
                intArrayOf(SHIFT_LEFT,Z,X,C,V,B,N,M,COMMA,PERIOD,SLASH,SHIFT_RIGHT),
                intArrayOf(SPACE)
        )
        val qwertz = arrayOf( // QWERTZ English
                intArrayOf(TAB,Q,W,E,R,T,Z,U,I,O,P,LEFT_BRACKET,RIGHT_BRACKET,BACKSLASH),
                intArrayOf(UNKNOWN,A,S,D,F,G,H,J,K,L,SEMICOLON,APOSTROPHE),
                intArrayOf(SHIFT_LEFT,Y,X,C,V,B,N,M,COMMA,PERIOD,SLASH,SHIFT_RIGHT),
                intArrayOf(SPACE)
        )
        val azerty = arrayOf( // AZERTY Windows
                intArrayOf(TAB,A,Z,E,R,T,Y,U,I,O,P,72,74,BACKSLASH),
                intArrayOf(UNKNOWN,Q,S,D,F,G,H,J,K,L,M,68),
                intArrayOf(SHIFT_LEFT,W,X,C,V,B,N,COMMA,PERIOD,SLASH,UNKNOWN,SHIFT_RIGHT),
                intArrayOf(SPACE)
        )
        val colemak = arrayOf( // Colemak
                intArrayOf(TAB,Q,W,F,P,G,J,L,U,Y,SEMICOLON,LEFT_BRACKET,RIGHT_BRACKET,BACKSLASH),
                intArrayOf(BACKSPACE,A,R,S,T,D,H,N,E,I,O,APOSTROPHE),
                intArrayOf(SHIFT_LEFT,Z,X,C,V,B,K,M,COMMA,PERIOD,SLASH,SHIFT_RIGHT),
                intArrayOf(SPACE)
        )
        val dvorak = arrayOf( // Dvorak
                intArrayOf(TAB,APOSTROPHE,COMMA,PERIOD,P,Y,F,G,C,R,L,SLASH,EQUALS,BACKSLASH),
                intArrayOf(UNKNOWN,A,O,E,U,I,D,H,T,N,S,MINUS),
                intArrayOf(SHIFT_LEFT,SEMICOLON,Q,J,K,X,B,M,W,V,Z,SHIFT_RIGHT),
                intArrayOf(SPACE)
        )
        val dvorak_lh = arrayOf( // Dvorak Left handed
                intArrayOf(TAB,SEMICOLON,Q,B,Y,U,R,S,O,PERIOD,UNKNOWN,UNKNOWN,EQUALS,BACKSLASH),
                intArrayOf(UNKNOWN,MINUS,K,C,D,T,H,E,A,Z,UNKNOWN,UNKNOWN),
                intArrayOf(SHIFT_LEFT,APOSTROPHE,X,G,V,W,N,I,COMMA,UNKNOWN,UNKNOWN,SHIFT_RIGHT),
                intArrayOf(SPACE)
        )


        val keys = listOf(
                qwerty, qwerty_hhk, qwertz, azerty, colemak, dvorak, dvorak_lh
        )

        keys.forEach { println(it.toConfigStr()) }
    }

    private fun Array<IntArray>.toConfigStr() =
            this.map { it.joinToString(",") }.joinToString(";")

}

class GetKeycode : Game() {

    private lateinit var font: BitmapFont
    private lateinit var batch: SpriteBatch

    private var keyHit = "(keycode will be displayed here)"

    override fun create() {
        font = BitmapFont()
        batch = SpriteBatch()

        Gdx.input.inputProcessor = Con(this)
    }

    override fun getScreen(): Screen {
        return super.getScreen()
    }

    override fun setScreen(screen: Screen?) {
        super.setScreen(screen)
    }

    override fun render() {
        Gdx.graphics.setTitle("Get Keycode — F: ${Gdx.graphics.framesPerSecond}")

        gdxClearAndSetBlend(.1f,.1f,.1f,1f)

        batch.inUse {
            font.draw(batch, "Hit a key", 10f, 20f)
            font.draw(batch, keyHit, 10f, 42f)
        }
    }

    override fun pause() {
        super.pause()
    }

    override fun resume() {
        super.resume()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
    }

    override fun dispose() {
        super.dispose()
    }

    class Con(val host: GetKeycode): InputAdapter() {
        override fun keyDown(keycode: Int): Boolean {
            host.keyHit = "Key down: $keycode (${Input.Keys.toString(keycode)})"
            return true
        }
    }
}

fun main() {val appConfig = LwjglApplicationConfiguration()
    appConfig.resizable = false
    appConfig.width = 256
    appConfig.height = 64
    appConfig.foregroundFPS = 60
    appConfig.backgroundFPS = 60

    val gdxWindow = GetKeycode()

    LwjglApplication(gdxWindow, appConfig)
    MakeKeylayoutFile.invoke()
}