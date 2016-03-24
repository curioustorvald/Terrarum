/* Original code author: Sean Laurvick
 * This code is based on the original author's code written in Lua.
 */

package com.Torvald.spriteAnimation

import com.Torvald.Terrarum.Game
import com.Torvald.Terrarum.Terrarum
import com.jme3.math.FastMath
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image
import org.newdawn.slick.SlickException
import org.newdawn.slick.SpriteSheet

class SpriteAnimation @Throws(SlickException::class)
constructor() {

    private var spriteImage: SpriteSheet? = null
    var height: Int = 0
        private set
    var width: Int = 0
        private set
    private var currentFrame = 1
    private var currentRow = 1
    private var nFrames: Int = 0
    private var nRows: Int = 0
    private var delay = 200
    private var delta = 0
    private val looping = true
    private var animationRunning = true
    private var flipHorizontal = false
    private var flipVertical = false
    private var visible = false

    private val offsetX = 0
    private val offsetY = 0

    private var prevScale = 1f
    private var currentImage: Image? = null

    /**
     * Sets spritesheet.
     * MUST be called AFTER setDimension.
     * @param imagePath path to the sprite sheet image.
     * *
     * @throws SlickException
     */
    @Throws(SlickException::class)
    fun setSpriteImage(imagePath: String) {
        spriteImage = SpriteSheet(imagePath, this.width, this.height)
    }

    /**
     * Sets animation delay. Will default to 200 if not called.
     * @param delay in milliseconds
     */
    fun setDelay(delay: Int) {
        this.delay = delay
    }

    /**
     * Sets sprite dimension. This is necessary.
     * @param w
     * *
     * @param h
     */
    fun setDimension(w: Int, h: Int) {
        width = w
        height = h
    }

    /**
     * Sets sheet rows and animation frames. Will default to
     * 1, 1 (still image of top left from the sheet) if not called.
     * @param rows
     * *
     * @param frames
     */
    fun setRowsAndFrames(rows: Int, frames: Int) {
        nRows = rows
        nFrames = frames
    }

    fun setAsVisible() {
        visible = true
    }

    fun setAsInvisible() {
        visible = false
    }

    fun update(delta: Int) {
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
                this.currentFrame = this.currentFrame % this.nFrames + 1
                this.delta = 0
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
    @JvmOverloads fun render(g: Graphics, posX: Float, posY: Float, scale: Float = 1f) {
        var scale = scale
        scale *= Terrarum.game.screenZoom

        // Null checking
        if (currentImage == null) {
            currentImage = getScaledSprite(scale)
        }

        if (visible) {
            // re-scale image if scale has been changed
            if (prevScale != scale) {
                currentImage = getScaledSprite(scale)
                prevScale = scale
            }

            val flippedImage = currentImage!!.getFlippedCopy(flipHorizontal, flipVertical)

            flippedImage.startUse()
            flippedImage.drawEmbedded(
                    Math.round(posX * Terrarum.game.screenZoom).toFloat(),
                    Math.round(posY * Terrarum.game.screenZoom).toFloat(),
                    FastMath.floor(width * scale).toFloat(),
                    FastMath.floor(height * scale).toFloat()
            )
            flippedImage.endUse()
        }
    }

    fun switchSprite(newRow: Int) {
        currentRow = newRow

        //if beyond the frame index then reset
        if (currentFrame > nFrames) {
            reset()
        }
    }

    fun switchSprite(newRow: Int, newMax: Int) {
        if (newMax > 0) {
            nFrames = newMax
        }

        currentRow = newRow

        //if beyond the frame index then reset
        if (currentFrame > nFrames) {
            reset()
        }
    }

    fun switchSpriteDelay(newDelay: Int) {
        if (newDelay > 0) {
            delay = newDelay
        }
    }

    fun switchSprite(newRow: Int, newMax: Int, newDelay: Int) {
        if (newMax > 0) {
            nFrames = newMax
        }

        if (newDelay > 0) {
            delay = newDelay
        }

        currentRow = newRow

        //if beyond the frame index then reset
        if (currentFrame > nFrames) {
            reset()
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

    fun flippedHorizontal(): Boolean {
        return flipHorizontal
    }

    fun flippedVertical(): Boolean {
        return flipVertical
    }

    private fun getScaledSprite(scale: Float): Image {
        val selectedImage = spriteImage!!.getSprite(currentFrame - 1, currentRow - 1)
        //Image selectedImage = sprites[currentRow - 1][currentFrame - 1];

        // resample
        /*float nearestResampleScale = (scale > 1) ? Math.round(scale) : 1;
        float linearResampleScale = scale / nearestResampleScale;

        // scale 1.8 -> resample in 2(nearest), then resample in 0.9(linear)
        // scale by nearestResampleScale (2, 3, ...)
        selectedImage.setFilter(Image.FILTER_NEAREST);
        Image selImgNearestScaled = selectedImage.getScaledCopy(nearestResampleScale);
        // scale by linearResampleScale (.x)
        Image selImgLinearScaled;
        if (scale % 1 > 0) {
            selImgNearestScaled.setFilter(Image.FILTER_LINEAR);
            selImgLinearScaled = selImgNearestScaled.getScaledCopy(linearResampleScale);
            return selImgLinearScaled;
        }
        else {
            return selImgNearestScaled;
        }*/
        selectedImage.filter = Image.FILTER_NEAREST
        return selectedImage.getScaledCopy(scale)
    }
}
