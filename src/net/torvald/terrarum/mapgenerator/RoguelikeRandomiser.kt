package net.torvald.terrarum.mapgenerator

import net.torvald.IntArrayStack
import net.torvald.colourutil.Col4096
import net.torvald.random.HQRNG
import org.newdawn.slick.Color
import java.util.*

/**
 * Created by minjaesong on 16-02-23.
 */
object RoguelikeRandomiser {

    val POTION_PRIMARY_COLSET = intArrayOf(15, 15, 7, 7, 0, 0)

    var potionColours: HashMap<Int, Col4096> = HashMap()
    var coloursDiscovered: HashMap<Col4096, Boolean> = HashMap()

    val coloursTaken: ArrayList<Col4096> = ArrayList()

    var seed: Long = 0
    private val random: Random = HQRNG()

    private val POTION_HEAL_TIER1 = 0x00
    private val POTION_HEAL_TIRE2 = 0x01

    private val POTION_MAGIC_REGEN_TIER1 = 0x10

    private val POTION_BERSERK_TIER1 = 0x20



    fun setupColours() {

    }

    /**
     * For external classes/objects, does not touch COLOUR SET
     * @param arr Array of Int(0-15)
     */
    fun composeColourFrom(arr: IntArray): Color {
        val colourElements = arr.copyOf()
        shuffleArrayInt(colourElements, HQRNG())

        val colourStack = IntArrayStack(colourElements)

        return Col4096(colourStack.pop(),
                       colourStack.pop(),
                       colourStack.pop())
                .toSlickColour()
    }



    fun shuffleArrayInt(ar: IntArray, rnd: Random) {
        for (i in ar.size - 1 downTo 0) {
            val index = rnd.nextInt(i + 1);
            // Simple swap
            val a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }
}