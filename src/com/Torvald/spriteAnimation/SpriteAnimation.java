
/* Original code author: Sean Laurvick
 * This code is based on the original author's code written in Lua.
 */

package com.Torvald.spriteAnimation;

import com.Torvald.Terrarum.Game;
import com.Torvald.Terrarum.Terrarum;
import com.jme3.math.FastMath;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.SpriteSheet;

public class SpriteAnimation {

    private SpriteSheet spriteImage;
    private Image[][] sprites;
    private int height;
    private int width;
    private int currentFrame = 1;
    private int currentRow = 1;
    private int nFrames;
    private int nRows;
    private int delay = 200;
    private int delta = 0;
    private boolean looping = true;
    private boolean animationRunning = true;
    private boolean flipHorizontal = false;
    private boolean flipVertical = false;
    private boolean visible = false;

    private int offsetX = 0;
    private int offsetY = 0;

    private float prevScale = 1f;
    private Image currentImage;

    public SpriteAnimation() throws SlickException{

    }

    /**
     * Sets spritesheet.
     * MUST be called AFTER setDimension.
     * @param imagePath path to the sprite sheet image.
     * @throws SlickException
     */
    public void setSpriteImage(String imagePath) throws SlickException {
        spriteImage = new SpriteSheet(imagePath, this.width, this.height);
    }

    /**
     * Sets animation delay. Will default to 200 if not called.
     * @param delay in milliseconds
     */
    public void setDelay(int delay) {
        this.delay = delay;
    }

    /**
     * Sets sprite dimension. This is necessary.
     * @param w
     * @param h
     */
    public void setDimension(int w, int h) {
        width = w;
        height = h;
    }

    /**
     * Sets sheet rows and animation frames. Will default to
     * 1, 1 (still image of top left from the sheet) if not called.
     * @param rows
     * @param frames
     */
    public void setRowsAndFrames(int rows, int frames) {
        nRows = rows;
        nFrames = frames;
    }

    /**
     * Compose (load from spritesheet) as attributes defined.
     * If attributes were not defined, will throw exception of
     * SlickException or ArraySizeException.
     * @throws SlickException
     */
    public void composeSprite() throws SlickException {
        this.sprites = new Image[this.nRows][this.nFrames];

        for (int i=0; i<this.nRows; i++){
            for (int j=0; j<this.nFrames; j++){
                this.sprites[i][j] = spriteImage.getSprite(j, i);
                this.sprites[i][j].setFilter(Image.FILTER_LINEAR);
            }
        }
    }

    public void setAsVisible() {
        visible = true;
    }

    public void setAsInvisible() {
        visible = false;
    }

    public void update(int delta){
        if (animationRunning){//skip this if animation is stopped
            this.delta += delta;

            //check if it's time to advance the frame
            if ( this.delta >= ( this.delay ) ){
                //if set to not loop, keep the frame at the last frame
                if ( this.currentFrame == this.nFrames && !(this.looping) ){
                    this.currentFrame = this.nFrames - 1;
                }

                //advance one frame, then reset delta counter
                this.currentFrame = (this.currentFrame % this.nFrames) + 1;
                this.delta = 0;
            }
        }
    }

    /**
     * Render to specific coordinates. Will assume bottom-center point as image position.
     * Will round to integer.
     * @param g
     * @param posX bottom-center point
     * @param posY bottom-center point
     * @param scale
     */
    public void render(Graphics g, float posX, float posY, float scale){
        scale *= Terrarum.game.getScreenZoom();

        // Null checking
        if (currentImage == null) {
            currentImage = getScaledSprite(scale);
        }

        if (visible) {
            // re-scale image if scale has been changed
            if (prevScale != scale) {
                currentImage = getScaledSprite(scale);
                prevScale = scale;
            }

            Image flippedImage = currentImage.getFlippedCopy(flipHorizontal, flipVertical);

            flippedImage.startUse();
            flippedImage.drawEmbedded(
                      Math.round(posX * Terrarum.game.getScreenZoom())
                    , Math.round(posY * Terrarum.game.getScreenZoom())
                    , FastMath.floor(width * scale)
                    , FastMath.floor(height * scale)
            );
            flippedImage.endUse();
        }
    }

    public void render(Graphics g, float posX, float posY){
        render(g, posX, posY, 1);
    }

    public void switchSprite(int newRow){
        currentRow = newRow;

        //if beyond the frame index then reset
        if (currentFrame > nFrames){
            reset();
        }
    }

    public void switchSprite(int newRow, int newMax){
        if (newMax > 0){
            nFrames = newMax;
        }

        currentRow = newRow;

        //if beyond the frame index then reset
        if (currentFrame > nFrames){
            reset();
        }
    }

    public void switchSpriteDelay(int newDelay){
        if (newDelay > 0){
            delay = newDelay;
        }
    }

    public void switchSprite(int newRow, int newMax, int newDelay){
        if (newMax > 0){
            nFrames = newMax;
        }

        if (newDelay > 0){
            delay = newDelay;
        }

        currentRow = newRow;

        //if beyond the frame index then reset
        if (currentFrame > nFrames){
            reset();
        }
    }

    public void reset(){
        currentFrame = 1;
    }

    public void start(){ //starts the animation
        animationRunning = true;
    }

    public void start(int selectFrame){ //starts the animation
        animationRunning = true;

        //optional: seleft the frame no which to start the animation
        currentFrame = selectFrame;
    }

    public void stop(){
        animationRunning = false;
    }

    public void stop(int selectFrame){
        animationRunning = false;

        currentFrame = selectFrame;
    }

    public void flip(boolean horizontal, boolean vertical){
        flipHorizontal = horizontal;
        flipVertical = vertical;
    }

    public boolean flippedHorizontal() {
        return flipHorizontal;
    }

    public boolean flippedVertical() {
        return flipVertical;
    }

    public int getWidth(){
        return width;
    }

    public int getHeight(){
        return height;
    }

    private Image getScaledSprite(float scale) {
        Image selectedImage = sprites[currentRow - 1][currentFrame - 1];

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
        selectedImage.setFilter(Image.FILTER_NEAREST);
        return selectedImage.getScaledCopy(scale);
    }
}
