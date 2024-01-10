package net.torvald.terrarum.modulecomputers.gameactors

import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.modulebasegame.gameactors.BlockBox
import net.torvald.terrarum.modulebasegame.gameactors.FixtureBase
import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory
import net.torvald.terrarum.modulecomputers.ui.UIHomeComputer
import net.torvald.tsvm.*
import net.torvald.tsvm.peripheral.AdapterConfig
import net.torvald.tsvm.peripheral.GraphicsAdapter
import net.torvald.tsvm.peripheral.VMProgramRom

/**
 * Created by minjaesong on 2021-12-04.
 */
class FixtureHomeComputer : FixtureBase {

    @Transient override val spawnNeedsFloor = true

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
        setHitboxDimension(TerrarumAppConfiguration.TILE_SIZE, TerrarumAppConfiguration.TILE_SIZE, 0, 0)

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

