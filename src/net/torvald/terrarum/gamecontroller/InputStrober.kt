package net.torvald.terrarum.gamecontroller

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import net.torvald.terrarum.App
import net.torvald.terrarum.TerrarumAppConfiguration
import net.torvald.unsafe.UnsafeHelper
import java.util.*

/**
 * BIG WARNING SIGN: since the strober will run on separate thread, ALWAYS BEWARE OF THE [ConcurrentModificationException]!
 *
 * Created by minjaesong on 2021-11-06.
 */
object InputStrober {

    const val KEY_DOWN = 0
    const val KEY_CHANGE = 1
    const val N_KEY_ROLLOVER = 8

    private const val JIFFIES = 5L

    var KEYBOARD_DELAYS = longArrayOf(0L,250000000L,0L,25000000L,0L)
    private var stroboTime = 0L
    private var stroboStatus = 0
    private var repeatCount = 0
    private val oldKeys = IntArray(N_KEY_ROLLOVER) { 0 }
    /** always Low Layer */
//        private var keymap = IME.getLowLayerByName(App.getConfigString("basekeyboardlayout"))

    private val thread = Thread({ while (!Thread.interrupted()) {
        try { if (Gdx.input != null) withKeyboardEvent() } catch (e: InterruptedException) { break }
    } }, "${TerrarumAppConfiguration.GAME_NAME}${this.javaClass.simpleName}")

    init {
//        println("InputStrobe start")
        thread.priority = 5
        thread.start()
    }

    fun dispose() {
        thread.interrupt()
    }

    fun resetKeyboardStrobo() {
        stroboStatus = 0
        repeatCount = 0
    }

    // code proudly stolen from tsvm's TVDOS.SYS
    private fun withKeyboardEvent() {
        strobeKeys()

        var keyChanged = !arrayEq(keybuf, oldKeys)
        val keyDiff = arrayDiff(keybuf, oldKeys)
        val keymap = IME.getLowLayerByName(App.getConfigString("basekeyboardlayout"))

        if (stroboStatus % 2 == 0 && keybuf[0] != 0) {
//            println("Key strobed: ${keybuf.joinToString()}; old: ${oldKeys.joinToString()}; changed = $keyChanged")

            stroboStatus += 1
            stroboTime = System.nanoTime()
            repeatCount += 1

            val shiftin = keybuf.containsSome(Input.Keys.SHIFT_LEFT, Input.Keys.SHIFT_RIGHT)
            val altgrin = keybuf.contains(Input.Keys.ALT_RIGHT) || keybuf.containsAll(Input.Keys.ALT_LEFT, Input.Keys.CONTROL_LEFT)
            val keysym0 = keysToStr(keymap, keybuf)
            val newKeysym0 = keysToStr(keymap, keyDiff)
            val keysym =
                    if (keysym0 == null) null
                    else if (shiftin && altgrin && keysym0[3]?.isNotEmpty() == true) keysym0[3]
                    else if (altgrin && keysym0[2]?.isNotEmpty() == true) keysym0[2]
                    else if (shiftin && keysym0[1]?.isNotEmpty() == true) keysym0[1]
                    else keysym0[0]
            val newKeysym =
                    if (newKeysym0 == null) null
                    else if (shiftin && altgrin && newKeysym0[3]?.isNotEmpty() == true) newKeysym0[3]
                    else if (altgrin && newKeysym0[2]?.isNotEmpty() == true) newKeysym0[2]
                    else if (shiftin && newKeysym0[1]?.isNotEmpty() == true) newKeysym0[1]
                    else newKeysym0[0]

            val headKeyCode = if (keyDiff.size < 1) keybuf[0] else keyDiff[0]

            if (!keyChanged) {
//                println("KEY_DOWN '$keysym' ($headKeyCode) $repeatCount")
                App.inputStrobed(TerrarumKeyboardEvent(KEY_DOWN, keysym, headKeyCode, repeatCount, keybuf))
            }
            else if (newKeysym != null) {
//                println("KEY_DOWC '$newKeysym' ($headKeyCode) $repeatCount")
                App.inputStrobed(TerrarumKeyboardEvent(KEY_DOWN, newKeysym, headKeyCode, repeatCount, keybuf))
            }

            arrayCopy(oldKeys, keybuf)
        }
        else if (keyChanged || keybuf[0] == 0) {
            stroboStatus = 0
            repeatCount = 0

            if (keybuf[0] == 0) {
                keyChanged = false
//                println("InputStrober idle")
                Thread.sleep(JIFFIES) // idle sleep time. This also determines the minimum time the key need to be held down to be recognised.
            }
        }
        else if (stroboStatus % 2 == 1 && System.nanoTime() - stroboTime < KEYBOARD_DELAYS[stroboStatus]) {
//            println("InputStrober state hold")
            Thread.sleep(JIFFIES) // key repeat duration accumulation time
        }
        else {
            stroboStatus += 1
            if (stroboStatus >= 4)
                stroboStatus = 2

//            println("InputStrober state change")
        }


    }


    private fun keysToStr(keymap: TerrarumKeyLayout, keys: IntArray): Array<String?>? {
        if (keys.isEmpty()) return null
        val headkey = keys[0]
        return keymap.symbols?.get(headkey)
    }

    private val keybuf = IntArray(N_KEY_ROLLOVER) { 0 }

    private fun strobeKeys() {
        var keysPushed = 0
        Arrays.fill(keybuf, 0)
        for (k in 1..254) {
            if (Gdx.input.isKeyPressed(k)) {
                keybuf[keysPushed] = k
                keysPushed += 1
            }

            if (keysPushed >= N_KEY_ROLLOVER) break
        }
    }

    private fun arrayCopy(target: IntArray, source: IntArray) {
        UnsafeHelper.memcpyRaw(source, UnsafeHelper.getArrayOffset(source), target, UnsafeHelper.getArrayOffset(target), 4L * N_KEY_ROLLOVER)
    }

    private fun arrayEq(a: IntArray, b: IntArray): Boolean {
        for (i in a.indices) {
            if (a[i] != b.getOrNull(i)) return false
        }
        return true
    }

    private fun arrayDiff(a: IntArray, b: IntArray): IntArray {
        return a.filter { !b.contains(it) }.toIntArray()
    }

}


data class TerrarumKeyboardEvent(
        val type: Int,
        val character: String?, // representative key symbol
        val headkey: Int, // representative keycode
        val repeatCount: Int,
        val keycodes: IntArray
)


fun IntArray.containsAll(vararg keys: Int): Boolean {
    keys.forEach {
        if (!this.contains(it)) return false
    }
    return true
}

fun IntArray.containsSome(vararg keys: Int): Boolean {
    keys.forEach {
        if (this.contains(it)) return true
    }
    return false
}
