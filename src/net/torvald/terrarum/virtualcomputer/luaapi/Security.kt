package net.torvald.terrarum.virtualcomputer.luaapi

import li.cil.repack.org.luaj.vm2.Globals
import li.cil.repack.org.luaj.vm2.LuaValue
import li.cil.repack.org.luaj.vm2.lib.OneArgFunction
import net.torvald.terrarum.gameworld.toUint
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils
import java.security.SecureRandom

/**
 * Created by minjaesong on 16-09-15.
 */
internal class Security(globals: Globals) {

    init {
        // load things. WARNING: THIS IS MANUAL!
        globals["security"] = LuaValue.tableOf()
        globals["security"]["toSHA256"] = SHA256sum()
        globals["security"]["toSHA1"] = SHA1sum()
        globals["security"]["toMD5"] = MD5sum()
        globals["security"]["randomBytes"] = SecureRandomHex()
        globals["security"]["decodeBase64"] = DecodeBase64()
        globals["security"]["encodeBase64"] = EncodeBase64()
    }

    /** @return byteArray as String */
    class SHA256sum : OneArgFunction() {
        override fun call(p0: LuaValue): LuaValue {
            val hashBytes = DigestUtils.sha256(p0.checkjstring())
            return LuaValue.valueOf(hashBytes.toStringRepresentation())
        }
    }

    /** @return byteArray as String */
    class SHA1sum: OneArgFunction() {
        override fun call(p0: LuaValue): LuaValue {
            val hashBytes = DigestUtils.sha1(p0.checkjstring())
            return LuaValue.valueOf(hashBytes.toStringRepresentation())
        }
    }

    /** @return byteArray as String */
    class MD5sum: OneArgFunction() {
        override fun call(p0: LuaValue): LuaValue {
            val hashBytes = DigestUtils.md5(p0.checkjstring())
            return LuaValue.valueOf(hashBytes.toStringRepresentation())
        }
    }

    /** @return byteArray as String */
    class SecureRandomHex: OneArgFunction() {
        override fun call(byteSize: LuaValue): LuaValue {
            val bytes = ByteArray(byteSize.checkint())
            SecureRandom().nextBytes(bytes)

            return LuaValue.valueOf(bytes.toStringRepresentation())
        }
    }

    /** @return String */
    class DecodeBase64: OneArgFunction() {
        override fun call(base64: LuaValue): LuaValue {
            val decodedBytes = Base64.decodeBase64(base64.checkjstring())
            return LuaValue.valueOf(decodedBytes.toStringRepresentation())
        }
    }

    /** @return byteArray as String */
    class EncodeBase64: OneArgFunction() {
        override fun call(inputString: LuaValue): LuaValue {
            val inputBytes = inputString.checkjstring().toByteArray(charset("UTF-8"))
            return LuaValue.valueOf(Base64.encodeBase64(inputBytes).toStringRepresentation())
        }
    }

    companion object {
        val hexLookup = charArrayOf(
                '0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
        )

        fun Byte.toHexString(): String {
            val bInt = this.toUint()
            return "${hexLookup[bInt.shr(8).and(0xf)]}${hexLookup[bInt.and(0xf)]}"
        }

        /** essentially, 0xFC to 0xFC.toChar() */
        fun ByteArray.toStringRepresentation(): String {
            val sb = StringBuilder()
            for (b in this)
                sb.append(b.toChar())
            return sb.toString()
        }
    }
}