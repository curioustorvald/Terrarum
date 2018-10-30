package net.torvald.random;

import java.util.Random;

/**
 * Xoroshiro128
 *
 * Note: low 4 bits are considered "dirty"; avoid these bits for making random set of booleans
 *
 * see https://github.com/SquidPony/SquidLib/blob/master/squidlib-util/src/main/java/squidpony/squidmath/XoRoRNG.java
 */
public class HQRNG extends Random {

    private static final long DOUBLE_MASK = (1L << 53) - 1;
    private static final double NORM_53 = 1. / (1L << 53);
    private static final long FLOAT_MASK = (1L << 24) - 1;
    private static final double NORM_24 = 1. / (1L << 24);

    private static final long serialVersionUID = 1018744536171610262L;

    private long state0, state1;

    public long getState0() {
        return state0;
    }

    public long getState1() {
        return state1;
    }

    /**
     * Creates a new generator seeded using four calls to Math.random().
     */
    public HQRNG() {
        this((long) ((Math.random() - 0.5) * 0x10000000000000L)
                        ^ (long) (((Math.random() - 0.5) * 2.0) * 0x8000000000000000L),
                (long) ((Math.random() - 0.5) * 0x10000000000000L)
                        ^ (long) (((Math.random() - 0.5) * 2.0) * 0x8000000000000000L));
    }
    /**
     * Constructs this XoRoRNG by dispersing the bits of seed using {@link #setSeed(long)} across the two parts of state
     * this has.
     * @param seed a long that won't be used exactly, but will affect both components of state
     */
    public HQRNG(final long seed) {
        setSeed(seed);
    }
    /**
     * Constructs this XoRoRNG by calling {@link #setSeed(long, long)} on the arguments as given; see that method for
     * the specific details (stateA and stateB are kept as-is unless they are both 0).
     * @param stateA the number to use as the first part of the state; this will be 1 instead if both seeds are 0
     * @param stateB the number to use as the second part of the state
     */
    public HQRNG(final long stateA, final long stateB) {
        setSeed(stateA, stateB);
    }

    @Override
    public final int next(int bits) {
        final long s0 = state0;
        long s1 = state1;
        final int result = (int)(s0 + s1) >>> (32 - bits);
        s1 ^= s0;
        state0 = (s0 << 55 | s0 >>> 9) ^ s1 ^ (s1 << 14); // a, b
        state1 = (s1 << 36 | s1 >>> 28); // c
        return result;
    }

    @Override
    public final long nextLong() {
        final long s0 = state0;
        long s1 = state1;
        final long result = s0 + s1;

        s1 ^= s0;
        state0 = (s0 << 55 | s0 >>> 9) ^ s1 ^ (s1 << 14); // a, b
        state1 = (s1 << 36 | s1 >>> 28); // c
        /*
        state0 = Long.rotateLeft(s0, 55) ^ s1 ^ (s1 << 14); // a, b
        state1 = Long.rotateLeft(s1, 36); // c
        */
        return result;
    }

    /**
     * Can return any int, positive or negative, of any size permissible in a 32-bit signed integer.
     * @return any int, all 32 bits are random
     */
    public int nextInt() {
        return (int)nextLong();
    }

    /**
     * Exclusive on the outer bound; the inner bound is 0. The bound may be negative, which will produce a non-positive
     * result.
     * @param bound the outer exclusive bound; may be positive or negative
     * @return a random int between 0 (inclusive) and bound (exclusive)
     */
    public int nextInt(final int bound) {
        return (int) ((bound * (nextLong() >>> 33)) >> 31);
    }
    /**
     * Inclusive lower, exclusive upper.
     * @param inner the inner bound, inclusive, can be positive or negative
     * @param outer the outer bound, exclusive, should be positive, should usually be greater than inner
     * @return a random int that may be equal to inner and will otherwise be between inner and outer
     */
    public int nextInt(final int inner, final int outer) {
        return inner + nextInt(outer - inner);
    }

    /**
     * Exclusive on the outer bound; the inner bound is 0. The bound may be negative, which will produce a non-positive
     * result.
     * @param bound the outer exclusive bound; may be positive or negative
     * @return a random long between 0 (inclusive) and bound (exclusive)
     */
    public long nextLong(long bound) {
        long rand = nextLong();
        final long randLow = rand & 0xFFFFFFFFL;
        final long boundLow = bound & 0xFFFFFFFFL;
        rand >>>= 32;
        bound >>= 32;
        final long z = (randLow * boundLow >> 32);
        long t = rand * boundLow + z;
        final long tLow = t & 0xFFFFFFFFL;
        t >>>= 32;
        return rand * bound + t + (tLow + randLow * bound >> 32) - (z >> 63) - (bound >> 63);
    }
    /**
     * Inclusive inner, exclusive outer; both inner and outer can be positive or negative.
     * @param inner the inner bound, inclusive, can be positive or negative
     * @param outer the outer bound, exclusive, can be positive or negative and may be greater than or less than inner
     * @return a random long that may be equal to inner and will otherwise be between inner and outer
     */
    public long nextLong(final long inner, final long outer) {
        return inner + nextLong(outer - inner);
    }

    public double nextDouble() {
        return (nextLong() & DOUBLE_MASK) * NORM_53;
    }

    public float nextFloat() {
        return (float) ((nextLong() & FLOAT_MASK) * NORM_24);
    }

    public boolean nextBoolean() {
        return nextLong() < 0L;
    }

    public void nextBytes(final byte[] bytes) {
        int i = bytes.length, n = 0;
        while (i != 0) {
            n = Math.min(i, 8);
            for (long bits = nextLong(); n-- != 0; bits >>>= 8) {
                bytes[--i] = (byte) bits;
            }
        }
    }

    /**
     * Sets the seed of this generator using one long, running that through LightRNG's algorithm twice to get the state.
     * @param seed the number to use as the seed
     */
    public void setSeed(final long seed) {

        long state = seed + 0x9E3779B97F4A7C15L,
                z = state;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        state0 = z ^ (z >>> 31);
        state += 0x9E3779B97F4A7C15L;
        z = state;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        state1 = z ^ (z >>> 31);
    }

    /**
     * Sets the seed of this generator using two longs, using them without changes unless both are 0 (then it makes the
     * state variable corresponding to stateA 1 instead).
     * @param stateA the number to use as the first part of the state; this will be 1 instead if both seeds are 0
     * @param stateB the number to use as the second part of the state
     */
    public void setSeed(final long stateA, final long stateB) {

        state0 = stateA;
        state1 = stateB;
        if((stateA | stateB) == 0L)
            state0 = 1L;
    }

}
