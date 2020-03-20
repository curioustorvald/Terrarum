package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import net.torvald.gdx.graphics.Cvec
import net.torvald.terrarum.*
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.blockproperties.BlockCodex
import net.torvald.terrarum.blockproperties.BlockPropUtil
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gameitem.ItemID
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.modulebasegame.gameworld.GameWorldExtension
import net.torvald.terrarum.modulebasegame.gameworld.WorldTime
import net.torvald.terrarum.modulebasegame.ui.Notification
import net.torvald.terrarum.modulebasegame.ui.UIPaletteSelector
import net.torvald.terrarum.modulebasegame.weather.WeatherMixer
import net.torvald.terrarum.realestate.LandUtil
import net.torvald.terrarum.serialise.WriteLayerDataZip.FILE_FOOTER
import net.torvald.terrarum.serialise.WriteLayerDataZip.PAYLOAD_FOOTER
import net.torvald.terrarum.serialise.WriteLayerDataZip.PAYLOAD_HEADER
import net.torvald.terrarum.serialise.toLittle
import net.torvald.terrarum.serialise.toLittleShort
import net.torvald.terrarum.serialise.toULittle48
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UINSMenu
import net.torvald.terrarum.worlddrawer.CreateTileAtlas.TILE_SIZE
import net.torvald.terrarum.worlddrawer.WorldCamera
import java.io.File
import java.io.FileOutputStream

/**
 * Created by minjaesong on 2018-07-06.
 */
class BuildingMaker(batch: SpriteBatch) : IngameInstance(batch) {

    private val menuYaml = Yaml("""
- File
 - New flat ter.
 - New rand. ter.
 - Export…
 - Export sel…
 - Import…
 - Save terrain…
 - Load terrain…
 -
 - Exit to Title : net.torvald.terrarum.modulebasegame.YamlCommandExit
- Tool
 - Pencil : net.torvald.terrarum.modulebasegame.YamlCommandToolPencil
 - Eraser : net.torvald.terrarum.modulebasegame.YamlCommandToolPencilErase
 - Wall Hammer : net.torvald.terrarum.modulebasegame.YamlCommandToolPencilEraseWall
 - Eyedropper : net.torvald.terrarum.modulebasegame.YamlCommandToolEyedropper
 - Add Selection : net.torvald.terrarum.modulebasegame.YamlCommandToolMarquee
 - Remove Sel. : net.torvald.terrarum.modulebasegame.YamlCommandToolMarqueeErase
 - Clear Sel. : net.torvald.terrarum.modulebasegame.YamlCommandToolMarqueeClear
 - Move Selected
 -
 - Hide/Show Sel. : net.torvald.terrarum.modulebasegame.YamlCommandToolToggleMarqueeOverlay
 -
 - Undo
 - Redo
- Time
 - Morning : net.torvald.terrarum.modulebasegame.YamlCommandSetTimeMorning
 - Noon : net.torvald.terrarum.modulebasegame.YamlCommandSetTimeNoon
 - Dusk : net.torvald.terrarum.modulebasegame.YamlCommandSetTimeDusk
 - Night : net.torvald.terrarum.modulebasegame.YamlCommandSetTimeNight
 - Set…
- Weather
 - Sunny
 - Raining
    """.trimIndent())

    private val timeNow = System.currentTimeMillis() / 1000

    val gameWorld = GameWorldExtension(1, 1024, 256, timeNow, timeNow, 0)

    init {
        // ghetto world for building

        println("[BuildingMaker] Generating builder world...")

        for (y in 0 until gameWorld.height) {
            gameWorld.setTileWall(0, y, Block.ILLUMINATOR_RED)
            gameWorld.setTileWall(gameWorld.width - 1, y, Block.ILLUMINATOR_RED)
            gameWorld.setTileTerrain(0, y, Block.ILLUMINATOR_RED_OFF)
            gameWorld.setTileTerrain(gameWorld.width - 1, y, Block.ILLUMINATOR_RED_OFF)
        }

        for (y in 150 until gameWorld.height) {
            for (x in 1 until gameWorld.width - 1) {
                // wall layer
                gameWorld.setTileWall(x, y, Block.DIRT)

                // terrain layer
                gameWorld.setTileTerrain(x, y, if (y == 150) Block.GRASS else Block.DIRT)
            }
        }

        // set time to summer
        gameWorld.worldTime.addTime(WorldTime.DAY_LENGTH * 32)

        world = gameWorld
    }


    override var actorNowPlaying: ActorHumanoid? = MovableWorldCamera(this)

    val uiToolbox = UINSMenu("Menu", 100, menuYaml)
    val notifier = Notification()
    val uiPaletteSelector = UIPaletteSelector(this)
    val uiPalette = UIBuildingMakerBlockChooser(this)
    val uiPenMenu = UIBuildingMakerPenMenu(this)


    val uiContainer = ArrayList<UICanvas?>()

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

    val selection = ArrayList<Point2i>()

    val blockMarkings = CommonResourcePool.getAsTextureRegionPack("blockmarkings_common")
    internal var showSelection = true
    val blockPointingCursor = object : ActorWithBody(Actor.RenderOrder.OVERLAY, usePhysics = false) {

        override var referenceID: ActorID = 1048575 // custom refID
        override val hitbox = Hitbox(0.0, 0.0, 16.0, 16.0)


        init {
            this.actorValue[AVKey.LUMR] = 1.0
            this.actorValue[AVKey.LUMG] = 1.0
        }

        override fun drawBody(batch: SpriteBatch) {
            batch.color = toolCursorColour[currentPenMode]
            batch.draw(blockMarkings.get(currentPenMode, 0), hitbox.startX.toFloat(), hitbox.startY.toFloat())
        }

        override fun drawGlow(batch: SpriteBatch) { }

        override fun dispose() {
        }

        override fun update(delta: Float) {
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

    private fun generateNewBlockMarkerVisible(x: Int, y: Int) = object : ActorWithBody(Actor.RenderOrder.OVERLAY, usePhysics = false) {
        override var referenceID: ActorID = blockPosToRefID(x, y) // custom refID
        override val hitbox = Hitbox(x * 16.0, y * 16.0, 16.0, 16.0)

        override fun drawBody(batch: SpriteBatch) {
            batch.color = blockMarkerColour
            drawSpriteInGoodPosition(blockMarkings.get(2,0), batch)
        }

        private fun drawSpriteInGoodPosition(sprite: TextureRegion, batch: SpriteBatch) {
            val leftsidePadding = world.width.times(TILE_SIZE) - WorldCamera.width.ushr(1)
            val rightsidePadding = WorldCamera.width.ushr(1)

            if (hitbox.startX in WorldCamera.x - hitbox.width..WorldCamera.x + WorldCamera.width.toDouble() &&
                hitbox.startY in WorldCamera.y - hitbox.height..WorldCamera.y + WorldCamera.height.toDouble()) {
                if (WorldCamera.xCentre > leftsidePadding && hitbox.centeredX <= rightsidePadding) {
                    // camera center neg, actor center pos
                    batch.draw(sprite,
                            hitbox.startX.toFloat() + world.width * TILE_SIZE,
                            hitbox.startY.toFloat()
                    )
                }
                else if (WorldCamera.xCentre < rightsidePadding && hitbox.centeredY >= leftsidePadding) {
                    // camera center pos, actor center neg
                    batch.draw(sprite,
                            hitbox.startX.toFloat() - world.width * TILE_SIZE,
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

        override fun drawGlow(batch: SpriteBatch) { }

        override fun update(delta: Float) { }

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
            addNewActor(a)
            actorsRenderOverlay.add(a)
            selection.add(Point2i(x, y))
        }
        catch (e: Error) { }
    }

    internal fun removeBlockMarker(x: Int, y: Int) {
        try {
            val a = getActorByID(blockPosToRefID(x, y))
            removeActor(a)
            actorsRenderOverlay.remove(a)
            selection.remove(Point2i(x, y))
        }
        catch (e: IllegalArgumentException) {
            // no actor to erase, do nothing
        }
    }

    companion object {
        const val PENMODE_PENCIL = 0
        const val PENMODE_PENCIL_ERASE = 1
        const val PENMODE_MARQUEE = 2
        const val PENMODE_MARQUEE_ERASE = 3
        const val PENMODE_EYEDROPPER = 4

        const val PENTARGET_TERRAIN = 1
        const val PENTARGET_WALL = 2

        val toolCursorColour = arrayOf(
                Color.YELLOW,
                Color.YELLOW,
                Color.MAGENTA,
                Color.MAGENTA,
                Color.WHITE
        )

        const val DEFAULT_POI_NAME = "The Yucky Panopticon"
    }

    private val actorsRenderOverlay = ArrayList<ActorWithBody>() // can be hidden (e.g. hide sel.)
    private val essentialOverlays = ArrayList<ActorWithBody>()

    init {
        gameWorld.worldTime.setTimeOfToday(WorldTime.HOUR_SEC * 10)
        gameWorld.globalLight = Cvec(.8f, .8f, .8f, .8f)

        essentialOverlays.add(blockPointingCursor)

        uiContainer.add(uiToolbox)
        uiContainer.add(uiPaletteSelector)
        uiContainer.add(notifier)
        uiContainer.add(uiPalette)
        uiContainer.add(uiPenMenu)



        uiToolbox.setPosition(0, 0)
        uiToolbox.isVisible = true
        uiToolbox.invocationArgument = arrayOf(this)

        uiPaletteSelector.setPosition(AppLoader.screenW - uiPaletteSelector.width, 0)
        uiPaletteSelector.isVisible = true

        notifier.setPosition(
                (AppLoader.screenW - notifier.width) / 2, AppLoader.screenH - notifier.height)


        actorNowPlaying?.setPosition(512 * 16.0, 149 * 16.0)


        uiPalette.setPosition(200, 100)


        IngameRenderer.setRenderedWorld(gameWorld)
    }

    override fun show() {
        Gdx.input.inputProcessor = BuildingMakerController(this)
        super.show()
    }

    private var updateAkku = 0.0

    override fun render(updateRate: Float) {
        Gdx.graphics.setTitle(TerrarumIngame.getCanonicalTitle())


        // ASYNCHRONOUS UPDATE AND RENDER //

        val dt = Gdx.graphics.rawDeltaTime
        updateAkku += dt

        var i = 0L
        while (updateAkku >= updateRate) {
            AppLoader.measureDebugTime("Ingame.update") { updateGame(updateRate) }
            updateAkku -= updateRate
            i += 1
        }
        AppLoader.setDebugTime("Ingame.updateCounter", i)

        // render? just do it anyway
        AppLoader.measureDebugTime("Ingame.render") { renderGame() }
        AppLoader.setDebugTime("Ingame.render-Light",
                (AppLoader.debugTimers["Ingame.render"] as Long) - ((AppLoader.debugTimers["Renderer.LightTotal"] as? Long) ?: 0)
        )

    }

    private var mouseOnUI = false
    internal var tappedOnUI = false // when true, even if the UI is closed, pen won't work unless your pen is lifted
    // must be set to TRUE by UIs

    private fun updateGame(delta: Float) {

        WeatherMixer.update(delta, actorNowPlaying, gameWorld)
        blockPointingCursor.update(delta)
        actorNowPlaying?.update(delta)
        var overwriteMouseOnUI = false
        uiContainer.forEach {
            it?.update(delta)
            if (it?.isVisible == true && it?.mouseUp == true) {
                overwriteMouseOnUI = true
            }
        }

        mouseOnUI = (overwriteMouseOnUI || uiPenMenu.isVisible)


        WorldCamera.update(world, actorNowPlaying)


        // make pen work HERE
        // when LEFT mouse is down
        if (!tappedOnUI && Gdx.input.isButtonPressed(AppLoader.getConfigInt("mouseprimary")) && !mouseOnUI) {

            makePenWork(Terrarum.mouseTileX, Terrarum.mouseTileY)
            // TODO drag support using bresenham's algo
            //      for some reason it just doesn't work...
        }
        else if (!uiPenMenu.isVisible && Gdx.input.isButtonPressed(AppLoader.getConfigInt("mousesecondary"))) {
            // open pen menu
            // position the menu to where the cursor is
            uiPenMenu.posX = Terrarum.mouseScreenX - uiPenMenu.width / 2
            uiPenMenu.posY = Terrarum.mouseScreenY - uiPenMenu.height / 2
            uiPenMenu.posX = uiPenMenu.posX.coerceIn(0, AppLoader.screenW - uiPenMenu.width)
            uiPenMenu.posY = uiPenMenu.posY.coerceIn(0, AppLoader.screenH - uiPenMenu.height)

            // actually open
            uiPenMenu.setAsOpen()
        }

        BlockPropUtil.dynamicLumFuncTickClock()
    }

    private fun renderGame() {
        _testMarkerDrawCalls = 0L

        IngameRenderer.invoke(false, actorsRenderOverlay = if (showSelection) actorsRenderOverlay + essentialOverlays else essentialOverlays, uisToDraw = uiContainer)

        AppLoader.setDebugTime("Test.MarkerDrawCalls", _testMarkerDrawCalls)
    }

    override fun resize(width: Int, height: Int) {
        IngameRenderer.resize(AppLoader.screenW, AppLoader.screenH)
        uiToolbox.setPosition(0, 0)
        notifier.setPosition(
                (AppLoader.screenW - notifier.width) / 2, AppLoader.screenH - notifier.height)

        println("[BuildingMaker] Resize event")
    }

    fun setPencilColour(itemID: ItemID) {
        uiPaletteSelector.fore = itemID
        currentPenMode = PENMODE_PENCIL
        currentPenTarget = PENTARGET_TERRAIN // TERRAIN is arbitrary chosen to prevent possible conflict; for the pencil itself this property does nothing
    }

    override fun dispose() {
        blockMarkings.dispose()
        uiPenMenu.dispose()
    }

    private fun makePenWork(x: Int, y: Int) {
        val world = gameWorld
        val palSelection = uiPaletteSelector.fore

        when (currentPenMode) {
            // test paint terrain layer
            PENMODE_PENCIL -> {
                if (palSelection < BlockCodex.MAX_TERRAIN_TILES)
                    world.setTileTerrain(x, y, palSelection)
                else if (palSelection < 2 * BlockCodex.MAX_TERRAIN_TILES)
                    world.setTileWall(x, y, palSelection - BlockCodex.MAX_TERRAIN_TILES)
            }
            PENMODE_PENCIL_ERASE -> {
                if (currentPenTarget and PENTARGET_WALL != 0)
                    world.setTileWall(x, y, Block.AIR)
                else
                    world.setTileTerrain(x, y, Block.AIR)
            }
            PENMODE_EYEDROPPER -> {
                uiPaletteSelector.fore = if (world.getTileFromTerrain(x, y) == Block.AIR)
                    world.getTileFromWall(x, y)!! + BlockCodex.MAX_TERRAIN_TILES
                else
                    world.getTileFromTerrain(x, y)!!
            }
            PENMODE_MARQUEE -> {
                addBlockMarker(x, y)
            }
            PENMODE_MARQUEE_ERASE -> {
                removeBlockMarker(x, y)
            }
        }
    }

    private fun getSelectionTotalDimension(): Point2i {
        selection.sortBy { it.y * world.width + it.x }
        return selection.last() - selection.first()
    }

    private fun serialiseSelection(outfile: File) {
        // save format: sparse list encoded in following binary format:
        /*
        Header: TEaT0bLD -- magic: Terrarum Attachment
                Int8 version number -- always 1
                Int8 number of layers -- always 2 or 3
                Int8 number of payloads -- always 2 or 3
                int8 compression algorithm -- always 1 (DEFLATE)
                Int16 width

        The rest: payloads defined in the map data format
                  Payloads: array of (Int48 tileAddress, UInt16 blockID)
                  Payload names: TerL, WalL, WirL for Terrain, Wall and Wire respectively

        Footer: EndTEM \xFF\xFE -- magic: end of attachment with BOM

        Endian: LITTLE
         */
        // proc:
        //   translate boxes so that leftmost point is (0,0)
        //   write to the list using translated coords

        val payloads = arrayOf("WalL", "TerL", "WirL")

        val selectionDim = getSelectionTotalDimension()
        val fos = FileOutputStream(outfile)
        // write header
        fos.write("TEaT0bLD".toByteArray())
        fos.write(byteArrayOf(1,3,3,1))
        fos.write(selectionDim.x.toLittleShort())
        // write wall -> terrain -> wire (order defined in GameWorld.TERRAIN/WALL/WIRE)
        payloads.forEachIndexed { index, it ->
            fos.write(PAYLOAD_HEADER); fos.write(it.toByteArray())
            selection.forEach {
                val tile = world.getTileFrom(index, it.x, it.y)!!
                val addr = LandUtil.getBlockAddr(world, it.x - selectionDim.x, it.y - selectionDim.y)
                fos.write(addr.toULittle48())
                fos.write(tile.toLittle())
            }
            fos.write(PAYLOAD_FOOTER)
        }
        fos.write(FILE_FOOTER)
        fos.close()
    }
}

class BuildingMakerController(val screen: BuildingMaker) : InputAdapter() {
    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        screen.uiContainer.forEach { it?.touchUp(screenX, screenY, pointer, button) }
        screen.tappedOnUI = false
        return true
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        screen.uiContainer.forEach { it?.mouseMoved(screenX, screenY) }
        return true
    }

    override fun keyTyped(character: Char): Boolean {
        screen.uiContainer.forEach { it?.keyTyped(character) }
        return true
    }

    override fun scrolled(amount: Int): Boolean {
        screen.uiContainer.forEach { it?.scrolled(amount) }
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

class MovableWorldCamera(val parent: BuildingMaker) : ActorHumanoid(0, usePhysics = false) {

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
            (AppLoader.screenW - hitbox.width) / 2.0,
            (AppLoader.screenH - hitbox.height) / 2.0
    )
    private var coerceInEnd = Point2d(
            parent.world.width * TILE_SIZE - (AppLoader.screenW - hitbox.width) / 2.0,
            parent.world.height * TILE_SIZE - (AppLoader.screenH - hitbox.height) / 2.0
    )

    override fun update(delta: Float) {
        super.update(delta)

        // confine the camera so it won't wrap
        this.hitbox.hitboxStart.setCoerceIn(coerceInStart, coerceInEnd)
    }

    override fun drawBody(batch: SpriteBatch) {
    }

    override fun drawGlow(batch: SpriteBatch) {
    }

    override fun onActorValueChange(key: String, value: Any?) {
    }

}

class YamlCommandExit : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        AppLoader.setScreen(TitleScreen(AppLoader.batch))
    }
}

class YamlCommandSetTimeMorning : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).gameWorld.worldTime.setTimeOfToday(WorldTime.parseTime("7h00"))
    }
}

class YamlCommandSetTimeNoon : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).gameWorld.worldTime.setTimeOfToday(WorldTime.parseTime("12h30"))
    }
}

class YamlCommandSetTimeDusk : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).gameWorld.worldTime.setTimeOfToday(WorldTime.parseTime("18h40"))
    }
}

class YamlCommandSetTimeNight : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).gameWorld.worldTime.setTimeOfToday(WorldTime.parseTime("0h30"))
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
            (args[0] as BuildingMaker).removeBlockMarker(it.x, it.y)
        }
    }
}

class YamlCommandToolToggleMarqueeOverlay : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).showSelection = !(args[0] as BuildingMaker).showSelection
    }
}
