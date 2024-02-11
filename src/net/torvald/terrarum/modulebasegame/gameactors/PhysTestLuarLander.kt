package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.gameactors.*

/**
 * Created by minjaesong on 2018-01-17.
 */
class PhysTestLuarLander : ActorWithBody(RenderOrder.MIDTOP, PhysProperties.PHYSICS_OBJECT), Controllable {

    @Transient private val texture = Texture(ModMgr.getGdxFile("basegame", "sprites/phystest_lunarlander.tga"))

    override val hitbox: Hitbox

    init {
        hitbox = Hitbox(0.0, 0.0, 0.0, 0.0)
        setHitboxDimension(texture.width, texture.height, 0, 0)

        actorValue[AVKey.SPEED] = 8.0
        avBaseMass = 18650.0
    }

    override fun run() {
        super.run()
    }

    override fun updateImpl(delta: Float) {
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            controllerV!!.y = avSpeedCap
        }
    }

    override fun keyDown(keycode: Int): Boolean {
        return true
    }

    override fun drawGlow(frameDelta: Float, batch: SpriteBatch) {
    }

    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        batch.color = Color.WHITE
        batch.draw(texture, hitbox.startX.toFloat(), hitbox.endY.toFloat(), hitbox.width.toFloat(), -hitbox.height.toFloat())
    }

    override fun onActorValueChange(key: String, value: Any?) {
        super.onActorValueChange(key, value)
    }

    override fun dispose() {
        super.dispose()
        texture.dispose()
    }
}