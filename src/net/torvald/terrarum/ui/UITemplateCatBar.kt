package net.torvald.terrarum.ui

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

    catIcons: TextureRegionPack = CommonResourcePool.getAsTextureRegionPack("inventory_category"),
    catArrangement: IntArray, // icon order
    catIconsMeaning: List<Array<String>>, // sortedBy: catArrangement
    catIconsLabels: List<() -> String>,

) : UITemplate(parent) {

    val catBar = UIItemCatBar(
        parent,
        (parent.width - UIInventoryFull.catBarWidth) / 2,
        42 - UIInventoryFull.YPOS_CORRECTION + (App.scr.height - UIInventoryFull.internalHeight) / 2,
        UIInventoryFull.internalWidth,
        UIInventoryFull.catBarWidth,
        true,
        catIcons, catArrangement, catIconsMeaning, catIconsLabels
    )

    override fun getUIitems(): List<UIItem> {
        return listOf(catBar)
    }
}