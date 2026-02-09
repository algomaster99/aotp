package io.github.chains_project.aotp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

record HeapRootSegments(
    long baseOffset,
    long count,
    int rootsCount,
    long maxSizeInBytes,
    int maxSizeInElems
) {}

/**
 * Size = 48 bytes
 */
record ArchiveMappedHeapHeader(
    long ptrmapStartPos,
    long oopmapStartPos,
    HeapRootSegments heapRootSegments
) {}
/**
 * Size = 40 bytes
 */
record ArchiveStreamedHeapHeader(
    long forwardingOffset,
    long rootsOffset,
    long rootHighestObjectIndexTableOffset,
    long numRoots,
    long numArchivedObjects
) {}

// https://github.com/openjdk/jdk/blob/f4607ed0a7ea2504c1d72dd3dab0b21e583fa0e7/src/hotspot/share/cds/filemap.hpp#L102
public class FileMapHeader {
    long coreRegionAlignment;
    int objAlignment;
    long narrowOopBase;
    int narrowOopShift;
    boolean compactStrings;
    boolean compactHeaders;
    long maxHeapSize;
    int narrowOopMode;
    boolean objectStreamingMode;
    boolean compressedOops;
    boolean compressedClassPointers;
    int narrowKlassPointerBits;
    int narrowKlassShift;
    long clonedVtablesOffset;
    long earlySerializedDataOffset;
    long serializedDataOffset;
    String jvmIdent;
    long classLocationConfigOffset;
    boolean verifyLocal;
    boolean verifyRemote;
    boolean hasPlatformOrAppClasses;
    long requestedBaseAddress;
    long mappedBaseAddress;
    boolean useOptimizedModuleHandling;
    boolean hasAotLinkedClasses;
    boolean hasFullModuleGraph;
    long rwPtrmapStartPos;
    long roPtrmapStartPos;
    ArchiveMappedHeapHeader archiveMappedHeapHeader;
    ArchiveStreamedHeapHeader archiveStreamedHeapHeader;

    // TODO: these don't show up in the aot.map file. Confirm that their values are encoded,
    byte compilerType;
    int typeProfileLevel;
    int typeProfileArgsLimit;
    int typeProfileParmsLimit;
    long typeProfileWidth;
    long bciProfileWidth;
    boolean profileTraps;
    boolean typeProfileCasts;
    int specTrapLimitExtraEntries;

    public FileMapHeader(LittleEndianRandomAccessFile dis) throws IOException {
        coreRegionAlignment = dis.readLong();
        objAlignment = dis.readInt();

        dis.skipBytes(4);

        narrowOopBase = dis.readLong();
        narrowOopShift = dis.readInt();
        compactStrings = dis.readBoolean();
        compactHeaders = dis.readBoolean();
        dis.skipBytes(2);
        maxHeapSize = dis.readLong();
        narrowOopMode = dis.readInt();
        objectStreamingMode = dis.readBoolean();
        compressedOops = dis.readBoolean();
        compressedClassPointers = dis.readBoolean();
        dis.skipBytes(1);
        narrowKlassPointerBits = dis.readInt();
        narrowKlassShift = dis.readInt();
        clonedVtablesOffset = dis.readLong();
        earlySerializedDataOffset = dis.readLong();
        serializedDataOffset = dis.readLong();
        // jvm_ident is 256 bytes (null-terminated string)
        byte[] jvmIdentBytes = new byte[256];
        dis.read(jvmIdentBytes);
        jvmIdent = new String(jvmIdentBytes, StandardCharsets.UTF_8);
        classLocationConfigOffset = dis.readLong();
        verifyLocal = dis.readBoolean();
        verifyRemote = dis.readBoolean();
        hasPlatformOrAppClasses = dis.readBoolean();
        dis.skipBytes(5);
        requestedBaseAddress = dis.readLong();
        mappedBaseAddress = dis.readLong();
        useOptimizedModuleHandling = dis.readBoolean();
        hasAotLinkedClasses = dis.readBoolean();
        hasFullModuleGraph = dis.readBoolean();
        dis.skipBytes(5);
        rwPtrmapStartPos = dis.readLong();
        roPtrmapStartPos = dis.readLong();
        
        long ptrmapStartPos = dis.readLong();
        long oopmapStartPos = dis.readLong();
        long baseOffset = dis.readLong();
        long count = dis.readLong();
        int rootsCount = dis.readInt();
        dis.skipBytes(4);
        long maxSizeInBytes = dis.readLong();
        int maxSizeInElems = dis.readInt();
        dis.skipBytes(4);
        archiveMappedHeapHeader = new ArchiveMappedHeapHeader(ptrmapStartPos, oopmapStartPos, new HeapRootSegments(baseOffset, count, rootsCount, maxSizeInBytes, maxSizeInElems));

        long forwardingOffset = dis.readLong();
        long rootsOffset = dis.readLong();
        long rootHighestObjectIndexTableOffset = dis.readLong();
        long numRoots = dis.readLong();
        long numArchivedObjects = dis.readLong();
        archiveStreamedHeapHeader = new ArchiveStreamedHeapHeader(forwardingOffset, rootsOffset, rootHighestObjectIndexTableOffset, numRoots, numArchivedObjects);

        compilerType = (byte) (dis.readBoolean() ? 1 : 0);
        typeProfileLevel = dis.readInt();
        dis.skipBytes(3);
        typeProfileArgsLimit = dis.readInt();
        typeProfileParmsLimit = dis.readInt();
        typeProfileWidth = dis.readLong();
        bciProfileWidth = dis.readLong();
        profileTraps = dis.readBoolean();
        typeProfileCasts = dis.readBoolean();
        specTrapLimitExtraEntries = dis.readInt();
        dis.skipBytes(2);
    }

}
