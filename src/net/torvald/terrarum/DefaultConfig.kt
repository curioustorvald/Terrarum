package net.torvald.terrarum

import com.badlogic.gdx.Input
import com.google.gson.JsonArray
import com.google.gson.JsonObject

/**
 * Keys must be all lowercase
 *
 * Created by minjaesong on 16-03-12.
 */
object DefaultConfig {
    fun fetch(): JsonObject {
        val jsonObject = JsonObject()

        jsonObject.addProperty("displayfps", 0) // 0: no limit, non-zero: limit
        jsonObject.addProperty("usevsync", true)


        jsonObject.addProperty("imtooyoungtodie", false) // no perma-death
        jsonObject.addProperty("language", Terrarum.sysLang)
        jsonObject.addProperty("notificationshowuptime", 6500)
        jsonObject.addProperty("multithread", true) // experimental!



        // control-gamepad
        jsonObject.addProperty("joypadkeyn", 4)
        jsonObject.addProperty("joypadkeyw", 1)
        jsonObject.addProperty("joypadkeys", 2)
        jsonObject.addProperty("joypadkeye", 3) // logitech indices

        jsonObject.addProperty("joypadlup", 4)
        jsonObject.addProperty("joypadrup", 5)
        jsonObject.addProperty("joypadldown", 6)
        jsonObject.addProperty("joypadrdown", 7) // logitech indices

        jsonObject.addProperty("joypadlstickx", 0)
        jsonObject.addProperty("joypadlsticky", 1)
        jsonObject.addProperty("joypadrstickx", 2)
        jsonObject.addProperty("joypadrsticky", 3) // 0-1-2-3 but sometimes 3-2-1-0 ?! what the actual fuck?

        jsonObject.addProperty("joypadlabelstyle", "msxb360") // "nwii", "logitech", "sonyps", "msxb360", "generic"



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

        val keyquickbars = JsonArray(); for (i in Input.Keys.NUMPAD_1..Input.Keys.NUMPAD_9) keyquickbars.add(i); keyquickbars.add(Input.Keys.NUMPAD_0) // NUM_1 to NUM_0
        jsonObject.add("keyquickbars", keyquickbars)

        jsonObject.addProperty("mouseprimary", Input.Buttons.LEFT) // left mouse
        jsonObject.addProperty("mousesecondary", Input.Buttons.RIGHT) // right mouse


        jsonObject.addProperty("pcgamepadenv", "console")

        jsonObject.addProperty("safetywarning", true)


        jsonObject.addProperty("maxparticles", 768)


        jsonObject.addProperty("fullframelightupdate", false)


        return jsonObject
    }
}

/*

Additional description goes here

 */