package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.*
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.modulebasegame.gameworld.GameWorldExtension
import net.torvald.terrarum.modulebasegame.gameworld.WorldTime
import net.torvald.terrarum.modulebasegame.ui.Notification
import net.torvald.terrarum.modulebasegame.weather.WeatherMixer
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.ui.UINSMenu
import net.torvald.terrarum.worlddrawer.LightmapRenderer
import net.torvald.terrarum.worlddrawer.WorldCamera
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2018-07-06.
 */
class BuildingMaker(batch: SpriteBatch) : IngameInstance(batch) {

    private val menuYaml = Yaml("""
- File
 - New
 - Export…
 - Export sel…
 - Import…
 - Save world…
 - Load world…
 - Exit to Title : net.torvald.terrarum.modulebasegame.YamlCommandExit
- Tool
 - Pencil
 - Eyedropper
 - Select mrq.
 - Move
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

        for (y in 150 until gameWorld.height) {
            for (x in 0 until gameWorld.width) {
                // wall layer
                gameWorld.setTileWall(x, y, Block.DIRT)

                // terrain layer
                gameWorld.setTileTerrain(x, y, if (y == 150) Block.GRASS else Block.DIRT)
            }
        }

        // set time to summer
        gameWorld.time.addTime(WorldTime.DAY_LENGTH * 32)

        world = gameWorld
    }


    override var actorNowPlaying: ActorHumanoid? = MovableWorldCamera()

    val uiToolbox = UINSMenu("Menu", 100, menuYaml)
    val notifier = Notification()

    val uiContainer = ArrayList<UICanvas>()

    val blockPointingCursor = object : ActorWithBody(Actor.RenderOrder.OVERLAY) {

        override var referenceID: ActorID? = Terrarum.generateUniqueReferenceID(renderOrder)
        val body = TextureRegionPack(Gdx.files.internal("assets/graphics/blocks/block_markings_common.tga"), 16, 16)
        override val hitbox = Hitbox(0.0, 0.0, 16.0, 16.0)

        init {
            this.actorValue[AVKey.LUMR] = 1.0
            this.actorValue[AVKey.LUMG] = 1.0
        }

        override fun drawBody(batch: SpriteBatch) {
            batch.color = Color.YELLOW
            batch.draw(body.get(0, 0), hitbox.startX.toFloat(), hitbox.startY.toFloat())
        }

        override fun drawGlow(batch: SpriteBatch) { }

        override fun dispose() {
            body.dispose()
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

    private val actorsRenderOverlay = ArrayList<ActorWithBody>()

    init {
        gameWorld.time.setTimeOfToday(WorldTime.HOUR_SEC * 10)
        gameWorld.globalLight = Color(.8f,.8f,.8f,.8f)

        actorsRenderOverlay.add(blockPointingCursor)

        uiContainer.add(uiToolbox)
        uiContainer.add(notifier)



        uiToolbox.setPosition(0, 0)
        uiToolbox.isVisible = true
        uiToolbox.invocationArgument = arrayOf(this)

        notifier.setPosition(
                (Terrarum.WIDTH - notifier.width) / 2, Terrarum.HEIGHT - notifier.height)


        actorNowPlaying?.setPosition(512 * 16.0, 149 * 16.0)



        LightmapRenderer.fireRecalculateEvent()
    }

    override fun show() {
        Gdx.input.inputProcessor = BuildingMakerController(this)
        super.show()
    }

    override fun render(delta: Float) {
        Gdx.graphics.setTitle(Ingame.getCanonicalTitle())


        // ASYNCHRONOUS UPDATE AND RENDER //


        // TODO async update
        updateGame(delta)

        // render? just do it anyway
        renderGame()
    }

    private fun updateGame(delta: Float) {
        KeyToggler.update(false)

        WeatherMixer.update(delta, actorNowPlaying, gameWorld)
        blockPointingCursor.update(delta)
        actorNowPlaying?.update(delta)
        uiContainer.forEach { it.update(delta) }

        WorldCamera.update(world, actorNowPlaying)
    }

    private fun renderGame() {
        IngameRenderer.invoke(world as GameWorldExtension, actorsRenderOverlay = actorsRenderOverlay, uisToDraw = uiContainer)
    }

    override fun resize(width: Int, height: Int) {
        IngameRenderer.resize(Terrarum.WIDTH, Terrarum.HEIGHT)
        uiToolbox.setPosition(0, 0)
        notifier.setPosition(
                (Terrarum.WIDTH - notifier.width) / 2, Terrarum.HEIGHT - notifier.height)
        println("[BuildingMaker] Resize event")
    }

    override fun dispose() {
        blockPointingCursor.dispose()
    }
}

class BuildingMakerController(val screen: BuildingMaker) : InputAdapter() {
    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        screen.uiContainer.forEach { it.touchUp(screenX, screenY, pointer, button) }
        return true
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        screen.uiContainer.forEach { it.mouseMoved(screenX, screenY) }
        return true
    }

    override fun keyTyped(character: Char): Boolean {
        screen.uiContainer.forEach { it.keyTyped(character) }
        return true
    }

    override fun scrolled(amount: Int): Boolean {
        screen.uiContainer.forEach { it.scrolled(amount) }
        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        screen.uiContainer.forEach { it.keyUp(keycode) }
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        screen.uiContainer.forEach { it.touchDragged(screenX, screenY, pointer) }
        return true
    }

    override fun keyDown(keycode: Int): Boolean {
        screen.uiContainer.forEach { it.keyDown(keycode) }
        return true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        screen.uiContainer.forEach { it.touchDown(screenX, screenY, pointer, button) }
        return true
    }
}

class MovableWorldCamera : ActorHumanoid(0, usePhysics = false) {

    init {
        referenceID = Terrarum.PLAYER_REF_ID
        isNoClip = true

        setHitboxDimension(1, 1, 0, 0)


        actorValue[AVKey.SPEED] = 8.0
        actorValue[AVKey.SPEEDBUFF] = 1.0
        actorValue[AVKey.ACCEL] = ActorHumanoid.WALK_ACCEL_BASE
        actorValue[AVKey.ACCELBUFF] = 1.0
        actorValue[AVKey.JUMPPOWER] = 0.0
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
        Terrarum.setScreen(TitleScreen(Terrarum.batch))
    }
}

class YamlCommandSetTimeMorning : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).gameWorld.time.setTimeOfToday(WorldTime.parseTime("7h00"))
    }
}

class YamlCommandSetTimeNoon : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).gameWorld.time.setTimeOfToday(WorldTime.parseTime("12h30"))
    }
}

class YamlCommandSetTimeDusk : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).gameWorld.time.setTimeOfToday(WorldTime.parseTime("18h40"))
    }
}

class YamlCommandSetTimeNight : YamlInvokable {
    override fun invoke(args: Array<Any>) {
        (args[0] as BuildingMaker).gameWorld.time.setTimeOfToday(WorldTime.parseTime("0h30"))
    }
}