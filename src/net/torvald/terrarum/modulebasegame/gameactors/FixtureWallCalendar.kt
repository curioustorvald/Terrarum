package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.INGAME
import net.torvald.terrarum.TerrarumAppConfiguration.TILE_SIZE
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.modulebasegame.ui.UIWallCalendar
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 2023-08-08.
 */
class FixtureWallCalendar : FixtureBase {

    constructor() : super(
            BlockBox(BlockBox.NO_COLLISION, 1, 1),
            nameFun = { Lang["ITEM_CALENDAR"] },
            mainUI = UIWallCalendar()
    ) {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/calendar.tga")

        density = 600.0
        setHitboxDimension(TILE_SIZE, TILE_SIZE, 0, 0)

        makeNewSprite(TextureRegionPack(itemImage.texture, TILE_SIZE, TILE_SIZE)).let {
            it.setRowsAndFrames(1,1)
        }

        actorValue[AVKey.BASEMASS] = 1.0
    }

    override var tooltipText: String?
        get() = Lang.getAndUseTemplate("CONTEXT_CALENDAR_DATE_FORMAT_YMD_DDD", false,
            world!!.worldTime.years,
            world!!.worldTime.getMonthNameFull(),
            world!!.worldTime.calendarDay,
            world!!.worldTime.getDayNameFull()
        )//INGAME.world.worldTime.getFormattedCalendarDay()
        set(value) {}
}