package net.torvald.spriteanimation

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.jme3.math.FastMath
import net.torvald.terrarum.Second
import net.torvald.terrarum.gameactors.ActorWithBody

/**
 * Created by minjaesong on 2024-02-14.
 */
class SingleImageSprite(parentActor: ActorWithBody, val img: TextureRegion) : SpriteAnimation(parentActor) {

    override val currentDelay: Second = 1f

    override fun update(delta: Float) {
    }

    var cellWidth: Int = img.regionWidth
    var cellHeight: Int = img.regionHeight

    override fun render(
        frameDelta: Float,
        batch: SpriteBatch,
        posX: Float,
        posY: Float,
        scale: Float,
        mode: Int,
        forcedColourFilter: Color?
    ) {
        batch.color = forcedColourFilter ?: colourFilter

        val tx = (parentActor.hitboxTranslateX) * scale
        val txF = (parentActor.hitboxTranslateX + parentActor.baseHitboxW) * scale
        val ty = (parentActor.hitboxTranslateY + (cellHeight - parentActor.baseHitboxH)) * scale
        val tyF = (parentActor.hitboxTranslateY + parentActor.baseHitboxH) * scale

        if (flipHorizontal && flipVertical) {
            batch.draw(img,
                FastMath.floor(posX).toFloat() + txF,
                FastMath.floor(posY).toFloat() + tyF,
                -FastMath.floor(cellWidth * scale).toFloat(),
                -FastMath.floor(cellHeight * scale).toFloat()
            )
        }
        else if (flipHorizontal && !flipVertical) {
            batch.draw(img,
                FastMath.floor(posX).toFloat() + txF,
                FastMath.floor(posY).toFloat() - ty,
                -FastMath.floor(cellWidth * scale).toFloat(),
                FastMath.floor(cellHeight * scale).toFloat()
            )
        }
        else if (!flipHorizontal && flipVertical) {
            batch.draw(img,
                FastMath.floor(posX).toFloat() - tx,
                FastMath.floor(posY).toFloat() + tyF,
                FastMath.floor(cellWidth * scale).toFloat(),
                -FastMath.floor(cellHeight * scale).toFloat()
            )
        }
        else {
            batch.draw(img,
                FastMath.floor(posX).toFloat() - tx,
                FastMath.floor(posY).toFloat() - ty,
                FastMath.floor(cellWidth * scale).toFloat(),
                FastMath.floor(cellHeight * scale).toFloat()
            )
        }
    }

    override fun dispose() {
    }

}