package net.torvald.terrarum.gamecontroller

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import net.torvald.terrarum.App

/**
 * BIG WARNING SIGN: since the strober will run on separate thread, ALWAYS BEWARE OF THE [ConcurrentModificationException]!
 *
 * Created by minjaesong on 2021-11-06.
 */
object InputStrober {

    const val KEY_DOWN = 0
    const val KEY_CHANGE = 1
    const val N_KEY_ROLLOVER = 8

    var KEYBOARD_DELAYS = longArrayOf(0L,250000000L,0L,25000000L,0L)
    private var stroboTime = 0L
    private var stroboStatus = 0
    private var repeatCount = 0
    private var oldKeys = IntArray(N_KEY_ROLLOVER) { 0 }
    /** always Low Layer */
//        private var keymap = IME.getLowLayerByName(App.getConfigString("basekeyboardlayout"))

    private val thread = Thread { while (!Thread.interrupted()) {
        if (Gdx.input != null) withKeyboardEvent()
    } }

    init {
//        println("InputStrobe start")
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
        val keys = strobeKeys()
        var keyChanged = !arrayEq(keys, oldKeys)
        val keyDiff = arrayDiff(keys, oldKeys)
        val keymap = IME.getLowLayerByName(App.getConfigString("basekeyboardlayout"))

//        println("Key strobed: ${keys.joinToString()}")

        if (stroboStatus % 2 == 0 && keys[0] != 0) {
            stroboStatus += 1
            stroboTime = System.nanoTime()
            repeatCount += 1

            val shiftin = keys.contains(Input.Keys.SHIFT_LEFT) || keys.contains(Input.Keys.SHIFT_RIGHT)
            val keysym0 = keysToStr(keymap, keys)
            val newKeysym0 = keysToStr(keymap, keyDiff)
            val keysym = if (keysym0 == null) null
            else if (shiftin && keysym0[1]?.isNotBlank() == true) keysym0[1]
            else keysym0[0]
            val newKeysym = if (newKeysym0 == null) null
            else if (shiftin && newKeysym0[1]?.isNotBlank() == true) newKeysym0[1]
            else newKeysym0[0]

            val headKeyCode = if (keyDiff.size < 1) keys[0] else keyDiff[0]

            if (!keyChanged) {
//                    println("KEY_DOWN '$keysym' ($headKeyCode) $repeatCount; ${keys.joinToString()}")
                App.inputStrobed(TerrarumKeyboardEvent(KEY_DOWN, keysym, headKeyCode, repeatCount, keys))
            }
            else if (newKeysym != null) {
//                    println("KEY_DOWC '$newKeysym' ($headKeyCode) $repeatCount; ${keys.joinToString()}")
                App.inputStrobed(TerrarumKeyboardEvent(KEY_DOWN, newKeysym, headKeyCode, repeatCount, keys))
            }

            oldKeys = keys // don't put this outside of if-cascade
        }
        else if (keyChanged || keys[0] == 0) {
            stroboStatus = 0
            repeatCount = 0

            if (keys[0] == 0) keyChanged = false
        }
        else if (stroboStatus % 2 == 1 && System.nanoTime() - stroboTime < KEYBOARD_DELAYS[stroboStatus]) {
            Thread.sleep(1L)
        }
        else {
            stroboStatus += 1
            if (stroboStatus >= 4)
                stroboStatus = 2
        }
    }


    private fun keysToStr(keymap: TerrarumKeyLayout, keys: IntArray): Array<String?>? {
        if (keys.isEmpty()) return null
        val headkey = keys[0]
        return keymap.symbols?.get(headkey)
    }

    private fun strobeKeys(): IntArray {
        var keysPushed = 0
        val keyEventBuffers = IntArray(N_KEY_ROLLOVER) { 0 }
        for (k in 1..254) {
            if (Gdx.input.isKeyPressed(k)) {
                keyEventBuffers[keysPushed] = k
                keysPushed += 1
            }

            if (keysPushed >= N_KEY_ROLLOVER) break
        }
        return keyEventBuffers
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