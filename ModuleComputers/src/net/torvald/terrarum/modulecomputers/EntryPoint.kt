package net.torvald.terrarum.modulecomputers

import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.ModuleEntryPoint

/**
 * Created by minjaesong on 2021-12-03.
 */
class EntryPoint : ModuleEntryPoint() {

    private val moduleName = "dwarventech"

    override fun invoke() {
        ModMgr.GameItemLoader.invoke(moduleName)
        ModMgr.GameWatchdogLoader.register(moduleName, NetFrameWatchdog())
        println("[${moduleName[0].toUpperCase()}${moduleName.substring(1)}] Dirtboard(tm) go drrrrr")
    }

    override fun dispose() {
    }

}