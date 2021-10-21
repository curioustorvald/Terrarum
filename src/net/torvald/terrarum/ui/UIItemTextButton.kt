package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.BlendMode
import net.torvald.terrarum.blendNormal
import net.torvald.terrarum.langpack.Lang

/**
 * Text button. Height of hitbox is extended (double lineHeight, or 40 px) for better clicking
 *
 * Created by minjaesong on 2017-03-13.
 */
open class UIItemTextButton(
        parentUI: UICanvas,
        /** Stored text (independent to the Langpack) */
        val labelText: String,
        initialX: Int,
        initialY: Int,
        override val width: Int,
        val readFromLang: Boolean = false,

        /** Colour when mouse is over */
        val activeCol: Color = Toolkit.Theme.COL_ACTIVE,
        /** Colour when mouse is over */
        val activeBackCol: Color = UIItemTextButtonList.DEFAULT_BACKGROUND_ACTIVECOL,
        /** Colour when mouse is over */
        val activeBackBlendMode: String = BlendMode.NORMAL,
        /** Colour when clicked/selected */
        val highlightCol: Color = Toolkit.Theme.COL_HIGHLIGHT,
        /** Colour when clicked/selected */
        val highlightBackCol: Color = UIItemTextButtonList.DEFAULT_BACKGROUND_HIGHLIGHTCOL,
        /** Colour when clicked/selected */
        val highlightBackBlendMode: String = BlendMode.NORMAL,
        /** Colour on normal status */
        val inactiveCol: Color = Toolkit.Theme.COL_LIST_DEFAULT,
        val backgroundCol: Color = UIItemTextButtonList.DEFAULT_BACKGROUNDCOL,
        val backgroundBlendMode: String = BlendMode.NORMAL,


        val paddingLeft:  Int = 0,
        val paddingRight: Int = 0,

        val alignment: Alignment = Alignment.CENTRE,
        val hitboxSize: Int = UIItemTextButton.height,

        val tags: Array<String> = arrayOf("")
) : UIItem(parentUI, initialX, initialY) {

    companion object {
        val font = App.fontGame
        val height = font.lineHeight.toInt()

        enum class Alignment {
            CENTRE, LEFT, RIGHT
        }
    }

    /** Actually displayed text (changes with the app language) */
    val label: String
        get() = if (readFromLang) Lang[labelText] else labelText


    override val height: Int = hitboxSize

    var highlighted: Boolean = false

    override fun update(delta: Float) {
        super.update(delta)


    }

    override fun render(batch: SpriteBatch, camera: Camera) {
        val textW = font.getWidth(label)


        // draw background
        /*if (highlighted) {
            BlendMode.resolve(highlightBackBlendMode, batch)
            batch.color = highlightBackCol
            Toolkit.fillArea(batch, posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())
        }
        else if (mouseUp) {
            BlendMode.resolve(activeBackBlendMode, batch)
            batch.color = activeBackCol
            Toolkit.fillArea(batch, posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())
        }
        else {
            batch.color = backgroundCol
            BlendMode.resolve(backgroundBlendMode, batch)
            Toolkit.fillArea(batch, posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())
        }*/


        blendNormal(batch)


        batch.color = if (highlighted) highlightCol
        else if (mouseUp) activeCol
        else inactiveCol

        font.draw(batch,
                label,
//                "$label/H:${highlighted.toInt()}, M:${mouseUp.toInt()}",
                when (alignment) {
                    Alignment.CENTRE -> posX.toFloat() + width.minus(textW).div(2) + (paddingLeft - paddingRight).div(2)
                    Alignment.LEFT -> posX.toFloat() + paddingLeft
                    Alignment.RIGHT -> width - paddingRight - textW.toFloat()
                },
                posY.toFloat() + (hitboxSize - UIItemTextButton.height) / 2f
        )
    }

    override fun dispose() {
    }
}
