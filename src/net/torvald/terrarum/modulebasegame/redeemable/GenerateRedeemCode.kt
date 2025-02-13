package net.torvald.terrarum.modulebasegame.redeemable

import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.serialise.toBig64
import net.torvald.terrarum.toInt
import net.torvald.terrarum.utils.PasswordBase32
import java.util.*

/**
 * Created by minjaesong on 2025-02-13.
 */
object GenerateRedeemCode {

    private val itemTypeTable = hashMapOf(
        "" to 0,
        "wall" to 1,
        "item" to 2,
        "wire" to 3
    )

    private val moduleTable = hashMapOf(
        "basegame" to 0,
        "dwarventech" to 1,
    )

    operator fun invoke(itemID: ItemID, amountIndex: Int, isUnique: Boolean, receiver: UUID? = null, msgType: Int = 0, arg1: String = "", arg2: String = ""): String {
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

        val bytes = ByteArray(240)

        // sync pattern and flags
        bytes[0] = (isUnique.toInt() or (receiver != null).toInt(1) or 0xA0).toByte()
        bytes[1] = 0xA5.toByte()
        // compressed item name
        bytes[2] = (itemTypeTable[itemType]!! or moduleTable[itemModule]!!.shl(2) or amountIndex.and(15).shl(4)).toByte() // 0b nnnn mm cc
        bytes[3] = itemNumber.toByte() // 0b item number low
        bytes[4] = itemNumber.ushr(8).toByte()// 0b item number high

        // TODO ascii to baudot

        return PasswordBase32.encode(bytes, receiver?.toByteArray() ?: ByteArray(0))
    }

    private fun UUID.toByteArray(): ByteArray {
        return this.mostSignificantBits.toBig64() + this.leastSignificantBits.toBig64()
    }

}