package net.torvald.terrarum

import com.badlogic.gdx.Input
import com.google.gson.JsonArray
import com.google.gson.JsonObject

/**
 * Keys must be all lowercase
 *
 * Created by minjaesong on 2016-03-12.
 */
object DefaultConfig {
    fun fetch(): JsonObject {
        val jsonObject = JsonObject()

        jsonObject.addProperty("displayfps", 0) // 0: no limit, non-zero: limit
        jsonObject.addProperty("usevsync", false)
        jsonObject.addProperty("screenwidth", AppLoader.defaultW)
        jsonObject.addProperty("screenheight", AppLoader.defaultH)


        jsonObject.addProperty("imtooyoungtodie", false) // no perma-death
        jsonObject.addProperty("language", AppLoader.getSysLang())
        jsonObject.addProperty("notificationshowuptime", 6500)
        jsonObject.addProperty("multithread", true) // experimental!
        jsonObject.addProperty("multithreadedlight", false) // experimental!



        // control-gamepad
        jsonObject.addProperty("gamepadkeyn", 4)
        jsonObject.addProperty("gamepadkeyw", 1)
        jsonObject.addProperty("gamepadkeys", 2)
        jsonObject.addProperty("gamepadkeye", 3) // logitech indices

        jsonObject.addProperty("gamepadlup", 4)
        jsonObject.addProperty("gamepadrup", 5)
        jsonObject.addProperty("gamepadldown", 6)
        jsonObject.addProperty("gamepadrdown", 7) // logitech indices

        jsonObject.addProperty("gamepadlstickx", 0)
        jsonObject.addProperty("gamepadlsticky", 1)
        jsonObject.addProperty("gamepadrstickx", 2)
        jsonObject.addProperty("gamepadrsticky", 3) // 0-1-2-3 but sometimes 3-2-1-0 ?! what the actual fuck?

        jsonObject.addProperty("gamepadlabelstyle", "msxb360") // "nwii", "logitech", "sonyps", "msxb360", "generic"



        // control-keyboard (GDX key codes)
        jsonObject.addProperty("keyup", Input.Keys.E)
        jsonObject.addProperty("keyleft", Input.Keys.S)
        jsonObject.addProperty("keydown", Input.Keys.D)
        jsonObject.addProperty("keyright", Input.Keys.F) // ESDF Masterrace

        jsonObject.addProperty("keymovementaux", Input.Keys.A) // movement-auxiliary, or hookshot
        jsonObject.addProperty("keyinventory", Input.Keys.Q)
        jsonObject.addProperty("keyinteract", Input.Keys.R)
        jsonObject.addProperty("keyclose", Input.Keys.C)

        jsonObject.addProperty("keygamemenu", Input.Keys.TAB)
        jsonObject.addProperty("keyquicksel", Input.Keys.SHIFT_LEFT) // pie menu is now LShift because GDX does not read CapsLock
        val keyquickselalt = JsonArray(); keyquickselalt.add(Input.Keys.BACKSPACE); keyquickselalt.add(Input.Keys.CONTROL_LEFT); keyquickselalt.add(Input.Keys.BACKSLASH)
        // Colemak, Workman and some typers use CapsLock as Backspace, Apple-JIS and HHKB has Control in place of CapsLock and often re-assigned to Command
        // so these keys are treated as the same.
        // FOR ~~FUCKS~~ERGONOMICS' SAKE DON'T USE CTRL AND ALT AS A KEY!
        jsonObject.add("keyquickselalt", keyquickselalt)

        jsonObject.addProperty("keyjump", Input.Keys.SPACE)

        val keyquickslots = JsonArray(); for (i in Input.Keys.NUM_1..Input.Keys.NUM_9) keyquickslots.add(i); keyquickslots.add(Input.Keys.NUM_0) // NUM_1 to NUM_0
        jsonObject.add("keyquickslots", keyquickslots)

        jsonObject.addProperty("mouseprimary", Input.Buttons.LEFT) // left mouse
        jsonObject.addProperty("mousesecondary", Input.Buttons.RIGHT) // right mouse


        jsonObject.addProperty("pcgamepadenv", "console")

        //jsonObject.addProperty("safetywarning", true)


        jsonObject.addProperty("maxparticles", 768)

        jsonObject.addProperty("temperatureunit", 1) // -1: american, 0: kelvin, 1: celcius


        // "fancy" graphics settings
        jsonObject.addProperty("fxdither", true)
        //jsonObject.addProperty("fx3dlut", false)


        return jsonObject
    }
}

/*

Additional description goes here

 */