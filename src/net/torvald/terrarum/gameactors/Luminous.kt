package net.torvald.terrarum.gameactors

import com.badlogic.gdx.graphics.Color

/**
 * Created by minjaesong on 2016-02-19.
 */
interface Luminous {

    /**
     * Range of 0.0 - 4.0 for each channel
     *
     * Recommended implementation:
     *
    override var color: Color
        get() = Color(
            (actorValue.getAsFloat(AVKey.LUMR) ?: 0f) / LightmapRenderer.MUL_FLOAT,
            (actorValue.getAsFloat(AVKey.LUMG) ?: 0f) / LightmapRenderer.MUL_FLOAT,
            (actorValue.getAsFloat(AVKey.LUMB) ?: 0f) / LightmapRenderer.MUL_FLOAT,
            (actorValue.getAsFloat(AVKey.LUMA) ?: 0f) / LightmapRenderer.MUL_FLOAT,
        )
        set(value) {
            actorValue[AVKey.LUMR] = value.r * LightmapRenderer.MUL_FLOAT
            actorValue[AVKey.LUMG] = value.g * LightmapRenderer.MUL_FLOAT
            actorValue[AVKey.LUMB] = value.b * LightmapRenderer.MUL_FLOAT
            actorValue[AVKey.LUMA] = value.a * LightmapRenderer.MUL_FLOAT
        }
     */
    var color: Color

    /**
     * Arguments:
     *
     * Hitbox(x-offset, y-offset, width, height)
     * (Use ArrayList for normal circumstances)
     */
    val lightBoxList: List<Hitbox>
}