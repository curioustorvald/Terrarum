/* Original code author: Sean Laurvick
 * This code is based on the original author's code written in Lua.
 */

package net.torvald.spriteanimation

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.jme3.math.FastMath
import net.torvald.terrarum.gameactors.ActorWithPhysics
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

class SpriteAnimation(val parentActor: ActorWithPhysics) {

    private lateinit var textureRegion: TextureRegionPack

    var currentFrame = 0
    var currentRow = 0
    var nFrames: Int = 1
        private set
    var nRows: Int = 1
        private set
    var delay = 200f
    private var delta = 0f
    val looping = true
    private var animationRunning = true
    var flipHorizontal = false
    var flipVertical = false
    private val visible: Boolean
        get() = parentActor.isVisible

    private val offsetX = 0
    private val offsetY = 0

    var cellWidth: Int = 0
    var cellHeight: Int = 0

    var colorFilter = Color.WHITE

    fun setSpriteImage(regionPack: TextureRegionPack) {
        textureRegion = regionPack

        cellWidth = regionPack.tileW
        cellHeight = regionPack.tileH
    }

    /**
     * Sets sheet rows and animation frames. Will default to
     * 1, 1 (still image of top left from the sheet) if not called.
     * @param nRows
     * *
     * @param nFrames
     */
    fun setRowsAndFrames(nRows: Int, nFrames: Int) {
        this.nRows = nRows
        this.nFrames = nFrames
    }

    fun update(delta: Float) {
        if (animationRunning) {
            //skip this if animation is stopped
            this.delta += delta

            //check if it's time to advance the frame
            if (this.delta >= this.delay) {
                //if set to not loop, keep the frame at the last frame
                if (this.currentFrame == this.nFrames && !this.looping) {
                    this.currentFrame = this.nFrames - 1
                }

                //advance one frame, then reset delta counter
                this.currentFrame = this.currentFrame % this.nFrames
                this.delta = 0f
            }
        }
    }

    /**
     * Render to specific coordinates. Will assume bottom-center point as image position.
     * Will round to integer.
     * @param g
     * *
     * @param posX bottom-center point
     * *
     * @param posY bottom-center point
     * *
     * @param scale
     */
    @JvmOverloads fun render(batch: SpriteBatch, posX: Float, posY: Float, scale: Float = 1f) {
        if (cellWidth == 0 || cellHeight == 0) {
            throw Error("Sprite width or height is set to zero! ($cellWidth, $cellHeight); master: $parentActor")
        }

        if (visible) {
            val region = textureRegion.get(currentRow, currentFrame)
            batch.color = colorFilter

            batch.draw(region,
                    Math.round(posX).toFloat(),
                    FastMath.floor(posY).toFloat(),
                    FastMath.floor(cellWidth * scale).toFloat(),
                    FastMath.floor(cellHeight * scale).toFloat()
            )
        }
    }

    fun switchRow(newRow: Int) {
        currentRow = newRow % nRows

        //if beyond the frame index then reset
        if (currentFrame > nFrames) {
            reset()
        }
    }

    fun setSpriteDelay(newDelay: Float) {
        if (newDelay > 0) {
            delay = newDelay
        }
        else {
            throw IllegalArgumentException("Delay equal or less than zero")
        }
    }

    fun reset() {
        currentFrame = 1
    }

    fun start() {
        //starts the animation
        animationRunning = true
    }

    fun start(selectFrame: Int) {
        //starts the animation
        animationRunning = true

        //optional: seleft the frame no which to start the animation
        currentFrame = selectFrame
    }

    fun stop() {
        animationRunning = false
    }

    fun stop(selectFrame: Int) {
        animationRunning = false

        currentFrame = selectFrame
    }

    fun flip(horizontal: Boolean, vertical: Boolean) {
        flipHorizontal = horizontal
        flipVertical = vertical
    }

}
