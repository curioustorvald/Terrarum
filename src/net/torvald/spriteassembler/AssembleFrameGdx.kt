package net.torvald.spriteassembler

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import net.torvald.terrarum.linearSearch
import java.io.File

/**
 * Assembles the single frame of the animation, outputs Java AWT image.
 *
 * Created by minjaesong on 2019-01-06.
 */
object AssembleFramePixmap {

    // FIXME fuck this I'll use GDX

    operator fun invoke(properties: ADProperties, frameName: String, assembleConfig: AssembleConfig = AssembleConfig()): Pixmap {
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
        val canvas = Pixmap(assembleConfig.fw, assembleConfig.fh, Pixmap.Format.RGBA8888)


        println("Frame name: $frameName")
        transforms.forEach { println(it) }
        println("==========================")
        println("Transformed skeleton:")
        val transformList = AssembleFrameBase.makeTransformList(skeleton, transforms)
        transformList.forEach { (name, transform) ->
            println("$name transformedOut: $transform")
        }


        // actually draw
        canvas.blending = Pixmap.Blending.SourceOver

        bodyparts.forEachIndexed { index, image ->
            if (image != null) {
                val imgCentre = AssembleFrameBase.getCentreOf(image)
                val drawPos = transformList[index].second.invertY() + assembleConfig.origin - imgCentre

                canvas.drawPixmap(image, drawPos.x, drawPos.y)

                image.dispose()
            }
        }

        return canvas
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
        val transformOutput = ArrayList<Pair<String, ADPropertyObject.Vector2i>>()
        joints.forEach {
            transformOutput.add(it.name to it.position)
        }

        // process transform queue
        transforms.forEach { transform ->
            // TODO when the transform.joint.name == ADProperties.ALL_JOINT_SELECT_KEY]]

            val jointToMoveIndex = transformOutput.linearSearch { it.first == transform.joint.name }!!
            transformOutput[jointToMoveIndex] = transformOutput[jointToMoveIndex].first to transform.getTransformVector()
        }

        return transformOutput.toList()
    }

    fun getCentreOf(pixmap: Pixmap) = ADPropertyObject.Vector2i(pixmap.width / 2, pixmap.height / 2)
}