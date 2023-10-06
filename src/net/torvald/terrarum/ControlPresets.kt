package net.torvald.terrarum

import com.badlogic.gdx.Input

/**
 * Created by minjaesong on 2023-08-24.
 */
object ControlPresets {

    val wasd = hashMapOf<String, Int>(
        "control_key_up" to Input.Keys.W,
        "control_key_left" to Input.Keys.A,
        "control_key_down" to Input.Keys.S,
        "control_key_right" to Input.Keys.D,

        "control_key_jump" to Input.Keys.SPACE,
        "control_key_movementaux" to Input.Keys.SHIFT_LEFT, // movement-auxiliary, or hookshot
        "control_key_inventory" to Input.Keys.Q,
        "control_key_interact" to Input.Keys.E,
        "control_key_discard" to Input.Keys.R,
        "control_key_close" to Input.Keys.X, // this or hard-coded ESC
        "control_key_zoom" to Input.Keys.Z,

        "control_key_gamemenu" to Input.Keys.TAB,
        "control_key_crafting" to Input.Keys.F,
        "control_key_quicksel" to Input.Keys.CONTROL_LEFT, // pie menu is now LShift because CapsLock is actually used by the my bespoke keyboard input
    )

    val esdf = hashMapOf<String, Int>(
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
    )

    val ijkl = hashMapOf<String, Int>(
        "control_key_up" to Input.Keys.I,
        "control_key_left" to Input.Keys.J,
        "control_key_down" to Input.Keys.K,
        "control_key_right" to Input.Keys.L,

        "control_key_jump" to Input.Keys.SPACE,
        "control_key_movementaux" to Input.Keys.SEMICOLON, // movement-auxiliary, or hookshot
        "control_key_inventory" to Input.Keys.P,
        "control_key_interact" to Input.Keys.U,
        "control_key_discard" to Input.Keys.Y,
        "control_key_close" to Input.Keys.M, // this or hard-coded ESC
        "control_key_zoom" to Input.Keys.SLASH,

        "control_key_gamemenu" to Input.Keys.LEFT_BRACKET,
        "control_key_crafting" to Input.Keys.O,
        "control_key_quicksel" to Input.Keys.APOSTROPHE, // pie menu is now LShift because CapsLock is actually used by the my bespoke keyboard input
    )

    val empty = hashMapOf<String, Int>()

    val presets = hashMapOf( // unordered
        "WASD" to wasd,
        "ESDF" to esdf,
        "IJKL" to ijkl,
        "Custom" to empty,
    )

    val presetLabels = listOf( // ordered
        "WASD",
        "ESDF",
        "IJKL",
        "Custom",
    )

    fun getKey(label: String?): Int {
        if (label == null) return -1

        val presetName = App.getConfigString("control_preset_keyboard") ?: "Custom"

        return (presets[presetName] ?: throw IllegalStateException("No such keyboard preset: $presetName")).getOrDefault(label, App.getConfigInt(label))
    }
}