package io.github.chains_project.aotp.oops.klass;

import io.github.chains_project.aotp.utils.ByteReader;

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
        long vTablePointer = ByteReader.readLongLE(bytes, pos);
        pos += 8;

        int layoutHelper = ByteReader.readIntLE(bytes, pos);
        pos += 4;
        short kind = ByteReader.readShortLE(bytes, pos);
        pos += 2;
        byte miscFlags = (byte) bytes[pos];
        pos += 1;

        // 1-byte padding
        pos += 1;
        
        int superCheckOffset = ByteReader.readIntLE(bytes, pos);
        pos += 4;
        // 4-byte padding after superCheckOffset
        pos += 4;

        long name = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long secondarySuperCache = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long secondarySupers = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long[] primarySupers = new long[8];
        for (int i = 0; i < 8; i++) {
            primarySupers[i] = ByteReader.readLongLE(bytes, pos);
            pos += 8;
        }
        long javaMirror = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long superKlass = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long subklass = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long nextSibling = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long nextLink = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long classLoaderData = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long prototypeHeader = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long secondarySupersBitmap = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        byte hashSlot = (byte) bytes[pos];
        pos += 1;
        short sharedClassPathIndex = ByteReader.readShortLE(bytes, pos);
        pos += 2;        
        short aotClassFlags = ByteReader.readShortLE(bytes, pos);
        pos += 2;

        pos += 3;

        int vtableLen = ByteReader.readIntLE(bytes, pos);
        pos += 4;
        int archivedMirrorIndex = ByteReader.readIntLE(bytes, pos);
        pos += 4;
        long jfrTrace = ByteReader.readLongLE(bytes, pos);
        pos += 8;

        long annotations = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long packageEntry = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long arrayKlasses = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long constants = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long innerClasses = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long nestMembers = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long nestHost = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long permittedSubclasses = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long recordComponents = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long sourceDebugExtension = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        int nonStaticFieldSize = ByteReader.readIntLE(bytes, pos);
        pos += 4;
        int staticFieldSize = ByteReader.readIntLE(bytes, pos);
        pos += 4;
        int nonStaticOopMapSize = ByteReader.readIntLE(bytes, pos);
        pos += 4;
        int itableLen = ByteReader.readIntLE(bytes, pos);
        pos += 4;
        short nestHostIndex = ByteReader.readShortLE(bytes, pos);
        pos += 2;
        short thisClassIndex = ByteReader.readShortLE(bytes, pos);
        pos += 2;
        short staticOopFieldCount = ByteReader.readShortLE(bytes, pos);
        pos += 2;
        short idnumAllocatedCount = ByteReader.readShortLE(bytes, pos);
        pos += 2;
        byte initState = (byte) bytes[pos];
        pos += 1;
        byte referenceType = (byte) bytes[pos];
        pos += 1;
        short accessFlags = ByteReader.readShortLE(bytes, pos);
        pos += 2;
        short flagsForInstanceKlass = ByteReader.readShortLE(bytes, pos);
        pos += 2;
        byte statusForInstanceKlass = (byte) bytes[pos];
        pos += 1;
        InstanceClassFlags miscFlags_fromInstanceKlass = new InstanceClassFlags(flagsForInstanceKlass, statusForInstanceKlass);

        pos += 1;
        long initThread = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long oopMapCache = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long jniIds = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long methodsJmethodIds = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long depContext = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long depContextLastCleaned = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long osrNmethodsHead = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long breakpoints = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long previousVersions = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long cachedClassFile = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long jvmtiCachedClassFieldMap = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        // int verifyCount = readIntLE(bytes, pos);
        // pos += 4;
        // int sharedClassLoadCount = readIntLE(bytes, pos);
        // pos += 4;
        long methods = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long defaultMethods = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long localInterfaces = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long transitiveInterfaces = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long methodOrdering = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long defaultVtableIndices = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long fieldInfoStream = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long fieldInfoSearchTable = ByteReader.readLongLE(bytes, pos);
        pos += 8;
        long fieldsStatus = ByteReader.readLongLE(bytes, pos);
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
}

