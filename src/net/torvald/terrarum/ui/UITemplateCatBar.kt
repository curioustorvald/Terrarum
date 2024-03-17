package net.torvald.terrarum.ui

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import net.torvald.terrarum.App
import net.torvald.terrarum.CommonResourcePool
import net.torvald.terrarum.modulebasegame.ui.UIInventoryFull
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Basically a UIItemInventoryCatBar placed on a set position for your convenience
 *
 * Created by minjaesong on 2024-01-10.
 */
class UITemplateCatBar(
    parent: UICanvas,
    showSidebuttons: Boolean,

    catIcons: TextureRegionPack = CommonResourcePool.getAsTextureRegionPack("inventory_category"),
    catArrangement: IntArray, // icon order
    catIconsMeaning: List<Array<String>>, // sortedBy: catArrangement
    catIconsLabels: List<() -> String>,

    superLabels: List<() -> String> = listOf({ "" }, { "" }, { "" }),

) : UITemplate(parent) {

    val catBar = UIItemCatBar(
        parent,
        (parent.width - UIInventoryFull.catBarWidth) / 2,
        42 - UIInventoryFull.YPOS_CORRECTION + (App.scr.height - UIInventoryFull.internalHeight) / 2,
        UIInventoryFull.internalWidth,
        UIInventoryFull.catBarWidth,
        showSidebuttons,
        catIcons, catArrangement, catIconsMeaning, catIconsLabels, superLabels
    )

    override fun getUIitems(): List<UIItem> {
        return listOf(catBar)
    }

    override fun update(delta: Float) = catBar.update(delta)
    override fun render(frameDelta: Float, batch: SpriteBatch, camera: OrthographicCamera) = catBar.render(frameDelta, batch, camera)
    override fun dispose() = catBar.dispose()
}