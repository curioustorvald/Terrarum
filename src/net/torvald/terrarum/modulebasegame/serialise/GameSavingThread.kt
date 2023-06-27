package net.torvald.terrarum.modulebasegame.serialise


abstract class SavingThread(private val errorHandler: (Throwable) -> Unit) : Runnable {
    abstract fun save()

    override fun run() {
        try {
            save()
        }
        catch (e: Throwable) {
            e.printStackTrace()
            errorHandler(e)
        }
    }
}

const val SCREENCAP_WAIT_TRY_MAX = 256
