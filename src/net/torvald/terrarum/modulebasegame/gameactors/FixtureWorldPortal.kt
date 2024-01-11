package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.*
import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.FancyWorldgenLoadScreen
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.modulebasegame.serialise.LoadSavegame
import net.torvald.terrarum.modulebasegame.ui.UIWorldPortal
import net.torvald.terrarum.savegame.DiskSkimmer
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2023-05-28.
 */
class FixtureWorldPortal : Electric {

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 5, 2),
        nameFun = { Lang["ITEM_WORLD_PORTAL"] },
        mainUI = UIWorldPortal(),
//        inventory = FixtureInventory(200, CAPACITY_MODE_WEIGHT)
    ) {
        // TODO do something with (mainUI as UIWorldPortal).***
//        (mainUI as UIWorldPortal).let { ui ->
//            ui.transitionalCargo.chestInventory = this.inventory!!
//            ui.transitionalCargo.chestNameFun = this.nameFun
//        }

        (mainUI as UIWorldPortal).host = this
    }


    init {
        val itemImage = FixtureItemBase.getItemImageFromSheet("basegame", "sprites/fixtures/portal_device.tga", 80, 32)

        density = 2900.0
        setHitboxDimension(80, 32, 0, 0)
        makeNewSprite(TextureRegionPack(itemImage.texture, 80, 32)).let {
            it.setRowsAndFrames(1,3)
        }

        actorValue[AVKey.BASEMASS] = FixtureLogicSignalEmitter.MASS

        setWireSinkAt(2, 1, "digital_bit")
    }

    @Transient internal var teleportRequest: TeleportRequest? = null

    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        (sprite as SheetSpriteAnimation).currentFrame = (Math.random() * 3).toInt()
        super.drawBody(frameDelta, batch)
    }

    override fun onRisingEdge(readFrom: BlockBoxIndex) {
        printdbg(this, "teleport! $teleportRequest")
        teleportRequest?.let {
            if (it.worldDiskToLoad != null && it.worldLoadParam != null) {
                throw InternalError("Contradiction -- worldDiskToLoad and worldLoadParam are both not null: $teleportRequest")
            }

            val player = INGAME.actorGamer

            // load existing
            val jobAfterSave: () -> Unit
            if (it.worldDiskToLoad != null) {
                jobAfterSave = {
                    LoadSavegame(
                        App.savegamePlayers[player.uuid]!!.files[0],
                        it.worldDiskToLoad
                    )
                }
            }
            // create new
            else {
                jobAfterSave = {
                    val wx = it.worldLoadParam!!.width
                    val wy = it.worldLoadParam!!.height
                    val seed = it.worldLoadParam!!.worldGenSeed
                    val name = it.worldLoadParam!!.savegameName
                    printdbg(this, "generate for teleportation! Size=${wx}x${wy}, Name=$name, Seed=$seed")

                    val ingame = TerrarumIngame(App.batch)
                    val worldParam = TerrarumIngame.NewGameParams(player, it.worldLoadParam)
                    ingame.gameLoadInfoPayload = worldParam
                    ingame.gameLoadMode = TerrarumIngame.GameLoadMode.CREATE_NEW

                    Terrarum.setCurrentIngameInstance(ingame)
                    val loadScreen = FancyWorldgenLoadScreen(ingame, wx, wy)
                    App.setLoadScreen(loadScreen)
                }
            }

            INGAME.requestForceSave(jobAfterSave)


            teleportRequest = null
        }
    }

    override fun onSpawn(tx: Int, ty: Int) {
        INGAME.world.portalPoint = Point2i(tx, ty)
    }

    override fun reload() {
        super.reload()

        // TODO do something with (mainUI as UIWorldPortal).***
    }

    internal data class TeleportRequest(
        val worldDiskToLoad: DiskSkimmer?, // for loading existing worlds
        val worldLoadParam: TerrarumIngame.NewWorldParameters? // for creating new world
    ) {
        override fun toString(): String {
            return "TeleportRequest(disk: ${worldDiskToLoad?.diskFile?.name}, param: $worldLoadParam)"
        }
    }
}