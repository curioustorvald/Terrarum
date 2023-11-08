package net.torvald.terrarum

import net.torvald.terrarum.App.printdbg
import net.torvald.terrarum.App.printdbgerr
import net.torvald.terrarum.savegame.ByteArray64GrowableOutputStream
import net.torvald.terrarum.savegame.DiskSkimmer.Companion.read
import java.io.*
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Created by minjaesong on 2023-10-03.
 */
object CheckUpdate {

    private val versionNumFull = App.getVERSION_STRING_WITHOUT_SNAPSHOT()
    private val versionNumOnly = String.format(
        "%d.%d.%d",
        App.VERSION_RAW ushr 48,
        App.VERSION_RAW and 0xffff000000L ushr 24,
        App.VERSION_RAW and 0xffffffL
    )

    private val checkUpdateURL = setOf(
        "https://github.com/curioustorvald/Terrarum/releases/tag/v$versionNumOnly",
        "https://github.com/curioustorvald/Terrarum/releases/tag/v$versionNumFull",
    ).toList()

    private fun wget(url: String): String? {
        printdbg(this, "wget $url")

        var ret: String? = null
        var fail: Throwable? = null
        try {
            // check the http connection before we do anything to the fs
            val client = HttpClient.newBuilder().build()
            val request = HttpRequest.newBuilder().uri(URI.create(url)).build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            ret = if (response.statusCode() >= 400) null else response.body()

            printdbg(this, "HTTP ${response.statusCode()}")
        }
        catch (e: Throwable) {
            fail = e
            printdbgerr(this, "wget $url got error: ${e.stackTraceToString()}")
        }
        finally {
        }

        return ret
    }

    fun hasUpdate(): Boolean {
        var allNull = true

        val hasLatestAny = checkUpdateURL.any { url ->
            val http = wget(url)
            if (http != null) allNull = false
            http?.contains("a href=\"/curioustorvald/Terrarum/releases/latest") ?: false
        }

        return if (allNull) false else !hasLatestAny
    }


}