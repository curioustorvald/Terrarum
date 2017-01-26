/* Original code author: Sean Laurvick
 * This code is based on the original author's code written in Lua.
 */

package net.torvald.spriteanimation

import com.jme3.math.FastMath
import net.torvald.terrarum.gameactors.ActorWithSprite
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image
import org.newdawn.slick.SlickException
import org.newdawn.slick.SpriteSheet

class SpriteAnimation(val parentActor: ActorWithSprite, val cellWidth: Int, val cellHeight: Int) {

    private var spriteImage: SpriteSheet? = null
    private var currentFrame = 1 // one-based!
    private var currentRow = 1   // one-based!
    private var nFrames: Int = 1
    private var nRows: Int = 1
    private var delay = 200
    private var delta = 0
    private val looping = true
    private var animationRunning = true
    private var flipHorizontal = false
    private var flipVertical = false
    private val visible: Boolean
        get() = parentActor.isVisible

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
    fun setSpriteImage(imagePath: String) {
        spriteImage = SpriteSheet(imagePath, cellWidth, cellHeight)
    }

    fun setSpriteImage(image: Image) {
        spriteImage = SpriteSheet(image, cellWidth, cellHeight)
    }

    /**
     * Sets animation delay. Will default to 200 if not called.
     * @param delay in milliseconds
     */
    fun setDelay(delay: Int) {
        this.delay = delay
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
        if (cellWidth == 0 || cellHeight == 0) {
            throw Error("Sprite width or height is set to zero! ($cellWidth, $cellHeight); master: $parentActor")
        }

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
            flippedImage.draw(
                    Math.round(posX).toFloat(),
                    FastMath.floor(posY).toFloat(),
                    FastMath.floor(cellWidth * scale).toFloat(),
                    FastMath.floor(cellHeight * scale).toFloat()
            )
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
        selectedImage.filter = Image.FILTER_NEAREST
        return selectedImage.getScaledCopy(scale)
    }
}
