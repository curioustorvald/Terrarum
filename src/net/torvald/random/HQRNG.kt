package net.torvald.random

import java.util.Random

//import java.util.concurrent.locks.*;

/**
 * This class implements a better random number generator than the standard LCG that is implemented in java.util.Random.
 * It is based on [Numerical Recipes: The Art of Scientific Computing](http://www.amazon.com/gp/product/0521880688?ie=UTF8&tag=javamex-20&linkCode=as2&camp=1789&creative=9325&creativeASIN=0521880688),
 * and gives a good compromise between quality and speed. It is a combined generator: two XORShift generators are combined with an LCG and a multiply with carry generator.
 * (Without going into all the details here, notice the two blocks of three shifts each, which are the XORShifts; the first line which is the LCG, similar to the standard
 * Java Random algorithm, and the line between the two XORShifts, which is a multiply with carry generator.)
 * Note that this version is **not** thread-safe. In order to make it thread-safe, uncomment the lock-related lines. It is also **not** cryptographically secure, like the java.security.SecureRandom class.
 * @author Numerical Recipes
 */

class HQRNG @JvmOverloads constructor(seed: Long = System.nanoTime()) : Random() {

    //private Lock l = new ReentrantLock();
    private var u: Long = 0
    private var v = 4101842887655102017L
    private var w: Long = 1

    init {
        //l.lock();
        u = seed xor v
        nextLong()
        v = u
        nextLong()
        w = v
        nextLong()
        //l.unlock();
    }

    override fun nextLong(): Long {
        //    l.lock();
        try {
            u = u * 2862933555777941757L + 7046029254386353087L
            v = v xor v.ushr(17)
            v = v xor v.shl(31)
            v = v xor v.ushr(8)
            w = 4294957665L * w.and(0xffffffffL) + w.ushr(32)
            var x = u xor u.shl(21)
            x = x xor x.ushr(35)
            x = x xor x.shl(4)
            return x + v xor w
        }
        finally {
            //l.unlock();
        }
    }

    override fun next(bits: Int): Int {
        return nextLong().ushr(64 - bits).toInt()
    }

}