package net.torvald.terrarum.modulecomputers.gameactors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.Disposable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.torvald.terrarum.*
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.modulebasegame.gameactors.BlockBox
import net.torvald.terrarum.modulebasegame.gameactors.FixtureBase
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory
import net.torvald.terrarum.ui.Toolkit
import net.torvald.terrarum.ui.UICanvas
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import net.torvald.tsvm.*
import net.torvald.tsvm.peripheral.GraphicsAdapter
import net.torvald.tsvm.peripheral.ReferenceGraphicsAdapter
import net.torvald.tsvm.peripheral.VMProgramRom

/**
 * Created by minjaesong on 2021-12-04.
 */
class FixtureHomeComputer : FixtureBase {

    private val vm = VM(0x200000, TheRealWorld(), arrayOf(
            VMProgramRom(ModMgr.getPath("dwarventech", "bios/tsvmbios.rom"))
    ))
    private val vmRunner: VMRunner
    private val coroutineJob: Job

    constructor() : super(
            BlockBox(BlockBox.NO_COLLISION, 1, 1),
            mainUI = UIHomeComputer(),
            inventory = FixtureInventory(40, FixtureInventory.CAPACITY_MODE_COUNT),
            nameFun = { "Computer" }
    ) {
        density = 1400.0
        setHitboxDimension(TerrarumAppConfiguration.TILE_SIZE, TerrarumAppConfiguration.TILE_SIZE, 0, -1)

        makeNewSprite(TextureRegionPack(CommonResourcePool.getAsTextureRegion("dwarventech-sprites-fixtures-desktop_computer.tga").texture, TerrarumAppConfiguration.TILE_SIZE, TerrarumAppConfiguration.TILE_SIZE))
        sprite!!.setRowsAndFrames(1, 1)

        actorValue[AVKey.BASEMASS] = 20.0


        val gpu = ReferenceGraphicsAdapter(ModMgr.getPath("dwarventech", "gui"), vm)
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

        vmRunner = VMRunnerFactory(ModMgr.getPath("dwarventech", "bios"), vm, "js")
        coroutineJob = GlobalScope.launch {
            vmRunner.executeCommand(vm.roms[0]!!.readAll())
        }

        App.disposables.add(Disposable {
            vmRunner.close()
            coroutineJob.cancel("fixture disposal")
            vm.dispose()
        })
    }

    override fun reload() {
        super.reload()

        (mainUI as UIHomeComputer).vm = vm
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

    private var batch: SpriteBatch
    private var camera: OrthographicCamera

    internal lateinit var vm: VM

    init {
        batch = SpriteBatch()
        camera = OrthographicCamera(width.toFloat(), height.toFloat())
        //val m = Matrix4()
        //m.setToOrtho2D(0f, 0f, width.toFloat(), height.toFloat())
        batch.projectionMatrix = camera.combined
    }

    private val fbo = FrameBuffer(Pixmap.Format.RGBA8888, width, height, false)

    override fun updateUI(delta: Float) {
    }

    override fun renderUI(otherBatch: SpriteBatch, otherCamera: Camera) {
        otherBatch.end()

        fbo.inAction(camera, batch) {
            Gdx.gl.glClearColor(0f,0f,0f,1f) // to hide the crap might be there

            (vm.peripheralTable[1].peripheral as? GraphicsAdapter)?.let { gpu ->
                val clearCol = gpu.getBackgroundColour()
                Gdx.gl.glClearColor(clearCol.r, clearCol.g, clearCol.b, clearCol.a)
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

                gpu.render(Gdx.graphics.deltaTime, batch, drawOffX, drawOffY, true, fbo) // gpu.render will internally end() the fbo then begin() again before using the batch I've fed in
            }
        }

        otherBatch.begin()
        otherBatch.shader = null
        blendNormal(otherBatch)
        otherBatch.color = Color.WHITE
        otherBatch.draw(fbo.colorBufferTexture, posX.toFloat(), posY.toFloat(), width.toFloat(), height.toFloat())
        otherBatch.color = Toolkit.Theme.COL_INACTIVE
        Toolkit.drawBoxBorder(otherBatch, posX - 1, posY - 1, width + 2, height + 2)
    }

    override fun doOpening(delta: Float) {
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