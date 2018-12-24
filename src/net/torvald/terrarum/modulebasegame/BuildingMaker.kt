package net.torvald.terrarum.modulebasegame

import com.badlogic.gdx.Gdx
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
import net.torvald.terrarum.modulebasegame.ui.UIBuildingMakerToolbox
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarum.worlddrawer.LightmapRenderer
import net.torvald.terrarum.worlddrawer.WorldCamera
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import kotlin.system.measureNanoTime

/**
 * Created by minjaesong on 2018-07-06.
 */
class BuildingMaker(batch: SpriteBatch) : IngameInstance(batch) {

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


        world = gameWorld
    }


    override var actorNowPlaying: ActorHumanoid? = MovableWorldCamera()

    val uiToolbox = UIBuildingMakerToolbox()
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

    protected var updateDeltaCounter = 0.0
    protected val updateRate = 1.0 / Terrarum.TARGET_INTERNAL_FPS

    private val actorsRenderOverlay = ArrayList<ActorWithBody>()

    init {
        gameWorld.time.setTimeOfToday(WorldTime.HOUR_SEC * 10)
        gameWorld.globalLight = Color(.8f,.8f,.8f,.8f)

        actorsRenderOverlay.add(blockPointingCursor)

        uiContainer.add(uiToolbox)
        uiContainer.add(notifier)



        uiToolbox.setPosition(Terrarum.WIDTH - 20, 4)
        notifier.setPosition(
                (Terrarum.WIDTH - notifier.width) / 2, Terrarum.HEIGHT - notifier.height)


        actorNowPlaying?.setPosition(512 * 16.0, 149 * 16.0)



        LightmapRenderer.fireRecalculateEvent()
    }

    override fun render(delta: Float) {
        Gdx.graphics.setTitle(Ingame.getCanonicalTitle())


        // ASYNCHRONOUS UPDATE AND RENDER //


        /** UPDATE CODE GOES HERE */
        updateDeltaCounter += delta



        if (false && AppLoader.getConfigBoolean("multithread")) { // NO MULTITHREADING: camera don't like concurrent modification (jittery actor movements)
            // else, NOP;
        }
        else {
            var updateTries = 0
            while (updateDeltaCounter >= updateRate) {

                //updateGame(delta)
                AppLoader.debugTimers["Ingame.update"] = measureNanoTime { updateGame(delta) }

                updateDeltaCounter -= updateRate
                updateTries++

                if (updateTries >= Terrarum.UPDATE_CATCHUP_MAX_TRIES) {
                    break
                }
            }
        }



        /** RENDER CODE GOES HERE */
        //renderGame(batch)
        AppLoader.debugTimers["Ingame.render"] = measureNanoTime { renderGame() }
    }

    private fun updateGame(delta: Float) {
        KeyToggler.update(false)

        blockPointingCursor.update(delta)
        actorNowPlaying?.update(delta)
        uiContainer.forEach { it.update(delta) }

        WorldCamera.update(world, actorNowPlaying)
    }

    private fun renderGame() {
        IngameRenderer(world as GameWorldExtension, actorsRenderOverlay = actorsRenderOverlay, uisToDraw = uiContainer)
    }

    override fun resize(width: Int, height: Int) {
        IngameRenderer.resize(Terrarum.WIDTH, Terrarum.HEIGHT)
        uiToolbox.setPosition(Terrarum.WIDTH - 20, 4)
        notifier.setPosition(
                (Terrarum.WIDTH - notifier.width) / 2, Terrarum.HEIGHT - notifier.height)
        println("[BuildingMaker] Resize event")
    }

    override fun dispose() {
        IngameRenderer.dispose()
    }

    private val menuYaml = Yaml("""
- File
 - New
 - Export
 - Import
- Edit
    """.trimIndent())
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