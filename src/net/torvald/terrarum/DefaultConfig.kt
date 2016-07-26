package net.torvald.terrarum

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.torvald.terrarum.gamecontroller.Key

/**
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

        jsonObject.addProperty("joypadlstickx", 0)
        jsonObject.addProperty("joypadlsticky", 1)
        jsonObject.addProperty("joypadrstickx", 2)
        jsonObject.addProperty("joypadrsticky", 3) // logitech indices

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
        // some pro typers assign CapsLock to Backspace, LControl, or LCommand (Mac). Honestly, Control (Command for mac) and CapsLock must swap their places!
        jsonObject.add("keyquickselalt", keyquickselalt)

        jsonObject.addProperty("keyjump", Key.SPACE)

        val keyquickbars = JsonArray(); for (i in 2..11) keyquickbars.add(i) // NUM_1 to NUM_0
        jsonObject.add("keyquickbars", keyquickbars)




        return jsonObject
    }
}

/*

Additional description goes here

 */