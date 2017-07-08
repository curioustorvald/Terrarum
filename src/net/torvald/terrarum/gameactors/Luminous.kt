package net.torvald.terrarum.gameactors

import com.badlogic.gdx.graphics.Color

/**
 * Created by minjaesong on 16-02-19.
 */
interface Luminous {

    /**
     * Recommended implementation:
     *
    override var luminosity: Color
        get() = Color(
            (actorValue.getAsFloat(AVKey.LUMR) ?: 0f) / LightmapRenderer.MUL_FLOAT,
            (actorValue.getAsFloat(AVKey.LUMG) ?: 0f) / LightmapRenderer.MUL_FLOAT,
            (actorValue.getAsFloat(AVKey.LUMB) ?: 0f) / LightmapRenderer.MUL_FLOAT,
            1f
        )
        set(value) {
            actorValue[AVKey.LUMR] = value.r * LightmapRenderer.MUL_FLOAT
            actorValue[AVKey.LUMG] = value.g * LightmapRenderer.MUL_FLOAT
            actorValue[AVKey.LUMB] = value.b * LightmapRenderer.MUL_FLOAT
        }
     */
    var luminosity: Color

    /**
     * Arguments:
     *
     * Hitbox(x-offset, y-offset, width, height)
     * (Use ArrayList for normal circumstances)
     */
    val lightBoxList: List<Hitbox>
}