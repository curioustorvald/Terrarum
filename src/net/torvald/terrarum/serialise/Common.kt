package net.torvald.terrarum.serialise

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import net.torvald.terrarum.console.EchoError
import net.torvald.terrarum.gameworld.BlockLayer
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.WorldTime
import net.torvald.terrarum.tvda.ByteArray64
import net.torvald.terrarum.tvda.ByteArray64GrowableOutputStream
import net.torvald.terrarum.tvda.ByteArray64InputStream
import net.torvald.terrarum.tvda.ByteArray64Reader
import net.torvald.terrarum.utils.*
import org.apache.commons.codec.digest.DigestUtils
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

    const val GENVER = 4
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


        // BigInteger
        jsoner.setSerializer(BigInteger::class.java, object : Json.Serializer<BigInteger> {
            override fun write(json: Json, obj: BigInteger?, knownType: Class<*>?) {
                json.writeValue(obj?.toString())
            }

            override fun read(json: Json, jsonData: JsonValue, type: Class<*>?): BigInteger {
                return BigInteger(jsonData.asString())
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
                JsonFetcher.forEach(jsonData) { key, obj ->
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
                JsonFetcher.forEach(jsonData) { key, obj ->
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
                JsonFetcher.forEach(jsonData) { key, obj ->
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
                JsonFetcher.forEach(jsonData) { key, obj ->
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

            override fun read(json: Json, jsonData: JsonValue, type: Class<*>?): UUID {
                return UUID.fromString(jsonData.asString())
            }
        })
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
            unzipdBytes.add(byte.toByte())
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
                Ascii85.decode(buf[0], buf[1], buf[2], buf[3], buf[4]).forEach { unasciidBytes.add(it) }
                buf.fill(Ascii85.PAD_CHAR)
            }

            buf[bai % 5] = char.toChar()

            bai += 1
        }; Ascii85.decode(buf[0], buf[1], buf[2], buf[3], buf[4]).forEach { unasciidBytes.add(it) }

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

}
