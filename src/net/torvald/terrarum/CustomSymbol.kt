package net.torvald.terrarum

import com.badlogic.gdx.Input

/**
 * Created by minjaesong on 2019-08-11.
 */

private val keyToIcon = hashMapOf(
        Input.Keys.NUM_0 to 0xE010.toChar(),
        Input.Keys.NUM_1 to 0xE011.toChar(),
        Input.Keys.NUM_2 to 0xE012.toChar(),
        Input.Keys.NUM_3 to 0xE013.toChar(),
        Input.Keys.NUM_4 to 0xE014.toChar(),
        Input.Keys.NUM_5 to 0xE015.toChar(),
        Input.Keys.NUM_6 to 0xE016.toChar(),
        Input.Keys.NUM_7 to 0xE017.toChar(),
        Input.Keys.NUM_8 to 0xE018.toChar(),
        Input.Keys.NUM_9 to 0xE019.toChar(),

        Input.Keys.A to 0xE021.toChar(),
        Input.Keys.B to 0xE022.toChar(),
        Input.Keys.C to 0xE023.toChar(),
        Input.Keys.D to 0xE024.toChar(),
        Input.Keys.E to 0xE025.toChar(),
        Input.Keys.F to 0xE026.toChar(),
        Input.Keys.G to 0xE027.toChar(),
        Input.Keys.H to 0xE028.toChar(),
        Input.Keys.I to 0xE029.toChar(),
        Input.Keys.J to 0xE02A.toChar(),
        Input.Keys.K to 0xE02B.toChar(),
        Input.Keys.L to 0xE02C.toChar(),
        Input.Keys.M to 0xE02D.toChar(),

        Input.Keys.N to 0xE02E.toChar(),
        Input.Keys.O to 0xE02E.toChar(),
        Input.Keys.P to 0xE030.toChar(),
        Input.Keys.Q to 0xE031.toChar(),
        Input.Keys.R to 0xE032.toChar(),
        Input.Keys.S to 0xE033.toChar(),
        Input.Keys.T to 0xE034.toChar(),
        Input.Keys.U to 0xE035.toChar(),
        Input.Keys.V to 0xE036.toChar(),
        Input.Keys.W to 0xE037.toChar(),
        Input.Keys.X to 0xE038.toChar(),
        Input.Keys.Y to 0xE039.toChar(),
        Input.Keys.Z to 0xE03A.toChar()
)

fun keyToIcon(key: Int) = keyToIcon[key]!!

const val F1 = "${0xE090.toChar()}${0xE091.toChar()}"
const val F2 = "${0xE090.toChar()}${0xE092.toChar()}"
const val F3 = "${0xE090.toChar()}${0xE093.toChar()}"
const val F4 = "${0xE090.toChar()}${0xE094.toChar()}"
const val F5 = "${0xE090.toChar()}${0xE095.toChar()}"
const val F6 = "${0xE090.toChar()}${0xE096.toChar()}"
const val F7 = "${0xE090.toChar()}${0xE097.toChar()}"
const val F8 = "${0xE090.toChar()}${0xE098.toChar()}"
const val F9 = "${0xE090.toChar()}${0xE099.toChar()}"
const val F10 = "${0xE09D.toChar()}${0xE09A.toChar()}"
const val F11 = "${0xE09D.toChar()}${0xE09B.toChar()}"
const val F12 = "${0xE09D.toChar()}${0xE09C.toChar()}"
