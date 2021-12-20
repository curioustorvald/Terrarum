package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.spriteassembler.ADProperties
import net.torvald.spriteassembler.AssembleSheetPixmap
import net.torvald.terrarum.App
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.savegame.SimpleFileSystem
import net.torvald.terrarum.utils.PlayerLastStatus
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import java.util.*


/**
 * Game player (YOU!)
 *
 * Created by minjaesong on 2015-12-31.
 */

class IngamePlayer : ActorHumanoid {

    val creationTime = App.getTIME_T()
    var lastPlayTime = App.getTIME_T() // cumulative value for the savegame
    var totalPlayTime = 0L // cumulative value for the savegame

    val uuid = UUID.randomUUID()
    var worldCurrentlyPlaying: UUID = UUID(0L,0L) // only filled up on save and load; DO NOT USE THIS

    var spriteHeadTexture: TextureRegion? = null


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


    /** Copy of some of the player's props before get overwritten by the props saved in the world.
     *
     * This field is only there for loading multiplayer map on singleplayer instances where the world loader would
     * permanently changing player's props into multiplayer world's.
     */
    @Transient internal lateinit var unauthorisedPlayerProps: PlayerLastStatus

    fun backupPlayerProps(isMultiplayer: Boolean) {
        unauthorisedPlayerProps = PlayerLastStatus(this, isMultiplayer)
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
    fun reassembleSprite(sprite: SpriteAnimation?, spriteGlow: SpriteAnimation? = null) {
        if (animDesc != null && sprite != null) {
            _rebuild(animDesc!!, sprite)
            spriteHeadTexture = AssembleSheetPixmap.getHeadFromAssetsDir(animDesc!!)
        }
        if (animDescGlow != null && spriteGlow != null)
            _rebuild(animDescGlow!!, spriteGlow)

    }

    fun reassembleSprite(disk: SimpleFileSystem, sprite: SpriteAnimation?, spriteGlow: SpriteAnimation? = null) {
        if (animDesc != null && sprite != null) {
            _rebuild(disk, -1025L, animDesc!!, sprite)

            if (disk.getEntry(-1025L) != null)
                spriteHeadTexture = AssembleSheetPixmap.getHeadFromVirtualDisk(disk, -1025L, animDesc!!)
            else
                spriteHeadTexture = AssembleSheetPixmap.getHeadFromAssetsDir(animDesc!!)
        }
        if (animDescGlow != null && spriteGlow != null)
            _rebuild(disk, -1026L, animDescGlow!!, spriteGlow)
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

    private fun _rebuild(disk: SimpleFileSystem, entrynum: Long, ad: ADProperties, sprite: SpriteAnimation) {
        // TODO injecting held item/armour pictures? Would it be AssembleSheetPixmap's job?

        val pixmap = if (disk.getEntry(entrynum) != null) AssembleSheetPixmap.fromVirtualDisk(disk, entrynum, ad) else AssembleSheetPixmap.fromAssetsDir(ad)
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

    override fun getSpriteHead(): TextureRegion? {
        return spriteHeadTexture
    }
}