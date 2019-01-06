package net.torvald.spriteassembler

/**
 * Assembles the single frame of the animation, outputs GDX Pixmap.
 *
 * Created by minjaesong on 2019-01-06.
 */
object AssembleFrameGdxPixmap {

    operator fun invoke(properties: ADProperties, frameName: String) {
        val theAnim = properties.getAnimByFrameName(frameName)
        val skeleton = theAnim.skeleton.joints

        println("Test")

        // test print required body part filename
        skeleton.reversed().forEach {
            println(properties.toFilename(it.name))
        }
    }

}