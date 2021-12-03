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
        println("[${moduleName[0].uppercase()}${moduleName.substring(1)}] Dirtboard(tm) go drrrrr")
    }

    override fun dispose() {
    }

}