package net.torvald.spriteassembler

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.linearSearch
import java.io.File

/**
 * Assembles the single frame of the animation, outputs GDX Pixmap.
 *
 * The entire rendering is done by using pixmap. That is, no GPU access.
 *
 * Created by minjaesong on 2019-01-06.
 */
object AssembleSheetPixmap {

    operator fun invoke(properties: ADProperties, assembleConfig: AssembleConfig = AssembleConfig()): Pixmap {
        val canvas = Pixmap(properties.cols * assembleConfig.fw, properties.rows * assembleConfig.fh, Pixmap.Format.RGBA8888)
        canvas.blending = Pixmap.Blending.SourceOver


        // actually draw
        properties.transforms.forEach { t, _ ->
            drawThisFrame(t, canvas, properties, assembleConfig)
        }

        return canvas
    }

    private fun drawThisFrame(frameName: String,
                              canvas: Pixmap,
                              properties: ADProperties,
                              assembleConfig: AssembleConfig
    ) {
        val theAnim = properties.getAnimByFrameName(frameName)
        val skeleton = theAnim.skeleton.joints.reversed()
        val transforms = properties.getTransform(frameName)
        val bodyparts = Array<Pixmap?>(skeleton.size) {
            // if file does not exist, null it
            val file = File("assets/" + properties.toFilename(skeleton[it].name))

            //printdbg(this, "Loading file ${file.absolutePath}, exists: ${file.exists()}")

            /*return*/if (file.exists()) {
            Pixmap(Gdx.files.internal(file.path))
            }
            else {
                null
            }
        }
        val transformList = AssembleFrameBase.makeTransformList(skeleton, transforms)

        val animRow = theAnim.row
        val animFrame = properties.getFrameNumberFromName(frameName)

        AppLoader.printdbg(this, "Frame to draw: $frameName (R$animRow C$animFrame)")

        drawFrame(animRow, animFrame, canvas, bodyparts, transformList, assembleConfig)

        bodyparts.forEach { it?.dispose() }
    }

    private fun drawFrame(row: Int, column: Int,
                          canvas: Pixmap,
                          bodyparts: Array<Pixmap?>,
                          transformList: List<Pair<String, ADPropertyObject.Vector2i>>,
                          assembleConfig: AssembleConfig
    ) {
        val tmpFrame = Pixmap(assembleConfig.fw, assembleConfig.fh, Pixmap.Format.RGBA8888)

        bodyparts.forEachIndexed { index, image ->
            if (image != null) {
                val imgCentre = AssembleFrameBase.getCentreOf(image)
                val drawPos = transformList[index].second.invertY() + assembleConfig.origin - imgCentre

                tmpFrame.drawPixmap(image, drawPos.x, drawPos.y)
            }
        }

        canvas.drawPixmap(
                tmpFrame,
                (column - 1) * assembleConfig.fw,
                (row - 1) * assembleConfig.fh
        )

        tmpFrame.dispose()

    }

}

/**
 * @param fw Frame Width
 * @param fh Frame Height
 * @param origin Int vector of origin point, (0,0) being TOP-LEFT
 */
data class AssembleConfig(val fw: Int = 48, val fh: Int = 56, val origin: ADPropertyObject.Vector2i = ADPropertyObject.Vector2i(29, fh - 1))

object AssembleFrameBase {
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
                for (c in 0 until out.size) {
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