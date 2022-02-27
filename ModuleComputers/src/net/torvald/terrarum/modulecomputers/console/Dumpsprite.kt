package net.torvald.terrarum.modulecomputers.console

import com.badlogic.gdx.Gdx
import net.torvald.gdx.graphics.PixmapIO2
import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.terrarum.App
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.console.ConsoleCommand
import net.torvald.terrarum.console.Echo
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2022-02-27.
 */
internal object Dumpsprite : ConsoleCommand {

    override fun execute(args: Array<String>) {

        val player = Terrarum.ingame!!.actorNowPlaying!!.sprite!!

        val field = SpriteAnimation::class.java.getDeclaredField("textureRegion")
        field.isAccessible = true
        val textureRegion = field.get(player) as TextureRegionPack
        val texture = textureRegion.texture.textureData

        val pixmap = texture.consumePixmap()
        PixmapIO2.writeTGA(Gdx.files.absolute("${App.defaultDir}/Exports/PlayerTexture.tga"), pixmap, false)

    }

    override fun printUsage() {
        Echo("A spear to test the ingame's shield")
    }

}