package net.torvald.terrarum.serialise

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import net.torvald.random.HQRNG
import net.torvald.terrarum.TerrarumAppConfiguration
import net.torvald.terrarum.console.EchoError
import net.torvald.terrarum.gameworld.BlockLayer
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.WorldTime
import net.torvald.terrarum.savegame.ByteArray64
import net.torvald.terrarum.savegame.ByteArray64GrowableOutputStream
import net.torvald.terrarum.savegame.ByteArray64InputStream
import net.torvald.terrarum.savegame.ByteArray64Reader
import net.torvald.terrarum.utils.*
import net.torvald.terrarum.weather.BaseModularWeather
import net.torvald.terrarum.weather.WeatherDirBox
import net.torvald.terrarum.weather.WeatherMixer
import net.torvald.terrarum.weather.WeatherStateBox
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.io.StringReader
import java.math.BigInteger
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Created by minjaesong on 2021-08-26.
 */
object Common {

    const val GENVER = TerrarumAppConfiguration.VERSION_RAW
    const val COMP_NONE = 0
    const val COMP_GZIP = 1
    const val COMP_LZMA = 2

    val CHARSET = Charsets.UTF_8

    /** dispose of the `offendingObject` after rejection! */
    class BlockLayerHashMismatchError(val oldHash: String, val newHash: String, val offendingObject: BlockLayer) : Error("Old Hash $oldHash != New Hash $newHash")

    private fun Byte.tostr() = this.toInt().and(255).toString(16).padStart(2,'0')
    private val digester = DigestUtils.getSha256Digest()

    val jsoner = Json(JsonWriter.OutputType.json)

    // install custom (de)serialiser
    init {
        jsoner.ignoreUnknownFields = true
        jsoner.setUsePrototypes(false)
        jsoner.setIgnoreDeprecated(false)

        // BigInteger
        jsoner.setSerializer(BigInteger::class.java, object : Json.Serializer<BigInteger> {
            override fun write(json: Json, obj: BigInteger?, knownType: Class<*>?) {
                json.writeValue(obj?.toString())
            }

            override fun read(json: Json, jsonData: JsonValue, type: Class<*>?): BigInteger? {
                return if (jsonData.isNull) null else BigInteger(jsonData.asString())
            }
        })
        // ZipCodedStr
        jsoner.setSerializer(ZipCodedStr::class.java, object : Json.Serializer<ZipCodedStr> {
            override fun write(json: Json, obj: ZipCodedStr, knownType: Class<*>?) {
                json.writeValue(zipStrAndEnascii(obj.doc))
            }

            override fun read(json: Json, jsonData: JsonValue, type: Class<*>?): ZipCodedStr {
                return ZipCodedStr(unasciiAndUnzipStr(jsonData.asString()))
            }
        })
        // BlockLayer
        jsoner.setSerializer(BlockLayer::class.java, object : Json.Serializer<BlockLayer> {
            override fun write(json: Json, obj: BlockLayer, knownType: Class<*>?) {
                digester.reset()
                obj.bytesIterator().forEachRemaining { digester.update(it) }
                val hash = StringBuilder().let { sb -> digester.digest().forEach { sb.append(it.tostr()) }; sb.toString() }

                val layer = LayerInfo(hash, blockLayerToStr(obj), obj.width, obj.height)

                json.writeValue(layer)
            }

            override fun read(json: Json, jsonData: JsonValue, type: Class<*>): BlockLayer {
                // full manual
                try {
                    return strToBlockLayer(LayerInfo(
                            jsonData.getString("h"),
                            jsonData.getString("b"),
                            jsonData.getInt("x"),
                            jsonData.getInt("y")
                    ))
                }
                catch (e: BlockLayerHashMismatchError) {
                    EchoError(e.message ?: "")
                    return e.offendingObject
                }
            }
        })
        // WorldTime
        jsoner.setSerializer(WorldTime::class.java, object : Json.Serializer<WorldTime> {
            override fun write(json: Json, obj: WorldTime, knownType: Class<*>?) {
                json.writeValue(obj.TIME_T)
            }

            override fun read(json: Json, jsonData: JsonValue, type: Class<*>?): WorldTime {
                return WorldTime(jsonData.asLong())
            }
        })
        // HashArray
        jsoner.setSerializer(HashArray::class.java, object : Json.Serializer<HashArray<*>> {
            override fun write(json: Json, obj: HashArray<*>, knownType: Class<*>?) {
                json.writeObjectStart()
                obj.forEach { (k, v) ->
                    json.writeValue(k.toString(), v)
                }
                json.writeObjectEnd()
            }

            override fun read(json: Json, jsonData: JsonValue, type: Class<*>?): HashArray<*> {
                val hashMap = HashArray<Any>()
                JsonFetcher.forEachSiblings(jsonData) { key, obj ->
                    hashMap[key.toLong()] = json.readValue(null, obj)
                }
                return hashMap
            }
        })
        // HashedWirings
        jsoner.setSerializer(HashedWirings::class.java, object : Json.Serializer<HashedWirings> {
            override fun write(json: Json, obj: HashedWirings, knownType: Class<*>?) {
                json.writeObjectStart()
                obj.forEach { (k, v) ->
                    json.writeValue(k.toString(), v)
                }
                json.writeObjectEnd()
            }

            override fun read(json: Json, jsonData: JsonValue, type: Class<*>?): HashedWirings {
                val hashMap = HashedWirings()
                JsonFetcher.forEachSiblings(jsonData) { key, obj ->
                    hashMap[key.toLong()] = json.readValue(GameWorld.WiringNode::class.java, obj)
                }
                return hashMap
            }
        })
        // HashedWiringGraph
        jsoner.setSerializer(HashedWiringGraph::class.java, object : Json.Serializer<HashedWiringGraph> {
            override fun write(json: Json, obj: HashedWiringGraph, knownType: Class<*>?) {
                json.writeObjectStart()
                obj.forEach { (k, v) ->
                    json.writeValue(k.toString(), v, WiringGraphMap::class.java)
                }
                json.writeObjectEnd()
            }

            override fun read(json: Json, jsonData: JsonValue, type: Class<*>?): HashedWiringGraph {
                val hashMap = HashedWiringGraph()
                JsonFetcher.forEachSiblings(jsonData) { key, obj ->
                    hashMap[key.toLong()] = json.readValue(WiringGraphMap::class.java, obj)
                }
                return hashMap
            }
        })
        // WiringGraphMap; this serialiser is here just to reduce the JSON filesize
        jsoner.setSerializer(WiringGraphMap::class.java, object : Json.Serializer<WiringGraphMap> {
            override fun write(json: Json, obj: WiringGraphMap, knownType: Class<*>?) {
                json.writeObjectStart()
                obj.forEach { (k, v) ->
                    json.writeValue(k, v, GameWorld.WiringSimCell::class.java)
                }
                json.writeObjectEnd()
            }

            override fun read(json: Json, jsonData: JsonValue, type: Class<*>?): WiringGraphMap {
                val hashMap = WiringGraphMap()
                JsonFetcher.forEachSiblings(jsonData) { key, obj ->
                    hashMap[key] = json.readValue(GameWorld.WiringSimCell::class.java, obj)
                }
                return hashMap
            }
        })
        // UUID
        jsoner.setSerializer(UUID::class.java, object : Json.Serializer<UUID> {
            override fun write(json: Json, obj: UUID, knownType: Class<*>?) {
                json.writeValue(obj.toString())
            }

            override fun read(json: Json, jsonData: JsonValue, type: Class<*>?): UUID? {
                return if (jsonData.isNull) null else UUID.fromString(jsonData.asString())
            }
        })
        // HQRNG
        jsoner.setSerializer(HQRNG::class.java, object : Json.Serializer<HQRNG> {
            override fun write(json: Json, obj: HQRNG, knownType: Class<*>?) {
                json.writeValue("${obj.state0.toString()},${obj.state1.toString()}")
            }

            override fun read(json: Json, jsonData: JsonValue, type: Class<*>?): HQRNG {
                val rng = HQRNG()
                val seedstr = jsonData.asString().split(',')
                rng.setSeed(seedstr[0].toLong(), seedstr[1].toLong())
                return rng
            }
        })
        // kotlin.ByteArray
        jsoner.setSerializer(ByteArray::class.java, object : Json.Serializer<ByteArray> {
            override fun write(json: Json, obj: ByteArray, knownType: Class<*>?) {
                json.writeValue(bytesToZipdStr(obj.iterator()))
            }

            override fun read(json: Json, jsonData: JsonValue, type: Class<*>?): ByteArray? {
                return if (jsonData.isNull) return null else strToBytes(StringReader(jsonData.asString())).toByteArray()
            }
        })
        // WeatherStateBox
        jsoner.setSerializer(WeatherStateBox::class.java, object : Json.Serializer<WeatherStateBox> {
            override fun write(json: Json, obj: WeatherStateBox, knownType: Class<*>?) {
                json.writeValue("${obj.x};${obj.pM2};${obj.pM1};${obj.p0};${obj.p1};${obj.p2};${obj.p3}")
            }

            override fun read(json: Json, jsonData: JsonValue, type: Class<*>?): WeatherStateBox {
                return jsonData.asString().split(';').map { it.toFloat() }.let {
                    WeatherStateBox(it[0], it[1], it[2], it[3], it[4], it[5], it[6])
                }
            }
        })
        // WeatherDirBox
        jsoner.setSerializer(WeatherDirBox::class.java, object : Json.Serializer<WeatherDirBox> {
            override fun write(json: Json, obj: WeatherDirBox, knownType: Class<*>?) {
                json.writeValue("${obj.x};${obj.pM2};${obj.pM1};${obj.p0};${obj.p1};${obj.p2};${obj.p3}")
            }

            override fun read(json: Json, jsonData: JsonValue, type: Class<*>?): WeatherDirBox {
                try {
                    return jsonData.asString().split(';').map { it.toFloat() }.let {
                        WeatherDirBox(it[0], it[1], it[2], it[3], it[4], it[5], it[6])
                    }
                }
                // just for savegame compatibility
                catch (_: IllegalStateException) {
                    return WeatherDirBox(
                        jsonData.getFloat("x"),
                        jsonData.getFloat("pM2"),
                        jsonData.getFloat("pM1"),
                        jsonData.getFloat("p0"),
                        jsonData.getFloat("p1"),
                        jsonData.getFloat("p2"),
                        jsonData.getFloat("p3")
                    )
                }
            }
        })
        // BaseModularWeather
        jsoner.setSerializer(BaseModularWeather::class.java, object : Json.Serializer<BaseModularWeather> {
            override fun write(json: Json, obj: BaseModularWeather, knownType: Class<*>?) {
                json.writeValue(obj.identifier)
            }

            override fun read(json: Json, jsonData: JsonValue, type: Class<*>?): BaseModularWeather {
                return WeatherMixer.weatherDict[jsonData.asString()]!!
            }
        })
    }


    data class SpliceCmd(
        var strPos: Int,
        var stackDepth: Int = -1,
        var objStartPos: Int = -1,
        var objEndPos: Int = -1,
        var prependedCommaPos: Int = -1
    )

    fun scanForCompanionObjects(s: String): List<SpliceCmd> {
        val retBin = ArrayList<SpliceCmd>()

        val state = Stack<String>().also { it.push("lit") } // lit, esc, qot
        val parenStack = Stack<Char>() // { [ " '

        val searchStr = "Companion"
        val strCircBuf = StringBuilder()

        val workBin = Stack<SpliceCmd>()
        var kvMode = "key"

        s.forEachIndexed { index, c ->
            strCircBuf.append(c); if (strCircBuf.length > searchStr.length) strCircBuf.deleteCharAt(0)

            when (c) {
                '{', '[', '(' -> {
                    parenStack.push(c)
                    if (workBin.isNotEmpty()) {
                        workBin.peek().let {
                            if (it.objStartPos == -1) {
                                it.objStartPos = index
                                it.stackDepth = parenStack.size
                            }
                        }

//                        println("== workBin mod ==;")
//                        workBin.forEach { println("$it;") }
                    }
                }
                '}', ']', ')' -> {
                    if (workBin.isNotEmpty()) {

                        workBin.peek().let {
                            if (it.objEndPos == -1) {
                                if (parenStack.size == it.stackDepth) it.objEndPos = index
//                                println("  parenStack.size=${parenStack.size}, it=$it;")
                            }
                        }

//                        println("== workBin pop ==;")
//                        workBin.forEach { println("$it;") }

                        retBin.add(workBin.pop())
                    }
                    parenStack.pop()

                    if (kvMode == "val") {
                        kvMode = "key"
                    }
                }
                '\\' -> {
                    if (state.peek() == "esc") {
                        state.pop()
                    }
                    else {
                        state.push("esc")
                    }
                }
                '"', '\'' -> {
                    if (state.peek() == "esc")
                        state.pop()
                    else if (parenStack.peek() == c) {
                        parenStack.pop()
                        state.pop() // assuming no errors :p
                    }
                    else {
                        parenStack.push(c)
                        state.push("qot")
                    }
                }
                ':' -> {
                    if (state.peek() == "lit" && kvMode == "key") {
                        kvMode = "val"
                    }
                }
                ',' -> {
                    if (state.peek() == "lit" && kvMode == "val") {
                        kvMode = "key"
                    }
                }
                else -> {
                    if (state.peek() == "esc")
                        state.pop()
                }
            }

            if (strCircBuf.toString() == searchStr && kvMode == "key") {
                workBin.push(SpliceCmd(index))

//                println("== workBin push ==;")
//                workBin.forEach { println("$it;") }
            }

//            println("$c\t${state.peek()};")
        }

        return retBin
    }

    fun String.jsonRemoveCompanionObjects(): String {
        val objectIndices = scanForCompanionObjects(this)

        // search backwards for a valid comma
        // terminate when '{' is reached (means the Companion was the first object)
        objectIndices.forEach {
            var c = it.strPos - 1
            while (c > 0 && this[c] != ',' && this[c-1] != '{') {
                c -= 1
            }
            it.prependedCommaPos = c
        }

//        println(objectIndices)

        // splice the string
        // when the search results are indeed correct, they will have the following properties:
        // 1. str[objStartPos]='{' && str[objEndPos]='}'
        // 2. str[strPos]='n' // as in 'Companio_n_'


        // 1. fill str[prependedCommaPos : objEndPos+1] will null characters
        // 2. create the new string that has null chars filtered
        val sb = StringBuilder(this)
        objectIndices.forEach {
            for (k in it.prependedCommaPos..it.objEndPos) {
                sb[k] = '\u0000'
            }
        }
        return sb.filter { it != '\u0000' }.toString()
    }

    private data class LayerInfo(val h: String, val b: String, val x: Int, val y: Int)

    /**
     * @param b a BlockLayer
     * @return Bytes in [b] which are GZip'd then Ascii85-encoded
     */
    private fun blockLayerToStr(b: BlockLayer): String {
        return bytesToZipdStr(b.bytesIterator())
    }

    private fun strToBlockLayer(layerInfo: LayerInfo): BlockLayer {
        val layer = BlockLayer(layerInfo.x, layerInfo.y)
        val unzipdBytes = strToBytes(StringReader(layerInfo.b))

        // write to blocklayer and the digester
        digester.reset()
        var writeCursor = 0L
        unzipdBytes.forEach {
            if (writeCursor < layer.ptr.size) {
                layer.ptr[writeCursor] = it
                digester.update(it)
                writeCursor += 1
            }
        }

        // check hash
        val hash = StringBuilder().let { sb -> digester.digest().forEach { sb.append(it.tostr()) }; sb.toString() }

        if (hash != layerInfo.h) {
            throw BlockLayerHashMismatchError(layerInfo.h, hash, layer)
        }

        return layer
    }

    fun bytesToZipdStr(byteIterator: Iterator<Byte>): String = enasciiToString(zip(byteIterator))

    fun zip(ba: ByteArray64) = Common.zip(ba.iterator())
    fun zip(byteIterator: Iterator<Byte>): ByteArray64 {
        val bo = ByteArray64GrowableOutputStream()
        val zo = GZIPOutputStream(bo)

        // zip
        byteIterator.forEach {
            zo.write(it.toInt())
        }
        zo.flush(); zo.close()
        return bo.toByteArray64()
    }

    fun enasciiToString(ba: ByteArray64): String = enasciiToString(ba.iterator())
    fun enasciiToString(ba: Iterator<Byte>): String {
        val sb = StringBuilder()
        // enascii
        var bai = 0
        val buf = IntArray(4) { Ascii85.PAD_BYTE }
        ba.forEach {
            if (bai > 0 && bai % 4 == 0) {
                sb.append(Ascii85.encode(buf[0], buf[1], buf[2], buf[3]))
                buf.fill(Ascii85.PAD_BYTE)
            }

            buf[bai % 4] = it.toInt() and 255

            bai += 1
        }; sb.append(Ascii85.encode(buf[0], buf[1], buf[2], buf[3]))

        return sb.toString()
    }

    fun unzip(bytes: ByteArray64): ByteArray64 {
        val unzipdBytes = ByteArray64()
        val zi = GZIPInputStream(ByteArray64InputStream(bytes))
        while (true) {
            val byte = zi.read()
            if (byte == -1) break
            unzipdBytes.appendByte(byte.toByte())
        }
        zi.close()
        return unzipdBytes
    }

    fun unasciiToBytes(reader: Reader): ByteArray64 {
        val unasciidBytes = ByteArray64()

        // unascii
        var bai = 0
        val buf = CharArray(5) { Ascii85.PAD_CHAR }
        while (true) {
            val char = reader.read()
            if (char < 0) break
            if (bai > 0 && bai % 5 == 0) {
                unasciidBytes.appendBytes(Ascii85.decode(buf[0], buf[1], buf[2], buf[3], buf[4]))
                buf.fill(Ascii85.PAD_CHAR)
            }

            buf[bai % 5] = char.toChar()

            bai += 1
        }; unasciidBytes.appendBytes(Ascii85.decode(buf[0], buf[1], buf[2], buf[3], buf[4]))

        return unasciidBytes
    }

    fun getUnzipInputStream(bytes: ByteArray64): InputStream {
        return GZIPInputStream(ByteArray64InputStream(bytes))
    }

    fun strToBytes(reader: Reader): ByteArray64 = unzip(unasciiToBytes(reader))

    /**
     * @param [s] a String
     * @return UTF-8 encoded [s] which are GZip'd then Ascii85-encoded
     */
    fun zipStrAndEnascii(s: String): String {
        return Common.bytesToZipdStr(s.toByteArray(Common.CHARSET).iterator())
    }

    fun unasciiAndUnzipStr(s: String): String {
        return ByteArray64Reader(strToBytes(StringReader(s)), CHARSET).readText()
    }

    fun encodeUUID(uuid: UUID): String {
        val dash = ' '
        val useShort = (uuid.mostSignificantBits.and(0xF000L) == 0x4000L && uuid.leastSignificantBits.ushr(62) == 0b10L)

        /*(uuid.mostSignificantBits.toBig64() + uuid.leastSignificantBits.toBig64()).forEach {
            print("${it.toUint().toString(2).padStart(8,'0')} ")
        }
        println()*/

        val bytes = if (useShort) {
            (uuid.mostSignificantBits.toBig64() +
                    uuid.leastSignificantBits.toBig64()).let {
                it.sliceArray(0..5) +
                        (it[6].toUint().and(15).shl(4) or it[8].toUint().and(0x0F)).toByte() +
                        it[7] +
                        it.sliceArray(9..15) +
                        (it[8].toUint().and(0x30).shl(2)).toByte()
            }
        }
        else {
            uuid.mostSignificantBits.toBig64() +
                    uuid.leastSignificantBits.toBig64()
        }

        /*bytes.forEach {
            print("${it.toUint().toString(2).padStart(8,'0')} ")
        }
        println()*/

        return PasswordBase32.encode(bytes).let {
            if (useShort)
                "${it.substring(0..4)} ${it.substring(5..9)} ${it.substring(10..14)} ${it.substring(15..19)} ${it.substring(20..24)}"
            else
                "${it.substring(0..3)}$dash${it.substring(4..5)}$dash${it.substring(6..10)}$dash${it.substring(11..15)}$dash${it.substring(16..20)}$dash${it.substring(21)}"
        }
    }

    fun decodeToUUID(str: String): UUID {
        val code = str.replace(" ", "").trim()
        val b = PasswordBase32.decode(code + (if (code.length == 25) "Y" else ""), 16)

        /*b.forEach {
            print("${it.toUint().toString(2).padStart(8,'0')} ")
        }
        println()*/

        val bytes = if (code.length == 25) {
            val b6 = b[6].toUint()
            val jh = b[15].toUint().ushr(2) // 0b JJ00 0000 -> 0b 00JJ 0000
            b.sliceArray(0..5) +
            (b6.and(0xF0).ushr(4) or 0x40).toByte() +
            b[7] +
            (b6.and(0x0F) or jh or 0x80).toByte() +
            b.sliceArray(8..14)
        }
        else b

        /*bytes.forEach {
            print("${it.toUint().toString(2).padStart(8,'0')} ")
        }
        println()*/

        return UUID(bytes.toBigInt64(0), bytes.toBigInt64(8))
    }
}

class SaveLoadError(file: File?, cause: Throwable) : RuntimeException("An error occured while loading save file '${file?.absolutePath}'", cause)