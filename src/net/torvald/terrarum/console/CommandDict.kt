package net.torvald.terrarum.console

import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.App.printdbgerr
import net.torvald.terrarum.ModMgr
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Created by minjaesong on 2016-01-15.
 */
object CommandDict {

    internal val dict = hashMapOf<String, ConsoleCommand>()

    private val engineCommandList = listOf(
            "ActorsList",
            "Authenticator",
            "AVTracker",
            "Batch",
            "Echo",
            "EchoConsole",
            "EchoError",
            "Pause",
            "QuitApp",
            "ResizeScreen",
            "ScreencapNogui",
            "SetGlobalLightOverride",
            "SetLocale",
            "TakeScreenshot",
            "Unpause",
            "Version"
    )

    init {
        printdbg(this, ModMgr.loadOrder.reversed())
        printdbg(this, ModMgr.loadOrder.reversed().map { ModMgr.moduleInfo[it]?.packageName })

        ((listOf("$" to "net.torvald.terrarum")) + ModMgr.loadOrder.reversed().map { it to ModMgr.moduleInfo[it]?.packageName }).forEach { (modName, packageRoot) ->
            if (modName == "$" || modName != "$" && ModMgr.hasFile(modName, "commands.csv")) {
                val commandsList = if (modName == "$") engineCommandList else ModMgr.getFile(modName, "commands.csv").readLines()
                val packageConsole = "$packageRoot.console"

                printdbg(this, "Loading console commands from '${packageConsole}'")
//            printdbg(this, commandsList.joinToString())

                commandsList.forEach { commandName ->
                    val canonicalName = "$packageConsole.$commandName"
                    val it = Class.forName(canonicalName)

                    printdbg(this, "> Trying to instantiate ${it.canonicalName}")

                    try {
                        val instance = it.kotlin.objectInstance ?: it.kotlin.java.newInstance()

                        val aliases = instance.javaClass.getAnnotation(ConsoleAlias::class.java)?.aliasesCSV?.split(',')?.map { it.trim() }
                        val noexport = instance.javaClass.getAnnotation(ConsoleNoExport::class.java)

                        if (noexport == null) {

                            dict[instance.javaClass.simpleName.lowercase()] = instance as ConsoleCommand
                            aliases?.forEach {
                                dict[it] = instance as ConsoleCommand
                            }

                            printdbg(this, "Class instantiated: ${instance.javaClass.simpleName}")
                            if (aliases != null)
                                printdbg(this, "  Annotations: $aliases")
                        }
                    }
                    catch (e: ClassCastException) {
                        printdbgerr(this, "${it.canonicalName} is not a ConsoleCommand")
                    }
                    catch (e: InstantiationException) {
                        printdbgerr(this, "Could not instantiate ${it.canonicalName}")
                        e.printStackTrace(System.err)
                    }
                }
            }
        }


    }

    operator fun get(commandName: String): ConsoleCommand {
        return dict[commandName.lowercase()]!!
    }
}
