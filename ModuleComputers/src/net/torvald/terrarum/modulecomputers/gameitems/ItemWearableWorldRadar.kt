package net.torvald.terrarum.modulecomputers.gameitems

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.itemproperties.Material
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulecomputers.tsvmperipheral.WorldRadar
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.tsvm.PeripheralEntry
import net.torvald.tsvm.TheRealWorld
import net.torvald.tsvm.VM
import net.torvald.tsvm.peripheral.ExtDisp
import net.torvald.tsvm.peripheral.VMProgramRom

/**
 * Created by minjaesong on 2021-12-03.
 */
class ItemWearableWorldRadar(originalID: String) : GameItem(originalID) {

    override var dynamicID: ItemID = originalID
    override val originalName = "ITEM_COMPUTER_DIRTBOARD_FAKETM"
    override var baseMass = 2.0
    override var stackable = true
    override var inventoryCategory = Category.TOOL
    override val isUnique = false
    override val isDynamic = true
    override val material = Material()
    override val itemImage: TextureRegion
        get() = CommonResourcePool.getAsTextureRegion("basegame-sprites-fixtures-signal_source.tga")
    override var baseToolSize: Double? = baseMass


    val vm = VM(32768, TheRealWorld(), arrayOf(
            VMProgramRom(ModMgr.getPath("dwarventech", "bios/pipboot.rom")),
            VMProgramRom(ModMgr.getPath("dwarventech", "bios/pipcode.bas"))
    ))
    val ui = WearableWorldRadarUI(vm)

    init {
        super.equipPosition = EquipPosition.HAND_GRIP

        vm.getIO().blockTransferPorts[1].attachDevice(WorldRadar())
        vm.peripheralTable[1] = PeripheralEntry(
            ExtDisp(vm, 160, 140), 32768, 1, 0
        )

        App.disposables.add(Disposable { vm.dispose() })
        App.disposables.add(ui)
    }

    override fun effectWhenEquipped(actor: ActorWithBody, delta: Float) {
        (Terrarum.ingame!! as TerrarumIngame).wearableDeviceUI = ui
    }

    override fun effectOnUnequip(actor: ActorWithBody, delta: Float) {
        (Terrarum.ingame!! as TerrarumIngame).wearableDeviceUI = null
    }
}

class WearableWorldRadarUI(val device: VM) : UICanvas() {

    override var width = 160
    override var height = 140
    override var openCloseTime = 0f

    override fun updateUI(delta: Float) {
        device.update(delta)
    }

    override fun renderUI(batch: SpriteBatch, camera: Camera) {
        batch.color = Color.WHITE
        (device.peripheralTable[1].peripheral as? ExtDisp)?.render(batch, posX.toFloat(), posY.toFloat())

        batch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(batch, posX-1, posY-1, width+2, height+2)
    }

    override fun doOpening(delta: Float) {
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
    }

    override fun dispose() {
    }


}