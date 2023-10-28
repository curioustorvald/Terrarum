package net.torvald.terrarum

import com.badlogic.gdx.Input

/**
 * Keys must be all lowercase
 *
 * Created by minjaesong on 2016-03-12.
 */
object DefaultConfig {

    val hashMap = hashMapOf<String, Any>(
            "jvm_xmx" to 4,
            "jvm_extra_cmd" to "",

            "displayfps" to 0, // 0: no limit, non-zero: limit
            "displayfpsidle" to 0, // 0: no limit, non-zero: limit
            "displaycolourdepth" to 8,
            "usevsync" to false,
            "screenwidth" to TerrarumScreenSize.defaultW,
            "screenheight" to TerrarumScreenSize.defaultH,
            "fullscreen" to false,
            "atlastexsize" to 2048,

            "language" to App.getSysLang(),
            "notificationshowuptime" to 4000, // 4s
            "selecteditemnameshowuptime" to 4000, // 4s
            "autosaveinterval" to 300000, // 5m
            "multithread" to true,

            "showhealthmessageonstartup" to true,

            "usexinput" to true, // when FALSE, LT+RT input on xbox controller is impossible

            "control_preset_keyboard" to "WASD",

            "control_gamepad_keyn" to 3,
            "control_gamepad_keyw" to 2,
            "control_gamepad_keys" to 0,
            "control_gamepad_keye" to 1, // xbox indices

            "control_gamepad_lup" to 4,
            "control_gamepad_rup" to 5,
            "control_gamepad_select" to 6,
            "control_gamepad_start" to 7,

            "control_gamepad_ltrigger" to 8,
            "control_gamepad_rtrigger" to 9,
            "control_gamepad_lthumb" to 10,
            "control_gamepad_rthumb" to 11,


            "control_gamepad_axislx" to 1,
            "control_gamepad_axisly" to 0,
            "control_gamepad_axisrx" to 3,
            "control_gamepad_axisry" to 2, // 0-1-2-3 but sometimes 3-2-1-0 ?! what the actual fuck?
            "control_gamepad_triggeraxis" to 4, // positive: LT, negative: RT (xbox pad)
            "control_gamepad_triggeraxis2" to 5, // just in case... (RT)

            // to accomodate shifted zero point of analog stick
            "control_gamepad_axiszeropoints" to doubleArrayOf(0.0,0.0,0.0,0.0),

            "control_gamepad_labelstyle" to "msxbone", // "nwii", "logitech", "sonyps", "msxb360", "msxbone"

            // control-keyboard (GDX key codes,
            "control_key_up" to Input.Keys.E,
            "control_key_left" to Input.Keys.S,
            "control_key_down" to Input.Keys.D,
            "control_key_right" to Input.Keys.F, // ESDF Masterrace

            "control_key_jump" to Input.Keys.SPACE,
            "control_key_movementaux" to Input.Keys.A, // movement-auxiliary, or hookshot
            "control_key_inventory" to Input.Keys.Q,
            "control_key_interact" to Input.Keys.R,
            "control_key_discard" to Input.Keys.T,
            "control_key_close" to Input.Keys.C, // this or hard-coded ESC
            "control_key_zoom" to Input.Keys.Z,

            "control_key_gamemenu" to Input.Keys.TAB,
            "control_key_crafting" to Input.Keys.W,
            "control_key_quicksel" to Input.Keys.SHIFT_LEFT, // pie menu is now LShift because CapsLock is actually used by the my bespoke keyboard input
            "control_mouse_quicksel" to Input.Buttons.MIDDLE, // middle click to open pie menu

            // Colemak, Workman and some typers use CapsLock as Backspace, Apple-JIS and HHKB has Control in place of CapsLock and often re-assigned to Command
            // so these keys are treated as the same.
            // FOR ~~FUCKS~~ERGONOMICS' SAKE DON'T USE CTRL AND ALT AS A KEY!
            "control_key_quickslots" to ((Input.Keys.NUM_1..Input.Keys.NUM_9) + arrayOf(Input.Keys.NUM_0)).map { 1.0*it }.toDoubleArray(),
            "control_key_quickselalt" to intArrayOf(Input.Keys.BACKSPACE, Input.Keys.CONTROL_LEFT, Input.Keys.BACKSLASH).map { 1.0*it }.toDoubleArray(),

            "control_key_toggleime" to Input.Keys.ALT_RIGHT,

            "config_mouseprimary" to Input.Buttons.LEFT, // left mouse
            "config_mousesecondary" to Input.Buttons.RIGHT, // right mouse


            "pcgamepadenv" to "console",

            //"safetywarning" to true,


            "maxparticles" to 768,

            "temperatureunit" to 1, // -1: american, 0: kelvin, 1: celcius


            // "fancy" graphics settings
            "fx_dither" to true,
            "fx_retro" to false,
            "fx_backgroundblur" to true,
            "fx_streamerslayout" to false,
            "fx_differential" to false,
            //"fx_3dlut" to false,

            "basekeyboardlayout" to "en_intl_qwerty",
            "inputmethod" to "none",

            "screenmagnifying" to 1.0,
            "screenmagnifyingfilter" to "none", // "none", "bilinear", "hq2x"

            "fx_newlight" to false,

            "debug_key_deltat_benchmark" to Input.Keys.SLASH,
            "debug_deltat_benchmark_sample_sizes" to 2048,


            "mastervolume" to 1.0,
            "musicvolume" to 1.0,
            "bgmvolume" to 1.0,
            "sfxvolume" to 1.0,

            "lightpasses" to 2,



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