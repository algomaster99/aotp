package io.github.chains_project.aotp;

/**
 * Concrete {@link ClassEntry} representing an instance Klass.
 * https://github.com/openjdk/jdk/blob/62c7e9aefd4320d9d0cd8fa10610f59abb4de670/src/hotspot/share/oops/instanceKlass.hpp#L134
 */
public final class InstanceClass extends ClassEntry {

    private InstanceClass(int layoutHelper,
                          short kind,
                          byte miscFlags,
                          int superCheckOffset,
                          long name,
                          long secondarySuperCache,
                          long secondarySupers,
                          long primarySupers,
                          long javaMirror,
                          long superKlass,
                          long subklass,
                          long nextSibling,
                          long nextLink,
                          long classLoaderData,
                          long prototypeHeader,
                          int secondarySupersBitmap,
                          byte hashSlot) {
        super(layoutHelper,
              kind,
              miscFlags,
              superCheckOffset,
              name,
              secondarySuperCache,
              secondarySupers,
              primarySupers,
              javaMirror,
              superKlass,
              subklass,
              nextSibling,
              nextLink,
              classLoaderData,
              prototypeHeader,
              secondarySupersBitmap,
              hashSlot);
    }

    /**
     * Parse a single InstanceClass from region bytes at {@code offset}. The first
     * field at this offset is {@code layoutHelper}. There is a 4-byte padding
     * before {@code superCheckOffset}, as per the current AOT layout.
     */
    public static InstanceClass parse(byte[] bytes, int offset) {
        int pos = offset;

        // created by the compiler to support dynamic polymorphism
        long vTablePointer = readLongLE(bytes, pos);
        pos += 8;

        int layoutHelper = readIntLE(bytes, pos);
        pos += 4;
        short kind = readShortLE(bytes, pos);
        pos += 2;
        byte miscFlags = (byte) bytes[pos];
        pos += 1;

        // 1-byte padding
        pos += 1;
        
        int superCheckOffset = readIntLE(bytes, pos);
        pos += 4;
        // 4-byte padding after superCheckOffset
        pos += 4;

        long name = readLongLE(bytes, pos);
        pos += 8;
        long secondarySuperCache = readLongLE(bytes, pos);
        pos += 8;
        long secondarySupers = readLongLE(bytes, pos);
        pos += 8;
        long primarySupers = readLongLE(bytes, pos);
        pos += 8;
        long javaMirror = readLongLE(bytes, pos);
        pos += 8;
        long superKlass = readLongLE(bytes, pos);
        pos += 8;
        long subklass = readLongLE(bytes, pos);
        pos += 8;
        long nextSibling = readLongLE(bytes, pos);
        pos += 8;
        long nextLink = readLongLE(bytes, pos);
        pos += 8;
        long classLoaderData = readLongLE(bytes, pos);
        pos += 8;
        long prototypeHeader = readLongLE(bytes, pos);
        pos += 8;
        int secondarySupersBitmap = readIntLE(bytes, pos);
        pos += 4;
        byte hashSlot = (byte) bytes[pos];
        pos += 1;

        return new InstanceClass(layoutHelper,
                                 kind,
                                 miscFlags,
                                 superCheckOffset,
                                 name,
                                 secondarySuperCache,
                                 secondarySupers,
                                 primarySupers,
                                 javaMirror,
                                 superKlass,
                                 subklass,
                                 nextSibling,
                                 nextLink,
                                 classLoaderData,
                                 prototypeHeader,
                                 secondarySupersBitmap,
                                 hashSlot);
    }

    private static short readShortLE(byte[] bytes, int offset) {
        return (short) ((bytes[offset] & 0xFF)
             | ((bytes[offset + 1] & 0xFF) << 8));
    }

    private static int readIntLE(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
             | ((bytes[offset + 1] & 0xFF) << 8)
             | ((bytes[offset + 2] & 0xFF) << 16)
             | ((bytes[offset + 3] & 0xFF) << 24);
    }

    private static long readLongLE(byte[] bytes, int offset) {
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

