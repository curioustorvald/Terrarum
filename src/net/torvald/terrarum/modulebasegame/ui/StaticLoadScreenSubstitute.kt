package net.torvald.terrarum.modulebasegame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.gdxClearAndEnableBlend
import net.torvald.terrarum.langpack.Lang

/**
 * Created by minjaesong on 2023-07-08.
 */
object StaticLoadScreenSubstitute {

    operator fun invoke(batch: SpriteBatch) {
        batch.end()

        gdxClearAndEnableBlend(.063f, .070f, .086f, 1f)

        batch.begin()

        batch.color = Color.WHITE
        val txt = Lang["MENU_IO_LOADING"]
        App.fontGame.draw(batch, txt, (App.scr.width - App.fontGame.getWidth(txt)) / 2f, (App.scr.height - App.fontGame.lineHeight) / 2f)

    }

}