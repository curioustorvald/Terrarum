package net.torvald.terrarum

import com.badlogic.gdx.Input
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex

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


        //jsonObject.addProperty("imtooyoungtodie", false) // no perma-death
        jsonObject.addProperty("language", AppLoader.getSysLang())
        jsonObject.addProperty("notificationshowuptime", 4000)
        jsonObject.addProperty("multithread", true) // experimental!
        jsonObject.addProperty("multithreadedlight", false) // experimental!

        jsonObject.addProperty("showhealthmessageonstartup", true)

        // control-gamepad

        // "config_key", "config_mouse", "config_gamepad" are keyword recognised by control setup UI

        jsonObject.addProperty("usexinput", true) // when FALSE, LT+RT input on xbox controller is impossible

        jsonObject.addProperty("config_gamepadkeyn", 3)
        jsonObject.addProperty("config_gamepadkeyw", 2)
        jsonObject.addProperty("config_gamepadkeys", 0)
        jsonObject.addProperty("config_gamepadkeye", 1) // xbox indices

        jsonObject.addProperty("config_gamepadlup", 4)
        jsonObject.addProperty("config_gamepadrup", 5)
        jsonObject.addProperty("config_gamepadselect", 6)
        jsonObject.addProperty("config_gamepadstart", 7)

        jsonObject.addProperty("config_gamepadltrigger", 8)
        jsonObject.addProperty("config_gamepadrtrigger", 9)
        jsonObject.addProperty("config_gamepadlthumb", 10)
        jsonObject.addProperty("config_gamepadrthumb", 11)


        jsonObject.addProperty("config_gamepadaxislx", 1)
        jsonObject.addProperty("config_gamepadaxisly", 0)
        jsonObject.addProperty("config_gamepadaxisrx", 3)
        jsonObject.addProperty("config_gamepadaxisry", 2) // 0-1-2-3 but sometimes 3-2-1-0 ?! what the actual fuck?
        jsonObject.addProperty("config_gamepadtriggeraxis", 4) // positive: LT, negative: RT (xbox pad)
        jsonObject.addProperty("config_gamepadtriggeraxis2", 5) // just in case... (RT)

        val axesZeroPoints = JsonArray(); axesZeroPoints.add(-0.011f); axesZeroPoints.add(-0.022f); axesZeroPoints.add(-0.033f); axesZeroPoints.add(-0.044f)
        jsonObject.add("gamepadaxiszeropoints", axesZeroPoints) // to accomodate shifted zero point of analog stick

        jsonObject.addProperty("gamepadlabelstyle", "msxbone") // "nwii", "logitech", "sonyps", "msxb360", "msxbone"

        // control-keyboard (GDX key codes)
        jsonObject.addProperty("config_keyup", Input.Keys.E)
        jsonObject.addProperty("config_keyleft", Input.Keys.S)
        jsonObject.addProperty("config_keydown", Input.Keys.D)
        jsonObject.addProperty("config_keyright", Input.Keys.F) // ESDF Masterrace

        jsonObject.addProperty("config_keymovementaux", Input.Keys.A) // movement-auxiliary, or hookshot
        jsonObject.addProperty("config_keyinventory", Input.Keys.Q)
        jsonObject.addProperty("config_keyinteract", Input.Keys.R)
        jsonObject.addProperty("config_keyclose", Input.Keys.C) // this or hard-coded ESC
        jsonObject.addProperty("config_keyzoom", Input.Keys.Z)

        jsonObject.addProperty("config_keygamemenu", Input.Keys.TAB)
        jsonObject.addProperty("config_keyquicksel", Input.Keys.SHIFT_LEFT) // pie menu is now LShift because GDX does not read CapsLock
        val keyquickselalt = JsonArray(); keyquickselalt.add(Input.Keys.BACKSPACE); keyquickselalt.add(Input.Keys.CONTROL_LEFT); keyquickselalt.add(Input.Keys.BACKSLASH)
        // Colemak, Workman and some typers use CapsLock as Backspace, Apple-JIS and HHKB has Control in place of CapsLock and often re-assigned to Command
        // so these keys are treated as the same.
        // FOR ~~FUCKS~~ERGONOMICS' SAKE DON'T USE CTRL AND ALT AS A KEY!
        jsonObject.add("config_keyquickselalt", keyquickselalt)
        jsonObject.addProperty("config_mousequicksel", Input.Buttons.MIDDLE) // middle click to open pie menu

        jsonObject.addProperty("config_keyjump", Input.Keys.SPACE)

        val keyquickslots = JsonArray(); for (i in Input.Keys.NUM_1..Input.Keys.NUM_9) keyquickslots.add(i); keyquickslots.add(Input.Keys.NUM_0) // NUM_1 to NUM_0
        jsonObject.add("config_keyquickslots", keyquickslots)

        jsonObject.addProperty("config_mouseprimary", Input.Buttons.LEFT) // left mouse
        jsonObject.addProperty("config_mousesecondary", Input.Buttons.RIGHT) // right mouse


        jsonObject.addProperty("pcgamepadenv", "console")

        //jsonObject.addProperty("safetywarning", true)


        jsonObject.addProperty("maxparticles", 768)

        jsonObject.addProperty("temperatureunit", 1) // -1: american, 0: kelvin, 1: celcius


        // "fancy" graphics settings
        jsonObject.addProperty("fxdither", true)
        jsonObject.addProperty("fxretro", false)
        //jsonObject.addProperty("fx3dlut", false)


        // settings regarding debugger
        val buildingMakerFavs = JsonArray()
        arrayOf(
                Block.GLASS_CRUDE,
                Block.PLANK_NORMAL,
                Block.PLANK_BIRCH,
                Block.STONE_QUARRIED,
                Block.STONE_BRICKS,

                Block.STONE_TILE_WHITE,
                Block.TORCH,
                "wall@" + Block.PLANK_NORMAL,
                "wall@" + Block.PLANK_BIRCH,
                "wall@" + Block.GLASS_CRUDE
        ).forEach {
            buildingMakerFavs.add(it)
        }
        jsonObject.add("buildingmakerfavs", buildingMakerFavs)


        return jsonObject
    }
}

/*

Additional description goes here

 */