package io.github.chains_project.aotp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.google.common.io.LittleEndianDataInputStream;

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
    // TODO: accomodate more fields

    public FileMapHeader(LittleEndianDataInputStream dis) throws IOException {
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
    }

}
