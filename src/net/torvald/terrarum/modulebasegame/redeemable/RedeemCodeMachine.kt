package net.torvald.terrarum.modulebasegame.redeemable

import javazoom.jl.decoder.Crc16
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.serialise.toBig64
import net.torvald.terrarum.serialise.toUint
import net.torvald.terrarum.toInt
import net.torvald.terrarum.utils.PasswordBase32
import net.torvald.unicode.CURRENCY
import java.security.MessageDigest
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.xor

/**
 * Created by minjaesong on 2025-02-13.
 */
object RedeemCodeMachine {

    private val itemTypeTable = hashMapOf(
        "" to 0,
        "wall" to 1,
        "item" to 2,
        "wire" to 3
    )

    private val itemTypeTableReverse = itemTypeTable.entries.associate { it.value to it.key }

    private val moduleTable = hashMapOf(
        "basegame" to 0,
        "dwarventech" to 1,
    )

    private val moduleTableReverse = moduleTable.entries.associate { it.value to it.key }

    private val amountIndexToAmount = intArrayOf(1,2,3,4,5,6,10,12,15,20,24,32,50,100,250,500)

    val initialPassword = listOf( // will be list of 256 bits of something
        "N'Gasta! Kvaka! Kvakis! ahkstas ",
        "so novajxletero (oix jhemile) so",
        "Ranetauw. Ricevas gxin pagintaj ",
        "membrauw kaj aliaj individuauw, ",
        "kiujn iamaniere tusxas so raneta",
        " aktivado. En gxi aperas informa",
        "uw unuavice pri so lokauw so cxi",
        "umonataj kunvenauw, sed nature a"
    ).map { MessageDigest.getInstance("SHA-256").digest(it.toByteArray()) }

    fun encode(itemID: ItemID, amountIndex: Int, isUnique: Boolean, receiver: UUID? = null, msgType: Int = 0, args: String = ""): String {
        // filter item ID
        val itemType = itemID.substringBefore("@")
        val (itemModule, itemNumber0) = itemID.substringAfter("@").split(":")
        val itemNumber = itemNumber0.toInt()

        if (itemType !in itemTypeTable.keys)
            throw IllegalArgumentException("Unsupported type for ItemID: $itemID")
        if (itemModule !in moduleTable.keys)
            throw IllegalArgumentException("Unsupported module for ItemID: $itemID")
        if (itemNumber !in 0..65535)
            throw IllegalArgumentException("Unsupported item number for ItemID: $itemID")

        val isShortCode = true

        val bytes = ByteArray(isShortCode.toInt().plus(1) * 15)

        // sync pattern and flags
        bytes[0] = (isUnique.toInt() or 0xA4).toByte()
        bytes[1] = 0xA5.toByte()
        // compressed item name
        // 0b nnnn mm cc
        bytes[2] = (itemTypeTable[itemType]!! or moduleTable[itemModule]!!.shl(2) or amountIndex.and(15).shl(4)).toByte()
        bytes[3] = itemNumber.toByte() // 0b item number low
        bytes[4] = itemNumber.ushr(8).toByte()// 0b item number high

        // convert ascii to baudot
        val paddedTextBits = (asciiToBaudot(args) + "00000").let { // always end the message with NUL
            // then fill the remainder with random bits
            val remaining = (if (isShortCode) 60 else 180) - it.length
            it + StringBuilder().let { sb ->
                repeat(remaining) { sb.append((Math.random() < 0.5).toInt()) }
            }.toString()
        }

        // fill upper nybble of bytes[5] first
        // lower nybble will have msgType
        bytes[5] = ((msgType and 15) or (paddedTextBits.substring(0, 4).toInt(2).shl(4))).toByte()

        var c = 4
        for (i in 6 until bytes.size - 2) {
            bytes[i] = paddedTextBits.substring(c, c + 8).toInt(2).toByte()
            c += 8
        }

        val crc16 = Crc16().let {
            for (i in 0 until bytes.size - 2) {
                it.add_bits(bytes[i].toInt(), 8)
            }
            it.checksum().toInt()
        }

        bytes[bytes.size - 2] = crc16.ushr(8).toByte()
        bytes[bytes.size - 1] = crc16.toByte()


        val basePwd = initialPassword[bytes[bytes.size - 1].toUint().shr(2).and(7)]
        val receiverPwd = receiver?.toByteArray() ?: ByteArray(16) // 128 bits of something

        // xor basePWD with receiverPwd
        for (i in 0 until 32) {
            basePwd[i] = basePwd[i] xor receiverPwd[i % 16]
        }

        return PasswordBase32.encode(bytes, basePwd)
    }

    private fun UUID.toByteArray(): ByteArray {
        return this.mostSignificantBits.toBig64() + this.leastSignificantBits.toBig64()
    }

    fun decode(codeStr: String, decoderUUID: UUID? = null): RedeemVoucher? {
        val receiverPwd = decoderUUID?.toByteArray() ?: ByteArray(16) // 128 bits of something

        // 0x [byte 29] [byte 30]
        val crc = PasswordBase32.decode(codeStr.substring(codeStr.length - 8), 5).let {
            it[3].toUint().shl(8) or it[4].toUint()
        }

        val basePwdIndex = crc.shr(2).and(7)

        val password = initialPassword[basePwdIndex].let { basePwd ->
            ByteArray(32) { i ->
                basePwd[i] xor receiverPwd[i % 16]
            }
        }

        // try to decode the input string
        val decoded = PasswordBase32.decode(codeStr, if (codeStr.length > 24) 30 else 15, password)

        // check CRC
        val crc2 = decoded[decoded.size - 2].toUint().shl(8) or decoded[decoded.size - 1].toUint()

        // if CRC fails...
        if (crc != crc2)
            return null


        val reusable = (decoded[0] and 1) != 0.toByte()

        val itemCategory = itemTypeTableReverse[decoded[2].toUint() and 3]!!
        val itemModule = moduleTableReverse[decoded[2].toUint().ushr(2) and 3]!!
        val itemAmount = amountIndexToAmount[decoded[2].toUint().ushr(4)]
        val itemNumber = decoded[3].toUint() or decoded[4].toUint().shl(8)

        val itemID = ("$itemModule:$itemNumber").let { if (itemCategory.isNotBlank()) "$itemCategory@$it" else it }

        val messageTemplateIndex = decoded[5].toUint() and 15

        // baudot to ascii
        val baudotBits = StringBuilder().let {
            it.append(decoded[5].toUint().ushr(4).toString(2).padStart(4,'0'))
            for (i in 6 until decoded.size - 2) {
                it.append(decoded[i].toUint().toString(2).padStart(8,'0'))
            }
        }.toString()
        val decodedStr = baudotToAscii(baudotBits).substringBefore('\u0000') // remove trailing random bits

        return RedeemVoucher(itemID, itemAmount, reusable, messageTemplateIndex, decodedStr.split('\n'))
    }

    data class RedeemVoucher(val itemID: ItemID, val amount: Int, val reusable: Boolean, val msgTemplateIndex: Int, val args: List<String>)


    val tableLtrs = "\u0000E\nA SIU\rDRJNFCKTZLWHYPQOBG\u000FMXV\u000E"
    val tableFigs = "\u00003\n- '87\r\u00054\u0007,!:(5+)2${CURRENCY}6019?&\u000F./;\u000E"

    val setLtrs = (tableLtrs).toHashSet()
    val setFigs = tableFigs.toHashSet()

    val codeLtrs = tableLtrs.mapIndexed { index: Int, c: Char -> c to index.toString(2).padStart(5,'0') }.toMap()
    val codeFigs = tableFigs.mapIndexed { index: Int, c: Char -> c to index.toString(2).padStart(5,'0') }.toMap()

    val basket = arrayOf(codeLtrs, codeFigs) // think of it as a "type basket" of a typewriter
    val basket2 = arrayOf(tableLtrs, tableFigs) // think of it as a "type basket" of a typewriter

    private fun isInLtrs(char: Char) = char in setLtrs
    private fun isInFigs(char: Char) = char in setFigs
    private fun nextShift(char: Char) = if (isInLtrs(char)) 0 else if (isInFigs(char)) 1 else null

    val shiftCode = arrayOf("11111", "11011")

    private fun asciiToBaudot(instr: String): String {
        val sb = StringBuilder()

        var currentShift = 0 // initially ltrs
        var cursor = 0

        while (cursor < instr.length) {
            val char = instr[cursor].uppercaseChar()
            val nextShift = nextShift(char)

            // decide if a shift must prepend
            if (nextShift != null && currentShift != nextShift) {
                sb.append(shiftCode[nextShift])
                currentShift = nextShift
            }

            // append baudot code into the string builder
            if (nextShift != null) {
                sb.append(basket[currentShift][char])
            }

            // advance to the next character
            cursor += 1
        }

        return sb.toString()
    }

    private fun baudotToAscii(binaryStr: String): String {
        val sb = StringBuilder()

        var currentShift = 0 // initially ltrs
        var cursor = 0

        while (cursor < binaryStr.length) {
            val number = binaryStr.substring(cursor, cursor + 5).toInt(2)

            if (number == 0b11011)
                currentShift = 1
            else if (number == 0b11111)
                currentShift = 0
            else
                sb.append(basket2[currentShift][number])

            cursor += 5
        }

        return sb.toString()
    }

}