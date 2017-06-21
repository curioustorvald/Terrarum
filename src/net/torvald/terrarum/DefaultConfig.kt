package net.torvald.terrarum

import com.badlogic.gdx.Input
import com.google.gson.JsonArray
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

        jsonObject.addProperty("displayfps", 60)
        jsonObject.addProperty("usevsync", true)


        jsonObject.addProperty("smoothlighting", true)
        jsonObject.addProperty("imtooyoungtodie", false) // perma-death
        jsonObject.addProperty("language", TerrarumGDX.sysLang)
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

        jsonObject.addProperty("joypadlabelstyle", "generic") // "nwii", "logitech", "sonyps", "msxb360", "generic"



        // control-keyboard (Java key codes. This is what Minecraft also uses)
        jsonObject.addProperty("keyup", Input.Keys.E)
        jsonObject.addProperty("keyleft", Input.Keys.S)
        jsonObject.addProperty("keydown", Input.Keys.D)
        jsonObject.addProperty("keyright", Input.Keys.F)

        jsonObject.addProperty("keymovementaux", Input.Keys.A) // movement-auxiliary, or hookshot
        jsonObject.addProperty("keyinventory", Input.Keys.W)
        jsonObject.addProperty("keyinteract", Input.Keys.R)
        jsonObject.addProperty("keyclose", Input.Keys.C)

        jsonObject.addProperty("keygamemenu", Input.Keys.TAB)
        jsonObject.addProperty("keyquicksel", Key.CAPS_LOCK) // pie menu
        val keyquickselalt = JsonArray(); keyquickselalt.add(Input.Keys.BACKSPACE); keyquickselalt.add(Key.L_COMMAND); keyquickselalt.add(Input.Keys.CONTROL_LEFT)
        // Colemak, Workman and some typers use CapsLock as Backspace, Apple-JIS and HHKB has Control in place of CapsLock and often re-assigned to Command
        // so these keys are treated as the same.
        // FOR ~~FUCKS~~ERGONOMICS' SAKE DON'T USE CTRL AND ALT AS A KEY!
        jsonObject.add("keyquickselalt", keyquickselalt)

        jsonObject.addProperty("keyjump", Input.Keys.SPACE)

        val keyquickbars = JsonArray(); for (i in 2..11) keyquickbars.add(i) // NUM_1 to NUM_0
        jsonObject.add("keyquickbars", keyquickbars)

        jsonObject.addProperty("mouseprimary", Input.Buttons.LEFT) // left mouse
        jsonObject.addProperty("mousesecondary", Input.Buttons.RIGHT) // right mouse


        jsonObject.addProperty("pcgamepadenv", "console")

        jsonObject.addProperty("safetywarning", true)


        jsonObject.addProperty("maxparticles", 768)



        return jsonObject
    }
}

/*

Additional description goes here

 */