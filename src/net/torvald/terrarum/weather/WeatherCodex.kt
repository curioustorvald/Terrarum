package net.torvald.terrarum.weather

import com.badlogic.gdx.graphics.Color
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

    private val pathToImage = "weathers"

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

        val JSON = JsonFetcher(path)

        val skyboxModel = JSON.getString("skyboxGradColourMap")
        val lightboxModel = JSON.getString("daylightClut")

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
            skyboxGradColourMap = getSkyboxModelByName(modname, skyboxModel),
            daylightClut = getLightboxModelByName(modname, lightboxModel),
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

    private fun getSkyboxModelByName(modname: String, name: String): SkyboxModel {
        return if (name.startsWith("model:")) {
            when (name.substring(6)) {
                "hosek" -> SkyboxModelHosek
                else -> throw UnsupportedOperationException("Unknown skybox model: '$name'")
            }
        }
        else if (name.startsWith("lut:")) {
            val filename = name.substring(4)
            val colourMap = GdxColorMap(ModMgr.getGdxFile(modname, "$pathToImage/${filename}"))
            SkyboxGradSimple(colourMap)
        }
        else if (name.startsWith("static:")) {
            val argstr = name.substring(7)
            val args = argstr.split(',').map {
                if (it.length == 7) // #RRGGBB
                    Color(
                        it.substring(1, 3).toInt(16) / 255.0f,
                        it.substring(3, 5).toInt(16) / 255.0f,
                        it.substring(5, 7).toInt(16) / 255.0f,
                        1f,
                    )
                else if (it.length == 9) // #RRGGBBAA
                    Color(
                        it.substring(1, 3).toInt(16) / 255.0f,
                        it.substring(3, 5).toInt(16) / 255.0f,
                        it.substring(5, 7).toInt(16) / 255.0f,
                        it.substring(7, 9).toInt(16) / 255.0f,
                    )
                else if (it.length == 4) // #RGB
                    Color(
                        it.substring(1, 2).toInt(16) / 15.0f,
                        it.substring(2, 3).toInt(16) / 15.0f,
                        it.substring(3, 4).toInt(16) / 15.0f,
                        1f,
                    )
                else if (it.length == 5) // #RGBA
                    Color(
                        it.substring(1, 2).toInt(16) / 15.0f,
                        it.substring(2, 3).toInt(16) / 15.0f,
                        it.substring(3, 4).toInt(16) / 15.0f,
                        it.substring(4, 5).toInt(16) / 15.0f,
                    )
                else throw IllegalArgumentException("Unknown colour code: $it")
            }
            SkyboxGradSimple(GdxColorMap(args.size, 1, args))
        }
        else {
            throw UnsupportedOperationException("Unknown skybox: '$name'")
        }
    }

    private fun getLightboxModelByName(modname: String, name: String): GdxColorMap {
        return if (name.startsWith("lut:")) {
            val filename = name.substring(4)
            GdxColorMap(ModMgr.getGdxFile(modname, "$pathToImage/${filename}"))
        }
        else if (name.startsWith("static:")) {
            val argstr = name.substring(7)
            val args = argstr.split(',').map { Color.WHITE }
            GdxColorMap(args.size, 1, args)
        }
        else {
            throw UnsupportedOperationException("Unknown skybox: '$name'")
        }
    }
}