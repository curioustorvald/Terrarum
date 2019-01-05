package net.torvald.spriteassembler

import java.io.InputStream
import java.io.Reader
import java.util.*
import kotlin.collections.HashMap

class ADProperties {
    private val javaProp = Properties()

    /** Every key is CAPITALISED */
    private val propTable = HashMap<String, List<ADPropertyObject>>()

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

            propTable[propName.capitalize()] = propsList
        }
    }

    operator fun get(identifier: String) = propTable[identifier.capitalize()]
    val keys
        get() = propTable.keys
    fun containsKey(key: String) = propTable.containsKey(key)
    fun forEach(predicate: (String, List<ADPropertyObject>) -> Unit) = propTable.forEach(predicate)
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
        /** example valid input: ```sprites/test.tga``` */
        fun isADstring(property: String) = !isADvariable(property)
    }

    data class Vector2i(var x: Int, var y: Int)

    enum class ADPropertyType {
        NAME_ONLY,  // "sprite/test.tga" to nothing
        IVEC2,      // "LEG_RIGHT" to (1,-1)
        FLOAT,      // "DELAY" to 0.15
        STRING_PAIR // "SKELETON" to "SKELETON_DEFAULT"
    }

    override fun toString(): String {
        return "$variable ${input ?: ""}: $type"
    }
}