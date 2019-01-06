package net.torvald.spriteassembler

import net.torvald.terrarum.AppLoader.printdbg
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


        //printdbg(this, "==============================")

        properties[frameName].forEach { printdbg(this, it) }
    }

}

/**
 * @param fw Frame Width
 * @param fh Frame Height
 * @param ox Origin-X, leftmost point being zero
 * @param oy Origin-Y, bottommost point being zero
 */
data class AssembleConfig(val fw: Int = 48, val fh: Int = 56, val ox: Int = 29, val oy: Int = 0)