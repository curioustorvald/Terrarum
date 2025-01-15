package net.torvald.terrarum.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.imagefont.BigAlphNum
import net.torvald.terrarum.utils.PasswordBase32
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2025-01-15.
 */
class UIItemRedeemCodeArea(
    parentUI: UICanvas,
    initialX: Int,
    initialY: Int,
    val textCols: Int,
    val textRows: Int,

) : UIItem(parentUI, initialX, initialY) {

    private val CELL_W = 16
    private val CELL_H = 24

    override val width = textCols * CELL_W
    override val height = textRows * CELL_H

    init {
        CommonResourcePool.addToLoadingList("spritesheet:terrarum_redeem_code_form") {
            TextureRegionPack(Gdx.files.internal("assets/graphics/code_input_cells.tga"), CELL_W, CELL_H)
        }
        CommonResourcePool.loadAll()
    }

    private var inputFormTiles = CommonResourcePool.getAsTextureRegionPack("spritesheet:terrarum_redeem_code_form")


    private val inputText = StringBuilder(textCols * textRows)
    private var textCaret = 0
    fun clearInput() { inputText.clear(); textCaret = 0 }
    fun acceptChar(char: Char): Boolean {
        if (textCaret in 0 until textCols * textRows) {
            inputText.insert(textCaret, char)
            textCaret++
            return true
        }
        else return false
    }
    fun backspace(): Boolean {
        if (textCaret in 1 until textCols * textRows) {
            inputText.deleteCharAt(textCaret - 1)
            textCaret--
            return true
        }
        else return false
    }
    fun reverseBackspace(): Boolean {
        if (textCaret in 0 until (textCols * textRows - 1)) {
            inputText.deleteCharAt(textCaret)
            return true
        }
        else return false
    }
    fun __moveCursorBackward(delta: Int = 1) {
        textCaret = (textCaret - 1).coerceIn(0, textCols * textRows)
    }
    fun __moveCursorForward(delta: Int = 1) {
        textCaret = (textCaret + 1).coerceIn(0, textCols * textRows)
    }

    fun getInputAsString() = inputText.toString()
    fun getInputAsBinary(codebook: RedeemCodebook) = codebook.toBinary(inputText.toString())

    private val caretCol = Toolkit.Theme.COL_SELECTED

    override fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) {
        super.render(frameDelta, batch, camera)

        val lineCol = if (isActive) Toolkit.Theme.COL_MOUSE_UP else Toolkit.Theme.COL_INACTIVE

        // draw border
        batch.color = lineCol
        Toolkit.drawBoxBorder(batch, posX, posY, width, height)

        // draw cells
        batch.color = Toolkit.Theme.COL_INACTIVE
        for (y in 0 until textRows) {
            for (x in 0 until textCols) {
                batch.draw(inputFormTiles.get(if (x == 0) 0 else if (x == textCols - 1) 2 else 1, 0),
                    posX.toFloat() + CELL_W * x, posY.toFloat() + CELL_H * y
                )
            }
        }

        // draw texts
        batch.color = Color.WHITE
        for (y in 0 until textRows) {
            for (x in 0 until textCols) {
                BigAlphNum.draw(
                    batch,
                    "${inputText.getOrElse(y * textRows + x) { 'A' }}",
                    posX + CELL_W * x + 2f,
                    posY + CELL_H * y + 4f
                )
            }
        }

        // draw caret
        batch.color = caretCol
        val cx = textCaret % textCols
        val cy = textCaret / textCols
        Toolkit.drawStraightLine(batch,
            posX + CELL_W * cx - 1,
            posY + CELL_H * cy + 1,
            posY + CELL_H * cy + 1 + 20, 2, true)
    }

    override fun dispose() {
    }
}

interface RedeemCodebook {
    fun toBinary(inputString: String): ByteArray

    companion object {
        object Base32RedeemCodebook : RedeemCodebook {
            override fun toBinary(inputString: String): ByteArray {
                return PasswordBase32.decode(inputString, inputString.length)
            }
        }
    }
}