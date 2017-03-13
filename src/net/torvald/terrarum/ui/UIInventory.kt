package net.torvald.terrarum.ui

import org.newdawn.slick.GameContainer
import org.newdawn.slick.Graphics
import org.newdawn.slick.Input

/**
 * Created by SKYHi14 on 2017-03-13.
 */
class UIInventory : UICanvas {
    override var width: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}
    override var height: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}
    override var handler: UIHandler?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}
    override var openCloseTime: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}


    private val categories = arrayOf(
            "GAME_INVENTORY_WEAPONS", // weapons and tools
            "CONTEXT_ITEM_EQUIPMENT_PLURAL",
            "CONTEXT_ITEM_ARMOR",
            "GAME_INVENTORY_INGREDIENTS",
            "GAME_INVENTORY_POTIONS",
            "GAME_INVENTORY_BLOCKS",
            "GAME_INVENTORY_WALLPAPERS",
            "MENU_LABEL_ALL"
    )

    override fun update(gc: GameContainer, delta: Int) {
        TODO("not implemented")
    }

    override fun render(gc: GameContainer, g: Graphics) {
        TODO("not implemented")
    }

    override fun processInput(gc: GameContainer, delta: Int, input: Input) {
        TODO("not implemented")
    }

    override fun doOpening(gc: GameContainer, delta: Int) {
        TODO("not implemented")
    }

    override fun doClosing(gc: GameContainer, delta: Int) {
        TODO("not implemented")
    }

    override fun endOpening(gc: GameContainer, delta: Int) {
        TODO("not implemented")
    }

    override fun endClosing(gc: GameContainer, delta: Int) {
        TODO("not implemented")
    }
}