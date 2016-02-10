package com.Torvald.Rand;

import java.util.Random;
//import java.util.concurrent.locks.*;

/**
 * This class implements a better random number generator than the standard LCG that is implemented in java.util.Random.
 * It is based on <a href="http://www.amazon.com/gp/product/0521880688?ie=UTF8&tag=javamex-20&linkCode=as2&camp=1789&creative=9325&creativeASIN=0521880688">Numerical Recipes: The Art of Scientific Computing</a>,
 * and gives a good compromise between quality and speed. It is a combined generator: two XORShift generators are combined with an LCG and a multiply with carry generator.
 * (Without going into all the details here, notice the two blocks of three shifts each, which are the XORShifts; the first line which is the LCG, similar to the standard
 * Java Random algorithm, and the line between the two XORShifts, which is a multiply with carry generator.)
 * Note that this version is <b>not</b> thread-safe. In order to make it thread-safe, uncomment the lock-related lines. It is also <b>not</b> cryptographically secure, like the java.security.SecureRandom class.
 * @author Numerical Recipes
 */

public class HQRNG extends Random {

    //private Lock l = new ReentrantLock();
    private long u;
    private long v = 4101842887655102017L;
    private long w = 1;

    public HQRNG() {
        this(System.nanoTime());
    }
    public HQRNG(long seed) {
        //l.lock();
        u = seed ^ v;
        nextLong();
        v = u;
        nextLong();
        w = v;
        nextLong();
        //l.unlock();
    }

    public long nextLong() {
        //    l.lock();
        try {
            u = u * 2862933555777941757L + 7046029254386353087L;
            v ^= v >>> 17;
            v ^= v << 31;
            v ^= v >>> 8;
            w = 4294957665L * (w & 0xffffffff) + (w >>> 32);
            long x = u ^ (u << 21);
            x ^= x >>> 35;
            x ^= x << 4;
            long ret = (x + v) ^ w;
            return ret;
        } finally {
            //l.unlock();
        }
    }

    protected int next(int bits) {
        return (int) (nextLong() >>> (64-bits));
    }

}