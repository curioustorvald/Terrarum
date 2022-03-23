package net.torvald.spriteanimation

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.GdxRuntimeException
import com.jme3.math.FastMath
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.Second
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.printStackTrace
import net.torvald.terrarum.savegame.ByteArray64Reader
import net.torvald.terrarum.savegame.EntryID
import net.torvald.terrarum.savegame.SimpleFileSystem
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.spriteassembler.ADProperties
import net.torvald.terrarum.spriteassembler.AssembleFrameBase
import net.torvald.terrarum.spriteassembler.AssembleSheetPixmap
import java.io.InputStream
import java.util.*
import kotlin.collections.HashMap

/**
 * This class should not be serialised; save its Animation Description Language instead.
 *
 * Created by minjaesong on 2022-03-23.
 */
class AssembledSpriteAnimation(
        @Transient val adp: ADProperties,
        parentActor: ActorWithBody,
        @Transient val disk: SimpleFileSystem?, // specify if the resources for the animation is contained in the disk archive
        @Transient val bodypartToFileMap: EntryID? // which file in the disk contains bodypart-to-fileid mapping for this particular instance of sprite animation
) : SpriteAnimation(parentActor) {

    constructor(adp: ADProperties, parentActor: ActorWithBody) : this(adp, parentActor, null, null)

    var currentFrame = 0 // while this number is zero-based, the frame number on the ADP is one-based
    var currentAnimation = "" // e.g. ANIM_IDLE ANIM_RUN (no frame numbers!)
        set(value) {
            field = value
            currentFrame = 0
        }

//    @Transient var init = false

    override val currentDelay: Second
        get() = (if (overrideDelay > 0f) overrideDelay else adp.animations[currentAnimation]?.delay ?: 1f).coerceAtLeast(1f / 16f)

    var overrideDelay = 0f // set to 0f to not use this field

    @Transient private val res = HashMap<String, TextureRegion?>()

    init {
//        init = true

        val fileGetter = if (disk != null) {
            val bodypartMapping = Properties()
            bodypartMapping.load(ByteArray64Reader(disk.getFile(bodypartToFileMap!!)!!.bytes, Common.CHARSET))

            AssembleSheetPixmap.getVirtualDiskFileGetter(bodypartMapping, disk)
        }
        else AssembleSheetPixmap.getAssetsDirFileGetter(adp)

        adp.bodyparts.forEach { res[it] = getPartTexture(fileGetter, it) }
    }

    private var delta = 0f

    override fun update(delta: Float) {
        this.delta += delta

        //println("delta accumulation: $delta, currentDelay: $currentDelay")

        //check if it's time to advance the frame
        while (this.delta >= currentDelay) {
            // advance frame
            currentFrame = (currentFrame + 1) % (adp.animations[currentAnimation]?.frames ?: 2)

            // discount counter
            this.delta -= currentDelay
        }    
    }

    override fun render(batch: SpriteBatch, posX: Float, posY: Float, scale: Float) {
        if (parentActor.isVisible) {

            val tx = (parentActor.hitboxTranslateX) * scale
            val txF = (parentActor.hitboxTranslateX + parentActor.baseHitboxW) * scale
            val ty = (parentActor.hitboxTranslateY + (adp.frameHeight - parentActor.baseHitboxH)) * scale
            val tyF = (parentActor.hitboxTranslateY + parentActor.baseHitboxH) * scale

            adp.animations[currentAnimation]!!.let { theAnim ->
                val skeleton = theAnim.skeleton.joints.reversed()
                val transforms = adp.getTransform("${currentAnimation}_${1+currentFrame}")
                val bodypartOrigins = adp.bodypartJoints

                AssembleFrameBase.makeTransformList(skeleton, transforms).forEach { (name, bodypartPos) ->
                    if (false) { // inject item's image

                    }
                    else {
                        res[name]?.let { image ->
                            val imgCentre = bodypartOrigins[name]!!.invertX()
                            val drawPos = adp.origin + bodypartPos + imgCentre

                            if (flipHorizontal && flipVertical) {
                                batch.draw(image,
                                        FastMath.floor(posX).toFloat() + txF + drawPos.x,
                                        FastMath.floor(posY).toFloat() + tyF + drawPos.y,
                                        -FastMath.floor(adp.frameWidth * scale).toFloat(),
                                        -FastMath.floor(adp.frameHeight * scale).toFloat()
                                )
                            }
                            else if (flipHorizontal && !flipVertical) {
                                batch.draw(image,
                                        FastMath.floor(posX).toFloat() + txF + drawPos.x,
                                        FastMath.floor(posY).toFloat() - ty + drawPos.y,
                                        -FastMath.floor(adp.frameWidth * scale).toFloat(),
                                        FastMath.floor(adp.frameHeight * scale).toFloat()
                                )
                            }
                            else if (!flipHorizontal && flipVertical) {
                                batch.draw(image,
                                        FastMath.floor(posX).toFloat() - tx + drawPos.x,
                                        FastMath.floor(posY).toFloat() + tyF + drawPos.y,
                                        FastMath.floor(adp.frameWidth * scale).toFloat(),
                                        -FastMath.floor(adp.frameHeight * scale).toFloat()
                                )
                            }
                            else {
                                batch.draw(image,
                                        FastMath.floor(posX).toFloat() - tx + drawPos.x,
                                        FastMath.floor(posY).toFloat() - ty + drawPos.y,
                                        FastMath.floor(adp.frameWidth * scale).toFloat(),
                                        FastMath.floor(adp.frameHeight * scale).toFloat()
                                )
                            }
                        }
                    }
                }


            }

        }
    }

    override fun dispose() {
        res.values.forEach { try { it?.texture?.dispose() } catch (_: GdxRuntimeException) {} }
    }

    companion object {
        private fun getPartTexture(getFile: (String) -> InputStream?, partName: String): TextureRegion? {
            getFile(partName)?.let {
                val bytes = it.readAllBytes()
                return TextureRegion(Texture(Pixmap(bytes, 0, bytes.size)))
            }
            return null
        }
    }
}