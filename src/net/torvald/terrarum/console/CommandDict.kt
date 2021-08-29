package net.torvald.terrarum.console

import net.torvald.terrarum.AppLoader.printdbg
import net.torvald.terrarum.AppLoader.printdbgerr
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.ModMgr.loadOrder
import net.torvald.terrarum.modulebasegame.console.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import kotlin.streams.toList

/**
 * Created by minjaesong on 2016-01-15.
 */
object CommandDict {

    internal val dict = hashMapOf<String, ConsoleCommand>()

    init {
        printdbg(this, ModMgr.loadOrder.reversed())
        printdbg(this, ModMgr.loadOrder.reversed().map { ModMgr.moduleInfo[it]?.packageName })

        (listOf("net.torvald.terrarum") + ModMgr.loadOrder.reversed().map { ModMgr.moduleInfo[it]?.packageName }.filter { it != null }).forEach{ packageRoot ->
            printdbg(this, packageRoot)
            val packageConsole = "$packageRoot.console"
            val stream = ClassLoader.getSystemClassLoader().getResourceAsStream(packageConsole.replace('.','/'))
            val reader = BufferedReader(InputStreamReader(stream))

            reader.lines()
                    .filter{ it.endsWith(".class") && !it.contains('$') }
                    .map { Class.forName("$packageConsole.${it.substring(0, it.lastIndexOf('.'))}") }
                    .forEach {

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

    operator fun get(commandName: String): ConsoleCommand {
        return dict[commandName.lowercase()]!!
    }
}
