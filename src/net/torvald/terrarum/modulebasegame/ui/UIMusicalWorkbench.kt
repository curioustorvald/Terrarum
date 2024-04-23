package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.modulebasegame.gameactors.FixtureMechanicalTines.Companion.findSetBits
import net.torvald.terrarum.ui.UICanvas

/**
 * Created by minjaesong on 2024-04-22.
 */
class UIMusicalWorkbench : UICanvas(
    toggleKeyLiteral = "control_key_inventory",
    toggleButtonLiteral = "control_gamepad_start"
) {

    data class ComposerRow(
        var notes: Long,
        var len: Int,
        var arp: Int // 0: none, 1: up, 2: down
    ) {
        fun toPunchedNotes(): List<Long> {
            val ret = MutableList<Long>(len) { 0L }

            when (arp) {
                0 -> { ret[0] = notes }
                1, 2 -> {
                    val arpNotes = findSetBits(notes).let { if (arp == 2) it.reversed() else it }

                    for (i in 0 until minOf(len, arpNotes.size)) {
                        ret[i] = 1L shl arpNotes[i]
                    }
                }
            }

            return ret
        }
    }


    private val rowBuf = ArrayList<ComposerRow>()
    override var width: Int
        get() = TODO("Not yet implemented")
        set(value) {}
    override var height: Int
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun updateImpl(delta: Float) {
        TODO("Not yet implemented")
    }

    override fun renderImpl(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }


}

