package net.torvald.terrarum.weather

import org.newdawn.slick.Image
import java.util.*

/**
 * Note: Colour maps are likely to have sparse data points
 * (i.e., they have 2 475 px in width and you'll need 79 200 data points for a day)
 * so between two must be interpolated.
 *
 * Created by minjaesong on 16-07-11.
 */
data class BaseModularWeather(
        val globalLightColourMap: Image,
        var skyboxGradColourMap: Image,
        val classification: String,
        var extraImages: ArrayList<Image>
)