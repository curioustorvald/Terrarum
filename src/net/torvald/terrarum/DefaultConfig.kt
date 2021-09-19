package net.torvald.terrarum

import com.badlogic.gdx.Input

/**
 * Keys must be all lowercase
 *
 * Created by minjaesong on 2016-03-12.
 */
object DefaultConfig {

    val hashMap = hashMapOf<String, Any>(
            "displayfps" to 0, // 0: no limit, non-zero: limit
            "usevsync" to false,
            "screenwidth" to TerrarumScreenSize.defaultW,
            "screenheight" to TerrarumScreenSize.defaultH,
            "atlastexsize" to 2048,

            "language" to App.getSysLang(),
            "notificationshowuptime" to 4000,
            "multithread" to true,
            "multithreadedlight" to false,

            "showhealthmessageonstartup" to true,

            "usexinput" to true, // when FALSE, LT+RT input on xbox controller is impossible

            "config_gamepadkeyn" to 3,
            "config_gamepadkeyw" to 2,
            "config_gamepadkeys" to 0,
            "config_gamepadkeye" to 1, // xbox indices

            "config_gamepadlup" to 4,
            "config_gamepadrup" to 5,
            "config_gamepadselect" to 6,
            "config_gamepadstart" to 7,

            "config_gamepadltrigger" to 8,
            "config_gamepadrtrigger" to 9,
            "config_gamepadlthumb" to 10,
            "config_gamepadrthumb" to 11,


            "config_gamepadaxislx" to 1,
            "config_gamepadaxisly" to 0,
            "config_gamepadaxisrx" to 3,
            "config_gamepadaxisry" to 2, // 0-1-2-3 but sometimes 3-2-1-0 ?! what the actual fuck?
            "config_gamepadtriggeraxis" to 4, // positive: LT, negative: RT (xbox pad)
            "config_gamepadtriggeraxis2" to 5, // just in case... (RT)

            // to accomodate shifted zero point of analog stick
            "gamepadaxiszeropoints" to doubleArrayOf(-0.011, -0.022, -0.033, -0.044),

            "gamepadlabelstyle" to "msxbone", // "nwii", "logitech", "sonyps", "msxb360", "msxbone"

            // control-keyboard (GDX key codes,
            "config_keyup" to Input.Keys.E,
            "config_keyleft" to Input.Keys.S,
            "config_keydown" to Input.Keys.D,
            "config_keyright" to Input.Keys.F, // ESDF Masterrace

            "config_keymovementaux" to Input.Keys.A, // movement-auxiliary, or hookshot
            "config_keyinventory" to Input.Keys.Q,
            "config_keyinteract" to Input.Keys.R,
            "config_keydiscard" to Input.Keys.T,
            "config_keyclose" to Input.Keys.C, // this or hard-coded ESC
            "config_keyzoom" to Input.Keys.Z,

            "config_keygamemenu" to Input.Keys.TAB,
            "config_keyquicksel" to Input.Keys.SHIFT_LEFT, // pie menu is now LShift because GDX does not read CapsLock
            // Colemak, Workman and some typers use CapsLock as Backspace, Apple-JIS and HHKB has Control in place of CapsLock and often re-assigned to Command
            // so these keys are treated as the same.
            // FOR ~~FUCKS~~ERGONOMICS' SAKE DON'T USE CTRL AND ALT AS A KEY!
            "config_keyquickselalt" to intArrayOf(Input.Keys.BACKSPACE, Input.Keys.CONTROL_LEFT, Input.Keys.BACKSLASH),
            "config_mousequicksel" to Input.Buttons.MIDDLE, // middle click to open pie menu

            "config_keyjump" to Input.Keys.SPACE,

            "config_keyquickslots" to (Input.Keys.NUM_0..Input.Keys.NUM_9).toList().toIntArray(),

            "config_mouseprimary" to Input.Buttons.LEFT, // left mouse
            "config_mousesecondary" to Input.Buttons.RIGHT, // right mouse


            "pcgamepadenv" to "console",

            //"safetywarning" to true,


            "maxparticles" to 768,

            "temperatureunit" to 1, // -1: american, 0: kelvin, 1: celcius


            // "fancy" graphics settings
            "fxdither" to true,
            "fxretro" to false,
            "fxblurredbackground" to true,
            //"fx3dlut" to false,


            // settings regarding debugger
            /*"buildingmakerfavs" to arrayOf(
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
            )*/
    )
}

/*

Additional description goes here

 */