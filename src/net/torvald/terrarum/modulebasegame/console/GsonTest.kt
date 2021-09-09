package net.torvald.terrarum.modulebasegame.console

/**
 * Created by minjaesong on 2016-02-10.
 */
/*internal object GsonTest : ConsoleCommand {
    override fun execute(args: Array<String>) {
        if (args.size == 2) {

            val jsonBuilder = JsonWriter.getJsonBuilder()


            val jsonString = jsonBuilder.toJson((Terrarum.ingame!! as TerrarumIngame).actorNowPlaying)

            //val avelem = Gson().toJson((Terrarum.ingame!! as Ingame).actorNowPlaying)
            //val jsonString = avelem.toString()

            val bufferedWriter: BufferedWriter
            val writer: FileWriter
            try {
                writer = FileWriter(AppLoader.defaultDir + "/Exports/" + args[1] + ".json")
                bufferedWriter = BufferedWriter(writer)

                bufferedWriter.write(jsonString)
                bufferedWriter.close()

                Echo("GsonTest: exported to " + args[1] + ".json")
            }
            catch (e: IOException) {
                Echo("GsonTest: IOException raised.")
                e.printStackTrace()
            }

        }
        else {
            printUsage()
        }
    }

    override fun printUsage() {

        Echo("Usage: gsontest filename-without-extension")
    }
}*/
