package net.torvald.terrarum.modulebasegame

import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.modulebasegame.audio.audiobank.InstrumentLoader
import net.torvald.terrarum.modulebasegame.imagefont.WatchFont
import net.torvald.terrarum.weather.WeatherMixer

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
        ModMgr.GameOreLoader.invoke(moduleName)
        ModMgr.GameLanguageLoader.invoke(moduleName)
        ModMgr.GameCraftingRecipeLoader.invoke(moduleName)
        ModMgr.GameAudioLoader.invoke(moduleName)
        ModMgr.GameWeatherLoader.invoke(moduleName)

        WeatherCodex.weatherById["titlescreen"] =
            WeatherCodex.getById("generic01")?.copy(identifier = "titlescreen", windSpeed = 1f) ?: WeatherMixer.DEFAULT_WEATHER

        // load virtual instruments
        printdbg(this, "Loading virtual instrument 'spieluhr@41'")
        InstrumentLoader.load("spieluhr", "basegame", "audio/effects/notes/spieluhr.ogg", 41)

        if (App.IS_DEVELOPMENT_BUILD) {
            println("[EntryPoint] Crafting Recipes: ")
            CraftingRecipeCodex.props.forEach { item, recipes ->
                println("[EntryPoint] $item ->")
                recipes.forEach {
                    print("    ")
                    println(it)
                }
            }
        }

        println("\n[Basegame.EntryPoint] Welcome back!")
    }


    override fun dispose() {
        WatchFont.dispose()
    }
}