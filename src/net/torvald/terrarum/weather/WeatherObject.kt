package net.torvald.terrarum.weather

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable

/**
 * Created by minjaesong on 2023-08-21.
 */
abstract class WeatherObject : Disposable {

    /** vec3(posX, posY, scale) */
    var pos: Vector3 = Vector3(0f, 0f, 1f)

    var posX: Float
        get() = pos.x
        set(value) { pos.x = value }
    var posY: Float
        get() = pos.y
        set(value) { pos.y = value }
    var posZ: Float
        get() = pos.z
        set(value) { pos.z = value }
    var scale: Float = 1f

    var alpha: Float = 1f

    var flagToDespawn = false

    abstract fun update()
    abstract fun render(batch: SpriteBatch, offsetX: Float, offsetY: Float)

}