package net.torvald.terrarum.modulecomputers.gameactors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.drawBodyInGoodPosition
import net.torvald.terrarum.modulebasegame.gameactors.BlockBox
import net.torvald.terrarum.modulebasegame.gameactors.FixtureBase
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory
import net.torvald.terrarum.modulecomputers.tsvmperipheral.TsvmAudioBridge
import net.torvald.terrarum.modulecomputers.ui.UIHomeComputer
import net.torvald.terrarum.ui.Toolkit
import net.torvald.tsvm.*
import net.torvald.tsvm.peripheral.AdapterConfig
import net.torvald.tsvm.peripheral.AudioAdapter
import net.torvald.tsvm.peripheral.GraphicsAdapter
import net.torvald.tsvm.peripheral.VMProgramRom
import java.io.File

/**
 * A fully-featured in-world computer running an actual TSVM instance.
 *
 * The VM runs on its own thread, spawned when the machine is powered on and terminated when it is
 * powered off. Its screen is shown two ways: shrunk and SCREEN-blended onto the world sprite (this
 * class), and full-size with the CRT shader inside [UIHomeComputer].
 *
 * The TSVM runtime lives in a separate, swappable `TerranTSVM.jar` (declared via `extrajars` in the
 * module metadata) so it can be upgraded by simply replacing that file.
 *
 * Created by minjaesong on 2021-12-04, rewritten 2026-06-28.
 */
class FixtureHomeComputer : FixtureBase {

    // --- VM + peripherals. All @Transient: the VM holds native memory and cannot be serialised, so
    //     it is rebuilt by reload() after deserialisation and (re)started on power-on. ---
    @Transient lateinit var vm: VM; private set
    @Transient private lateinit var vmRunner: VMRunner
    @Transient private var vmThread: Thread? = null

    @Transient var gpu: GraphicsAdapter? = null; private set
    @Transient private var audioBridge: TsvmAudioBridge? = null

    @Transient private var monitorFbo: FrameBuffer? = null
    @Transient private var monitorBatch: SpriteBatch? = null
    @Transient private var monitorCamera: OrthographicCamera? = null

    @Transient @Volatile var poweredOn = false; private set

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 2, 2),
        mainUI = UIHomeComputer(),
        inventory = FixtureInventory(40, FixtureInventory.CAPACITY_MODE_COUNT),
        nameFun = { "Computer" }
    ) {
        density = 1400.0
        setHitboxDimension(TILE_SIZE * 2, TILE_SIZE * 2, 0, 0)

        makeNewSprite(FixtureBase.getSpritesheet("dwarventech", "sprites/fixtures/computer_operator_terminal.tga", TILE_SIZE * 2, TILE_SIZE * 2)).let {
            it.setRowsAndFrames(1, 1)
        }

        actorValue[AVKey.BASEMASS] = 20.0

        buildVM()
    }

    /** (Re)creates the VM object and binds it to the UI. Does NOT create GL peripherals or start the thread. */
    private fun buildVM() {
        if (::vm.isInitialized) { try { vm.dispose() } catch (_: Throwable) {} } // free the old native memory
        vm = VM(biosDir(), RAM_SIZE, TheRealWorld(), arrayOf(VMProgramRom(File(biosRom()))), CARD_SLOTS, HashMap())
        (mainUI as? UIHomeComputer)?.let {
            it.vm = vm
            it.fixture = this
        }
    }

    /**
     * Power on: create the GL-side peripherals (on the GL thread), wire the spatial audio bridge, then
     * spawn the VM execution thread that runs the BIOS. Idempotent.
     */
    fun startVM() {
        if (poweredOn) return
        poweredOn = true

        // GL resources must be created on the GL thread; runOnGLThread runs inline if we already are.
        CommonResourcePool.runOnGLThread {
            vm.init()

            val g = GraphicsAdapter(guiDir(), vm, GRAPHICSCONFIG)
            gpu = g
            vm.peripheralTable[1] = PeripheralEntry(g)

            // route the VM's audio into two world-positioned dynamic tracks (left + right)
            val bridge = TsvmAudioBridge(this)
            audioBridge = bridge
            vm.audioSinkFactory = bridge.sinkFactory
            vm.peripheralTable[2] = PeripheralEntry(AudioAdapter(vm))
            bridge.start()

            vm.getPrintStream = { g.getPrintStream() }
            vm.getErrorStream = { g.getErrorStream() }
            vm.getInputStream = { g.getInputStream() }

            monitorFbo = FrameBuffer(Pixmap.Format.RGBA8888, GRAPHICSCONFIG.width, GRAPHICSCONFIG.height, false)
            monitorBatch = FlippingSpriteBatch()
            monitorCamera = OrthographicCamera(GRAPHICSCONFIG.width.toFloat(), GRAPHICSCONFIG.height.toFloat())
        }

        vmRunner = VMRunnerFactory(biosDir(), vm, "js")
        vmThread = Thread({
            try {
                vmRunner.executeCommand(vm.roms[0]!!.readAll())
            }
            catch (e: Throwable) {
                if (e !is InterruptedException) e.printStackTrace()
            }
        }, "FixtureHomeComputer.VM!${vm.id}").also {
            it.isDaemon = true
            it.start()
        }
    }

    /** Power off: stop audio, park & join the VM thread, then dispose peripherals on the GL thread. */
    fun stopVM() {
        if (!poweredOn) return
        poweredOn = false

        audioBridge?.stop()

        // Stop execution FIRST, then dispose peripherals — disposing while the runner thread is alive
        // would let it touch freed native pointers and crash.
        try { vm.park() } catch (_: Throwable) {}
        vmThread?.interrupt()
        try { vmRunner.close() } catch (_: Throwable) {}
        try { vmThread?.join(2000L) } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
        vmThread = null

        CommonResourcePool.runOnGLThread {
            for (i in 1 until vm.peripheralTable.size) {
                try { vm.peripheralTable[i].peripheral?.dispose() } catch (_: Throwable) {}
                vm.peripheralTable[i] = PeripheralEntry()
            }
            gpu = null
            monitorFbo?.dispose(); monitorFbo = null
            monitorBatch?.dispose(); monitorBatch = null
            monitorCamera = null
        }

        vm.audioSinkFactory = null
        audioBridge = null
        // the VM object itself is kept (its native memory is reused); startVM() re-inits it via vm.init()
    }

    /** Restart the running VM (BIOS reboot) without fully powering down. */
    fun resetVM() {
        if (!poweredOn) return
        stopVM()
        startVM()
    }

    override fun onInteract(mx: Double, my: Double) {
        // first interaction powers the machine on; the UI itself is opened by the Ingame click handler
        if (!poweredOn) startVM()
    }

    override fun updateImpl(delta: Float) {
        inOperation = poweredOn
        super.updateImpl(delta)
        if (poweredOn && vm.isRunning) {
            try { vm.update(delta) } catch (_: Throwable) {}
        }
    }

    // ------------------------------------------------------------------ world rendering

    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        blendNormalStraightAlpha(batch)
        super.drawBody(frameDelta, batch)

        if (poweredOn) {
            renderMonitorToFbo(frameDelta, batch)
            drawOverlay(frameDelta, batch, 1f)
        }
    }

    override fun drawEmissive(frameDelta: Float, batch: SpriteBatch) {
        blendNormalStraightAlpha(batch)
        super.drawEmissive(frameDelta, batch)

        // reuse the framebuffer already rendered in drawBody this frame, at half intensity so the
        // SCREEN blend in the colour pass + this emissive pass don't blow out to white
        if (poweredOn) drawOverlay(frameDelta, batch, 0.5f)
    }

    /** Renders the live VM screen into [monitorFbo]. Safe to nest: FrameBufferManager restores the world FBO. */
    private fun renderMonitorToFbo(frameDelta: Float, worldBatch: SpriteBatch) {
        val g = gpu ?: return
        val fbo = monitorFbo ?: return
        val mb = monitorBatch ?: return
        val cam = monitorCamera ?: return

        worldBatch.end()
        fbo.inAction(cam, mb) {
            val bg = g.getBackgroundColour()
            Gdx.gl.glClearColor(bg.r, bg.g, bg.b, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            // gpu.render internally end()s then begin()s the fbo before drawing with the batch we feed it
            g.render(frameDelta, mb, 0f, 0f, true, null, fbo)
        }
        worldBatch.begin()
    }

    private fun drawOverlay(frameDelta: Float, batch: SpriteBatch, intensity: Float) {
        val fbo = monitorFbo ?: return
        val bg = gpu?.getBackgroundColour() ?: Color.BLACK

        val posX = hitbox.startX.toFloat()
        val posY = hitbox.startY.toFloat()

        drawBodyInGoodPosition(posX, posY) { sx, sy ->
            blendScreen(batch)

            // letterbox background: fill the whole screen area with the TSVM background colour
            batch.color = Color(bg.r * intensity, bg.g * intensity, bg.b * intensity, 1f)
            Toolkit.fillArea(batch, sx + SCREEN_X, sy + SCREEN_Y, SCREEN_W.toFloat(), SCREEN_H.toFloat())

            // the framebuffer is 5:4; fit it by height inside the 16:9 area and centre it horizontally
            // (FlippingSpriteBatch.draw(Texture) handles the FBO's vertical orientation)
            batch.color = Color(intensity, intensity, intensity, 1f)
            batch.draw(fbo.colorBufferTexture, sx + SCREEN_X + FB_X_GAP, sy + SCREEN_Y, FB_W, SCREEN_H.toFloat())

            // status LEDs
            if (poweredOn) {
                batch.color = Color(0f, intensity, 0f, 1f) // green power LED
                Toolkit.fillArea(batch, sx + PWR_LED_X, sy + LED_Y, LED_SZ.toFloat(), LED_SZ.toFloat())
            }
            if (isIdle()) {
                batch.color = Color(intensity, 0.5f * intensity, 0f, 1f) // amber idle LED
                Toolkit.fillArea(batch, sx + IDLE_LED_X, sy + LED_Y, LED_SZ.toFloat(), LED_SZ.toFloat())
            }

            blendNormalStraightAlpha(batch)
            batch.color = Color.WHITE
        }
    }

    /** The amber idle lamp is lit while the machine is powered but the operator console is not focused. */
    private fun isIdle(): Boolean = poweredOn && (mainUI?.isOpened != true)

    override fun reload() {
        super.reload()
        stopVM()
        buildVM()
        // a deserialised machine comes up powered-off; the player powers it back on by interacting
    }

    override fun despawn() {
        stopVM()
        freeVM()
        super.despawn()
    }

    override fun dispose() {
        stopVM()
        freeVM()
        super.dispose()
    }

    private fun freeVM() {
        if (::vm.isInitialized) { try { vm.dispose() } catch (_: Throwable) {} }
    }

    private fun biosDir() = ModMgr.getGdxFile("dwarventech", "bios").path()
    private fun biosRom() = ModMgr.getGdxFile("dwarventech", "bios/tsvmbios.js").path()
    private fun guiDir() = ModMgr.getGdxFile("dwarventech", "gui").path()

    companion object {
        private const val RAM_SIZE = 8388608L // 8 MiB
        private const val CARD_SLOTS = 8

        val GRAPHICSCONFIG = AdapterConfig(
            "crt_color",
            // empty chrRomPath => use the canonical 112x224 font ROM bundled inside TerrarumTSVM.jar
            // (the mod's gui/FontROM7x14.tga is a different, oversized font that doesn't fit this config)
            560, 448, 80, 32, 253, 255, 256L shl 10, "", 0.0f, GraphicsAdapter.TEXT_TILING_SHADER_COLOUR
        )

        // sprite-local pixel geometry (sprite is 32x32 = 2x2 tiles)
        private const val SCREEN_X = 8f
        private const val SCREEN_Y = 3f
        private const val SCREEN_W = 16
        private const val SCREEN_H = 9
        // 5:4 framebuffer fitted by height inside the 16x9 window, then horizontally centred:
        //   FB_W = SCREEN_H * 5/4 = 11.25 ; FB_X_GAP = (SCREEN_W - FB_W)/2 = 2.375
        private const val FB_W = 11.25f
        private const val FB_X_GAP = 2.375f

        private const val LED_SZ = 2
        private const val LED_Y = 10f
        private const val PWR_LED_X = 27f
        private const val IDLE_LED_X = 3f
    }
}
