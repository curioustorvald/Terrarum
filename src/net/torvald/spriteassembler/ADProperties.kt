package net.torvald.spriteassembler

import java.io.InputStream
import java.io.Reader
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

data class Joint(val name: String, val position: ADPropertyObject.Vector2i) {
    override fun toString() = "$name $position"
}

data class Skeleton(val name: String, val joints: List<Joint>) {
    override fun toString() = "$name=$joints"
}

data class Animation(val name: String, val delay: Float, val row: Int, val frames: Int, val skeleton: Skeleton) {
    override fun toString() = "$name delay: $delay, row: $row, frames: $frames, skeleton: ${skeleton.name}"
}

class ADProperties {
    private val javaProp = Properties()

    /** Every key is CAPITALISED */
    private val propTable = HashMap<String, List<ADPropertyObject>>()

    /** list of bodyparts used by all the skeletons */
    lateinit var bodyparts: List<String>; private set
    lateinit var bodypartFiles: List<String>; private set
    /** properties that are being used as skeletons */
    lateinit var skeletons: HashMap<String, Skeleton>; private set
    /** properties that are recognised as animations */
    lateinit var animations: HashMap<String, Animation>; private set

    private val reservedProps = listOf("SPRITESHEET", "EXTENSION")
    private val animMustContain = listOf("DELAY", "ROW", "SKELETON")

    lateinit var baseFilename: String; private set
    lateinit var extension: String; private set

    private val animFrameSuffixRegex = Regex("""_[0-9]+""")

    constructor(reader: Reader) {
        javaProp.load(reader)
        continueLoad()
    }

    constructor(inputStream: InputStream) {
        javaProp.load(inputStream)
        continueLoad()
    }

    private fun continueLoad() {
        javaProp.keys.forEach { propName ->
            val propsStr = javaProp.getProperty(propName as String)
            val propsList = propsStr.split(';').map { ADPropertyObject(it) }

            propTable[propName.toUpperCase()] = propsList
        }

        // set reserved values for the animation: filename, extension
        baseFilename = get("SPRITESHEET")!![0].variable
        extension = get("EXTENSION")!![0].variable

        val bodyparts = HashSet<String>()
        val skeletons = HashMap<String, Skeleton>()
        val animations = HashMap<String, Animation>()
        val animFrames = HashMap<String, Int>()
        // scan every props, write down anim frames for later use
        propTable.keys.forEach {
            if (animFrameSuffixRegex.containsMatchIn(it)) {
                val animName = getAnimNameFromFrame(it)
                val frameNumber = it.drop(animName.length + 1).toInt()

                // if animFrames does not have our entry, add it.
                // otherwise, max() against the existing value
                if (animFrames.containsKey(animName)) {
                    animFrames[animName] = maxOf(animFrames[animName]!!, frameNumber)
                }
                else {
                    animFrames[animName] = frameNumber
                }
            }
        }
        // populate skeletons and animations
        forEach { s, list ->
            // Map-ify. If it has variable == "SKELETON", the 's' is likely an animation
            // and thus, uses whatever the "input" used by the SKELETON is a skeleton
            val propsHashMap = HashMap<String, Any?>()
            list.forEach {
                propsHashMap[it.variable.toUpperCase()] = it.input
            }

            // if it is indeed anim, populate animations list
            if (propsHashMap.containsKey("SKELETON")) {
                val skeletonName = propsHashMap["SKELETON"] as String
                val skeletonDef = get(skeletonName) ?: throw Error("Skeleton definition for $skeletonName not found")

                skeletons.put(skeletonName, Skeleton(skeletonName, skeletonDef.toJoints()))
                animations.put(s, Animation(
                        s,
                        propsHashMap["DELAY"] as Float,
                        (propsHashMap["ROW"] as Float).toInt(),
                        animFrames[s]!!,
                        Skeleton(skeletonName, skeletonDef.toJoints())
                ))
            }
        }

        // populate the bodyparts using skeletons
        skeletons.forEach { (_, prop: Skeleton) ->
            prop.joints.forEach {
                bodyparts.add(it.name)
            }
        }

        this.bodyparts = bodyparts.toList().sorted()
        this.skeletons = skeletons
        this.animations = animations
        this.bodypartFiles = this.bodyparts.map { toFilename(it) }
    }

    operator fun get(identifier: String) = propTable[identifier.toUpperCase()]
    val keys
        get() = propTable.keys
    fun containsKey(key: String) = propTable.containsKey(key)
    fun forEach(predicate: (String, List<ADPropertyObject>) -> Unit) = propTable.forEach(predicate)

    fun toFilename(partName: String) =
            "${this.baseFilename}${partName.toLowerCase()}${this.extension}"

    fun getAnimByFrameName(frameName: String): Animation {
        return animations[getAnimNameFromFrame(frameName)]!!
    }

    private fun getAnimNameFromFrame(s: String) = s.substring(0 until s.lastIndexOf('_'))

    private fun List<ADPropertyObject>.toJoints() = List(this.size) {
        Joint(this[it].variable, this[it].input!! as ADPropertyObject.Vector2i)
    }

}

/**
 * @param propertyRaw example inputs:
 * - ```DELAY 0.15```
 * - ```LEG_RIGHT 0,-1```
 *
 * Created by minjaesong on 2019-01-05.
 */
class ADPropertyObject(propertyRaw: String) {

    /** If the input is like ```UPPER_TORSO``` (that is, not a variable-input pair), this holds the string UPPER_TORSO. */
    val variable: String
    val input: Any?
        get() = when (type) {
            ADPropertyType.IVEC2 -> field!! as Vector2i
            ADPropertyType.FLOAT -> field!! as Float
            ADPropertyType.STRING_PAIR -> field!! as String
            else -> null
        }
    val type: ADPropertyType


    init {
        val propPair = propertyRaw.split(variableInputSepRegex)

        if (isADvariable(propertyRaw)) {
            variable = propPair[0]
            val inputStr = propPair[1]!!

            if (isADivec2(inputStr)) {
                type = ADPropertyType.IVEC2
                input = toADivec2(inputStr)
            }
            else if (isADfloat(inputStr)) {
                type = ADPropertyType.FLOAT
                input = toADfloat(inputStr)
            }
            else {
                type = ADPropertyType.STRING_PAIR
                input = inputStr
            }
        }
        else {
            variable = propertyRaw
            input = null
            type = ADPropertyType.NAME_ONLY
        }
    }

    companion object {
        private val floatRegex = Regex("""-?[0-9]+(\.[0-9]*)?""")
        private val ivec2Regex = Regex("""-?[0-9]+,-?[0-9]+""")
        private val variableInputSepRegex = Regex(""" +""")

        fun isADivec2(s: String) = ivec2Regex.matches(s)
        fun isADfloat(s: String) = floatRegex.matches(s) && !ivec2Regex.containsMatchIn(s)

        fun toADivec2(s: String) = if (isADivec2(s))
                Vector2i(s.substringBefore(',').toInt(), s.substringAfter(',').toInt())
            else throw IllegalArgumentException("Input not in ivec2 format: $s")
        fun toADfloat(s: String) = if (isADfloat(s))
                s.toFloat()
            else throw IllegalArgumentException("Input not in ivec2 format: $s")

        /** example valid input: ```LEG_RIGHT 0,1``` */
        fun isADvariable(property: String) = variableInputSepRegex.containsMatchIn(property)
        /** example valid input: ```sprites/test``` */
        fun isADstring(property: String) = !isADvariable(property)
    }

    data class Vector2i(var x: Int, var y: Int) {
        override fun toString() = "$x, $y"
    }

    enum class ADPropertyType {
        NAME_ONLY,  // "sprite/test.tga" to nothing
        IVEC2,      // "LEG_RIGHT" to (1,-1)
        FLOAT,      // "DELAY" to 0.15
        STRING_PAIR // "SKELETON" to "SKELETON_DEFAULT"
    }

    override fun toString(): String {
        return "$variable ${input ?: ""}: ${type.toString().toLowerCase()}"
    }
}