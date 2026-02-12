package io.github.chains_project.aotp.utils;

/**
 * Little-endian readers for primitive types from byte arrays.
 * Used when parsing AOT/CDS region bytes (format is little-endian).
 */
public final class ByteReader {

    private ByteReader() {}

    public static short readShortLE(byte[] bytes, int offset) {
        int lo = bytes[offset] & 0xFF;
        int hi = bytes[offset + 1] & 0xFF;
        return (short) (lo | (hi << 8));
    }

    public static int readIntLE(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
             | ((bytes[offset + 1] & 0xFF) << 8)
             | ((bytes[offset + 2] & 0xFF) << 16)
             | ((bytes[offset + 3] & 0xFF) << 24);
    }

    public static long readLongLE(byte[] bytes, int offset) {
        return ((long) bytes[offset] & 0xFF)
             | (((long) bytes[offset + 1] & 0xFF) << 8)
             | (((long) bytes[offset + 2] & 0xFF) << 16)
             | (((long) bytes[offset + 3] & 0xFF) << 24)
             | (((long) bytes[offset + 4] & 0xFF) << 32)
             | (((long) bytes[offset + 5] & 0xFF) << 40)
             | (((long) bytes[offset + 6] & 0xFF) << 48)
             | (((long) bytes[offset + 7] & 0xFF) << 56);
    }
}
