package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockPropUtil
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gamecontroller.TerrarumKeyboardEvent
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameparticles.ParticleBase
import net.torvald.terrarum.gameworld.BlockLayerI16
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.gameworld.WorldTime
import net.torvald.terrarum.modulebasegame.ui.UIBuildingMakerBlockChooser
import net.torvald.terrarum.modulebasegame.ui.UIBuildingMakerPenMenu
import net.torvald.terrarum.modulebasegame.ui.UIPaletteSelector
import net.torvald.terrarum.modulebasegame.ui.UIScreenZoom
import net.torvald.terrarum.serialise.Common
import net.torvald.terrarum.serialise.PointOfInterest
import net.torvald.terrarum.serialise.POILayer
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.weather.WeatherMixer
import net.torvald.terrarum.ui.UINSMenu
import net.torvald.terrarum.utils.OrePlacement
import net.torvald.terrarum.worlddrawer.BlocksDrawer
import net.torvald.terrarum.worlddrawer.LightmapRenderer
import net.torvald.terrarum.worlddrawer.WorldCamera
import net.torvald.util.CircularArray
import java.io.File

/**
 * Created by minjaesong on 2018-07-06.
 */
class BuildingMaker(batch: FlippingSpriteBatch) : IngameInstance(batch) {

    private val menuYaml = Yaml("""
- File
 - New Flat ter. : net.torvald.terrarum.modulebasegame.YamlCommandNewFlatTerrain
 - New Rand. ter.
 - Export… : net.torvald.terrarum.modulebasegame.YamlCommandToolExportTest
 - Import… : net.torvald.terrarum.modulebasegame.YamlCommandToolImportTest
 - Exit to Title : net.torvald.terrarum.modulebasegame.YamlCommandExit
- Edit
 - Clear Selections : net.torvald.terrarum.modulebasegame.YamlCommandClearSelection
- Time
 - Dawn : net.torvald.terrarum.modulebasegame.YamlCommandSetTimeDawn
 - Sunrise : net.torvald.terrarum.modulebasegame.YamlCommandSetTimeSunrise
 - Morning : net.torvald.terrarum.modulebasegame.YamlCommandSetTimeMorning
 - Noon : net.torvald.terrarum.modulebasegame.YamlCommandSetTimeNoon
 - Dusk : net.torvald.terrarum.modulebasegame.YamlCommandSetTimeDusk
 - Sunset : net.torvald.terrarum.modulebasegame.YamlCommandSetTimeSunset
 - Night : net.torvald.terrarum.modulebasegame.YamlCommandSetTimeNight
 - Midnight : net.torvald.terrarum.modulebasegame.YamlCommandSetTimeMidnight
 - Set…
    """.trimIndent())

    lateinit var gameWorld: GameWorld

    override val backgroundMusicPlayer = TerrarumBackgroundMusicPlayer()

    init {
        gameUpdateGovernor = ConsistentUpdateRate
        YamlCommandNewFlatTerrain().invoke(arrayOf(this))
    }

    internal fun reset() {
        gameUpdateGovernor.reset()
        WeatherMixer.forceSolarElev = 5.0
    }


    override var actorNowPlaying: ActorHumanoid? = MovableWorldCamera(this)

    val uiToolbox = UINSMenu("Menu", 100, menuYaml)
    val uiPaletteSelector = UIPaletteSelector(this)
    val uiPalette = UIBuildingMakerBlockChooser(this)
    val uiPenMenu = UIBuildingMakerPenMenu(this)

    val uiGetPoiName = UIBuildingMakerGetFilename() // used for both import and export

    val keyboardUsedByTextInput: Boolean
        get() = uiGetPoiName.textInput.isEnabled

    private val pensMustShowSelection = arrayOf(
            PENMODE_MARQUEE, PENMODE_MARQUEE_ERASE
    )

    var currentPenMode = PENMODE_PENCIL
        set(value) {
            field = value
            if (value in pensMustShowSelection) {
                showSelection = true
            }
        }
    var currentPenTarget = PENTARGET_TERRAIN

    val selection = ArrayList<Long>()

    val blockMarkings = CommonResourcePool.getAsTextureRegionPack("blockmarkings_common")
    internal var showSelection = true
    val blockPointingCursor = object : ActorWithBody(Actor.RenderOrder.OVERLAY, physProp = PhysProperties.MOBILE_OBJECT()) {

        override var referenceID: ActorID = 1048575 // custom refID
        override val hitbox = Hitbox(0.0, 0.0, 16.0, 16.0)


        init {
            this.actorValue[AVKey.LUMR] = 1.0
            this.actorValue[AVKey.LUMG] = 1.0
        }

        override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
            batch.color = toolCursorColour[currentPenMode]
            batch.draw(blockMarkings.get(currentPenMode, 0), hitbox.startX.toFloat(), hitbox.startY.toFloat())
        }

        override fun drawGlow(frameDelta: Float, batch: SpriteBatch) { }

        override fun dispose() {
        }

        override fun updateImpl(delta: Float) {
            hitbox.setPosition(
                    Terrarum.mouseTileX * 16.0,
                    Terrarum.mouseTileY * 16.0
            )
        }

        override fun onActorValueChange(key: String, value: Any?) { }

        override fun run() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    private val blockMarkerColour = Color(0xe0e0e0ff.toInt())

    internal fun blockPosToRefID(x: Int, y: Int) = 1048576 + (y * gameWorld.width + x)

    private var _testMarkerDrawCalls = 0L

    private fun generateNewBlockMarkerVisible(x: Int, y: Int) = object : ActorWithBody(Actor.RenderOrder.OVERLAY, physProp = PhysProperties.MOBILE_OBJECT()) {
        override var referenceID: ActorID = blockPosToRefID(x, y) // custom refID
        override val hitbox = Hitbox(x * 16.0, y * 16.0, 16.0, 16.0)

        override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
            batch.color = blockMarkerColour
            drawSpriteInGoodPosition(blockMarkings.get(2,0), batch)
        }

        private fun drawSpriteInGoodPosition(sprite: TextureRegion, batch: SpriteBatch) {
            val leftsidePadding = world!!.width.times(TILE_SIZE) - WorldCamera.width.ushr(1)
            val rightsidePadding = WorldCamera.width.ushr(1)

            if (hitbox.startX in WorldCamera.x - hitbox.width..WorldCamera.x + WorldCamera.width.toDouble() &&
                hitbox.startY in WorldCamera.y - hitbox.height..WorldCamera.y + WorldCamera.height.toDouble()) {
                if (WorldCamera.xCentre > leftsidePadding && hitbox.centeredX <= rightsidePadding) {
                    // camera center neg, actor center pos
                    batch.draw(sprite,
                            hitbox.startX.toFloat() + world!!.width * TILE_SIZE,
                            hitbox.startY.toFloat()
                    )
                }
                else if (WorldCamera.xCentre < rightsidePadding && hitbox.centeredY >= leftsidePadding) {
                    // camera center pos, actor center neg
                    batch.draw(sprite,
                            hitbox.startX.toFloat() - world!!.width * TILE_SIZE,
                            hitbox.startY.toFloat()
                    )
                }
                else {
                    batch.draw(sprite,
                            hitbox.startX.toFloat(),
                            hitbox.startY.toFloat()
                    )
                }

                _testMarkerDrawCalls += 1
            }
        }

        override fun drawGlow(frameDelta: Float, batch: SpriteBatch) { }

        override fun updateImpl(delta: Float) { }

        override fun onActorValueChange(key: String, value: Any?) { }

        override fun run() {
            TODO("not implemented")
        }

        override fun dispose() {

        }
    }

    internal fun addBlockMarker(x: Int, y: Int) {
        try {
            val a = generateNewBlockMarkerVisible(x, y)
            if (!theGameHasActor(a.referenceID)) {
                selection.add(toAddr(x, y))
                actorsRenderOverlay.add(a)
                queueActorAddition(a)
            }
        }
        catch (e: Error) { }
    }

    internal fun removeBlockMarker(x: Int, y: Int) {
        try {
            selection.remove(toAddr(x, y))
            val a = getActorByID(blockPosToRefID(x, y))
            actorsRenderOverlay.remove(a)
            queueActorRemoval(a)
        }
        catch (e: Throwable) {
            // no actor to erase, do nothing
//            e.printStackTrace()
        }
    }

    internal var imported: PointOfInterest? = null

    companion object {
        const val PENMODE_PENCIL = 0
        const val PENMODE_PENCIL_ERASE = 1
        const val PENMODE_MARQUEE = 2
        const val PENMODE_MARQUEE_ERASE = 3
        const val PENMODE_EYEDROPPER = 4
        const val PENMODE_IMPORT = 8

        const val PENTARGET_TERRAIN = 1
        const val PENTARGET_WALL = 2
        const val PENTARGET_ORE = 4

        val toolCursorColour = arrayOf(
            Color.YELLOW,
            Color.YELLOW,
            Color.MAGENTA,
            Color.MAGENTA,
            Color.WHITE,
            Color.WHITE,
            Color.WHITE,
            Color.WHITE,
            Color.MAGENTA,
        )

        const val DEFAULT_POI_NAME = "The Yucky Panopticon"
    }

    private val actorsRenderOverlay = ArrayList<ActorWithBody>() // can be hidden (e.g. hide sel.)
    private val essentialOverlays = ArrayList<ActorWithBody>()

    init {
        essentialOverlays.add(blockPointingCursor)

        uiContainer.add(
            uiToolbox,
            uiPaletteSelector,
            notifier,
            uiPalette,
            uiPenMenu,
            uiGetPoiName,
            UIScreenZoom(),
        )



        uiToolbox.setPosition(0, 0)
        uiToolbox.isVisible = true
        uiToolbox.invocationArgument = arrayOf(this)

        uiPaletteSelector.setPosition(0, App.scr.height - uiPaletteSelector.height)
        uiPaletteSelector.isVisible = true

        notifier.setPosition(
            (Toolkit.drawWidth - notifier.width) / 2,
            App.scr.height - notifier.height - App.scr.tvSafeGraphicsHeight
        )


        actorNowPlaying?.setPosition(512 * 16.0, 149 * 16.0)


        uiPalette.setPosition(200, 100)


        IngameRenderer.setRenderedWorld(gameWorld)
        WeatherMixer.internalReset(this)
    }

    override fun show() {
        Gdx.input.inputProcessor = BuildingMakerController(this)
        App.audioMixer.reset()
        super.show()
    }


    private var disableMouseClick = false
    var mousePrimaryJustDown = false; private set

    override fun renderImpl(updateRate: Float) {
        IngameRenderer.setRenderedWorld(gameWorld)

        super.renderImpl(updateRate)



        Gdx.graphics.setTitle(TerrarumIngame.getCanonicalTitle())


        // ASYNCHRONOUS UPDATE AND RENDER //
        gameUpdateGovernor.update(Gdx.graphics.deltaTime, App.UPDATE_RATE, updateGame, renderGame
        )
        App.setDebugTime("Ingame.Render - (Light + Tiling)",
                ((App.debugTimers["Ingame.Render"]) ?: 0) -
                (
                        ((App.debugTimers["Renderer.Lanterns"]) ?: 0) +
                        ((App.debugTimers["Renderer.LightPrecalc"]) ?: 0) +
                        ((App.debugTimers["Renderer.LightRuns"]) ?: 0) +
                        ((App.debugTimers["Renderer.LightToScreen"]) ?: 0) +
                        ((App.debugTimers["Renderer.Tiling"]) ?: 0)
                )
        )



        if (!Gdx.input.isButtonPressed(App.getConfigInt("config_mouseprimary"))) {
            disableMouseClick = false
        }
    }

    private var mouseOnUI = false
    internal var tappedOnUI = false // when true, even if the UI is closed, pen won't work unless your pen is lifted
    // must be set to TRUE by UIs

    private val updateGame = { delta: Float ->

        blockPointingCursor.update(delta)

        if (!keyboardUsedByTextInput) {
            actorNowPlaying?.update(delta)
        }

        var overwriteMouseOnUI = false
        uiContainer.forEach {
            it?.update(delta)
            if (it?.isVisible == true && it?.mouseUp == true) {
                overwriteMouseOnUI = true
            }
        }

        mouseOnUI = (overwriteMouseOnUI || uiPenMenu.isVisible /*|| uiPalette.isVisible*/ || uiGetPoiName.isVisible)


        WorldCamera.update(world, actorNowPlaying)


        // make pen work HERE
        // when LEFT mouse is down
        if (!tappedOnUI && Terrarum.mouseDown && !mouseOnUI) {

            makePenWork(Terrarum.mouseTileX, Terrarum.mouseTileY)
            // TODO drag support using bresenham's algo
            //      for some reason it just doesn't work...
        }
        else if (!uiPenMenu.isVisible && Gdx.input.isButtonPressed(App.getConfigInt("config_mousesecondary"))) {
            // open pen menu
            // position the menu to where the cursor is
            uiPenMenu.posX = Terrarum.mouseScreenX - uiPenMenu.width / 2
            uiPenMenu.posY = Terrarum.mouseScreenY - uiPenMenu.height / 2
            uiPenMenu.posX = uiPenMenu.posX.coerceIn(0, App.scr.width - uiPenMenu.width)
            uiPenMenu.posY = uiPenMenu.posY.coerceIn(0, App.scr.height - uiPenMenu.height)

            // actually open
            uiPenMenu.setAsOpen()
        }


        actorAdditionQueue.forEach { forceAddActor(it.first, it.second) }
        actorAdditionQueue.clear()
        actorRemovalQueue.forEach { forceRemoveActor(it.first, it.second) }
        actorRemovalQueue.clear()


        BlockPropUtil.dynamicLumFuncTickClock()
        WeatherMixer.update(delta, actorNowPlaying, gameWorld)
        if (WORLD_UPDATE_TIMER % 2 == 1L) {
            fillUpWiresBuffer()
        }


        backgroundMusicPlayer.update(this, delta)
    }


    private val maxRenderableWires = ReferencingRanges.ACTORS_WIRES.last - ReferencingRanges.ACTORS_WIRES.first + 1
    private val wireActorsContainer = Array(maxRenderableWires) { WireActor(ReferencingRanges.ACTORS_WIRES.first + it).let {
        forceAddActor(it)
        /*^let*/ it
    } }
    var selectedWireRenderClass = ""
    private var oldSelectedWireRenderClass = ""


    private fun fillUpWiresBuffer() {
        val for_y_start = (WorldCamera.y.toFloat() / TILE_SIZE).floorToInt() - LightmapRenderer.LIGHTMAP_OVERRENDER
        val for_y_end = for_y_start + BlocksDrawer.tilesInVertical + 2* LightmapRenderer.LIGHTMAP_OVERRENDER

        val for_x_start = (WorldCamera.x.toFloat() / TILE_SIZE).floorToInt() - LightmapRenderer.LIGHTMAP_OVERRENDER
        val for_x_end = for_x_start + BlocksDrawer.tilesInHorizontal + 2* LightmapRenderer.LIGHTMAP_OVERRENDER

        var wiringCounter = 0
        for (y in for_y_start..for_y_end) {
            for (x in for_x_start..for_x_end) {
                if (wiringCounter >= maxRenderableWires) break

                val (wires, nodes) = world.getAllWiresFrom(x, y)

                wires?.forEach {
                    val wireActor = wireActorsContainer[wiringCounter]

                    wireActor.setWire(it, x, y, nodes!![it]!!.cnx)

                    if (WireCodex[it].renderClass == selectedWireRenderClass || selectedWireRenderClass == "wire_render_all") {
                        wireActor.renderOrder = Actor.RenderOrder.OVERLAY
                    }
                    else {
                        wireActor.renderOrder = Actor.RenderOrder.FAR_BEHIND
                    }

                    wireActor.isUpdate = true
                    wireActor.isVisible = true
                    wireActor.forceDormant = false

                    wiringCounter += 1
                }

            }
        }

        for (i in wiringCounter until maxRenderableWires) {
            wireActorsContainer[i].isUpdate = false
            wireActorsContainer[i].isVisible = false
            wireActorsContainer[i].forceDormant = true
        }
    }

    private val particles = CircularArray<ParticleBase>(16, true)

    private val renderGame = { delta: Float ->
        _testMarkerDrawCalls = 0L

        IngameRenderer.invoke(delta, false,
            screenZoom,
            listOf(),
            listOf(),
            listOf(),
            listOf(),
            listOf(),
            if (showSelection) actorsRenderOverlay + essentialOverlays else essentialOverlays,
            particles,
            uiContainer = uiContainer
        )

        App.setDebugTime("Test.MarkerDrawCalls", _testMarkerDrawCalls)
    }

    override fun resize(width: Int, height: Int) {
        IngameRenderer.resize(App.scr.width, App.scr.height)
        val drawWidth = Toolkit.drawWidth

        uiToolbox.setPosition(0, 0)
        notifier.setPosition(
            (Toolkit.drawWidth - notifier.width) / 2,
            App.scr.height - notifier.height - App.scr.tvSafeGraphicsHeight
        )

        println("[BuildingMaker] Resize event")
    }

    fun setPencilColour(itemID: ItemID) {
        uiPaletteSelector.fore = itemID
        currentPenMode = PENMODE_PENCIL
        currentPenTarget = PENTARGET_TERRAIN // TERRAIN is arbitrary chosen to prevent possible conflict; for the pencil itself this property does nothing
    }

    override fun dispose() {
//        blockMarkings.dispose()
        uiPenMenu.dispose()
        uiGetPoiName.dispose()
        backgroundMusicPlayer.dispose()
    }

    fun getPoiNameForExport(w: Int, h: Int, callback: (String) -> Unit) {
        uiGetPoiName.let {
            it.title = "Export"
            it.labelDo = "Export"
            it.text = listOf("WH: $w\u00D7$h", "Name of the POI:")
            it.confirmCallback = { s ->
                callback(s)
                disableMouseClick = true
            }
            it.cancelCallback = {
                disableMouseClick = true
            }
            it.setPosition(
                240,
                32
            )
            it.reset()
            it.setAsOpen()
        }
    }

    fun getPoiNameForImport(callback: (String) -> Unit) {
        uiGetPoiName.let {
            it.title = "Import"
            it.labelDo = "Import"
            it.text = listOf("Name of the POI:")
            it.confirmCallback = { s ->
                callback(s)
                disableMouseClick = true
            }
            it.cancelCallback = {
                disableMouseClick = true
            }
            it.setPosition(
                240,
                32
            )
            it.reset()
            it.setAsOpen()
        }
    }

    private fun getNearbyTilesPos8(x: Int, y: Int): Array<Point2i> {
        return arrayOf(
            Point2i(x + 1, y),
            Point2i(x + 1, y + 1),
            Point2i(x, y + 1),
            Point2i(x - 1, y + 1),
            Point2i(x - 1, y),
            Point2i(x - 1, y - 1),
            Point2i(x, y - 1),
            Point2i(x + 1, y - 1)
        )
    }
    private fun getNearbyOres8(x: Int, y: Int): List<OrePlacement> {
        return getNearbyTilesPos8(x, y).map { world.getTileFromOre(it.x, it.y) }
    }

    private fun makePenWork(x: Int, y: Int) {
        val world = gameWorld
        val palSelection = uiPaletteSelector.fore

        if (!disableMouseClick) {
            when (currentPenMode) {
                // test paint terrain layer
                PENMODE_PENCIL -> {
                    if (palSelection.startsWith("wall@"))
                        world.setTileWall(x, y, palSelection.substring(5), true)
                    else if (palSelection.startsWith("ore@")) {
                        // get autotiling placement
                        val autotiled = getNearbyOres8(x, y).foldIndexed(0) { index, acc, placement ->
                            acc or (placement.item == palSelection).toInt(index)
                        }
                        val placement = BlocksDrawer.connectLut16[autotiled]

                        world.setTileOre(x, y, palSelection, placement)
                    }
                    else
                        world.setTileTerrain(x, y, palSelection, true)
                }

                PENMODE_PENCIL_ERASE -> {
                    if (currentPenTarget and PENTARGET_TERRAIN != 0)
                        world.setTileTerrain(x, y, Block.AIR, true)
                    if (currentPenTarget and PENTARGET_WALL != 0)
                        world.setTileWall(x, y, Block.AIR, true)
                    if (currentPenTarget and PENTARGET_ORE != 0)
                        world.setTileOre(x, y, Block.AIR, 0)
                }

                PENMODE_EYEDROPPER -> {
                    uiPaletteSelector.fore = if (world.getTileFromTerrain(x, y) == Block.AIR)
                        "wall@" + world.getTileFromWall(x, y)
                    else
                        world.getTileFromTerrain(x, y)
                }

                PENMODE_MARQUEE -> {
                    addBlockMarker(x, y)
                }

                PENMODE_MARQUEE_ERASE -> {
                    removeBlockMarker(x, y)
                }

                PENMODE_IMPORT -> {
                    importPoi(x, y)
                    disableMouseClick = true
                }
            }
        }
    }

    private fun importPoi(x: Int, y: Int) {
        imported?.let {
            it.placeOnWorld(listOf(it.layers.first().name), world, x, y) // TODO show layer list and ticker using right click?
        }
    }

    override fun inputStrobed(e: TerrarumKeyboardEvent) {
        uiContainer.forEach {
            it?.inputStrobed(e)
        }
    }
}

class BuildingMakerController(val screen: BuildingMaker) : InputAdapter() {
    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        screen.uiContainer.forEach { it?.touchUp(screenX, screenY, pointer, button) }
        screen.tappedOnUI = false
        return true
    }

    override fun keyTyped(character: Char): Boolean {
        screen.uiContainer.forEach { it?.keyTyped(character) }
        return true
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        screen.uiContainer.forEach { it?.scrolled(amountX, amountY) }
        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        screen.uiContainer.forEach { it?.keyUp(keycode) }
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        screen.uiContainer.forEach { it?.touchDragged(screenX, screenY, pointer) }
        return true
    }

    override fun keyDown(keycode: Int): Boolean {
        screen.uiContainer.forEach { it?.keyDown(keycode) }
        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        screen.uiContainer.forEach { it?.touchDown(screenX, screenY, pointer, button) }
        return true
    }
}

class MovableWorldCamera(val parent: BuildingMaker) : ActorHumanoid(0, physProp = PhysProperties.MOBILE_OBJECT()) {

    init {
        referenceID = Terrarum.PLAYER_REF_ID
        isNoClip = true

        setHitboxDimension(1, 1, 0, 0)


        actorValue[AVKey.SPEED] = 8.0
        actorValue[AVKey.SPEEDBUFF] = 1.0
        actorValue[AVKey.ACCEL] = ActorHumanoid.WALK_ACCEL_BASE
        actorValue[AVKey.ACCELBUFF] = 4.0
        actorValue[AVKey.JUMPPOWER] = 0.0
        actorValue[AVKey.FRICTIONMULT] = 4.0
    }

    // TODO resize-aware
    private var coerceInStart = Point2d(
            (App.scr.width - hitbox.width) / 2.0,
            (App.scr.height - hitbox.height) / 2.0
    )
    private var coerceInEnd = Point2d(
            parent.world.width * TILE_SIZE - (App.scr.width - hitbox.width) / 2.0,
            parent.world.height * TILE_SIZE - (App.scr.height - hitbox.height) / 2.0
    )

    override fun updateImpl(delta: Float) {
        super.updateImpl(delta)

        // confine the camera so it won't wrap
        this.hitbox.hitboxStart.setCoerceIn(coerceInStart, coerceInEnd)
    }

    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
    }

    override fun drawGlow(frameDelta: Float, batch: SpriteBatch) {
    }

    override fun onActorValueChange(key: String, value: Any?) {
    }

}

class YamlCommandExit : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        App.setScreen(TitleScreen(App.batch))
    }
}

class YamlCommandSetTimeDawn : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).gameWorld.worldTime.setTimeOfToday(WorldTime.parseTime("6h00"))
        WeatherMixer.forceSolarElev = -3.0
    }
}

class YamlCommandSetTimeSunrise : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).gameWorld.worldTime.setTimeOfToday(WorldTime.parseTime("6h00"))
        WeatherMixer.forceSolarElev = 0.0
    }
}

class YamlCommandSetTimeMorning : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).gameWorld.worldTime.setTimeOfToday(WorldTime.parseTime("6h00"))
        WeatherMixer.forceSolarElev = 5.0
    }
}

class YamlCommandSetTimeNoon : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).gameWorld.worldTime.setTimeOfToday(WorldTime.parseTime("12h00"))
        WeatherMixer.forceSolarElev = 30.0
    }
}

class YamlCommandSetTimeDusk : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).gameWorld.worldTime.setTimeOfToday(WorldTime.parseTime("18h00"))
        WeatherMixer.forceSolarElev = 3.0
    }
}

class YamlCommandSetTimeSunset : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).gameWorld.worldTime.setTimeOfToday(WorldTime.parseTime("18h00"))
        WeatherMixer.forceSolarElev = 0.0
    }
}

class YamlCommandSetTimeNight : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).gameWorld.worldTime.setTimeOfToday(WorldTime.parseTime("18h00"))
        WeatherMixer.forceSolarElev = -8.0
    }
}

class YamlCommandSetTimeMidnight : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).gameWorld.worldTime.setTimeOfToday(WorldTime.parseTime("0h00"))
        WeatherMixer.forceSolarElev = -30.0
    }
}

class YamlCommandToolPencil : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).currentPenMode = BuildingMaker.PENMODE_PENCIL
        (args[0] as BuildingMaker).currentPenTarget = BuildingMaker.PENTARGET_TERRAIN
    }
}

class YamlCommandToolPencilErase : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).currentPenMode = BuildingMaker.PENMODE_PENCIL_ERASE
        (args[0] as BuildingMaker).currentPenTarget = BuildingMaker.PENTARGET_TERRAIN
    }
}

class YamlCommandToolPencilEraseWall : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).currentPenMode = BuildingMaker.PENMODE_PENCIL_ERASE
        (args[0] as BuildingMaker).currentPenTarget = BuildingMaker.PENTARGET_WALL
    }
}

class YamlCommandToolEyedropper : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).currentPenMode = BuildingMaker.PENMODE_EYEDROPPER
        (args[0] as BuildingMaker).currentPenTarget = BuildingMaker.PENTARGET_TERRAIN + BuildingMaker.PENTARGET_WALL
    }
}

class YamlCommandToolMarquee : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).currentPenMode = BuildingMaker.PENMODE_MARQUEE
    }
}

class YamlCommandToolMarqueeErase : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).currentPenMode = BuildingMaker.PENMODE_MARQUEE_ERASE
    }
}

class YamlCommandToolMarqueeClear : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).selection.toList().forEach {
            val (x, y) = (it % 4294967296).toInt() to (it / 4294967296).toInt()
            (args[0] as BuildingMaker).removeBlockMarker(x, y)
        }
    }
}

class YamlCommandToolToggleMarqueeOverlay : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).showSelection = !(args[0] as BuildingMaker).showSelection
    }
}

class YamlCommandToolExportTest : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        val ui = (args[0] as BuildingMaker)
        if (ui.selection.isEmpty()) return

        val marked = HashSet<Long>()
        fun isMarked(x: Int, y: Int) = marked.contains(toAddr(x, y))


        // get the bounding box
        var minX = 2147483647
        var minY = 2147483647
        var maxX = -1
        var maxY = -1
        ui.selection.forEach {
            val (x, y) = (it % 4294967296).toInt() to (it / 4294967296).toInt()

            if (x < minX) minX = x
            if (y < minY) minY = y
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y

            marked.add(it)
        }

        ui.getPoiNameForExport(maxX - minX + 1, maxY - minY + 1) { name ->
            // prepare POI
            val poi = PointOfInterest(
                name,
                maxX - minX + 1,
                maxY - minY + 1,
                ui.world.tileNumberToNameMap,
                ui.world.tileNameToNumberMap
            )
            val layer = POILayer(name)
            val terr = BlockLayerI16(poi.w, poi.h)
            val wall = BlockLayerI16(poi.w, poi.h)
            layer.blockLayer = arrayListOf(terr, wall)
            poi.layers.add(layer)

            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    if (isMarked(x, y)) {
                        terr.unsafeSetTile(x - minX, y - minY, ui.world.getTileFromTerrainRaw(x, y))
                        wall.unsafeSetTile(x - minX, y - minY, ui.world.getTileFromWallRaw(x, y))
                    }
                    else {
                        terr.unsafeSetTile(x - minX, y - minY, -1)
                        wall.unsafeSetTile(x - minX, y - minY, -1)
                    }
                }
            }


            // process POI for export
            val json = Common.jsoner
            val jsonStr = json.toJson(poi)


            val dir = App.defaultDir + "/Exports/"
            val dirAsFile = File(dir)
            if (!dirAsFile.exists()) {
                dirAsFile.mkdir()
            }

            File(dirAsFile, "$name.poi").writeText(jsonStr)
        }
    }
}

private fun Point2i.toAddr() = toAddr(this.x, this.y)
private fun toAddr(x: Int, y: Int) = (y.toLong().shl(32) or x.toLong().and(0xFFFFFFFFL))

class YamlCommandClearSelection : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        val ui = (args[0] as BuildingMaker)
        try {
            (ui.selection.clone() as ArrayList<Long>).forEach { i ->
                ui.removeBlockMarker((i % 4294967296).toInt(), (i / 4294967296).toInt())
            }
        }
        catch (e: NullPointerException) {}
    }
}

class YamlCommandNewFlatTerrain : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        YamlCommandClearSelection().invoke(args)
        val ui = (args[0] as BuildingMaker)

        println("[BuildingMaker] Generating builder world...")

        val timeNow = System.currentTimeMillis() / 1000
        ui.gameWorld = GameWorld(90*12, 90*4, timeNow, timeNow)

        for (y in 0 until ui.gameWorld.height) {
            ui.gameWorld.setTileWall(0, y, Block.ILLUMINATOR_RED, true)
            ui.gameWorld.setTileWall(ui.gameWorld.width - 1, y, Block.ILLUMINATOR_RED, true)
            ui.gameWorld.setTileTerrain(0, y, Block.ILLUMINATOR_RED_OFF, true)
            ui.gameWorld.setTileTerrain(ui.gameWorld.width - 1, y, Block.ILLUMINATOR_RED_OFF, true)
        }

        for (y in 150 until ui.gameWorld.height) {
            for (x in 1 until ui.gameWorld.width - 1) {
                // wall layer
                ui.gameWorld.setTileWall(x, y, Block.DIRT, true)

                // terrain layer
                ui.gameWorld.setTileTerrain(x, y, if (y == 150) Block.GRASS else Block.DIRT, true)
            }
        }
        ui.world = ui.gameWorld


        // set time to summer morning
        ui.gameWorld.worldTime.addTime(WorldTime.DAY_LENGTH * 32)
        ui.gameWorld.worldTime.setTimeOfToday(18062)

        ui.reset()
    }
}

class YamlCommandToolImportTest : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        val ui = (args[0] as BuildingMaker)
        ui.getPoiNameForImport { name ->
            YamlCommandClearSelection().invoke(args)

            val jsonFile = File(App.defaultDir, "/Exports/$name.poi")
            if (jsonFile.exists()) {
                val json = jsonFile.readText()

                val poi = Common.jsoner.fromJson(PointOfInterest().javaClass, json).also {
                    it.getReadyToBeUsed(ui.world.tileNameToNumberMap)
                }

                ui.imported = poi
                ui.currentPenMode = BuildingMaker.PENMODE_IMPORT
            }
        }
    }
}