package net.torvald.terrarum.utils

import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Created by minjaesong on 2023-02-05.
 */
class BloomFilter(capacity: Int, k: Int) {
    private var set: ByteArray

    private var keySize = 0
    private var setSize: Int = 0

    private var empty = true

    private var md: MessageDigest? = null

    init {
        setSize = capacity
        set = ByteArray(setSize)
        keySize = k
        empty = true
        md = try {
            MessageDigest.getInstance("MD5")
        }
        catch (e: NoSuchAlgorithmException) {
            throw IllegalArgumentException("Error : MD5 Hash not found")
        }
    }

    fun makeEmpty() {
        set = ByteArray(setSize)
        empty = true
        md = try {
            MessageDigest.getInstance("MD5")
        }
        catch (e: NoSuchAlgorithmException) {
            throw IllegalArgumentException("Error : MD5 Hash not found")
        }
    }

    fun restoreFromByteArray(oldSet: ByteArray) {
        if (this.set.size != oldSet.size)
            throw IllegalStateException("Cannot restore from bytes: expected ${this.set.size} byte-set, got ${oldSet.size} instead")

        this.set = oldSet

        empty = set.all { it == 0.toByte() }
    }

    fun isEmpty() = empty

    private fun getHash(i: Int): Int {
        md!!.reset()
        val bytes: ByteArray = ByteBuffer.allocate(4).putInt(i).array()
        md!!.update(bytes, 0, bytes.size)
        return Math.abs(BigInteger(1, md!!.digest()).toInt()) % (set.size - 1)
    }

    fun add(obj: Int) {
        val tmpset = getSetArray(obj)
        for (i in tmpset) set[i] = 1
        empty = false
    }

    operator fun contains(obj: Int): Boolean {
        val tmpset = getSetArray(obj)
        for (i in tmpset) if (set[i].toInt() != 1) return false
        return true
    }

    private fun getSetArray(obj: Int): IntArray {
        val tmpset = IntArray(keySize)
        tmpset[0] = getHash(obj)
        for (i in 1 until keySize) tmpset[i] = getHash(tmpset[i - 1])
        return tmpset
    }

    fun serialise(): ByteArray {
        val returningSet = ByteArray(setSize)
        System.arraycopy(set, 0, returningSet, 0, setSize)
        return returningSet
    }
}