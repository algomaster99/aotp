package io.github.chains_project.aotp.oops.klass;

record InstanceClassFlags(short flags, byte status) { }

/**
 * Concrete {@link ClassEntry} representing an instance Klass.
 * https://github.com/openjdk/jdk/blob/62c7e9aefd4320d9d0cd8fa10610f59abb4de670/src/hotspot/share/oops/instanceKlass.hpp#L134
 */
public final class InstanceClass extends ClassEntry {

    public final long annotations;
    public final long packageEntry;
    public final long arrayKlasses;
    public final long constants;
    public final long innerClasses;
    public final long nestMembers;
    public final long nestHost;
    public final long permittedSubclasses;
    public final long recordComponents;
    public final long sourceDebugExtension;
    public final int nonStaticFieldSize;
    public final int staticFieldSize;
    public final int nonStaticOopMapSize;
    public final int itableLen;
    public final short nestHostIndex;
    public final short thisClassIndex;
    public final short staticOopFieldCount;
    public final short idnumAllocatedCount;
    public final byte initState;
    public final byte referenceType;
    public final short accessFlags;
    public final InstanceClassFlags miscFlags_fromInstanceKlass;
    public final long initThread;
    public final long oopMapCache;
    public final long jniIds;
    public final long methodsJmethodIds;
    public final long depContext;
    public final long depContextLastCleaned;
    public final long osrNmethodsHead;
    // INCLUDE_JVMTI
    public final long breakpoints;
    public final long previousVersions;
    public final long cachedClassFile;
    public final long jvmtiCachedClassFieldMap;
    // NOT PRODUCT
    // TODO: confirm if they are not required
    // It is although hard to test them since AOTCache features do exist in DEBUG builds
    // public final int verifyCount;
    // public final int sharedClassLoadCount;

    public final long methods;
    public final long defaultMethods;
    public final long localInterfaces;
    public final long transitiveInterfaces;
    public final long methodOrdering;
    public final long defaultVtableIndices;
    public final long fieldInfoStream;
    public final long fieldInfoSearchTable;
    public final long fieldsStatus;


    private InstanceClass(long vTablePointer,
                          int layoutHelper,
                          short kind,
                          byte miscFlags,
                          int superCheckOffset,
                          long name,
                          long secondarySuperCache,
                          long secondarySupers,
                          long[] primarySupers,
                          long javaMirror,
                          long superKlass,
                          long subklass,
                          long nextSibling,
                          long nextLink,
                          long classLoaderData,
                          long prototypeHeader,
                          long secondarySupersBitmap,
                          byte hashSlot,
                          short sharedClassPathIndex,
                          short aotClassFlags,
                          int vtableLen,
                          int archivedMirrorIndex,
                          long jfrTrace,

                          // Fields from InstanceKlass
                          long annotations,
                          long packageEntry,
                          long arrayKlasses,
                          long constants,
                          long innerClasses,
                          long nestMembers,
                          long nestHost,
                          long permittedSubclasses,
                          long recordComponents,
                          long sourceDebugExtension,
                          int nonStaticFieldSize,
                          int staticFieldSize,
                          int nonStaticOopMapSize,
                          int itableLen,
                          short nestHostIndex,
                          short thisClassIndex,
                          short staticOopFieldCount,
                          short idnumAllocatedCount,
                          byte initState,
                          byte referenceType,
                          short accessFlags,
                          InstanceClassFlags miscFlags_fromInstanceKlass,
                          long initThread,
                          long oopMapCache,
                          long jniIds,
                          long methodsJmethodIds,
                          long depContext,
                          long depContextLastCleaned,
                          long osrNmethodsHead,
                          long breakpoints,
                          long previousVersions,
                          long cachedClassFile,
                          long jvmtiCachedClassFieldMap,
                        //   int verifyCount,
                        //   int sharedClassLoadCount,
                          long methods,
                          long defaultMethods,
                          long localInterfaces,
                          long transitiveInterfaces,
                          long methodOrdering,
                          long defaultVtableIndices,
                          long fieldInfoStream,
                          long fieldInfoSearchTable,
                          long fieldsStatus) {
        super(vTablePointer,
              layoutHelper,
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
              hashSlot,
              sharedClassPathIndex,
              aotClassFlags,
              vtableLen,
              archivedMirrorIndex,
              jfrTrace);
        this.annotations = annotations;
        this.packageEntry = packageEntry;
        this.arrayKlasses = arrayKlasses;
        this.constants = constants;
        this.innerClasses = innerClasses;
        this.nestMembers = nestMembers;
        this.nestHost = nestHost;
        this.permittedSubclasses = permittedSubclasses;
        this.recordComponents = recordComponents;
        this.sourceDebugExtension = sourceDebugExtension;
        this.nonStaticFieldSize = nonStaticFieldSize;
        this.staticFieldSize = staticFieldSize;
        this.nonStaticOopMapSize = nonStaticOopMapSize;
        this.itableLen = itableLen;
        this.nestHostIndex = nestHostIndex;
        this.thisClassIndex = thisClassIndex;
        this.staticOopFieldCount = staticOopFieldCount;
        this.idnumAllocatedCount = idnumAllocatedCount;
        this.initState = initState;
        this.referenceType = referenceType;
        this.accessFlags = accessFlags;
        this.miscFlags_fromInstanceKlass = miscFlags_fromInstanceKlass;
        this.initThread = initThread;
        this.oopMapCache = oopMapCache;
        this.jniIds = jniIds;
        this.methodsJmethodIds = methodsJmethodIds;
        this.depContext = depContext;
        this.depContextLastCleaned = depContextLastCleaned;
        this.osrNmethodsHead = osrNmethodsHead;
        this.breakpoints = breakpoints;
        this.previousVersions = previousVersions;
        this.cachedClassFile = cachedClassFile;
        this.jvmtiCachedClassFieldMap = jvmtiCachedClassFieldMap;
        // this.verifyCount = verifyCount;
        // this.sharedClassLoadCount = sharedClassLoadCount;
        this.methods = methods;
        this.defaultMethods = defaultMethods;
        this.localInterfaces = localInterfaces;
        this.transitiveInterfaces = transitiveInterfaces;
        this.methodOrdering = methodOrdering;
        this.defaultVtableIndices = defaultVtableIndices;
        this.fieldInfoStream = fieldInfoStream;
        this.fieldInfoSearchTable = fieldInfoSearchTable;
        this.fieldsStatus = fieldsStatus;
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
        long[] primarySupers = new long[8];
        for (int i = 0; i < 8; i++) {
            primarySupers[i] = readLongLE(bytes, pos);
            pos += 8;
        }
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
        long secondarySupersBitmap = readLongLE(bytes, pos);
        pos += 8;
        byte hashSlot = (byte) bytes[pos];
        pos += 1;
        short sharedClassPathIndex = readShortLE(bytes, pos);
        pos += 2;        
        short aotClassFlags = readShortLE(bytes, pos);
        pos += 2;

        pos += 3;

        int vtableLen = readIntLE(bytes, pos);
        pos += 4;
        int archivedMirrorIndex = readIntLE(bytes, pos);
        pos += 4;
        long jfrTrace = readLongLE(bytes, pos);
        pos += 8;

        long annotations = readLongLE(bytes, pos);
        pos += 8;
        long packageEntry = readLongLE(bytes, pos);
        pos += 8;
        long arrayKlasses = readLongLE(bytes, pos);
        pos += 8;
        long constants = readLongLE(bytes, pos);
        pos += 8;
        long innerClasses = readLongLE(bytes, pos);
        pos += 8;
        long nestMembers = readLongLE(bytes, pos);
        pos += 8;
        long nestHost = readLongLE(bytes, pos);
        pos += 8;
        long permittedSubclasses = readLongLE(bytes, pos);
        pos += 8;
        long recordComponents = readLongLE(bytes, pos);
        pos += 8;
        long sourceDebugExtension = readLongLE(bytes, pos);
        pos += 8;
        int nonStaticFieldSize = readIntLE(bytes, pos);
        pos += 4;
        int staticFieldSize = readIntLE(bytes, pos);
        pos += 4;
        int nonStaticOopMapSize = readIntLE(bytes, pos);
        pos += 4;
        int itableLen = readIntLE(bytes, pos);
        pos += 4;
        short nestHostIndex = readShortLE(bytes, pos);
        pos += 2;
        short thisClassIndex = readShortLE(bytes, pos);
        pos += 2;
        short staticOopFieldCount = readShortLE(bytes, pos);
        pos += 2;
        short idnumAllocatedCount = readShortLE(bytes, pos);
        pos += 2;
        byte initState = (byte) bytes[pos];
        pos += 1;
        byte referenceType = (byte) bytes[pos];
        pos += 1;
        short accessFlags = readShortLE(bytes, pos);
        pos += 2;
        short flagsForInstanceKlass = readShortLE(bytes, pos);
        pos += 2;
        byte statusForInstanceKlass = (byte) bytes[pos];
        pos += 1;
        InstanceClassFlags miscFlags_fromInstanceKlass = new InstanceClassFlags(flagsForInstanceKlass, statusForInstanceKlass);

        pos += 1;
        long initThread = readLongLE(bytes, pos);
        pos += 8;
        long oopMapCache = readLongLE(bytes, pos);
        pos += 8;
        long jniIds = readLongLE(bytes, pos);
        pos += 8;
        long methodsJmethodIds = readLongLE(bytes, pos);
        pos += 8;
        long depContext = readLongLE(bytes, pos);
        pos += 8;
        long depContextLastCleaned = readLongLE(bytes, pos);
        pos += 8;
        long osrNmethodsHead = readLongLE(bytes, pos);
        pos += 8;
        long breakpoints = readLongLE(bytes, pos);
        pos += 8;
        long previousVersions = readLongLE(bytes, pos);
        pos += 8;
        long cachedClassFile = readLongLE(bytes, pos);
        pos += 8;
        long jvmtiCachedClassFieldMap = readLongLE(bytes, pos);
        pos += 8;
        // int verifyCount = readIntLE(bytes, pos);
        // pos += 4;
        // int sharedClassLoadCount = readIntLE(bytes, pos);
        // pos += 4;
        long methods = readLongLE(bytes, pos);
        pos += 8;
        long defaultMethods = readLongLE(bytes, pos);
        pos += 8;
        long localInterfaces = readLongLE(bytes, pos);
        pos += 8;
        long transitiveInterfaces = readLongLE(bytes, pos);
        pos += 8;
        long methodOrdering = readLongLE(bytes, pos);
        pos += 8;
        long defaultVtableIndices = readLongLE(bytes, pos);
        pos += 8;
        long fieldInfoStream = readLongLE(bytes, pos);
        pos += 8;
        long fieldInfoSearchTable = readLongLE(bytes, pos);
        pos += 8;
        long fieldsStatus = readLongLE(bytes, pos);
        pos += 8;

        return new InstanceClass(vTablePointer,
                                 layoutHelper,
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
                                 hashSlot,
                                 sharedClassPathIndex,
                                 aotClassFlags,
                                 vtableLen,
                                 archivedMirrorIndex,
                                 jfrTrace,
                                 annotations,
                                 packageEntry,
                                 arrayKlasses,
                                 constants,
                                 innerClasses,
                                 nestMembers,
                                 nestHost,
                                 permittedSubclasses,
                                 recordComponents,
                                 sourceDebugExtension,
                                 nonStaticFieldSize,
                                 staticFieldSize,
                                 nonStaticOopMapSize,
                                 itableLen,
                                 nestHostIndex,
                                 thisClassIndex,
                                 staticOopFieldCount,
                                 idnumAllocatedCount,
                                 initState,
                                 referenceType,
                                 accessFlags,
                                 miscFlags_fromInstanceKlass,
                                 initThread,
                                 oopMapCache,
                                 jniIds,
                                 methodsJmethodIds,
                                 depContext,
                                 depContextLastCleaned,
                                 osrNmethodsHead,
                                 breakpoints,
                                 previousVersions,
                                 cachedClassFile,
                                 jvmtiCachedClassFieldMap,
                                //  verifyCount,
                                //  sharedClassLoadCount,
                                 methods,
                                 defaultMethods,
                                 localInterfaces,
                                 transitiveInterfaces,
                                 methodOrdering,
                                 defaultVtableIndices,
                                 fieldInfoStream,
                                 fieldInfoSearchTable,
                                 fieldsStatus);
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

