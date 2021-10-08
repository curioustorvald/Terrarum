package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import net.torvald.spriteanimation.HasAssembledSprite
import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.spriteassembler.ADProperties
import net.torvald.spriteassembler.AssembleSheetPixmap
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.tvda.SimpleFileSystem
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import java.util.*


/**
 * Game player (YOU!)
 *
 * Created by minjaesong on 2015-12-31.
 */

class IngamePlayer : ActorHumanoid {

    var uuid = UUID.randomUUID(); private set
    var worldCurrentlyPlaying: UUID = UUID(0L,0L) // only filled up on save and load; DO NOT USE THIS

    /** ADL for main sprite. Necessary. */
    @Transient var animDesc: ADProperties? = null
    /** ADL for glow sprite. Optional. */
    @Transient var animDescGlow: ADProperties? = null


    private constructor()

    constructor(animDescPath: String, animDescPathGlow: String?, born: Long) : super(born) {
        animDesc = ADProperties(Gdx.files.internal(animDescPath))
        if (animDescPathGlow != null) animDescGlow = ADProperties(Gdx.files.internal(animDescPathGlow))
        actorValue[AVKey.__HISTORICAL_BORNTIME] = born
    }

    /**
     * Creates new Player instance with empty elements (sprites, actorvalue, etc.).

     * **Use PlayerFactory to build player!**
     */
    init {
        referenceID = Terrarum.PLAYER_REF_ID // TODO assign random ID
        density = BASE_DENSITY
        collisionType = COLLISION_KINEMATIC
    }




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
    fun reassembleSprite(sprite: SpriteAnimation?, anim: ADProperties?, spriteGlow: SpriteAnimation? = null, animGlow: ADProperties? = null) {
        if (anim != null && sprite != null)
            _rebuild(anim, sprite)
        if (animGlow != null && spriteGlow != null)
            _rebuild(animGlow, spriteGlow)
    }

    fun reassembleSprite(disk: SimpleFileSystem, sprite: SpriteAnimation?, anim: ADProperties?, spriteGlow: SpriteAnimation? = null, animGlow: ADProperties? = null) {
        if (anim != null && sprite != null)
            _rebuild(disk, anim, sprite)
        if (animGlow != null && spriteGlow != null)
            _rebuild(disk, animGlow, spriteGlow)
    }

    private fun _rebuild(ad: ADProperties, sprite: SpriteAnimation) {
        // TODO injecting held item/armour pictures? Would it be AssembleSheetPixmap's job?

        val pixmap = AssembleSheetPixmap.fromAssetsDir(ad)
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

    private fun _rebuild(disk: SimpleFileSystem, ad: ADProperties, sprite: SpriteAnimation) {
        // TODO injecting held item/armour pictures? Would it be AssembleSheetPixmap's job?

        val pixmap = AssembleSheetPixmap.fromVirtualDisk(disk, ad)
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