package net.torvald.terrarum.modulecomputers.gameactors

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZEF
import net.torvald.terrarum.gameactors.drawBodyInGoodPosition
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameactors.BlockBox
import net.torvald.terrarum.modulebasegame.gameactors.Electric
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2025-03-30.
 */
class FixtureComputerConsole : Electric {

    @Transient override val spawnNeedsFloor = true
    @Transient override val spawnNeedsWall = false

    constructor() : super(
        BlockBox(BlockBox.ALLOW_MOVE_DOWN, 2, 2),
        nameFun = { Lang["ITEM_COMPUTER_CONSOLE"] }
    )

    @Transient lateinit var itemImageSheet: TextureRegionPack

    init {
        itemImageSheet = CommonResourcePool.getOrPut("spritesheet:dwarventech/sprites/fixtures/computers.tga") {
            TextureRegionPack(ModMgr.getGdxFile("dwarventech", "sprites/fixtures/computers.tga"), TILE_SIZE, TILE_SIZE)
        } as TextureRegionPack


    }

    override fun drawBody(frameDelta: Float, batch: SpriteBatch) {
        super.drawBody(frameDelta, batch)

        val sx = hitbox.startX.toFloat()
        val sy = hitbox.startY.toFloat()

        drawBodyInGoodPosition(sx, sy) { x, y ->
            batch.draw(itemImageSheet.get(0, 1), x, y)
            batch.draw(itemImageSheet.get(1, 1), x + TILE_SIZEF, y)
            batch.draw(itemImageSheet.get(0, 2), x, y + TILE_SIZEF)
            batch.draw(itemImageSheet.get(1, 2), x + TILE_SIZEF, y + TILE_SIZEF)
        }
    }
}