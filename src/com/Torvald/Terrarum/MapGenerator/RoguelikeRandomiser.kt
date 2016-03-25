package com.torvald.terrarum.mapgenerator

import com.torvald.colourutil.Col4096
import com.torvald.random.HQRNG
import java.util.*

/**
 * Created by minjaesong on 16-02-23.
 */
object RoguelikeRandomiser {

    private val POTION_PRIMARY_COLSET = intArrayOf(15, 15, 7, 7, 0, 0)

    private var potionColours: HashMap<Int, Col4096> = HashMap()
    private var coloursDiscovered: HashMap<Col4096, Boolean> = HashMap()

    val coloursTaken: ArrayList<Col4096> = ArrayList()

    private var seed: Long? = null
    private val random: Random = HQRNG()

    private val POTION_HEAL_TIER1 = 0x00
    private val POTION_HEAL_TIRE2 = 0x01

    private val POTION_MAGIC_REGEN_TIER1 = 0x10

    private val POTION_BERSERK_TIER1 = 0x20


    @JvmStatic
    fun setSeed(seed: Long) {
        this.seed = seed
    }

    @JvmStatic
    fun setupColours() {

    }


    fun getGeneratorSeed(): Long {
        return seed!!
    }
}