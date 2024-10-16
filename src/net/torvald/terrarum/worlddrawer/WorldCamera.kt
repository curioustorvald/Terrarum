package net.torvald.terrarum.worlddrawer

import com.badlogic.gdx.Gdx
import com.jme3.math.FastMath
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.fmod
import org.dyn4j.geometry.Vector2
import kotlin.math.roundToInt

/**
 * Created by minjaesong on 2016-12-30.
 */
object WorldCamera {

    //val zoom: Float
    //    get() = Terrarum.ingame?.screenZoom ?: 1f

    var x: Int = 0 // left position
        private set
    var y: Int = 0 // top position
        private set

    var tx: Int = 0 // tilewise left position
        private set
    var ty: Int = 0 // tilewise top position
        private set

    var width: Int = 0
        private set
    var height: Int = 0
        private set

    private var zoom = 1f
    private var zoomSamplePoint = 0f

    // zoomed coords. Currently only being used by the lightmaprenderer.
    // What about others? We just waste 3/4 of the framebuffer
    val zoomedX: Int
        get() {
            val t = (if (App.getConfigBoolean("fx_streamerslayout")) App.scr.chatWidth / 2 else 0).toFloat()
            return x + (width * zoomSamplePoint - (t * (zoom - 1f)) / 2f).toInt()
        }
    val zoomedY: Int
        get() = y + (height * zoomSamplePoint).toInt()

    val zoomedWidth: Int
        get() = (width / zoom).ceilToInt()
    val zoomedHeight: Int
        get() = (height / zoom).ceilToInt()

    var xEnd: Int = 0 // right position
        private set
    var yEnd: Int = 0 // bottom position
        private set

    inline val gdxCamX: Float // centre position
        get() = xCentre.toFloat()
    inline val gdxCamY: Float// centre position
        get() = yCentre.toFloat()

    inline val xCentre: Int
        get() = x + width.ushr(1)
    inline val yCentre: Int
        get() = y + height.ushr(1)

    var deltaX: Int = 0; private set
    var deltaY: Int = 0; private set

    var deltaTx: Int = 0; private set
    var deltaTy: Int = 0; private set

    private val nullVec = Vector2(0.0, 0.0)

    /** World width in pixels */
    var worldWidth = 0
        private set
    /** World height in pixels */
    var worldHeight = 0
        private set

    inline val camVector: com.badlogic.gdx.math.Vector2
        get() = com.badlogic.gdx.math.Vector2(gdxCamX, gdxCamY)

    /**
     * Must be called on RENDER, not UPDATE, before the IngameRenderer invocation.
     */
    fun update(world: GameWorld, player: ActorWithBody?) {
        if (player == null) return

        width = App.scr.width//FastMath.ceil(AppLoader.terrarumAppConfig.screenW / zoom) // div, not mul
        height = App.scr.height//FastMath.ceil(AppLoader.terrarumAppConfig.screenH / zoom)
        zoom = Terrarum.ingame?.screenZoom ?: 1f
        zoomSamplePoint = (1f - 1f / zoom) / 2f // will never quite exceed 0.5
        worldWidth = world.width * TILE_SIZE
        worldHeight = world.height * TILE_SIZE

        // TOP-LEFT position of camera border

        // some hacky equation to position player at the dead centre
        // implementing the "lag behind" camera the right way
        /*val pVecSum = if (player is ActorWithBody)
            player.externalV + (player.controllerV ?: nullVec)
        else
            nullVec

        x = ((player.hitbox.centeredX - pVecSum.x).toFloat() - (width / 2)).floorToInt() // X only: ROUNDWORLD implementation


        y = (FastMath.clamp(
                (player.hitbox.centeredY - pVecSum.y).toFloat() - height / 2,
                TILE_SIZEF,
                world.height * TILE_SIZE - height - TILE_SIZEF
        )).floorToInt().clampCameraY(world)*/


//        val fpsRatio = App.UPDATE_RATE / Gdx.graphics.deltaTime // if FPS=32 & RATE=64, ratio will be 0.5
        val fpsRatio = App.TICK_SPEED.toFloat() / Gdx.graphics.framesPerSecond
        val oldX = x.toDouble()
        val oldTx = tx.toDouble()
        val oldY = y.toDouble()
        val newX1 = (player.hitbox.centeredX) - (width / 2) + if (App.getConfigBoolean("fx_streamerslayout")) App.scr.chatWidth / 2 else 0
        val newX2 = newX1 + worldWidth
        val newTx1 = newX1 / TILE_SIZE
        val newTx2 = newX2 / TILE_SIZE
        val newX = if (Math.abs(newX1 - oldX) < Math.abs(newX2 - oldX)) newX1 else newX2
        val newTx = if (Math.abs(newTx1 - oldTx) < Math.abs(newTx2 - oldTx)) newTx1 else newTx2
        val newY = player.hitbox.centeredY - (height / 2)

        val pVecMagn = (player.externalV + player.controllerV).magnitude
        val cVecMagn = Math.sqrt((newX - oldX).sqr() + (newY - oldY).sqr()) * fpsRatio

//        println("$cVecMagn\t$pVecMagn\t${cVecMagn / pVecMagn}")



        val camSpeed = (1f - (1f / (2f * fpsRatio))).coerceIn(0.5f, 1f)

        val finalXnowrap = FastMath.interpolateLinear(camSpeed, oldX.toFloat(), newX.toFloat()).roundToInt()
        val finalX = FastMath.interpolateLinear(camSpeed, oldX.toFloat(), newX.toFloat()).roundToInt() fmod worldWidth
        val finalY = FastMath.interpolateLinear(camSpeed, oldY.toFloat(), newY.toFloat()).roundToInt().clampCameraY(world)

        val finalTxnowrap = FastMath.interpolateLinear(camSpeed, oldTx.toFloat(), newTx.toFloat()).roundToInt()
        val finalTx = (finalX.toFloat() / TILE_SIZE).floorToInt()
        val finalTy = (finalY.toFloat() / TILE_SIZE).floorToInt()
//        println("finalX=$finalX, finalXnowrap=$finalXnowrap")

        deltaX = if (finalX != finalXnowrap) finalXnowrap - x else finalX - x
        deltaY = finalY - y

        deltaTx = if (finalTx != finalTxnowrap) finalTxnowrap - tx else finalTx - tx
        deltaTy = finalTy - ty

        x = finalX
        y = finalY

        tx = finalTx
        ty = finalTy

        xEnd = x + width
        yEnd = y + height
    }

    private fun Int.clampCameraY(world: GameWorld): Int {
        return if (this < 0)
            0
        else if (this > worldHeight - App.scr.height)
            worldHeight - App.scr.height
        else
            this
    }

    private fun Float.clampCameraY(world: GameWorld): Float {
        return if (this < 0f)
            0f
        else if (this > worldHeight - App.scr.height)
            (worldHeight - App.scr.height).toFloat()
        else
            this
    }
}

