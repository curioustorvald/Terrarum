package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.toInt
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2024-03-16.
 */
class FixtureLogicSignalSevenSeg : Electric {

    @Transient override val spawnNeedsFloor = true
    @Transient override val spawnNeedsWall = true

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 4, 4),
        nameFun = { Lang["ITEM_LOGIC_SIGNAL_DISPLAY"] }
    )

    override fun getBlockBoxPositions(posX: Int, posY: Int): List<Pair<Int, Int>> {
        return listOf(
                                (posX+1 to posY+0), (posX+2 to posY+0),
                                (posX+1 to posY+1), (posX+2 to posY+1),
                                (posX+1 to posY+2), (posX+2 to posY+2),
            (posX+0 to posY+3), (posX+1 to posY+3), (posX+2 to posY+3), (posX+3 to posY+3)
        )
    }

    init {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/sevenseg.tga")
        val itemImage2 = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/sevenseg.tga")

        density = 1400.0
        setHitboxDimension(4*TILE_SIZE, 4*TILE_SIZE, 0, 1)

        makeNewSprite(TextureRegionPack(itemImage.texture, 4*TILE_SIZE, 4*TILE_SIZE)).let {
            it.setRowsAndFrames(2,16)
            it.delays = FloatArray(16) { Float.POSITIVE_INFINITY }
        }
        makeNewSpriteEmissive(TextureRegionPack(itemImage2.texture, 4*TILE_SIZE, 4*TILE_SIZE)).let {
            it.setRowsAndFrames(2,416)
            it.delays = FloatArray(16) { Float.POSITIVE_INFINITY }
        }

        (sprite as SheetSpriteAnimation).currentRow = 0
        (spriteEmissive as SheetSpriteAnimation).currentRow = 1

        setWireSinkAt(0, 3, "digital_bit")
        setWireSinkAt(1, 3, "digital_bit")
        setWireSinkAt(2, 3, "digital_bit")
        setWireSinkAt(3, 3, "digital_bit")
    }

    override fun reload() {
        super.reload()
        setWireSinkAt(0, 3, "digital_bit")
        setWireSinkAt(1, 3, "digital_bit")
        setWireSinkAt(2, 3, "digital_bit")
        setWireSinkAt(3, 3, "digital_bit")
        updateK()
    }

    override fun updateSignal() {
        updateK()
    }

    private fun updateK() {
        val state = isSignalHigh(0, 3).toInt(0) or
                isSignalHigh(1, 3).toInt(1) or
                isSignalHigh(2, 3).toInt(2) or
                isSignalHigh(3, 3).toInt(3)

        (sprite as SheetSpriteAnimation).currentFrame = state
        (spriteEmissive as SheetSpriteAnimation).currentFrame = state
    }
}