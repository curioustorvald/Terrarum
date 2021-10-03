package net.torvald.spriteassembler

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import net.torvald.terrarum.linearSearch

/**
 * Assembles the single frame of the animation, outputs GDX Pixmap.
 *
 * The entire rendering is done by using pixmap. That is, no GPU access.
 *
 * Created by minjaesong on 2019-01-06.
 */
object AssembleSheetPixmap {

    operator fun invoke(properties: ADProperties): Pixmap {
        val canvas = Pixmap(properties.cols * properties.frameWidth, properties.rows * properties.frameHeight, Pixmap.Format.RGBA8888)
        canvas.blending = Pixmap.Blending.SourceOver


        // actually draw
        properties.transforms.forEach { t, _ ->
            drawThisFrame(t, canvas, properties)
        }

        return canvas
    }

    private fun drawThisFrame(frameName: String,
                              canvas: Pixmap,
                              properties: ADProperties
    ) {
        val theAnim = properties.getAnimByFrameName(frameName)
        val skeleton = theAnim.skeleton.joints.reversed()
        val transforms = properties.getTransform(frameName)
        val bodypartOrigins = properties.bodyparts
        val bodypartImages = properties.bodyparts.keys.map {
            val file = Gdx.files.internal("assets/${properties.toFilename(it)}")
            it to (if (file.exists()) Pixmap(file) else null)
        }.toMap()
        val transformList = AssembleFrameBase.makeTransformList(skeleton, transforms)

        val animRow = theAnim.row
        val animFrame = properties.getFrameNumberFromName(frameName)

//        AppLoader.printdbg(this, "Frame to draw: $frameName (R$animRow C$animFrame)")

        drawFrame(animRow, animFrame, canvas, properties, bodypartOrigins, bodypartImages, transformList)

        bodypartImages.values.forEach { it?.dispose() }
    }

    private fun drawFrame(row: Int, column: Int,
                          canvas: Pixmap,
                          props: ADProperties,
                          bodypartOrigins: HashMap<String, ADPropertyObject.Vector2i>,
                          bodypartImages: Map<String, Pixmap?>,
                          transformList: List<Pair<String, ADPropertyObject.Vector2i>>
    ) {
        val tmpFrame = Pixmap(props.frameWidth, props.frameHeight, Pixmap.Format.RGBA8888)

        transformList.forEach { (name, pos) ->
            bodypartImages[name]?.let { image ->
                val imgCentre = bodypartOrigins[name]!!.invertX()
                val drawPos = props.origin + pos + imgCentre

                tmpFrame.drawPixmap(image, drawPos.x, props.frameHeight - drawPos.y - 1)
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