package net.torvald.terrarum.musicplayer

import net.torvald.terrarum.IngameInstance
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.ModuleEntryPoint
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.musicplayer.gui.MusicPlayer

/**
 * Created by minjaesong on 2023-12-23.
 */
class EntryPoint : ModuleEntryPoint() {
    override fun invoke() {
        ModMgr.GameExtraGuiLoader.register { ingame: TerrarumIngame ->  MusicPlayer(ingame) }
    }

    override fun dispose() {
    }
}