package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.Color
import net.torvald.terrarum.BlendMode
import net.torvald.terrarum.ui.UIItemTextButton.Companion.Alignment

/**
 * Created by minjaesong on 2025-01-22.
 */
class UIItemTextLabel(
    parentUI: UICanvas,
    /** Stored text (independent to the Langpack) */
    textfun: () -> String,
    initialX: Int,
    initialY: Int,
    override val width: Int,

    colour: Color = Color.WHITE,

    hasBorder: Boolean = false,

    paddingLeft:  Int = 0,
    paddingRight: Int = 0,

    alignment: Alignment = Alignment.CENTRE,

    tags: Array<String> = arrayOf("")
) : UIItemTextButton(
    parentUI, textfun, initialX, initialY, width,
    colour, colour, BlendMode.NORMAL, colour, colour, BlendMode.NORMAL,

    hasBorder = hasBorder,
    paddingLeft = paddingLeft,
    paddingRight = paddingRight,
    alignment = alignment,
    tags = tags

)