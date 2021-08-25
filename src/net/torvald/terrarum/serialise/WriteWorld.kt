package net.torvald.terrarum.serialise

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import net.torvald.terrarum.gameworld.BlockLayer
import net.torvald.terrarum.gameworld.WorldTime
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64GrowableOutputStream
import net.torvald.terrarum.modulecomputers.virtualcomputer.tvd.ByteArray64InputStream
import org.apache.commons.codec.digest.DigestUtils
import java.math.BigInteger
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Created by minjaesong on 2021-08-23.
 */
open class WriteWorld(val ingame: TerrarumIngame) {

    open fun invoke(): String {
        val world = ingame.world
        //return "{${world.getJsonFields().joinToString(",\n")}}"
        return jsoner.toJson(world)
    }

    fun encodeToByteArray64(): ByteArray64 {
        val world = ingame.world
        val ba = ByteArray64()
        ba.add('{'.code.toByte())
        world.getJsonFields().forEachIndexed { index, str ->
            if (index > 0) {
                ba.add(','.code.toByte())
                ba.add('\n'.code.toByte())
            }
            str.toByteArray().forEach { ba.add(it) }
        }
        ba.add('}'.code.toByte())
        return ba
    }

    companion object {
        /** dispose of the `offendingObject` after rejection! */
        class BlockLayerHashMismatchError(val offendingObject: BlockLayer) : Error()

        private fun Byte.tostr() = this.toInt().and(255).toString(16).padStart(2,'0')
        private val digester = DigestUtils.getSha256Digest()

        val jsoner = Json(JsonWriter.OutputType.json)

        // install custom (de)serialiser
        init {
            // BigInteger
            jsoner.setSerializer(BigInteger::class.java, object : Json.Serializer<BigInteger> {

                override fun write(json: Json, obj: BigInteger?, knownType: Class<*>?) {
                    json.writeValue(obj?.toString())
                }

                override fun read(json: Json, jsonData: JsonValue, type: Class<*>?): BigInteger {
                    return BigInteger(jsonData.asString())
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
                    // full auto
                    //return strToBlockLayer(json.fromJson(type, jsonData.toJson(JsonWriter.OutputType.minimal)) as LayerInfo)

                    // full manual
                    return strToBlockLayer(LayerInfo(
                            jsonData.getString("h"),
                            jsonData.getString("b"),
                            jsonData.getInt("x"),
                            jsonData.getInt("y")
                    ))
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
        }

        private data class LayerInfo(val h: String, val b: String, val x: Int, val y: Int)

        /**
         * @param b a BlockLayer
         * @return Bytes in [b] which are GZip'd then Ascii85-encoded
         */
        private fun blockLayerToStr(b: BlockLayer): String {
            val sb = StringBuilder()
            val bo = ByteArray64GrowableOutputStream()
            val zo = GZIPOutputStream(bo)

            // zip
            b.bytesIterator().forEachRemaining {
                zo.write(it.toInt())
            }
            zo.flush(); zo.close()

            // enascii
            val ba = bo.toByteArray64()
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

        private fun strToBlockLayer(layerInfo: LayerInfo): BlockLayer {
            val layer = BlockLayer(layerInfo.x, layerInfo.y)
            val unasciidBytes = ByteArray64()
            val unzipdBytes = ByteArray64()

            // unascii
            var bai = 0
            val buf = CharArray(5) { Ascii85.PAD_CHAR }
            layerInfo.b.forEach {
                if (bai > 0 && bai % 5 == 0) {
                    Ascii85.decode(buf[0], buf[1], buf[2], buf[3], buf[4]).forEach { unasciidBytes.add(it) }
                    buf.fill(Ascii85.PAD_CHAR)
                }

                buf[bai % 5] = it

                bai += 1
            }; Ascii85.decode(buf[0], buf[1], buf[2], buf[3], buf[4]).forEach { unasciidBytes.add(it) }

            // unzip
            val zi = GZIPInputStream(ByteArray64InputStream(unasciidBytes))
            while (true) {
                val byte = zi.read()
                if (byte == -1) break
                unzipdBytes.add(byte.toByte())
            }
            zi.close()

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
                throw BlockLayerHashMismatchError(layer)
            }

            return layer
        }
    }
}
