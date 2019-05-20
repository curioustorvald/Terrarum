package net.torvald.terrarum.modulebasegame

import net.torvald.terrarum.AppLoader
import net.torvald.terrarum.AppLoader.IS_DEVELOPMENT_BUILD
import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.ModuleEntryPoint
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.gameitem.GameItem
import net.torvald.terrarum.itemproperties.ItemCodex
import net.torvald.terrarum.itemproperties.MaterialCodex
import net.torvald.terrarum.modulebasegame.gameitems.BlockBase
import net.torvald.terrarum.modulebasegame.imagefont.WatchFont
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * The entry point for the module "Basegame"
 *
 * Created by minjaesong on 2018-06-21.
 */
class EntryPoint : ModuleEntryPoint() {

    private val moduleName = "basegame"

    override fun invoke() {

        // the order of invocation is important! Material should be the first as blocks and items are depend on it.
        ModMgr.GameMaterialLoader.invoke(moduleName)
        ModMgr.GameBlockLoader.invoke(moduleName)
        ModMgr.GameItemLoader.invoke(moduleName)
        ModMgr.GameLanguageLoader.invoke(moduleName)



        // load common resources to the AssetsManager
        AppLoader.resourcePool.addToLoadingList("$moduleName.items16") {
            TextureRegionPack(ModMgr.getGdxFile(moduleName, "items/items.tga"), 16, 16)
        }
        AppLoader.resourcePool.addToLoadingList("$moduleName.items24") {
            TextureRegionPack(ModMgr.getGdxFile(moduleName, "items/items24.tga"), 24, 24)
        }
        AppLoader.resourcePool.addToLoadingList("$moduleName.items48") {
            TextureRegionPack(ModMgr.getGdxFile(moduleName, "items/items48.tga"), 48, 48)
        }


        /////////////////////////////////
        // load customised item loader //
        /////////////////////////////////

        printdbg(this, "recording item ID ")

        // blocks.csvs are loaded by ModMgr beforehand
        // block items (blocks and walls are the same thing basically)
        for (i in ItemCodex.ITEM_TILES + ItemCodex.ITEM_WALLS) {
            val blockProp = BlockCodex.getOrNull(i % ItemCodex.ITEM_WALLS.first)

            if (blockProp != null) {
                ItemCodex.itemCodex[i] = object : GameItem(i) {
                    override val isUnique: Boolean = false
                    override var baseMass: Double = blockProp.density / 1000.0
                    override var baseToolSize: Double? = null
                    override val originalName = blockProp.nameKey
                    override var stackable = true
                    override var inventoryCategory = if (i in ItemCodex.ITEM_TILES) Category.BLOCK else Category.WALL
                    override var isDynamic = false
                    override val material = MaterialCodex.getOrDefault(blockProp.material)

                    init {
                        equipPosition = EquipPosition.HAND_GRIP

                        if (IS_DEVELOPMENT_BUILD)
                            print("$originalID ")
                    }

                    override fun startPrimaryUse(delta: Float): Boolean {
                        return BlockBase.blockStartPrimaryUse(this, i, delta)
                    }

                    override fun effectWhenEquipped(delta: Float) {
                        BlockBase.blockEffectWhenEquipped(delta)
                    }
                }
            }
        }



        println("Welcome back!")
    }

    override fun dispose() {
        WatchFont.dispose()
    }
}