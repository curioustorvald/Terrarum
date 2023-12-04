
package net.torvald.terrarum.modulebasegame.gameactors

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import net.torvald.terrarum.*
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZEF
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by minjaesong on 2017-01-07.
 */
internal class FixtureTapestry : FixtureBase {

    @Transient override val spawnNeedsWall = true

    var artName = ""; private set
    var artAuthor = ""; private set

//    val tw = 1
//    val th = 1

    private var rawBytes = ByteArray(256)
    private var frameBlock: ItemID = Block.PLANK_NORMAL

    private var tilewiseHitboxWidth by Delegates.notNull<Int>()
    private var tilewiseHitboxHeight by Delegates.notNull<Int>()

    private constructor() : super(
            BlockBox(BlockBox.NO_COLLISION, 1, 1),
            renderOrder = RenderOrder.BEHIND,
            nameFun = { Lang["ITEM_TAPESTRY"] }
    )

    constructor(rawBytes: ByteArray, framingMaterial: ItemID) : super(
            BlockBox(BlockBox.NO_COLLISION, 1, 1),
            renderOrder = RenderOrder.BEHIND,
            nameFun = { Lang["ITEM_TAPESTRY"] }
    ) {
        this.rawBytes = rawBytes
        this.frameBlock = framingMaterial
        reload()
    }

    override fun spawn(posX: Int, posY: Int, installersUUID: UUID?): Boolean = spawn(posX, posY, installersUUID, tilewiseHitboxWidth, tilewiseHitboxHeight)

    override fun reload() {
        super.reload()

        val (pixmap, name, author) = DecodeTapestry(rawBytes)
        artName = name
        artAuthor = author
        nameFun = { "$ccW$artAuthor, $ccC$artName" }

        if (pixmap.width % TILE_SIZE != 0 || pixmap.height % TILE_SIZE != 0 || pixmap.width * pixmap.height == 0) {
            throw UnsupportedOperationException("Tapestry size not multiple of tile size: (${pixmap.width}x${pixmap.height})")
        }

        // draw canvas and frame texture over the pixmap
        val tileFilename = "${frameBlock.replace(':','-')}"
        val frame = CommonResourcePool.getOrPut("tapestries-common-frame_$tileFilename.tga") {
            Pixmap(ModMgr.getGdxFilesFromEveryMod("tapestries/common/frame_$tileFilename.tga").last().second)
        } as Pixmap
        val canvas = CommonResourcePool.getOrPut("tapestries-common-canvas.tga") {
            Pixmap(ModMgr.getGdxFilesFromEveryMod("tapestries/common/canvas.tga").last().second)
        } as Pixmap

        tilewiseHitboxWidth = pixmap.width.div(TILE_SIZEF).ceilToInt()
        tilewiseHitboxHeight = pixmap.height.div(TILE_SIZEF).ceilToInt()

        // blend canvas texture
        for (y in 0 until pixmap.height) { for (x in 0 until pixmap.width) {
            val srcCol = canvas.getPixel(x % canvas.width, y % canvas.height)
            val dstCol = pixmap.getPixel(x, y)
            pixmap.drawPixel(x, y, dstCol rgbamul srcCol)
        } }

        // draw frame
        for (ty in 0 until tilewiseHitboxHeight) { for (tx in 0 until tilewiseHitboxWidth) {
            val srcx = TILE_SIZE * (if (tilewiseHitboxWidth == 1) 0 else if (tx == 0) 1 else if (tx == tilewiseHitboxWidth - 1) 3 else 2)
            val srcy = TILE_SIZE * (if (tilewiseHitboxHeight == 1) 0 else if (ty == 0) 1 else if (ty == tilewiseHitboxHeight - 1) 3 else 2)
            val dstx = tx * TILE_SIZE
            val dsty = ty * TILE_SIZE
            pixmap.drawPixmap(frame, srcx, srcy, TILE_SIZE, TILE_SIZE, dstx, dsty, TILE_SIZE, TILE_SIZE)
        } }


        val texture = Texture(pixmap)
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        val texturePack = TextureRegionPack(texture, pixmap.width, pixmap.height)

        makeNewSprite(texturePack)
        setHitboxDimension(pixmap.width, pixmap.height, 0, 0)
        setPosition(Terrarum.mouseX, Terrarum.mouseY)

        // redefine blockbox
        this.blockBox = BlockBox(BlockBox.NO_COLLISION, tilewiseHitboxWidth, tilewiseHitboxHeight)
        this.renderOrder = RenderOrder.BEHIND


        pixmap.dispose()
        INGAME.disposables.add(texture)
    }


    override var tooltipText: String? = "TEST\nSTRING"//if (artName.length + artAuthor.length > 0) "$artName\n$artAuthor" else null
}
