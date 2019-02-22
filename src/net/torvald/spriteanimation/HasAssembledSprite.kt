package net.torvald.spriteanimation

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import net.torvald.spriteassembler.ADProperties
import net.torvald.spriteassembler.AssembleSheetPixmap
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2019-01-18.
 */
interface HasAssembledSprite {

    var animDescPath: String

    // FIXME sometimes the animmation is invisible (row and nFrames mismatch -- row is changed to 1 but it's drawing 3rd frame?)

    fun reassembleSprite(sprite: SpriteAnimation) {
        _rebuild(ADProperties(Gdx.files.internal(animDescPath).read()), sprite)
    }

    /*fun rebuild(animDescPath: String, spriteAnimation: SpriteAnimation) {
        _rebuild(ADProperties(StringReader(animDescPath)), spriteAnimation)
    }

    fun rebuild(animDesc: FileHandle, spriteAnimation: SpriteAnimation) {
        _rebuild(ADProperties(animDesc.read()), spriteAnimation)
    }

    fun rebuild(javaProp: Properties, spriteAnimation: SpriteAnimation) {
        _rebuild(ADProperties(javaProp), spriteAnimation)
    }*/


    private fun _rebuild(ad: ADProperties, sprite: SpriteAnimation) {
        // TODO injecting held item/armour pictures? Would it be AssembleSheetPixmap's job?

        val pixmap = AssembleSheetPixmap(ad)
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

}
