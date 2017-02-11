package net.torvald.terrarum.gameactors

import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gamecontroller.mouseX
import net.torvald.terrarum.gamecontroller.mouseY
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Image

/**
 * Created by minjaesong on 2017-01-07.
 */
class TapestryObject(val image: Image, val artName: String, val artAuthor: String) : FixtureBase(physics = false) {

    // physics = false only speeds up for ~2 frames with 50 tapestries

    init {
        image.filter = Image.FILTER_NEAREST
        makeNewSprite(image.width, image.height, image)
        setHitboxDimension(image.width, image.height, 0, 0)
        setPosition(Terrarum.appgc.mouseX, Terrarum.appgc.mouseY)
    }

    override fun update(gc: GameContainer, delta: Int) {
        super.update(gc, delta)
    }

    override fun drawBody(g: Graphics) {
        super.drawBody(g)
    }
}
