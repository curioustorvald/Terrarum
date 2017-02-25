package net.torvald.terrarum

import net.torvald.random.HQRNG
import net.torvald.terrarum.gameactors.roundInt
import net.torvald.terrarum.virtualcomputer.computer.BaseTerrarumComputer
import net.torvald.terrarum.virtualcomputer.peripheral.PeripheralVideoCard
import net.torvald.terrarum.virtualcomputer.terminal.GraphicsTerminal
import org.lwjgl.opengl.GL11
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame
import java.util.*

/**
 * Created by SKYHi14 on 2017-02-23.
 */
class StateGraphicComputerTest : BasicGameState() {
    val computer = BaseTerrarumComputer(8)
    val monitor = GraphicsTerminal(computer)

    init {
        val videocard = PeripheralVideoCard()
        monitor.attachVideoCard(videocard)

        computer.attachTerminal(monitor)
        computer.attachPeripheral(videocard)
    }

    override fun init(container: GameContainer?, game: StateBasedGame?) {
        val vcard = (computer.getPeripheral("ppu") as PeripheralVideoCard).vram

        (0..3).forEach { vcard.sprites[it].setPaletteSet(64,33,12,62) }

        vcard.sprites[0].setAll(intArrayOf(
                0,0,0,0,0,1,1,1,
                0,0,0,0,1,1,1,1,
                0,0,0,0,2,2,2,3,
                0,0,0,2,3,2,3,3,
                0,0,0,2,3,2,2,3,
                0,0,0,2,2,3,3,3,
                0,0,0,0,0,3,3,3,
                0,0,0,0,2,2,1,2
        ))
        vcard.sprites[1].setAll(intArrayOf(
                1,1,0,0,0,0,0,0,
                1,1,1,1,1,0,0,0,
                3,2,3,0,0,0,0,0,
                3,2,3,3,3,0,0,0,
                3,3,2,3,3,3,0,0,
                3,2,2,2,2,0,0,0,
                3,3,3,3,0,0,0,0,
                2,2,0,0,0,0,0,0
        ))
        vcard.sprites[2].setAll(intArrayOf(
                0,0,0,2,2,2,1,2,
                0,0,2,2,2,2,1,1,
                0,0,3,3,2,1,3,1,
                0,0,3,3,3,1,1,1,
                0,0,3,3,1,1,1,1,
                0,0,0,0,1,1,1,0,
                0,0,0,2,2,2,0,0,
                0,0,2,2,2,2,0,0
        ))
        vcard.sprites[3].setAll(intArrayOf(
                2,1,2,2,2,0,0,0,
                1,1,2,2,2,2,0,0,
                1,3,1,2,3,3,0,0,
                1,1,1,3,3,3,0,0,
                1,1,1,1,3,3,0,0,
                0,1,1,1,0,0,0,0,
                0,0,2,2,2,0,0,0,
                0,0,2,2,2,2,0,0
        ))
    }

    var angle = 0.0

    override fun update(container: GameContainer, game: StateBasedGame?, delta: Int) {
        Terrarum.appgc.setTitle("VT — F: ${container.fps}" +
                                " — M: ${Terrarum.memInUse}M / ${Terrarum.memTotal}M / ${Terrarum.memXmx}M")
        monitor.update(container, delta)
        computer.update(container, delta)

        val vcard = (computer.getPeripheral("ppu") as PeripheralVideoCard)
        val sprites = vcard.vram.sprites

        angle += delta / 1000.0

        (0..3).forEach { vcard.vram.sprites[it].setPaletteSet(64,26,12,62) }

        sprites[0].posX = (Math.cos(angle) * 80 + 100).roundInt() - 8
        sprites[0].posY = (Math.sin(angle) * 0  + 100).roundInt() - 8

        sprites[1].posX = (Math.cos(angle) * 80 + 100).roundInt()
        sprites[1].posY = (Math.sin(angle) * 0  + 100).roundInt() - 8

        sprites[2].posX = (Math.cos(angle) * 80 + 100).roundInt() - 8
        sprites[2].posY = (Math.sin(angle) * 0  + 100).roundInt()

        sprites[3].posX = (Math.cos(angle) * 80 + 100).roundInt()
        sprites[3].posY = (Math.sin(angle) * 0  + 100).roundInt()

        //sprite.pal0 = (sprite.pal0 + 1) % 65
        //sprite.pal1 = (sprite.pal1 + 1) % 65
        //sprite.pal2 = (sprite.pal2 + 1) % 65
        //sprite.pal3 = (sprite.pal3 + 1) % 65


        //kotlin.repeat(256) {
            //vcard.vram.setBackgroundPixel(Random().nextInt(320), Random().nextInt(200), Random().nextInt(64))
            //vcard.vram.setBackgroundPixel(Random().nextInt(320), Random().nextInt(200), 15)
        //}
    }

    override fun getID() = Terrarum.STATE_ID_TEST_TTY

    override fun render(container: GameContainer, game: StateBasedGame?, g: Graphics) {
        monitor.render(container, g)
    }
}