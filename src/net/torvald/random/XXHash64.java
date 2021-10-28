package net.torvald.random;

import net.torvald.UnsafeHelper;

/**
 * Code from https://richardstartin.github.io/posts/xxhash
 * https://github.com/richardstartin/xxhash-benchmark
 * Created by minjaesong on 2021-10-28.
 */
public class XXHash64 {

    static final long PRIME64_1 = 0x9E3779B185EBCA87L;
    static final long PRIME64_2 = 0xC2B2AE3D27D4EB4FL;
    static final long PRIME64_3 = 0x165667B19E3779F9L;
    static final long PRIME64_4 = 0x85EBCA77C2B2AE63L;
    static final long PRIME64_5 = 0x27D4EB2F165667C5L;

    public static long hash(byte[] input, long seed) {
        long hash;
        long remaining = input.length;
        int offset = 0;

        if (remaining >= 32) {
            long v1 = seed + PRIME64_1 + PRIME64_2;
            long v2 = seed + PRIME64_2;
            long v3 = seed;
            long v4 = seed - PRIME64_1;

            do {
                v1 += getLong(input, offset) * PRIME64_2;
                v1 = Long.rotateLeft(v1, 31);
                v1 *= PRIME64_1;

                v2 += getLong(input, offset + 8) * PRIME64_2;
                v2 = Long.rotateLeft(v2, 31);
                v2 *= PRIME64_1;

                v3 += getLong(input, offset + 16) * PRIME64_2;
                v3 = Long.rotateLeft(v3, 31);
                v3 *= PRIME64_1;

                v4 += getLong(input, offset + 24) * PRIME64_2;
                v4 = Long.rotateLeft(v4, 31);
                v4 *= PRIME64_1;

                offset += 32;
                remaining -= 32;
            } while (remaining >= 32);

            hash = Long.rotateLeft(v1, 1)
                    + Long.rotateLeft(v2, 7)
                    + Long.rotateLeft(v3, 12)
                    + Long.rotateLeft(v4, 18);

            v1 *= PRIME64_2;
            v1 = Long.rotateLeft(v1, 31);
            v1 *= PRIME64_1;
            hash ^= v1;
            hash = hash * PRIME64_1 + PRIME64_4;

            v2 *= PRIME64_2;
            v2 = Long.rotateLeft(v2, 31);
            v2 *= PRIME64_1;
            hash ^= v2;
            hash = hash * PRIME64_1 + PRIME64_4;

            v3 *= PRIME64_2;
            v3 = Long.rotateLeft(v3, 31);
            v3 *= PRIME64_1;
            hash ^= v3;
            hash = hash * PRIME64_1 + PRIME64_4;

            v4 *= PRIME64_2;
            v4 = Long.rotateLeft(v4, 31);
            v4 *= PRIME64_1;
            hash ^= v4;
            hash = hash * PRIME64_1 + PRIME64_4;
        } else {
            hash = seed + PRIME64_5;
        }

        hash += input.length;

        while (remaining >= 8) {
            long k1 = getLong(input, offset);
            k1 *= PRIME64_2;
            k1 = Long.rotateLeft(k1, 31);
            k1 *= PRIME64_1;
            hash ^= k1;
            hash = Long.rotateLeft(hash, 27) * PRIME64_1 + PRIME64_4;
            offset += 8;
            remaining -= 8;
        }

        if (remaining >= 4) {
            hash ^= getInt(input, offset) * PRIME64_1;
            hash = Long.rotateLeft(hash, 23) * PRIME64_2 + PRIME64_3;
            offset += 4;
            remaining -= 4;
        }

        while (remaining != 0) {
            hash ^= input[offset] * PRIME64_5;
            hash = Long.rotateLeft(hash, 11) * PRIME64_1;
            --remaining;
            ++offset;
        }

        hash ^= hash >>> 33;
        hash *= PRIME64_2;
        hash ^= hash >>> 29;
        hash *= PRIME64_3;
        hash ^= hash >>> 32;
        return hash;
    }

    protected static long getLong(byte[] data, int offset) {
        return UnsafeHelper.INSTANCE.getUnsafe().getLong(data, offset + UnsafeHelper.INSTANCE.getArrayOffset(data));
    }
    protected static int getInt(byte[] data, int offset) {
        return UnsafeHelper.INSTANCE.getUnsafe().getInt(data, offset + UnsafeHelper.INSTANCE.getArrayOffset(data));
    }
}
