package net.torvald.spriteassembler

import com.badlogic.gdx.files.FileHandle
import net.torvald.terrarum.linearSearchBy
import net.torvald.terrarum.serialise.Common
import java.io.InputStream
import java.io.Reader
import java.util.*

internal data class Joint(val name: String, val position: ADPropertyObject.Vector2i) {
    override fun toString() = "$name $position"
}

internal data class Skeleton(val name: String, val joints: List<Joint>) {
    override fun toString() = "$name=$joints"
}


/**
 * @param name You know it
 * @param delay Delay between each frame in seconds
 * @param row STARTS AT ONE! Row in the final spritesheet, also act as the animation index.
 * @param frames number of frames this animation has
 * @param skeleton list of joints to be transformed
 */
internal data class Animation(val name: String, val delay: Float, val row: Int, val frames: Int, val skeleton: Skeleton) {
    override fun toString() = "$name delay: $delay, row: $row, frames: $frames, skeleton: ${skeleton.name}"
}

/** Later the 'translate' can be changed so that it represents affine transformation (Matrix2d) */
internal data class Transform(val joint: Joint, val translate: ADPropertyObject.Vector2i) {
    override fun toString() = "$joint transform: $translate"
}

class ADProperties {
    private var fileFrom = ""
    @Transient private var adlString = ""
    private val javaProp = Properties()

    /** Every key is CAPITALISED */
    private val propTable = HashMap<String, List<ADPropertyObject>>()

    /** list of bodyparts used by all the skeletons (HEAD, UPPER_TORSO, LOWER_TORSO) */
//    lateinit var bodyparts: List<String>; private set
    lateinit var bodypartFiles: List<String>; private set
    /** properties that are being used as skeletons (SKELETON_STAND) */
    internal lateinit var skeletons: HashMap<String, Skeleton>; private set
    /** properties that defines position of joint of the bodypart */
    internal val bodyparts = HashMap<String, ADPropertyObject.Vector2i>()
    /** properties that are recognised as animations (ANIM_RUN, ANIM)IDLE) */
    internal lateinit var animations: HashMap<String, Animation>; private set
    /** an "animation frame" property (ANIM_RUN_1, ANIM_RUN_2) */
    internal lateinit var transforms: HashMap<String, List<Transform>>; private set

    @Transient private val reservedProps = listOf("SPRITESHEET", "EXTENSION", "CONFIG", "BODYPARTS")
    @Transient private val animMustContain = listOf("DELAY", "ROW", "SKELETON")

    lateinit var baseFilename: String; private set
    lateinit var extension: String; private set
    var frameWidth: Int = -1; private set
    var frameHeight: Int = -1; private set
    var originX: Int = -1; private set
    internal val origin: ADPropertyObject.Vector2i
        get() = ADPropertyObject.Vector2i(originX, 0)

    @Transient private val animFrameSuffixRegex = Regex("""_[0-9]+""")

    @Transient private val ALL_JOINT = Joint(ALL_JOINT_SELECT_KEY, ADPropertyObject.Vector2i(0, 0))

    var rows = -1; private set
    var cols = -1; private set

    fun getRawADL() = adlString

    companion object {
        const val ALL_JOINT_SELECT_KEY = "ALL"
    }

    constructor(gdxFile: FileHandle) {
        fileFrom = gdxFile.path()
        adlString = gdxFile.readString(Common.CHARSET.name())
        javaProp.load(gdxFile.read())
        continueLoad()
    }

    constructor(reader: Reader) {
        adlString = reader.readText()
        javaProp.load(reader)
        continueLoad()
    }

    constructor(inputStream: InputStream) {
        adlString = inputStream.readAllBytes().toString(Common.CHARSET)
        javaProp.load(inputStream)
        continueLoad()
    }

    private fun continueLoad() {
        // sanity check
        reservedProps.forEach {
            try {
                javaProp[it]!!
            }
            catch (e: NullPointerException) {
                throw IllegalArgumentException("Prop '$it' not found from ${fileFrom.ifBlank { "(path unavailable)" }}", e)
            }
        }

        javaProp.keys.forEach { propName ->
            val propsStr = javaProp.getProperty(propName as String)
            val propsList = propsStr.split(';').map { ADPropertyObject(it) }

            propTable[propName.toUpperCase()] = propsList
        }

        // set reserved values for the animation: filename, extension
        baseFilename = get("SPRITESHEET")[0].name
        extension = get("EXTENSION")[0].name
        val frameSizeVec = get("CONFIG").linearSearchBy { it.name == "SIZE" }!!.input as ADPropertyObject.Vector2i
        frameWidth = frameSizeVec.x
        frameHeight = frameSizeVec.y
        originX = (get("CONFIG").linearSearchBy { it.name == "ORIGINX" }!!.input as Float).toInt()

        var maxColFinder = -1
        var maxRowFinder = -1
        val skeletons = HashMap<String, Skeleton>()
        val animations = HashMap<String, Animation>()
        val animFrames = HashMap<String, Int>()
        val transforms = HashMap<String, List<Transform>>()
        // scan every props, write down anim frames for later use
        propTable.keys.forEach {
            if (animFrameSuffixRegex.containsMatchIn(it)) {
                val animName = getAnimNameFromFrame(it)
                val frameNumber = getFrameNumberFromName(it)

                // if animFrames does not have our entry, add it.
                // otherwise, max() against the existing value
                if (animFrames.containsKey(animName)) {
                    animFrames[animName] = maxOf(animFrames[animName]!!, frameNumber)
                }
                else {
                    animFrames[animName] = frameNumber
                }

                maxColFinder = maxOf(maxColFinder, frameNumber)
            }
        }
        // populate skeletons and animations
        forEach { s, list ->
            // Map-ify. If it has variable == "SKELETON", the 's' is likely an animation
            // and thus, uses whatever the "input" used by the SKELETON is a skeleton
            val propsHashMap = HashMap<String, Any?>()
            list.forEach {
                propsHashMap[it.name.toUpperCase()] = it.input
            }

            // if it is indeed anim, populate animations list
            if (propsHashMap.containsKey("SKELETON")) {
                val skeletonName = propsHashMap["SKELETON"] as String
                val skeletonDef = get(skeletonName)

                skeletons[skeletonName] = Skeleton(skeletonName, skeletonDef.toJoints())
                animations[s] = Animation(
                        s,
                        propsHashMap["DELAY"] as Float,
                        (propsHashMap["ROW"] as Float).toInt(),
                        animFrames[s]!!,
                        Skeleton(skeletonName, skeletonDef.toJoints())
                )

                maxRowFinder = maxOf(maxRowFinder, animations[s]!!.row)
            }
        }

        // populate transforms
        animations.forEach { t, u ->
            for (fc in 1..u.frames) {
                val frameName = "${t}_$fc"
                val prop = get(frameName)

                var emptyList = prop.size == 1 && prop[0].name.isEmpty()

                val transformList = if (!emptyList) {
                    List(prop.size) { index ->
                        val jointNameToSearch = prop[index].name.toUpperCase()
                        val joint = if (jointNameToSearch == "ALL")
                            ALL_JOINT
                        else
                            u.skeleton.joints.linearSearchBy { it.name == jointNameToSearch }
                            ?: throw NullPointerException("No such joint: $jointNameToSearch")
                        val translate = prop[index].input as ADPropertyObject.Vector2i

                        Transform(joint, translate)
                    }
                }
                else {
                    // to make real empty list
                    List(0) { Transform(ALL_JOINT, ADPropertyObject.Vector2i(0, 0)) }
                }

                transforms[frameName] = transformList
            }
        }

        get("BODYPARTS").forEach {
            try {
                this.bodyparts[it.name] = (it.input as ADPropertyObject.Vector2i)
            }
            catch (e: NullPointerException) {
                if (it.name.isBlank())
                    throw IllegalArgumentException("Empty Bodypart name on BODYPARTS; try removing trailing semicolon (';')?")
                else
                    throw IllegalArgumentException("Bodyparts definition for '${it.name}' not found from ${fileFrom.ifBlank { "(path unavailable)" }}", e)
            }
        }

        this.skeletons = skeletons
        this.animations = animations
        this.bodypartFiles = this.bodyparts.keys.map { toFilename(it) }
        this.transforms = transforms

        cols = maxColFinder
        rows = maxRowFinder
    }

    operator fun get(identifier: String): List<ADPropertyObject> = propTable[identifier.toUpperCase()]!!
    val keys
        get() = propTable.keys
    fun containsKey(key: String) = propTable.containsKey(key)
    fun forEach(predicate: (String, List<ADPropertyObject>) -> Unit) = propTable.forEach(predicate)

    fun toFilename(partName: String) =
            "${this.baseFilename}${partName.toLowerCase()}${this.extension}"

    internal fun getAnimByFrameName(frameName: String) = animations[getAnimNameFromFrame(frameName)]!!
    internal fun getFrameNumberFromName(frameName: String) = frameName.substring(frameName.lastIndexOf('_') + 1 until frameName.length).toInt()

    internal fun getSkeleton(name: String) = skeletons[name]!!
    internal fun getTransform(name: String) = transforms[name]!!

    private fun getAnimNameFromFrame(s: String) = s.substring(0 until s.lastIndexOf('_'))

    private fun List<ADPropertyObject>.toJoints() = List(this.size) {
        Joint(this[it].name.toUpperCase(), this[it].input!! as ADPropertyObject.Vector2i)
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
    val name: String
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
            name = propPair[0]
            val inputStr = propPair[1]

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
            name = propertyRaw
            input = null
            type = ADPropertyType.NAME_ONLY
        }
    }

    internal companion object {
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

    internal data class Vector2i(var x: Int, var y: Int) {
        override fun toString() = "($x, $y)"

        operator fun plus(other: Vector2i) = Vector2i(this.x + other.x, this.y + other.y)
        operator fun minus(other: Vector2i) = Vector2i(this.x - other.x, this.y - other.y)

        fun invertY() = Vector2i(this.x, -this.y)
        fun invertX() = Vector2i(-this.x, this.y)
        fun invertXY() = Vector2i(-this.x, -this.y)
    }

    enum class ADPropertyType {
        NAME_ONLY,  // "sprite/test.tga" to nothing
        IVEC2,      // "LEG_RIGHT" to (1,-1)
        FLOAT,      // "DELAY" to 0.15
        STRING_PAIR // "SKELETON" to "SKELETON_DEFAULT"
    }

    override fun toString(): String {
        return "$name ${input ?: ""}: ${type.toString().toLowerCase()}"
    }
}