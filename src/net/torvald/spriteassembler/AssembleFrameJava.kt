package net.torvald.spriteassembler

import net.torvald.terrarum.linearSearch
import java.awt.Image
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File

/**
 * Assembles the single frame of the animation, outputs Java AWT image.
 *
 * Created by minjaesong on 2019-01-06.
 */
object AssembleFrameAWT {

    operator fun invoke(properties: ADProperties, frameName: String, assembleConfig: AssembleConfig = AssembleConfig()) {
        val theAnim = properties.getAnimByFrameName(frameName)
        val skeleton = theAnim.skeleton.joints.reversed()
        val transforms = properties.getTransform(frameName)
        val bodyparts = Array<Image?>(skeleton.size) {
            // if file does not exist, null it
            val file = File("assets/" + properties.toFilename(skeleton[it].name))

            //printdbg(this, "Loading file ${file.absolutePath}, exists: ${file.exists()}")

            val toolkit = Toolkit.getDefaultToolkit()
            /*return*/if (file.exists()) {
                toolkit.getImage(file.absolutePath)
            }
            else {
                null
            }
        }
        val canvas = BufferedImage(assembleConfig.fw, assembleConfig.fh, BufferedImage.TYPE_4BYTE_ABGR)


        println("Frame name: $frameName")
        transforms.forEach { println(it) }
        println("==========================")
        println("Transformed skeleton:")
        AssembleFrameBase.makeTransformList(skeleton, transforms).forEach { (name, transform) ->
            println("$name transformedOut: $transform")
        }
    }

}

/**
 * @param fw Frame Width
 * @param fh Frame Height
 * @param ox Origin-X, leftmost point being zero
 * @param oy Origin-Y, bottommost point being zero
 */
data class AssembleConfig(val fw: Int = 48, val fh: Int = 56, val ox: Int = 29, val oy: Int = 0)

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
            val jointToMoveIndex = transformOutput.linearSearch { it.first == transform.joint.name }!!
            transformOutput[jointToMoveIndex] = transformOutput[jointToMoveIndex].first to transform.getTransformVector()
        }

        return transformOutput.toList()
    }
}