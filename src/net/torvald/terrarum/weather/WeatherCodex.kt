package net.torvald.terrarum.weather

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarum.App
import net.torvald.terrarum.GdxColorMap
import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.tryDispose
import net.torvald.terrarum.utils.JsonFetcher
import net.torvald.terrarum.utils.forEachSiblings
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import java.io.File

/**
 * Created by minjaesong on 2024-04-13.
 */
class WeatherCodex : Disposable {

    internal val weatherById = HashMap<String, BaseModularWeather>()
    internal val weatherByTags = HashMap<String, MutableSet<BaseModularWeather>>()

    fun getById(id: String) = weatherById[id]

    fun getByAllTags(tags: Array<String>) = weatherById.values.filter { it.hasAllTags(tags) }.ifEmpty { null }
    fun getByAllTags(tags: Collection<String>) = weatherById.values.filter { it.hasAllTags(tags) }.ifEmpty { null }
    fun getByAllTagsOf(tag: String, vararg tags: String) = weatherById.values.filter { it.hasAllTags(tags.toList() + tag) }.ifEmpty { null }

    fun getByAnyTag(tags: Array<String>) = weatherById.values.filter { it.hasAnyTags(tags) }.ifEmpty { null }
    fun getByAnyTag(tags: Collection<String>) = weatherById.values.filter { it.hasAnyTags(tags) }.ifEmpty { null }
    fun getByAnyTag(tag: String, vararg tags: String) = weatherById.values.filter { it.hasAnyTags(tags.toList() + tag) }.ifEmpty { null }

    fun getByTag(tag: String) = weatherByTags[tag]


    init {
        App.disposables.add(this)
    }


    fun readFromJson(modname: String, file: File) = readFromJson(modname, file.path)

    fun readFromJson(modname: String, path: String) {
        /* JSON structure:
{
  "skyboxGradColourMap": "colourmap/sky_colour.tga", // string (path to image) for dynamic. Image must be RGBA8888 or RGB888
  "extraImages": [
      // if any, it will be like:
      sun01.tga,
      clouds01.tga,
      clouds02.tga,
      auroraBlueViolet.tga
  ]
}
         */
        val pathToImage = "weathers"

        val JSON = JsonFetcher(path)

        val skyboxInJson = JSON.getString("skyboxGradColourMap")
        val lightbox = JSON.getString("daylightClut")

        val cloudsMap = ArrayList<CloudProps>()
        val clouds = JSON["clouds"]
        clouds.forEachSiblings { name, json ->
            cloudsMap.add(CloudProps(
                name,
                TextureRegionPack(ModMgr.getGdxFile(modname, "$pathToImage/${json.getString("filename")}"), json.getInt("tw"), json.getInt("th")),
                json.getFloat("probability"),
                json.getFloat("baseScale"),
                json.getFloat("scaleVariance"),
                json.getFloat("altLow"),
                json.getFloat("altHigh"),
            ))
        }
        cloudsMap.sortBy { it.probability }


        val ident = JSON.getString("identifier")
        val tags = JSON.getString("tags").split(',')

        val obj = BaseModularWeather(
            identifier = ident,
            json = JSON,
            skyboxGradColourMap = GdxColorMap(ModMgr.getGdxFile(modname, "$pathToImage/${skyboxInJson}")),
            daylightClut = GdxColorMap(ModMgr.getGdxFile(modname, "$pathToImage/${lightbox}")),
            tags = tags,
            cloudChance = JSON.getFloat("cloudChance"),
            windSpeed = JSON.getFloat("windSpeed"),
            windSpeedVariance = JSON.getFloat("windSpeedVariance"),
            windSpeedDamping = JSON.getFloat("windSpeedDamping"),
            cloudGamma = JSON["cloudGamma"].asFloatArray().let { Vector2(it[0], it[1]) },
            cloudGammaVariance = JSON["cloudGammaVariance"].asFloatArray().let { Vector2(it[0], it[1]) },
            clouds = cloudsMap,
            shaderVibrancy = JSON["shaderVibrancy"].asFloatArray()
        )

        weatherById[ident] = obj
        tags.forEach {
            if (weatherByTags[it] == null) {
                weatherByTags[it] = mutableSetOf()
            }

            weatherByTags[it]!!.add(obj)
        }
    }

    override fun dispose() {
        weatherById.values.forEach {
            it.clouds.forEach { it.spriteSheet.tryDispose() }
        }
    }

    fun getRandom(tag: String? = null): BaseModularWeather {
        return if (tag == null) {
            var k = weatherById.values.random()
            if (k.identifier == "titlescreen") k = weatherById.values.random()
            k
        }
        else getByTag(tag)!!.random()
    }
}