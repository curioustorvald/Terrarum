package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.CreditSingleton
import net.torvald.terrarum.Second
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIItemTextArea

open class UITitleWallOfText(private val text: List<String>) : UICanvas() {


    private val textAreaHMargin = 48
    override var width = 600
    override var height = App.scr.height - textAreaHMargin * 2
    private val textArea = UIItemTextArea(this,
            (App.scr.width - width) / 2, textAreaHMargin,
            width, height
    )


    init {
        uiItems.add(textArea)

        textArea.setWallOfText(text)
    }

    override fun updateUI(delta: Float) {
        textArea.update(delta)
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        batch.color = Color.WHITE
        textArea.render(batch, camera)

        //AppLoader.printdbg(this, "Rendering texts of length ${text.size}")
    }

    override fun dispose() {
    }
}

class UITitleCredits(val remoCon: UIRemoCon) : UITitleWallOfText(CreditSingleton.credit)
class UITitleGPL3(val remoCon: UIRemoCon) : UITitleWallOfText(CreditSingleton.gpl3)
class UISystemInfo(val remoCon: UIRemoCon) : UITitleWallOfText(CreditSingleton.systeminfo)