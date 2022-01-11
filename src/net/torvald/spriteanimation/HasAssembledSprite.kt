package net.torvald.spriteanimation

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.spriteassembler.ADProperties
import net.torvald.terrarum.spriteassembler.AssembleSheetPixmap
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2019-01-18.
 */
interface HasAssembledSprite {

    /** ADL for main sprite. Necessary. */
    var animDesc: ADProperties?
    /** ADL for glow sprite. Optional. */
    var animDescGlow: ADProperties?

    var spriteHeadTexture: TextureRegion?

    // FIXME sometimes the animmation is invisible (row and nFrames mismatch -- row is changed to 1 but it's drawing 3rd frame?)

    /**
     * Example usage:
     * ```
     * this.animDescPath = "..."
     * this.animDescPathGlow = "..."
     * this.sprite = SpriteAnimation(actor)
     * this.spriteGlow = SpriteAnimation(actor)
     * reassembleSprite(this.sprite, this.spriteGlow)
     * ```
     */
    fun reassembleSprite(sprite: SpriteAnimation?, spriteGlow: SpriteAnimation? = null, item: GameItem?) {
        if (animDesc != null && sprite != null)
            _rebuild(animDesc!!, sprite, null)
        if (animDescGlow != null && spriteGlow != null)
            _rebuild(animDescGlow!!, spriteGlow, null)
    }

    /*fun reassembleSprite(disk: SimpleFileSystem, sprite: SpriteAnimation?, anim: ADProperties?, spriteGlow: SpriteAnimation? = null, animGlow: ADProperties? = null) {
        if (anim != null && sprite != null)
            _rebuild(disk, anim, sprite)
        if (animGlow != null && spriteGlow != null)
            _rebuild(disk, animGlow, spriteGlow)
    }*/

    private fun _rebuild(ad: ADProperties, sprite: SpriteAnimation, item: GameItem?) {
        // TODO injecting held item/armour pictures? Would it be AssembleSheetPixmap's job?

        val pixmap = AssembleSheetPixmap.fromAssetsDir(ad, item)
        val texture = Texture(pixmap)
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        pixmap.dispose()
        val regionPack = TextureRegionPack(texture, ad.frameWidth, ad.frameHeight)

        val newAnimDelays = FloatArray(ad.animations.size)
        val newAnimFrames = IntArray(ad.animations.size)

        ad.animations.forEach { t, u ->
            val index = u.row - 1
            newAnimDelays[index] = u.delay
            newAnimFrames[index] = u.frames
        }

        sprite.setSpriteImage(regionPack)
        sprite.delays = newAnimDelays
        sprite.nFrames = newAnimFrames
        sprite.nRows = newAnimDelays.size
    }

    /*private fun _rebuild(disk: SimpleFileSystem, ad: ADProperties, sprite: SpriteAnimation) {
        // TODO injecting held item/armour pictures? Would it be AssembleSheetPixmap's job?

        val pixmap = if (disk.getEntry(-1025) != null) AssembleSheetPixmap.fromVirtualDisk(disk, ad) else AssembleSheetPixmap.fromAssetsDir(ad)
        val texture = Texture(pixmap)
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        pixmap.dispose()
        val regionPack = TextureRegionPack(texture, ad.frameWidth, ad.frameHeight)

        val newAnimDelays = FloatArray(ad.animations.size)
        val newAnimFrames = IntArray(ad.animations.size)

        ad.animations.forEach { t, u ->
            val index = u.row - 1
            newAnimDelays[index] = u.delay
            newAnimFrames[index] = u.frames
        }

        sprite.setSpriteImage(regionPack)
        sprite.delays = newAnimDelays
        sprite.nFrames = newAnimFrames
        sprite.nRows = newAnimDelays.size
    }*/

}
