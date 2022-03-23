package net.torvald.terrarum.spriteassembler

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.GdxRuntimeException
import net.torvald.terrarum.App
import net.torvald.terrarum.ItemCodex
import net.torvald.terrarum.ReferencingRanges
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.linearSearch
import net.torvald.terrarum.savegame.ByteArray64InputStream
import net.torvald.terrarum.savegame.ByteArray64Reader
import net.torvald.terrarum.savegame.SimpleFileSystem
import net.torvald.terrarum.serialise.Common
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.*

/**
 * Assembles the single frame of the animation, outputs GDX Pixmap.
 *
 * The entire rendering is done by using pixmap. That is, no GPU access.
 *
 * Created by minjaesong on 2019-01-06.
 */
object AssembleSheetPixmap {

    const val MUGSHOT_PIXMAP_W = 48
    const val MUGSHOT_PIXMAP_H = 48

    /**
     * The name of the Bodypart here may or may not be case-sensitive (depends on your actual filesystem -- NTFS, APFS, Ext4, ...)
     */
    fun getAssetsDirFileGetter(properties: ADProperties): (String) -> InputStream? = { partName: String ->
        val file = Gdx.files.internal("assets/${properties.toFilename(partName)}")
        if (file.exists()) file.read() else null
    }

    /**
     * The name of the Bodypart is CASE-SENSITIVE!
     */
    fun getVirtualDiskFileGetter(bodypartMapping: Properties, disk: SimpleFileSystem): (String) -> InputStream? = { partName: String ->
        bodypartMapping.getProperty(partName).let {
            if (it != null)
                ByteArray64InputStream(disk.getFile(bodypartMapping.getProperty(partName).toLong())!!.bytes)
            else
                null
        }
    }


    private fun drawAndGetCanvas(properties: ADProperties, fileGetter: (String) -> InputStream?, injectedItem: GameItem?): Pixmap {
        val canvas = Pixmap(properties.cols * (properties.frameWidth), properties.rows * (properties.frameHeight), Pixmap.Format.RGBA8888)
        canvas.blending = Pixmap.Blending.SourceOver

        // actually draw
        properties.transforms.forEach { (t, _) ->
            drawThisFrame(t, canvas, properties, fileGetter, injectedItem)
        }

        return canvas
    }

    fun fromAssetsDir(properties: ADProperties, injectedItem: GameItem?) = drawAndGetCanvas(properties, getAssetsDirFileGetter(properties), injectedItem)

    fun fromVirtualDisk(disk: SimpleFileSystem, entrynum: Long, properties: ADProperties, injectedItem: GameItem?): Pixmap {
        val bodypartMapping = Properties()
        bodypartMapping.load(ByteArray64Reader(disk.getFile(entrynum)!!.bytes, Common.CHARSET))

        return drawAndGetCanvas(properties, getVirtualDiskFileGetter(bodypartMapping, disk), injectedItem)
    }

    fun getPartPixmap(getFile: (String) -> InputStream?, partName: String): Pixmap? {
        getFile(partName)?.let {
            val bytes = it.readAllBytes()
            return Pixmap(bytes, 0, bytes.size)
        }
        return null
    }

    fun getMugshotFromAssetsDir(properties: ADProperties): TextureRegion? {
        // TODO assemble from HAIR_FORE (optional), HAIR (optional) then HEAD (mandatory)
        val getter = getAssetsDirFileGetter(properties)
        val headPixmap = getPartPixmap(getter, "HEAD")
        val hairPixmap = getPartPixmap(getter, "HAIR")
        val hair2Pixmap = getPartPixmap(getter, "HAIR_FORE")

        if (headPixmap == null) throw FileNotFoundException("Bodyparts file of HEAD is not found!")
        return composeMugshot(properties, headPixmap, hairPixmap, hair2Pixmap)
    }

    fun getMugshotFromVirtualDisk(disk: SimpleFileSystem, entrynum: Long, properties: ADProperties): TextureRegion? {
        // TODO assemble from HAIR_FORE (optional), HAIR (optional) then HEAD (mandatory)
        val bodypartMapping = Properties()
        bodypartMapping.load(ByteArray64Reader(disk.getFile(entrynum)!!.bytes, Common.CHARSET))
        val getter = getVirtualDiskFileGetter(bodypartMapping, disk)
        val headPixmap = getPartPixmap(getter, "HEAD")
        val hairPixmap = getPartPixmap(getter, "HAIR")
        val hair2Pixmap = getPartPixmap(getter, "HAIR_FORE")

        if (headPixmap == null) throw FileNotFoundException("Bodyparts file of HEAD is not found!")
        return composeMugshot(properties, headPixmap, hairPixmap, hair2Pixmap)
    }

    private fun composeMugshot(properties: ADProperties, head: Pixmap, hair: Pixmap?, hair2: Pixmap?): TextureRegion {
        val canvas = Pixmap(MUGSHOT_PIXMAP_W, MUGSHOT_PIXMAP_H, Pixmap.Format.RGBA8888)
        val drawX = (canvas.width - head.width) / 2
        val drawY = (canvas.height - head.height) / 2
        val headOffset = properties.bodypartJoints["HEAD"]!!

        // TODO shift drawing pos using the properties BODYPARTS

        canvas.drawPixmap(head, drawX, drawY)
        hair?.let {
            val offset = properties.bodypartJoints["HAIR"]!! - headOffset
            canvas.drawPixmap(it, drawX - offset.x, drawY - offset.y)
        }
        hair2?.let {
            val offset = properties.bodypartJoints["HAIR_FORE"]!! - headOffset
            canvas.drawPixmap(it, drawX - offset.x, drawY - offset.y)
        }

        val tr = TextureRegion(Texture(canvas))

        canvas.dispose()
        head.dispose()
        hair?.dispose()
        hair2?.dispose()

        return tr
    }

    fun drawThisFrame(frameName: String,
                              canvas: Pixmap,
                              properties: ADProperties,
                              fileGetter: (String) -> InputStream?,
                              injectedItem: GameItem?
    ) {
        val theAnim = properties.getAnimByFrameName(frameName)
        val skeleton = theAnim.skeleton.joints.reversed()
        val transforms = properties.getTransform(frameName)
        val bodypartOrigins = properties.bodypartJoints
        val bodypartImages = properties.bodypartJoints.keys.map { partname ->
            fileGetter(partname).let { file ->
                if (file == null) partname to null
                else {
                    try {
                        val bytes = file.readAllBytes()
                        partname to Pixmap(bytes, 0, bytes.size)
                    }
                    catch (e: GdxRuntimeException) {
                        partname to null
                    }
                }
            }
        }.toMap()
        val transformList = AssembleFrameBase.makeTransformList(skeleton, transforms)

        val animRow = theAnim.row
        val animFrame = properties.getFrameNumberFromName(frameName)

//        AppLoader.printdbg(this, "Frame to draw: $frameName (R$animRow C$animFrame)")

        drawFrame(animRow, animFrame, canvas, properties, bodypartOrigins, bodypartImages, transformList, injectedItem)

        bodypartImages.values.forEach { it?.dispose() }
    }

    fun drawFrame(row: Int, column: Int,
                          canvas: Pixmap,
                          props: ADProperties,
                          bodypartOrigins: HashMap<String, ADPropertyObject.Vector2i>,
                          bodypartImages: Map<String, Pixmap?>,
                          transformList: List<Pair<String, ADPropertyObject.Vector2i>>,
                          injectedItem: GameItem?
    ) {
        val tmpFrame = Pixmap(props.frameWidth, props.frameHeight, Pixmap.Format.RGBA8888)

        transformList.forEach { (name, bodypartPos) ->
            if (name == "HELD_ITEM" && injectedItem != null) {
//                printdbg(this, "ID of the held item: ${injectedItem.originalID}")

                ItemCodex.getItemImage(injectedItem)?.let { textureRegion ->
//                    printdbg(this, "and it did have a textureregion")

                    val texdata = textureRegion.texture.textureData
                    val textureBackedByPixmap = texdata.isPrepared // texture backed by pixmap is always prepared without ordering it to prepare
                    if (!textureBackedByPixmap) texdata.prepare()

                    val imageSheet = if
                            (injectedItem.originalID.startsWith("${ReferencingRanges.PREFIX_DYNAMICITEM}:") ||
                             injectedItem.originalID.startsWith("item@") ||
                             injectedItem.originalID.startsWith("wire@"))
                        texdata.consumePixmap()
                    // super dirty and ugly hack because for some reason it just won't work
                    else if (injectedItem.originalID.startsWith("wall@"))
                        App.tileMaker.itemWallPixmap
                    else
                        App.tileMaker.itemTerrainPixmap


                    val drawPos = props.origin + bodypartPos

                    val pu = (textureRegion.u * texdata.width).toInt()
                    val pv = (textureRegion.v * texdata.height).toInt()
                    val imageWidth = textureRegion.regionWidth
                    val imageHeight = textureRegion.regionHeight

//                    printdbg(this, "uv: ($pu,$pv) uv2: ($pu2,$pv2) dim: ($imageWidth,$imageHeight) atlasdim: (${texdata.width},${texdata.height})")

                    tmpFrame.drawPixmap(imageSheet, drawPos.x, props.frameHeight - drawPos.y - 1 - imageHeight, pu, pv, imageWidth, imageHeight)

                    if (!textureBackedByPixmap) imageSheet.dispose()
                }
            }
            else {
                bodypartImages[name]?.let { image ->
                    val imgCentre = bodypartOrigins[name]!!.invertX()
                    val drawPos = props.origin + bodypartPos + imgCentre

                    tmpFrame.drawPixmap(image, drawPos.x, props.frameHeight - drawPos.y - 1)
                }
            }
        }

        canvas.drawPixmap(
                tmpFrame,
                (column - 1) * props.frameWidth,
                (row - 1) * props.frameHeight
        )


        tmpFrame.dispose()

    }

}

internal object AssembleFrameBase {
    /**
     * Returns joints list with tranform applied.
     * @param skeleton list of joints
     * @param transform ordered list of transforms should be applied. First come first serve.
     * @return List of pairs that contains joint name on left, final transform value on right
     */
    fun makeTransformList(joints: List<Joint>, transforms: List<Transform>): List<Pair<String, ADPropertyObject.Vector2i>> {
        // make our mutable list
        val out = ArrayList<Pair<String, ADPropertyObject.Vector2i>>()
        joints.forEach {
            out.add(it.name to it.position)
        }

        // process transform queue
        transforms.forEach { transform ->
            if (transform.joint.name == ADProperties.ALL_JOINT_SELECT_KEY) {
                // transform applies to all joints
                for (c in out.indices) {
                    out[c] = out[c].first to (out[c].second + transform.translate)
                }
            }
            else {
                val i = out.linearSearch { it.first == transform.joint.name }!!
                // transform applies to one specific joint in the list (one specific joint is a search result)
                out[i] = out[i].first to (out[i].second + transform.translate)
            }
        }

        return out.toList()
    }

    fun getCentreOf(pixmap: Pixmap) = ADPropertyObject.Vector2i(pixmap.width / 2, pixmap.height / 2)
}