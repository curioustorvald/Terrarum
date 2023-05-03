package net.torvald.terrarum.modulecomputers.gameactors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.Disposable
import kotlin.coroutines.*
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.BlockBox
import net.torvald.terrarum.modulebasegame.gameactors.FixtureBase
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.tsvm.*
import net.torvald.tsvm.peripheral.AdapterConfig
import net.torvald.tsvm.peripheral.GraphicsAdapter
import net.torvald.tsvm.peripheral.VMProgramRom
import net.torvald.unicode.*

/**
 * Created by minjaesong on 2021-12-04.
 */
class FixtureHomeComputer : FixtureBase {

    // TODO: write serialiser for TSVM && allow mods to have their own serialiser
    private val vm = VM(ModMgr.getGdxFile("dwarventech", "bios").path(), 0x200000, TheRealWorld(), arrayOf(
            VMProgramRom(ModMgr.getGdxFile("dwarventech", "bios/tsvmbios.js").path())
    ))
    @Transient private lateinit var vmRunner: VMRunner
//    @Transient private lateinit var coroutineJob: Job

    @Transient private var vmStarted = false
    @Transient private lateinit var disposableObj: Disposable

    constructor() : super(
            BlockBox(BlockBox.NO_COLLISION, 1, 1),
            mainUI = UIHomeComputer(),
            inventory = FixtureInventory(40, FixtureInventory.CAPACITY_MODE_COUNT),
            nameFun = { "Computer" }
    ) {
        density = 1400.0
        setHitboxDimension(TerrarumAppConfiguration.TILE_SIZE, TerrarumAppConfiguration.TILE_SIZE, 0, -1)

        makeNewSprite(FixtureBase.getSpritesheet("dwarventech", "sprites/fixtures/desktop_computer.tga", TILE_SIZE, TILE_SIZE)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = 20.0


        val gpu = GraphicsAdapter(ModMgr.getGdxFile("dwarventech", "gui").path(), vm, GRAPHICSCONFIG)
//        vm.getIO().blockTransferPorts[0].attachDevice(TestDiskDrive(vm, 0, ...))

        vm.peripheralTable[1] = PeripheralEntry(
                gpu,
                GraphicsAdapter.VRAM_SIZE,
                16,
                0
        )

        vm.getPrintStream = { gpu.getPrintStream() }
        vm.getErrorStream = { gpu.getErrorStream() }
        vm.getInputStream = { gpu.getInputStream() }

        (mainUI as UIHomeComputer).vm = vm
        (mainUI as UIHomeComputer).fixture = this
        vmRunner = VMRunnerFactory(ModMgr.getGdxFile("dwarventech", "bios").path(), vm, "js")
    }

    fun startVM() {
        /*if (!vmStarted) {
            vmStarted = true

            coroutineJob = GlobalScope.launch {
                vmRunner.executeCommand(vm.roms[0]!!.readAll())
            }

            disposableObj = Disposable {
                vmRunner.close()
                coroutineJob.cancel("fixture disposal")
                vm.dispose()
            }
            INGAME.disposables.add(disposableObj)
        }*/
    }

    fun stopVM() {
        /*if (vmStarted) {
            vmStarted = false

            vmRunner.close()
            coroutineJob.cancel("fixture disposal")
            vm.dispose()

            INGAME.disposables.remove(disposableObj)
        }*/
    }

    override fun reload() {
        super.reload()

        val gpu = GraphicsAdapter(ModMgr.getGdxFile("dwarventech", "gui").path(), vm, GRAPHICSCONFIG)
//        vm.getIO().blockTransferPorts[0].attachDevice(TestDiskDrive(vm, 0, ...))

        vm.peripheralTable[1] = PeripheralEntry(
                gpu,
                GraphicsAdapter.VRAM_SIZE,
                16,
                0
        )

        vm.getPrintStream = { gpu.getPrintStream() }
        vm.getErrorStream = { gpu.getErrorStream() }
        vm.getInputStream = { gpu.getInputStream() }

        (mainUI as UIHomeComputer).vm = vm
        (mainUI as UIHomeComputer).fixture = this
        vmRunner = VMRunnerFactory(ModMgr.getGdxFile("dwarventech", "bios").path(), vm, "js")
    }

    companion object {
        val GRAPHICSCONFIG = AdapterConfig(
                "crt_color",
                560, 448, 80, 32, 253, 255, 256 shl 10, "FontROM7x14.tga", 0.0f, GraphicsAdapter.TEXT_TILING_SHADER_COLOUR
        )
    }
}

internal class UIHomeComputer : UICanvas(
        toggleKeyLiteral = Input.Keys.ESCAPE, // FIXME why do I have specify ESC for it to function? ESC should be work as the default key
        toggleButtonLiteral = App.getConfigInt("control_gamepad_start"),
) {
    override var width = 640
    override var height = 480
    override var openCloseTime = 0f

    private val drawOffX = (width - 560).div(2).toFloat()
    private val drawOffY = (height - 448).div(2).toFloat()

    private var batch: FlippingSpriteBatch
    private var camera: OrthographicCamera

    internal lateinit var vm: VM
    internal lateinit var fixture: FixtureHomeComputer

    init {
        batch = FlippingSpriteBatch()
        camera = OrthographicCamera(width.toFloat(), height.toFloat())
        //val m = Matrix4()
        //m.setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat())
        batch.projectionMatrix = camera.combined
    }

    private val fbo = FrameBuffer(Pixmap.Format.RGBA8888, width, height, false)

    private val controlHelp =
            "${getKeycapPC(App.getConfigInt("control_key_inventory"))} ${Lang["GAME_ACTION_CLOSE"]}\u3000 " +
            "$KEYCAP_CTRL$KEYCAP_SHIFT$KEYCAP_T$KEYCAP_R Terminate\u3000" +
            "$KEYCAP_CTRL$KEYCAP_SHIFT$KEYCAP_R$KEYCAP_S Reset\u3000" +
            "$KEYCAP_CTRL$KEYCAP_SHIFT$KEYCAP_R$KEYCAP_Q SysRq"

    override fun updateUI(delta: Float) {
    }

    override fun renderUI(otherBatch: SpriteBatch, otherCamera: Camera) {
        otherBatch.end()

        fbo.inAction(camera, batch) {
            Gdx.gl.glClearColor(0f,0f,0f,1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT) // to hide the crap might be there

            (vm.peripheralTable[1].peripheral as? GraphicsAdapter)?.let { gpu ->
                val clearCol = gpu.getBackgroundColour()
                Gdx.gl.glClearColor(clearCol.r, clearCol.g, clearCol.b, clearCol.a)
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

                gpu.render(Gdx.graphics.deltaTime, batch, drawOffX, drawOffY, true, fbo) // gpu.render will internally end() the fbo then begin() again before using the batch I've fed in
            }
        }

        otherBatch.begin()
        otherBatch.shader = null
        blendNormalStraightAlpha(otherBatch)
        otherBatch.color = Color.WHITE
        otherBatch.draw(fbo.colorBufferTexture, posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())
        otherBatch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(otherBatch, posX - 1, posY - 1, width + 2, height + 2)

        App.fontGame.draw(otherBatch, controlHelp, posX, posY + height + 4)
    }

    override fun doOpening(delta: Float) {
        fixture.startVM()
    }

    override fun doClosing(delta: Float) {
    }

    override fun endOpening(delta: Float) {
    }

    override fun endClosing(delta: Float) {
    }

    override fun dispose() {
        fbo.dispose()
    }

}