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
        val sprite = (computer.getPeripheral("ppu") as PeripheralVideoCard).vram.sprites[0]

        sprite.setLine(0, intArrayOf(1,1,0,0,0,0,3,3))
        sprite.setLine(1, intArrayOf(1,1,0,0,0,0,3,3))
        sprite.setLine(2, intArrayOf(1,1,0,0,0,0,1,1))
        sprite.setLine(3, intArrayOf(1,1,1,1,1,1,1,1))
        sprite.setLine(4, intArrayOf(1,1,1,1,1,1,1,1))
        sprite.setLine(5, intArrayOf(0,0,0,0,0,0,1,1))
        sprite.setLine(6, intArrayOf(2,2,0,0,0,0,1,1))
        sprite.setLine(7, intArrayOf(2,2,0,0,0,0,1,1))

    }

    var angle = 0.0

    override fun update(container: GameContainer, game: StateBasedGame?, delta: Int) {
        Terrarum.appgc.setTitle("VT — F: ${container.fps}" +
                                " — M: ${Terrarum.memInUse}M / ${Terrarum.memTotal}M / ${Terrarum.memXmx}M")
        monitor.update(container, delta)
        computer.update(container, delta)

        val sprite = (computer.getPeripheral("ppu") as PeripheralVideoCard).vram.sprites[0]

        angle += delta / 500.0

        sprite.posX = (Math.cos(angle) * 80 + 100).roundInt()
        sprite.posY = (Math.sin(angle) * 80 + 100).roundInt()

        sprite.pal0 = (sprite.pal0 + 1) % 65
        sprite.pal1 = (sprite.pal1 + 1) % 65
        sprite.pal2 = (sprite.pal2 + 1) % 65
        sprite.pal3 = (sprite.pal3 + 1) % 65

        sprite.rotation = (angle * 2 / Math.PI).roundInt() % 4

    }

    override fun getID() = Terrarum.STATE_ID_TEST_TTY

    override fun render(container: GameContainer, game: StateBasedGame?, g: Graphics) {
        monitor.render(container, g)
    }
}