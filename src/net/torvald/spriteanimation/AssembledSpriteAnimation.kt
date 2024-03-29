package net.torvald.spriteanimation

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.GdxRuntimeException
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameactors.ActorWithBody.Companion.METER
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.modulebasegame.gameactors.Pocketed
import net.torvald.terrarum.savegame.ByteArray64Reader
import net.torvald.terrarum.savegame.EntryID
import net.torvald.terrarum.savegame.SimpleFileSystem
import net.torvald.terrarum.savegame.VDFileID
import net.torvald.terrarum.savegame.VDFileID.BODYPARTEMISSIVE_TO_ENTRY_MAP
import net.torvald.terrarum.savegame.VDFileID.BODYPARTGLOW_TO_ENTRY_MAP
import net.torvald.terrarum.savegame.VDFileID.BODYPART_TO_ENTRY_MAP
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.spriteassembler.ADProperties
import net.torvald.terrarum.spriteassembler.ADPropertyObject
import net.torvald.terrarum.spriteassembler.AssembleFrameBase
import net.torvald.terrarum.spriteassembler.AssembleSheetPixmap
import java.io.InputStream
import java.util.*

/**
 * This class should not be serialised; save its Animation Description Language instead.
 *
 * Created by minjaesong on 2022-03-23.
 */
class AssembledSpriteAnimation(
    @Transient val adp: ADProperties,
    parentActor: ActorWithBody,
    @Transient val disk: SimpleFileSystem?, // specify if the resources for the animation is contained in the disk archive
    @Transient val isGlow: Boolean,
    @Transient val isEmissive: Boolean
) : SpriteAnimation(parentActor) {

    constructor(adp: ADProperties, parentActor: ActorWithBody, isGlow: Boolean, isEmissive: Boolean) : this(adp, parentActor, null, isGlow, isEmissive)

    @Transient val bodypartToFileMap = if (isEmissive)
        BODYPARTEMISSIVE_TO_ENTRY_MAP
    else if (isGlow)
        BODYPARTGLOW_TO_ENTRY_MAP
    else
        BODYPART_TO_ENTRY_MAP


    var currentFrame = 0 // while this number is zero-based, the frame number on the ADP is one-based
        private set
    var currentAnimation = "" // e.g. ANIM_IDLE ANIM_RUN (no frame numbers!)
        set(value) {
            if (field != value) {
                currentFrame = 0
                currentAnimationMaxFrames = adp.animations[value]?.frames ?: 1
                currentAnimationBaseDelay = adp.animations[value]?.delay ?: 0.5f
            }
            field = value
        }
    private var currentAnimationMaxFrames = 1
    private var currentAnimationBaseDelay = 0.0625f

//    @Transient var init = false

    override val currentDelay: Second
        get() = (if (overrideDelay > 0f) overrideDelay else currentAnimationBaseDelay).coerceAtLeast(0.0625f)

    var overrideDelay = 0f // set to 0f to not use this field

    @Transient private val res = HashMap<String, TextureRegion?>()

    @Transient var headSprite: TextureRegion? = null
    @Transient var headSizeInMeter: Float? = null

    init {
//        init = true

        val fileGetter = if (disk != null) {
            val bodypartMapping = Properties()
            bodypartMapping.load(ByteArray64Reader(disk.getFile(bodypartToFileMap)!!.bytes, Common.CHARSET))

            AssembleSheetPixmap.getVirtualDiskFileGetter(bodypartMapping, disk)
        }
        else AssembleSheetPixmap.getAssetsDirFileGetter(adp)

        val fileGetterFallback = if (disk != null) {
            val bodypartMapping = Properties()
            bodypartMapping.load(ByteArray64Reader(disk.getFile(BODYPART_TO_ENTRY_MAP)!!.bytes, Common.CHARSET))

            AssembleSheetPixmap.getVirtualDiskFileGetter(bodypartMapping, disk)
        }
        else AssembleSheetPixmap.getAssetsDirFileGetter(adp)

        adp.bodyparts.forEach { res[it] = getPartTexture(fileGetter, fileGetterFallback, it) }

        val (mugPixmap, headSprite0) = if (disk != null)
            AssembleSheetPixmap.getMugshotFromVirtualDisk(disk, -1025L, adp)
        else
            AssembleSheetPixmap.getMugshotFromAssetsDir(adp)

        headSprite = headSprite0

        mugPixmap?.let { pixmap ->
            // measure width
            val m = (0 until pixmap.height).map { y ->
                (0 until pixmap.width).fold(0) { acc, x ->
                    acc + (pixmap.getPixel(x, y).and(255) > 128).toInt()
                }
            }.filter { it > 0 }.average() / METER
            if (m > 0) headSizeInMeter = m.toFloat() * 0.36f
        }

        mugPixmap?.dispose()
    }

    private var delta = 0f

    override fun update(delta: Float) {
        this.delta += delta

        //println("delta accumulation: $delta, currentDelay: $currentDelay")

        //check if it's time to advance the frame
        while (this.delta >= currentDelay) {
            // advance frame
            currentFrame = (currentFrame + 1) % currentAnimationMaxFrames

            // discount counter
            this.delta -= currentDelay
        }
    }

    private fun fetchItemImage(mode: Int, item: GameItem) = when (mode) {
        0 -> ItemCodex.getItemImage(item)
        1 -> ItemCodex.getItemImageGlow(item)
        2 -> ItemCodex.getItemImageEmissive(item)
        else -> throw IllegalArgumentException()
    }

    fun renderThisAnimation(batch: SpriteBatch, posX: Float, posY: Float, scale: Float, animName: String, mode: Int = 0) {
        val oldBatchColour = batch.color.cpy()

        val animNameRoot = animName.substring(0, animName.indexOfLast { it == '_' }).ifBlank { return@renderThisAnimation }
        // quick fix for the temporary de-sync bug in which when the update-rate per frame is much greater than once, it attempts to load animation with blank name

        val tx = -(parentActor.hitboxTranslateX) * scale
        val txFlp = -(parentActor.hitboxTranslateX) * scale
        // flipping will not be symmetrical if baseHitboxWidth is odd number
        val ty = -(parentActor.hitboxTranslateY - parentActor.baseHitboxH) * scale
        val tyFlp = (parentActor.hitboxTranslateY) * scale


        adp.animations[animNameRoot]?.let { theAnim ->
            val skeleton = theAnim.skeleton.joints.reversed()
            val transforms = adp.getTransform(animName)
            val bodypartOrigins = adp.bodypartJoints

            AssembleFrameBase.makeTransformList(skeleton, transforms).forEach { (name, bodypartPos0) ->
                var bodypartPos = bodypartPos0.invertY()
                if (flipVertical) bodypartPos = bodypartPos.invertY()
                if (flipHorizontal) bodypartPos = bodypartPos.invertX()
                bodypartPos += ADPropertyObject.Vector2i(1,0)

                // draw held items/armours?
                if (name in jointNameToEquipPos) {
                    batch.color = if (mode > 0) Color.WHITE else oldBatchColour
                    ItemCodex[(parentActor as? Pocketed)?.inventory?.itemEquipped?.get(jointNameToEquipPos[name]!!)]?.let { item ->
                        fetchItemImage(mode, item)?.let { image ->
                            val drawPos = adp.origin + bodypartPos // imgCentre for held items are (0,0)
                            val w = image.regionWidth * scale
                            val h = image.regionHeight * scale
                            val fposX = posX.floorToFloat() + drawPos.x * scale
                            val fposY = (posY - 0.5f).floorToFloat() + drawPos.y * scale - h

                            // draw
                            if (flipHorizontal && flipVertical)
                                batch.draw(image, fposX + txFlp, fposY + tyFlp, -w, -h)
                            else if (flipHorizontal && !flipVertical)
                                batch.draw(image, fposX + txFlp, fposY + ty, -w, h)
                            else if (!flipHorizontal && flipVertical)
                                batch.draw(image, fposX + tx, fposY + tyFlp, w, -h)
                            else
                                batch.draw(image, fposX + tx, fposY + ty, w, h)
                        }
                    }
                }
                else {
                    batch.color = oldBatchColour
                    res[name]?.let { image ->
                        var imgCentre = bodypartOrigins[name]!!
                        if (flipVertical) imgCentre = imgCentre.invertY()
                        if (flipHorizontal) imgCentre = imgCentre.invertX()

                        val drawPos = adp.origin + bodypartPos - imgCentre
                        val w = image.regionWidth * scale
                        val h = image.regionHeight * scale
                        val fposX = posX.floorToFloat() + drawPos.x * scale
                        val fposY = (posY - 0.5f).floorToFloat() + drawPos.y * scale

                        if (flipHorizontal && flipVertical)
                            batch.draw(image, fposX + txFlp, fposY + tyFlp, -w, -h)
                        else if (flipHorizontal && !flipVertical)
                            batch.draw(image, fposX + txFlp, fposY + ty, -w, h)
                        else if (!flipHorizontal && flipVertical)
                            batch.draw(image, fposX + tx, fposY + tyFlp, w, -h)
                        else
                            batch.draw(image, fposX + tx, fposY + ty, w, h)

                    }
                }
            }
        } ?: throw NullPointerException("Animation with name '$animNameRoot' is not found")
    }

    /**
     * Held items will ignore forcedColourFilter if mode > 0
     *
     * @param mode specifies the mode for drawing the held item. 0=diffuse, 1=glow, 2=emissive
     */
    override fun render(frameDelta: Float, batch: SpriteBatch, posX: Float, posY: Float, scale: Float, mode: Int, forcedColourFilter: Color?) {
        if (parentActor.isVisible) {
            batch.color = forcedColourFilter ?: colourFilter
            renderThisAnimation(batch, posX, posY, scale, "${currentAnimation}_${1+currentFrame}", mode)
        }
    }

    override fun dispose() {
        res.values.forEach { it?.texture?.tryDispose() }
    }

    companion object {
        val jointNameToEquipPos = hashMapOf(
                "HELD_ITEM" to GameItem.EquipPosition.HAND_GRIP
                // TODO fill in with armours/etc
        )

        private fun getPartTexture(getFile: (String) -> InputStream?, getFileFallback: (String) -> InputStream?, partName: String): TextureRegion? {
            getFile(partName)?.let {
                val bytes = it.readAllBytes()
                return TextureRegion(Texture(Pixmap(bytes, 0, bytes.size)))
            }
            getFileFallback(partName)?.let {
                val bytes = it.readAllBytes()

                // filter the image so that it's either transparent or black
                val pixmap = Pixmap(bytes, 0, bytes.size)
                for (y in 0 until pixmap.height) {
                    for (x in 0 until pixmap.width) {
                        val c = pixmap.getPixel(x, y)
                        pixmap.drawPixel(x, y, c and 0xFF)
                    }
                }

                return TextureRegion(Texture(pixmap))
            }
            return null
        }
    }
}