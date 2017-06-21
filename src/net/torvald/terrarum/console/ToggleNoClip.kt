package net.torvald.terrarum.console

import net.torvald.terrarum.TerrarumGDX

/**
 * Created by minjaesong on 16-01-19.
 */
internal object ToggleNoClip : ConsoleCommand {
    override fun execute(args: Array<String>) {
        val status = TerrarumGDX.ingame!!.player!!.isNoClip()

        TerrarumGDX.ingame!!.player!!.setNoClip(!status)
        Echo("Set no-clip status to " + (!status).toString())
    }

    override fun printUsage() {
        Echo("toggle no-clip status of player")
    }
}
