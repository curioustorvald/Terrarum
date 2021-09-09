package net.torvald.random;

import net.torvald.UnsafeHelper;

/**
 * Code from https://richardstartin.github.io/posts/xxhash
 * https://github.com/richardstartin/xxhash-benchmark
 * Created by minjaesong on 2020-02-24
 */

public class XXHash32 {

    static final int PRIME1 = 0x9E3779B1;
    static final int PRIME2 = 0x85EBCA77;
    static final int PRIME3 = 0xC2B2AE3D;
    static final int PRIME4 = 0x27D4EB2F;
    static final int PRIME5 = 0x165667B1;

    public static int hashGeoCoord(int x, int y) {
        int p = ((x & 65535) << 16) | (y & 65535);
        return hash(new byte[]{(byte) p, (byte)(p >>> 8), (byte)(p >>> 16), (byte)(p >>> 24)}, 10000);
    }

    public static int hash(byte[] data, int seed) {
        int end = data.length;
        int offset = 0;
        int h32;
        if (data.length >= 16) {
            int limit = end - 16;
            int v1 = seed + PRIME1 + PRIME2;
            int v2 = seed + PRIME2;
            int v3 = seed;
            int v4 = seed - PRIME1;

            do {
                v1 += getInt(data, offset) * PRIME2;
                v1 = Integer.rotateLeft(v1, 13);
                v1 *= PRIME1;
                offset += 4;
                v2 += getInt(data, offset) * PRIME2;
                v2 = Integer.rotateLeft(v2, 13);
                v2 *= PRIME1;
                offset += 4;
                v3 += getInt(data, offset) * PRIME2;
                v3 = Integer.rotateLeft(v3, 13);
                v3 *= PRIME1;
                offset += 4;
                v4 += getInt(data, offset) * PRIME2;
                v4 = Integer.rotateLeft(v4, 13);
                v4 *= PRIME1;
                offset += 4;
            } while(offset <= limit);

            h32 = Integer.rotateLeft(v1, 1) + Integer.rotateLeft(v2, 7) + Integer.rotateLeft(v3, 12) + Integer.rotateLeft(v4, 18);
        } else {
            h32 = seed + PRIME5;
        }

        for(h32 += data.length; offset <= end - 4; offset += 4) {
            h32 += getInt(data, offset) * PRIME3;
            h32 = Integer.rotateLeft(h32, 17) * PRIME4;
        }

        while(offset < end) {
            h32 += (data[offset] & 255) * PRIME5;
            h32 = Integer.rotateLeft(h32, 11) * PRIME1;
            ++offset;
        }

        h32 ^= h32 >>> 15;
        h32 *= PRIME2;
        h32 ^= h32 >>> 13;
        h32 *= PRIME3;
        h32 ^= h32 >>> 16;
        return h32;
    }

    protected static int getInt(byte[] data, int offset) {
        return UnsafeHelper.INSTANCE.getUnsafe().getInt(data, offset + UnsafeHelper.INSTANCE.getArrayOffset(data));
    }
}