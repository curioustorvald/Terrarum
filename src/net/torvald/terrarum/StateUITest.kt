package net.torvald.terrarum

import net.torvald.terrarum.gameactors.ActorInventory
import net.torvald.terrarum.gameitem.InventoryItem
import net.torvald.terrarum.mapdrawer.MapCamera
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UIHandler
import net.torvald.terrarum.ui.UIItemTextButton
import net.torvald.terrarum.ui.UIItemTextButtonList
import org.newdawn.slick.Color
import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input
import org.newdawn.slick.state.BasicGameState
import org.newdawn.slick.state.StateBasedGame

/**
 * Created by SKYHi14 on 2017-03-13.
 */
class StateUITest : BasicGameState() {

    val ui = UIHandler(SimpleUI())

    val inventory = ActorInventory()

    init {
        ui.posX = 50
        ui.isVisible = true


        inventory.add(object : InventoryItem() {
            override val id: Int = 5656
            override var baseMass: Double = 12.0
            override var baseToolSize: Double? = 8.0
            override var category: String = "tool"
        })

        inventory.add(object : InventoryItem() {
            override val id: Int = 4633
            override var baseMass: Double = 1.4
            override var baseToolSize: Double? = null
            override var category: String = "bulk"
        })
    }

    override fun init(container: GameContainer?, game: StateBasedGame?) {
    }

    override fun update(container: GameContainer, game: StateBasedGame, delta: Int) {
        ui.update(container, delta)
    }

    override fun getID() = Terrarum.STATE_ID_TEST_UI

    override fun render(container: GameContainer, game: StateBasedGame, g: Graphics) {
        ui.render(container, game, g)
    }
}







private class SimpleUI : UICanvas {
    override var width = 400
    override var height = 600
    override var handler: UIHandler? = null
    override var openCloseTime: Int = UICanvas.OPENCLOSE_GENERIC

    val buttons = UIItemTextButtonList(
            this,
            arrayOf(
                    "GAME_INVENTORY_WEAPONS", // weapons and tools
                    "CONTEXT_ITEM_EQUIPMENT_PLURAL",
                    "CONTEXT_ITEM_ARMOR",
                    "GAME_INVENTORY_INGREDIENTS",
                    "GAME_INVENTORY_POTIONS",
                    "GAME_INVENTORY_BLOCKS",
                    "GAME_INVENTORY_WALLPAPERS",
                    "MENU_LABEL_ALL"
            ),
            300, height,
            readFromLang = true
    )

    override fun update(gc: GameContainer, delta: Int) {
        Terrarum.gameLocale = "fiFI" // hot swap this to test

        buttons.update(gc, delta)
    }

    override fun render(gc: GameContainer, g: Graphics) {
        g.color = Color(0x282828)
        g.fillRect(0f, 0f, 300f, 600f)


        buttons.render(gc, g)
    }

    override fun processInput(gc: GameContainer, delta: Int, input: Input) {
    }

    override fun doOpening(gc: GameContainer, delta: Int) {
        UICanvas.doOpeningFade(handler, openCloseTime)
    }

    override fun doClosing(gc: GameContainer, delta: Int) {
        UICanvas.doClosingFade(handler, openCloseTime)
    }

    override fun endOpening(gc: GameContainer, delta: Int) {
        UICanvas.endOpeningFade(handler)
    }

    override fun endClosing(gc: GameContainer, delta: Int) {
        UICanvas.endClosingFade(handler)
    }
}