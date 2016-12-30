package net.torvald.terrarum

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.torvald.terrarum.gamecontroller.Key

/**
 * Keys must be all lowercase
 *
 * Created by minjaesong on 16-03-12.
 */
object DefaultConfig {
    fun fetch(): JsonObject {
        val jsonObject = JsonObject()

        jsonObject.addProperty("smoothlighting", true)
        jsonObject.addProperty("imtooyoungtodie", false) // perma-death
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

        jsonObject.addProperty("joypadlstickx", 3)
        jsonObject.addProperty("joypadlsticky", 2)
        jsonObject.addProperty("joypadrstickx", 1)
        jsonObject.addProperty("joypadrsticky", 0) // logitech indices

        jsonObject.addProperty("joypadlabelstyle", "generic") // "nwii", "logitech", "sonyps", "msxb360", "generic"



        // control-keyboard (Java key codes. This is what Minecraft also uses)
        jsonObject.addProperty("keyup", Key.E)
        jsonObject.addProperty("keyleft", Key.S)
        jsonObject.addProperty("keydown", Key.D)
        jsonObject.addProperty("keyright", Key.F)

        jsonObject.addProperty("keymovementaux", Key.A) // movement-auxiliary, or hookshot
        jsonObject.addProperty("keyinventory", Key.W)
        jsonObject.addProperty("keyinteract", Key.R)
        jsonObject.addProperty("keyclose", Key.C)

        jsonObject.addProperty("keygamemenu", Key.TAB)
        jsonObject.addProperty("keyquicksel", Key.CAPS_LOCK) // pie menu
        val keyquickselalt = JsonArray(); keyquickselalt.add(Key.BACKSPACE); keyquickselalt.add(Key.L_COMMAND); keyquickselalt.add(Key.L_CONTROL)
        // Colemak, Workman and some typers use CapsLock as Backspace, Apple-JIS and HHKB has Control in place of CapsLock and often re-assigned to Command
        // so these keys are treated as the same.
        // FOR ~~FUCKS~~ERGONOMICS' SAKE DON'T USE CTRL AND ALT AS A KEY!
        jsonObject.add("keyquickselalt", keyquickselalt)

        jsonObject.addProperty("keyjump", Key.SPACE)

        val keyquickbars = JsonArray(); for (i in 2..11) keyquickbars.add(i) // NUM_1 to NUM_0
        jsonObject.add("keyquickbars", keyquickbars)

        jsonObject.addProperty("mouseprimary", 0) // left mouse
        jsonObject.addProperty("mousesecondary", 1) // right mouse


        jsonObject.addProperty("pcgamepadenv", "console")

        jsonObject.addProperty("safetywarning", true)


        return jsonObject
    }
}

/*

Additional description goes here

 */