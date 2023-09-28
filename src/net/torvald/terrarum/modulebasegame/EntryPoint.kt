package net.torvald.terrarum.modulebasegame

import net.torvald.terrarum.*
import net.torvald.terrarum.App.IS_DEVELOPMENT_BUILD
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.blockproperties.BlockProp
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.itemproperties.CraftingCodex
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

    override fun getTitleScreen(batch: FlippingSpriteBatch): IngameInstance? {
        return TitleScreen(batch)
    }

    override fun invoke() {

        printdbg(this, "Hello, world!")

        // load common resources to the AssetsManager
        CommonResourcePool.addToLoadingList("$moduleName.items") {
            ItemSheet(ModMgr.getGdxFile(moduleName, "items/items.tga"))
        }
        CommonResourcePool.loadAll()


        // the order of invocation is important! Material should be the first as blocks and items are depend on it.
        ModMgr.GameMaterialLoader.invoke(moduleName)
        ModMgr.GameItemLoader.invoke(moduleName)
        ModMgr.GameBlockLoader.invoke(moduleName)
        ModMgr.GameLanguageLoader.invoke(moduleName)
        ModMgr.GameCraftingRecipeLoader.invoke(moduleName)

        println("Crafting Recipes: ")
        CraftingRecipeCodex.props.forEach { item, recipes ->
            println("$item ->")
            recipes.forEach {
                print("    ")
                println(it)
            }
        }

        /////////////////////////////////
        // load customised item loader //
        /////////////////////////////////

        printdbg(this, "recording item ID ")

        // blocks.csvs are loaded by ModMgr beforehand
        // block items (blocks and walls are the same thing basically)
        for (tile in BlockCodex.getAll()) {
            ItemCodex[tile.id] = makeNewItemObj(tile, false)

            if (IS_DEVELOPMENT_BUILD) print(tile.id+" ")

            if (BlockCodex[tile.id].isWallable) {
                ItemCodex["wall@" + tile.id] = makeNewItemObj(tile, true).also {
                    it.tags.add("WALL")
                }
                if (IS_DEVELOPMENT_BUILD) print("wall@" + tile.id + " ")
            }
        }

        // crafting recipes: tile -> 2x wall
        BlockCodex.getAll().filter { it.isWallable && it.isSolid && !it.isActorBlock }.forEach { tile ->
            CraftingRecipeCodex.addRecipe(CraftingCodex.CraftingRecipe(
                "",
                arrayOf(CraftingCodex.CraftingIngredients(
                    tile.id, CraftingCodex.CraftingItemKeyMode.VERBATIM, 1
                )),
                2,
                "wall@"+tile.id,
                moduleName
            ))
        }

        println("\n[Basegame.EntryPoint] Welcome back!")
    }

    private fun makeNewItemObj(tile: BlockProp, isWall: Boolean) = object : GameItem(
            if (isWall) "wall@"+tile.id else tile.id
    ) {
        override val isUnique: Boolean = false
        override var baseMass: Double = tile.density / 1000.0
        override var baseToolSize: Double? = null
        override var stackable = true
        override var inventoryCategory = if (isWall) Category.WALL else Category.BLOCK
        override var isDynamic = false
        override val materialId = tile.material
//        override val itemImage: TextureRegion
//            get() {
//                val itemSheetNumber = App.tileMaker.tileIDtoItemSheetNumber(originalID)
//                val bucket =  if (isWall) BlocksDrawer.tileItemWall else BlocksDrawer.tileItemTerrain
//                return bucket.get(
//                        itemSheetNumber % App.tileMaker.ITEM_ATLAS_TILES_X,
//                        itemSheetNumber / App.tileMaker.ITEM_ATLAS_TILES_X
//                )
//            }

        init {
            equipPosition = EquipPosition.HAND_GRIP
            tags.addAll(tile.tags)
        }

        override val originalName: String =
            if (isWall && tags.contains("UNLIT")) "${tile.nameKey}>>=BLOCK_UNLIT_TEMPLATE>>=BLOCK_WALL_NAME_TEMPLATE"
            else if (isWall) "${tile.nameKey}>>=BLOCK_WALL_NAME_TEMPLATE"
            else if (tags.contains("UNLIT")) "${tile.nameKey}>>=BLOCK_UNLIT_TEMPLATE"
            else tile.nameKey

        override fun startPrimaryUse(actor: ActorWithBody, delta: Float): Long {
            return BlockBase.blockStartPrimaryUse(actor, this, dynamicID, delta)
        }

        override fun effectWhileEquipped(actor: ActorWithBody, delta: Float) {
            BlockBase.blockEffectWhenEquipped(actor, delta)
        }
    }


    override fun dispose() {
        WatchFont.dispose()
    }
}