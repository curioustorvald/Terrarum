package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.BlendMode
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.fillRect

/**
 * Created by minjaesong on 2017-07-16.
 */
open class UIItemImageButton(
        parent: UICanvas,
        val image: TextureRegion,

        val buttonCol: Color = Color.WHITE,
        val buttonBackCol: Color = Color(0),
        val buttonBackBlendMode: String = BlendMode.NORMAL,

        val activeCol: Color = Color(0xfff066_ff.toInt()),
        val activeBackCol: Color = Color(0xb0b0b0_ff.toInt()),
        val activeBackBlendMode: String = BlendMode.MULTIPLY,

        override var posX: Int,
        override var posY: Int,
        override val width: Int = image.regionWidth,
        override val height: Int = image.regionHeight
) : UIItem(parent) {


    override fun render(batch: SpriteBatch) {
        // draw background
        if (mouseUp) {
            BlendMode.resolve(activeBackBlendMode)
            batch.color = activeBackCol
        }
        else {
            BlendMode.resolve(buttonBackBlendMode)
            batch.color = buttonBackCol
        }

        batch.fillRect(posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())


        // draw image
        blendNormal()

        batch.color = if (mouseUp) activeCol else buttonCol
        batch.draw(image, (posX + (width - image.regionWidth) / 2).toFloat(), (posY + (height - image.regionHeight) / 2).toFloat())
    }

    override fun dispose() {
        image.texture.dispose()
    }
}